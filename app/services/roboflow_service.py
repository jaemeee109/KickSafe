# services/roboflow_service.py
# ------------------------------------------
# YOLOv8-OBB 모델을 사용한 객체 탐지 + KickSafe 위험도 스코어링 로직
# - Roboflow에서 받은 YOLOv8 OBB txt 데이터셋으로 학습한 모델(.pt)을 사용
# - 이미지 1장을 입력받아:
#     1) OBB 추론
#     2) 위험 점수(0~100) 및 위험 등급 계산
# ------------------------------------------

import io
import logging
from typing import List, Tuple

import numpy as np
from PIL import Image
from fastapi import HTTPException
from starlette import status
from ultralytics import YOLO

from app.core.config import get_settings
from app.schemas import OrientedBBox, RiskDetail, DetectionResult

logger = logging.getLogger(__name__)
settings = get_settings()

# YOLOv8-OBB 모델 로드 (애플리케이션 시작 시 1번만)
try:
    logger.info(f"Loading YOLOv8-OBB model from: {settings.YOLO_MODEL_PATH}")
    _yolo_model = YOLO(settings.YOLO_MODEL_PATH)
except Exception as e:  # 실제 런타임까지는 모델이 없을 수 있으므로 예외 처리
    logger.error(f"YOLOv8-OBB 모델 로드 실패: {e}")
    _yolo_model = None


def _ensure_model_loaded():
    """
    모델이 정상적으로 로드되지 않은 경우, API에서 500 오류를 발생시키기 위한 헬퍼.
    """
    if _yolo_model is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="YOLOv8-OBB 모델이 로드되지 않았습니다. "
                   "YOLO_MODEL_PATH 설정 또는 학습/모델 경로를 확인해주세요.",
        )


def _open_image(image_bytes: bytes) -> Image.Image:
    """
    업로드된 이미지 바이트를 PIL.Image로 변환
    """
    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        return image
    except Exception as e:
        logger.error(f"이미지 디코딩 실패: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="유효한 이미지 형식이 아닙니다.",
        )


def _parse_obb_result(result) -> List[OrientedBBox]:
    """
    YOLOv8-OBB 결과를 OrientedBBox 리스트로 변환
    - OBB 정보가 있으면 OBB 기준
    - 없으면 일반 bbox(boxes) 기준으로 4꼭짓점 근사
    """
    detections: List[OrientedBBox] = []

    # names: 클래스 인덱스 -> 클래스 이름
    names = result.names

    obb = getattr(result, "obb", None)
    if obb is not None and hasattr(obb, "xyxyxyxy"):
        # xyxyxyxy:
        #  - (N, 8)  형태: [x1, y1, x2, y2, x3, y3, x4, y4]
        #  - (N, 4,2) 형태: [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
        xyxyxyxy = obb.xyxyxyxy.cpu().numpy()
        confs = obb.conf.cpu().numpy()
        clses = obb.cls.cpu().numpy().astype(int)

        for i in range(len(xyxyxyxy)):
            # 좌표를 1차원 리스트(길이 8 또는 4)로 평탄화
            coords_flat = np.array(xyxyxyxy[i]).reshape(-1).tolist()

            # 8개 좌표가 온 경우: OBB 4꼭짓점으로 사용
            if len(coords_flat) == 8:
                x1, y1, x2, y2, x3, y3, x4, y4 = coords_flat

            # 4개 좌표만 온 경우: (x1, y1, x2, y2)를 직사각형 OBB로 확장
            elif len(coords_flat) == 4:
                x1, y1, x2, y2 = coords_flat
                x3, y3 = x2, y2
                x4, y4 = x1, y2

            # 그 외 길이는 예상하지 못한 형식이므로 스킵
            else:
                logger.warning(
                    f"예상치 못한 OBB 좌표 길이입니다. "
                    f"len={len(coords_flat)}, coords={coords_flat}"
                )
                continue

            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            detections.append(
                OrientedBBox(
                    label=label,
                    confidence=conf,
                    x1=x1,
                    y1=y1,
                    x2=x2,
                    y2=y2,
                    x3=x3,
                    y3=y3,
                    x4=x4,
                    y4=y4,
                )
            )
        return detections

    # OBB 정보가 없다면 일반 bbox로 근사 (직사각형)
    boxes = getattr(result, "boxes", None)
    if boxes is not None and hasattr(boxes, "xyxy"):
        xyxy = boxes.xyxy.cpu().numpy()  # (N, 4) [x1, y1, x2, y2]
        confs = boxes.conf.cpu().numpy()
        clses = boxes.cls.cpu().numpy().astype(int)

        for i in range(len(xyxy)):
            x1, y1, x2, y2 = xyxy[i].tolist()
            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            # 축 정렬 bbox를 OBB 형태로 복제 (회전 없는 사각형으로 취급)
            detections.append(
                OrientedBBox(
                    label=label,
                    confidence=conf,
                    x1=x1,
                    y1=y1,
                    x2=x2,
                    y2=y1,
                    x3=x2,
                    y3=y2,
                    x4=x1,
                    y4=y2,
                )
            )

    return detections

# ------------------------------------------
# KickSafe 전용 위험 점수 / 위험 등급 로직
# ------------------------------------------

# ⚠️ 중요:
# 아래 RISK_WEIGHTS dict의 key는 Roboflow 데이터셋에서 설정한
# 클래스 이름과 동일해야 합니다. (필요시 자유롭게 수정 가능)
RISK_WEIGHTS = {
    # === Roboflow data.yaml 의 실제 클래스 이름 ===
    # names:
    #   0: Patinetes electricos - v4 2023-07-28 9-05pm
    #   1: electric-scooter
    #
    # → 둘 다 "전동킥보드 주행" 상황이므로
    #   기본 위험도 10~20점 정도로 설정 (원하시면 수치는 조정 가능)
    "Patinetes electricos - v4 2023-07-28 9-05pm": 20.0,
    "electric-scooter": 10.0,

    # === 향후 더 세분화된 데이터셋을 쓸 때를 위한 공통 카테고리(지금은 안 써도 OK) ===

    # 보호장비 관련
    "no_helmet": 30.0,         # 헬멧 미착용
    "helmet": 5.0,             # 헬멧 착용 (위험도 낮음)

    # 탑승 인원 관련
    "two_riders": 25.0,        # 2인 이상 탑승
    "three_riders": 35.0,      # 3인 이상 탑승 (있다면)

    # 주행 위치/환경
    "sidewalk_riding": 20.0,   # 인도 주행
    "road_riding": 10.0,       # 차도 주행(기본 위험)
    "bike_lane_riding": 5.0,   # 자전거 도로 (상대적으로 안전)

    # 교통법규 위반
    "red_light_violation": 30.0,   # 신호위반
    "wrong_way": 25.0,             # 역주행
    "crosswalk_violation": 20.0,   # 횡단보도 위 주행

    # 주변 객체(충돌 위험)
    "car_near": 15.0,          # 차량 근접
    "pedestrian_near": 15.0,   # 보행자 근접
    "scooter_near": 10.0,      # 다른 킥보드 근접

    # 시간/가시성
    "night": 10.0,             # 야간
    "poor_visibility": 15.0,   # 우천/안개 등 시야 불량
}

# YOLO가 내보내는 "원래 라벨명"을 KickSafe 위험 카테고리로 매핑하기 위한 별칭 맵
# - 왼쪽: Roboflow / YOLO 결과에서 나오는 label
# - 오른쪽: 위 RISK_WEIGHTS 딕셔너리의 key
LABEL_ALIAS_MAP = {
    # 현재 data.yaml 기준으로는 YOLO 라벨이 이미
    # RISK_WEIGHTS 의 key 로 들어가 있으므로 꼭 넣지 않아도 됩니다.
    #
    # 필요하면 이런 식으로 별칭을 추가해서 공통 카테고리로 합칠 수 있습니다.
    # "Patinetes electricos - v4 2023-07-28 9-05pm": "electric-scooter",
}

# RISK_WEIGHTS / LABEL_ALIAS_MAP 어디에도 없는 라벨에 사용할 기본 가중치
DEFAULT_RISK_WEIGHT: float = 5.0


def _risk_level_from_score(score: float) -> Tuple[str, str]:
    """
    위험 점수(0~100)를 KickSafe에서 사용할 등급으로 변환

    - 0 ~ 24  : LOW / 안전
    - 25 ~ 49 : MEDIUM / 주의
    - 50 ~ 74 : HIGH / 위험
    - 75 ~ 100: CRITICAL / 매우 위험
    """
    if score < 25:
        return "LOW", "안전"
    if score < 50:
        return "MEDIUM", "주의"
    if score < 75:
        return "HIGH", "위험"
    return "CRITICAL", "매우 위험"


def _calculate_risk(detections: List[OrientedBBox]) -> RiskDetail:
    """
    YOLO 검출 결과 리스트를 기반으로 KickSafe 위험 점수/등급 계산

    - 각 객체(label)의 위험 가중치 * 신뢰도(confidence)를 모두 합산
    - YOLO 원래 라벨명(det.label)을 LABEL_ALIAS_MAP을 통해
      KickSafe 위험 카테고리(RISK_WEIGHTS key)로 매핑하여 사용
    - 최대값은 settings.RISK_SCORE_MAX(기본 100)으로 클램핑
    """
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

    for det in detections:
        # 1) YOLO가 뱉은 원본 라벨
        raw_label = det.label

        # 2) KickSafe 위험 카테고리로 매핑 (없으면 원본 라벨 그대로 사용)
        normalized_label = LABEL_ALIAS_MAP.get(raw_label, raw_label)

        # 3) 위험 가중치 조회 (없으면 DEFAULT_RISK_WEIGHT 사용)
        weight = RISK_WEIGHTS.get(normalized_label, DEFAULT_RISK_WEIGHT)

        # 4) 신뢰도와 곱해서 점수 기여도 계산
        contrib = weight * float(det.confidence)

        if contrib <= 0:
            continue

        score += contrib

        # risk_factors에는 원본 라벨과 실제 사용된 카테고리 이름을 함께 남김
        if normalized_label == raw_label:
            factor_label = raw_label
        else:
            factor_label = f"{raw_label} -> {normalized_label}"

        risk_factors.append(
            f"{factor_label} (conf={det.confidence:.2f}) => +{contrib:.1f}점"
        )

    # 0 ~ RISK_SCORE_MAX (기본 0~100) 범위로 클램핑
    score = float(np.clip(score, 0.0, float(settings.RISK_SCORE_MAX)))
    level, level_kor = _risk_level_from_score(score)

    return RiskDetail(
        risk_score=score,
        risk_level=level,
        risk_level_kor=level_kor,
        risk_factors=risk_factors,
    )


# ------------------------------------------
# 외부에서 호출할 메인 함수
# ------------------------------------------

def analyze_image(image_bytes: bytes) -> DetectionResult:
    """
    FastAPI 엔드포인트에서 호출할 핵심 함수
    1) 이미지 디코딩
    2) YOLOv8-OBB 추론
    3) OBB 파싱
    4) 위험도 계산
    5) DetectionResult 반환
    """
    _ensure_model_loaded()

    image = _open_image(image_bytes)

    # YOLOv8-OBB 추론 수행
    # - task="obb"는 OBB 모델일 때 자동으로 판단되지만 명시적으로 사용 가능
    try:
        results = _yolo_model.predict(source=image, verbose=False)
    except Exception as e:
        logger.error(f"YOLO 추론 중 오류 발생: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="YOLOv8-OBB 추론 중 오류가 발생했습니다.",
        )

    if not results:
        detections: List[OrientedBBox] = []
    else:
        detections = _parse_obb_result(results[0])

    risk = _calculate_risk(detections)

    return DetectionResult(
        detections=detections,
        risk=risk,
    )
