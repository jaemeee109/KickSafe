# services/roboflow_service.py
# ------------------------------------------
# YOLOv8-OBB 모델을 사용한 객체 탐지 + KickSafe 위험도 스코어링 로직
# - (추가) COCO 사전학습 모델로 person 카운트(동승자 수) 추정
# ------------------------------------------

import io
import logging
from typing import List, Tuple, Optional

import numpy as np
from PIL import Image
from fastapi import HTTPException
from starlette import status
from ultralytics import YOLO

from app.core.config import get_settings
from app.schemas import OrientedBBox, RiskDetail, DetectionResult

logger = logging.getLogger(__name__)
settings = get_settings()

# -----------------------------
# 모델 로드
# -----------------------------
try:
    logger.info(f"Loading YOLOv8-OBB model from: {settings.YOLO_MODEL_PATH}")
    _yolo_model = YOLO(settings.YOLO_MODEL_PATH)

    print("[MODEL][PRINT] YOLO_MODEL_PATH =", settings.YOLO_MODEL_PATH)
    print("[MODEL][PRINT] names =", _yolo_model.names)

    logger.warning(f"[MODEL] names={_yolo_model.names}")  # warning은 보통 콘솔에 잘 뜸
except Exception as e:
    logger.error(f"YOLOv8-OBB 모델 로드 실패: {e}")
    _yolo_model = None

# (추가) person 검출용 COCO 모델 (동승자 수 추정)
# - ultralytics가 없으면 requirements에 이미 있으므로 OK
# - 첫 실행 시 yolov8n.pt 자동 다운로드될 수 있음
try:
    logger.info("Loading YOLOv8 person model (COCO): yolov8n.pt")
    _person_model = YOLO("yolov8n.pt")
except Exception as e:
    logger.warning(f"Person 모델 로드 실패(동승자 추정 비활성): {e}")
    _person_model = None


def _ensure_model_loaded():
    if _yolo_model is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="YOLOv8-OBB 모델이 로드되지 않았습니다. "
                   "YOLO_MODEL_PATH 설정 또는 학습/모델 경로를 확인해주세요.",
        )


def _open_image(image_bytes: bytes) -> Image.Image:
    try:
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as e:
        logger.error(f"이미지 디코딩 실패: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효한 이미지 형식이 아닙니다.",
        )


def _parse_obb_result(result) -> List[OrientedBBox]:
    detections: List[OrientedBBox] = []
    names = result.names

    obb = getattr(result, "obb", None)
    if obb is not None and hasattr(obb, "xyxyxyxy"):
        xyxyxyxy = obb.xyxyxyxy.cpu().numpy()
        confs = obb.conf.cpu().numpy()
        clses = obb.cls.cpu().numpy().astype(int)

        for i in range(len(xyxyxyxy)):
            coords_flat = np.array(xyxyxyxy[i]).reshape(-1).tolist()

            if len(coords_flat) == 8:
                x1, y1, x2, y2, x3, y3, x4, y4 = coords_flat
            elif len(coords_flat) == 4:
                x1, y1, x2, y2 = coords_flat
                x3, y3 = x2, y2
                x4, y4 = x1, y2
            else:
                logger.warning(
                    f"예상치 못한 OBB 좌표 길이입니다. len={len(coords_flat)}, coords={coords_flat}"
                )
                continue

            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            detections.append(
                OrientedBBox(
                    label=label,
                    confidence=conf,
                    x1=x1, y1=y1,
                    x2=x2, y2=y2,
                    x3=x3, y3=y3,
                    x4=x4, y4=y4,
                )
            )
        return detections

    boxes = getattr(result, "boxes", None)
    if boxes is not None and hasattr(boxes, "xyxy"):
        xyxy = boxes.xyxy.cpu().numpy()
        confs = boxes.conf.cpu().numpy()
        clses = boxes.cls.cpu().numpy().astype(int)

        for i in range(len(xyxy)):
            x1, y1, x2, y2 = xyxy[i].tolist()
            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            detections.append(
                OrientedBBox(
                    label=label,
                    confidence=conf,
                    x1=x1, y1=y1,
                    x2=x2, y2=y1,
                    x3=x2, y3=y2,
                    x4=x1, y4=y2,
                )
            )

    return detections


# ------------------------------------------
# 위험 점수 로직
# ------------------------------------------

RISK_WEIGHTS = {
    "Patinetes electricos - v4 2023-07-28 9-05pm": 20.0,
    "electric-scooter": 10.0,

    # 보호장비 관련(모델이 실제로 출력해야 효과 있음)
    "no_helmet": 30.0,
    "helmet": 5.0,

    # 탑승 인원 관련(모델이 출력하면 사용)
    "two_riders": 25.0,
    "three_riders": 35.0,

    # 주행 위치/환경
    "sidewalk_riding": 20.0,
    "road_riding": 10.0,
    "bike_lane_riding": 5.0,

    # 교통법규 위반
    "red_light_violation": 30.0,
    "wrong_way": 25.0,
    "crosswalk_violation": 20.0,

    # 주변 객체(충돌 위험)
    "car_near": 15.0,
    "pedestrian_near": 15.0,
    "scooter_near": 10.0,

    # 시간/가시성
    "night": 10.0,
    "poor_visibility": 15.0,

    # (추가) rider가 기본 출력일 때 최소 가중치 부여
    "rider": 10.0,  # 기본 주행 위험(현재 모델이 rider만 뱉는 상황 대응)
}

LABEL_ALIAS_MAP = {
    # 필요 시 별칭 추가
}

DEFAULT_RISK_WEIGHT: float = 5.0

# (추가) 동승자 추정 가중치
PASSENGER_EXTRA_WEIGHT = 25.0   # (rider_count-1) 1명당 추가 위험
RIDER_COUNT_CAP = 6             # 너무 과대검출될 경우 상한


def _risk_level_from_score(score: float) -> Tuple[str, str]:
    if score < 25:
        return "LOW", "안전"
    if score < 50:
        return "MEDIUM", "주의"
    if score < 75:
        return "HIGH", "위험"
    return "CRITICAL", "매우 위험"


def _obb_to_aabb(det: OrientedBBox) -> Tuple[float, float, float, float]:
    xs = [det.x1, det.x2, det.x3, det.x4]
    ys = [det.y1, det.y2, det.y3, det.y4]
    return float(min(xs)), float(min(ys)), float(max(xs)), float(max(ys))


def _iou(a: Tuple[float, float, float, float], b: Tuple[float, float, float, float]) -> float:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b

    ix1 = max(ax1, bx1)
    iy1 = max(ay1, by1)
    ix2 = min(ax2, bx2)
    iy2 = min(ay2, by2)

    iw = max(0.0, ix2 - ix1)
    ih = max(0.0, iy2 - iy1)
    inter = iw * ih
    if inter <= 0:
        return 0.0

    area_a = max(0.0, (ax2 - ax1)) * max(0.0, (ay2 - ay1))
    area_b = max(0.0, (bx2 - bx1)) * max(0.0, (by2 - by1))
    denom = area_a + area_b - inter
    return float(inter / denom) if denom > 0 else 0.0


def _detect_person_boxes(image: Image.Image, conf_thres: float = 0.10) -> List[Tuple[float, float, float, float]]:
    """
    COCO person(클래스 0) 박스 리스트 반환 (axis-aligned bbox)
    """
    if _person_model is None:
        return []

    try:
        r = _person_model.predict(
            source=image,
            verbose=False,
            conf=0.10,
            imgsz=960
        )[0]

    except Exception as e:
        logger.warning(f"Person 추론 실패(동승자 추정 스킵): {e}")
        return []

    boxes = getattr(r, "boxes", None)
    if boxes is None or not hasattr(boxes, "xyxy"):
        return []

    xyxy = boxes.xyxy.cpu().numpy()
    confs = boxes.conf.cpu().numpy()
    clses = boxes.cls.cpu().numpy().astype(int)

    person_boxes: List[Tuple[float, float, float, float]] = []
    for i in range(len(xyxy)):
        if clses[i] != 0:  # COCO person class id = 0
            continue
        if float(confs[i]) < conf_thres:
            continue
        x1, y1, x2, y2 = xyxy[i].tolist()
        person_boxes.append((float(x1), float(y1), float(x2), float(y2)))

    return person_boxes


def _estimate_rider_count(detections: List[OrientedBBox], image: Image.Image) -> Optional[int]:
    rider_dets = [d for d in detections if d.label == "rider"]
    if not rider_dets:
        return None

    # rider 영역
    rider_aabb = _obb_to_aabb(rider_dets[0])

    persons = _detect_person_boxes(image, conf_thres=0.20)
    if not persons:
        return None

    rx1, ry1, rx2, ry2 = rider_aabb

    cnt = 0
    for px1, py1, px2, py2 in persons:
        # 중심점이 rider 영역 안에 있으면 탑승자로 간주
        cx = (px1 + px2) / 2
        cy = (py1 + py2) / 2
        if rx1 <= cx <= rx2 and ry1 <= cy <= ry2:
            cnt += 1

    return int(np.clip(cnt, 1, RIDER_COUNT_CAP))



def _calculate_risk(detections: List[OrientedBBox], image: Image.Image) -> RiskDetail:
    if not detections:
        score = 0.0
        level, level_kor = _risk_level_from_score(score)
        return RiskDetail(
            risk_score=score,
            risk_level=level,
            risk_level_kor=level_kor,
            risk_factors=[],
        )

    score = 0.0
    risk_factors: List[str] = []

    # 1) 기존: 라벨 기반 가중치 합산
    for det in detections:
        raw_label = det.label
        normalized_label = LABEL_ALIAS_MAP.get(raw_label, raw_label)
        weight = RISK_WEIGHTS.get(normalized_label, DEFAULT_RISK_WEIGHT)
        contrib = weight * float(det.confidence)

        if contrib <= 0:
            continue

        score += contrib

        if normalized_label == raw_label:
            factor_label = raw_label
        else:
            factor_label = f"{raw_label} -> {normalized_label}"

        risk_factors.append(
            f"{factor_label} (conf={det.confidence:.2f}) => +{contrib:.1f}점"
        )

    # 2) (추가) 동승자 수 추정 → 추가 가중치
    rider_count = _estimate_rider_count(detections, image)
    #  디버그(응답에 강제로 노출)
    persons_dbg = _detect_person_boxes(image, conf_thres=0.10)
    risk_factors.append(f"[DBG] persons={len(persons_dbg)}, rider_count={rider_count}")
    if rider_count is not None and rider_count >= 2:
        extra = (rider_count - 1) * PASSENGER_EXTRA_WEIGHT
        score += float(extra)
        risk_factors.append(f"동승자 추정 {rider_count}명 => +{extra:.1f}점")

    # 2-1) 헬멧 탐지 실패 + 다인 탑승 → 무헬멧 위험 가중(규칙 기반 보완)
    has_helmet = any(d.label == "helmet" for d in detections)
    has_no_helmet = any(d.label == "no_helmet" for d in detections)

    # no_helmet이 직접 검출되면 그건 이미 위에서 RISK_WEIGHTS로 점수 반영됨
    # 여기서는 "다인 탑승인데 helmet이 하나도 안 잡힌 경우"만 추가 패널티
    if rider_count is not None and rider_count >= 2 and (not has_helmet) and (not has_no_helmet):
        score += 40.0
        risk_factors.append("헬멧 미탐지 + 다인 탑승 => +40점")

    # 3) 클램핑 + 등급
    score = float(np.clip(score, 0.0, float(settings.RISK_SCORE_MAX)))
    level, level_kor = _risk_level_from_score(score)

    return RiskDetail(
        risk_score=score,
        risk_level=level,
        risk_level_kor=level_kor,
        risk_factors=risk_factors,
    )



def analyze_image(image_bytes: bytes) -> DetectionResult:
    _ensure_model_loaded()

    image = _open_image(image_bytes)

    try:
        results = _yolo_model.predict(
            source=image,
            verbose=False,
            conf=0.10,  # 헬멧 같은 소형 객체 살리기
            imgsz=1024  # 해상도 올려 소형 객체 탐지 강화
        )
    except Exception as e:
        logger.error(f"YOLO 추론 중 오류 발생: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="YOLOv8-OBB 추론 중 오류가 발생했습니다.",
        )

    detections = _parse_obb_result(results[0]) if results else []

    # (중요) image를 _calculate_risk에 전달
    risk = _calculate_risk(detections, image)

    return DetectionResult(detections=detections, risk=risk)
