# app/services/roboflow_service.py
# ------------------------------------------
# [긴급 수정] 라벨 명칭 불일치 수정 (electric_scooter 추가)
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

# -----------------------------
# 1. 모델 로드
# -----------------------------
try:
    logger.info(f"Loading YOLOv8-OBB model from: {settings.YOLO_MODEL_PATH}")
    _yolo_model = YOLO(settings.YOLO_MODEL_PATH)
except Exception as e:
    logger.error(f"YOLOv8-OBB 모델 로드 실패: {e}")
    _yolo_model = None

try:
    logger.info("Loading YOLOv8 person model (COCO): yolov8n.pt")
    _person_model = YOLO("yolov8n.pt")
except Exception as e:
    logger.warning(f"Person 모델 로드 실패: {e}")
    _person_model = None


def _ensure_model_loaded():
    if _yolo_model is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="AI 모델 로드 실패",
        )


def _open_image(image_bytes: bytes) -> Image.Image:
    try:
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as e:
        raise HTTPException(status_code=400, detail="이미지 로드 실패")


def _parse_obb_result(result) -> List[OrientedBBox]:
    detections: List[OrientedBBox] = []
    names = result.names
    
    # 1. OBB (회전 박스) 결과 처리
    obb = getattr(result, "obb", None)
    if obb is not None and hasattr(obb, "xyxyxyxy"):
        xyxyxyxy = obb.xyxyxyxy.cpu().numpy()
        confs = obb.conf.cpu().numpy()
        clses = obb.cls.cpu().numpy().astype(int)

        for i in range(len(xyxyxyxy)):
            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            # [수정] electric_scooter (언더바) 추가!
            target_labels = [
                'rider', 'human', 'person', 
                'electric-scooter', 'electric_scooter', 'scooter', 'kickboard', 'patin'
            ]

            if label in target_labels:
                if conf < 0.15: continue
            else:
                if conf < 0.05: continue 

            coords = np.array(xyxyxyxy[i]).reshape(-1).tolist()
            if len(coords) == 8:
                x1, y1, x2, y2, x3, y3, x4, y4 = coords
                detections.append(
                    OrientedBBox(label=label, confidence=conf, x1=x1, y1=y1, x2=x2, y2=y2, x3=x3, y3=y3, x4=x4, y4=y4)
                )
        return detections

    # 2. 일반 Box (수평 박스) 결과 처리
    boxes = getattr(result, "boxes", None)
    if boxes is not None and hasattr(boxes, "xyxy"):
        xyxy = boxes.xyxy.cpu().numpy()
        confs = boxes.conf.cpu().numpy()
        clses = boxes.cls.cpu().numpy().astype(int)
        for i in range(len(xyxy)):
            conf = float(confs[i])
            cls_idx = int(clses[i])
            label = names.get(cls_idx, str(cls_idx))

            target_labels = [
                'rider', 'human', 'person', 
                'electric-scooter', 'electric_scooter', 'scooter', 'kickboard'
            ]

            if label in target_labels:
                if conf < 0.15: continue
            else:
                if conf < 0.05: continue

            x1, y1, x2, y2 = xyxy[i].tolist()
            detections.append(
                OrientedBBox(label=label, confidence=conf, x1=x1, y1=y1, x2=x2, y2=y1, x3=x2, y3=y2, x4=x1, y4=y2)
            )

    return detections


def _risk_level_from_score(score: float) -> Tuple[str, str]:
    if score < 25: return "LOW", "안전"
    if score < 71: return "MEDIUM", "주의"
    if score < 90: return "HIGH", "위험"
    return "CRITICAL", "매우 위험"


# =========================================================================
# [핵심] 킥보드 vs 사람 비율 계산 로직
# =========================================================================
def _calculate_risk(detections: List[OrientedBBox], image: Image.Image) -> RiskDetail:
    if not detections:
        return RiskDetail(risk_score=0.0, risk_level="LOW", risk_level_kor="안전", risk_factors=[])

    score = 0.0
    risk_factors: List[str] = []
    
    # 1. 객체 수 세기
    rider_count = 0
    scooter_count = 0
    has_no_helmet = False 

    # 재학습 라벨 감지 여부
    has_two_rider_label = False
    has_three_rider_label = False

    for det in detections:
        label = det.label.lower() 

        # A. 사람(rider) 카운트
        if label in ['rider', 'human', 'person']:
            rider_count += 1
            
        # B. 킥보드(scooter) 카운트 (언더바 포함!)
        elif label in ['electric-scooter', 'electric_scooter', 'scooter', 'kickboard', 'patin']:
            scooter_count += 1

        # C. 헬멧 미착용 체크
        elif 'no_helmet' in label:
            if not has_no_helmet:
                score += 20.0
                risk_factors.append("헬멧 미착용 감지 (+20점)")
                has_no_helmet = True
        
        # D. 재학습된 라벨
        elif label in ['two', 'two_riders', '2']:
             if not has_two_rider_label:
                has_two_rider_label = True
        elif label in ['three', 'three_riders', '3', 'multi']:
             if not has_three_rider_label:
                has_three_rider_label = True

    # -----------------------------------------------------------
    # [논리 판단] 킥보드 수와 사람 수 비교
    # -----------------------------------------------------------
    if scooter_count > 0:

        # 1. 킥보드 1대에 사람 2명 -> 2인 탑승! (이게 아까 0점 나왔던 케이스 해결)
        if scooter_count == 1 and rider_count == 2:
            # 라벨이 없어도 숫자로 판단
            if not has_two_rider_label:
                score += 50.0
                risk_factors.append("2인 탑승 의심 (킥보드 1대 / 사람 2명) (+50점)")

        # 2. 킥보드 1대에 사람 3명 이상 -> 다인 탑승!
        elif scooter_count == 1 and rider_count >= 3:
            if not has_three_rider_label:
                score += 80.0
                risk_factors.append(f"3인 이상 탑승 의심 ({rider_count}명) (+80점)")

        # 3. 킥보드 수와 사람 수가 같음 -> 1인 1기기 (안전)
        elif scooter_count == rider_count:
             risk_factors.append(f"1인 1기기 탑승 추정 (기기 {scooter_count}대 / 사람 {rider_count}명)")

        # 4. 사람이 더 많음 (일반화)
        elif rider_count > scooter_count:
             # 라벨 점수 받은 적 없으면 점수 부여
             if score < 50:
                 extra = (rider_count - scooter_count) * 30.0
                 score += extra
                 risk_factors.append(f"초과 인원 감지 (+{extra}점)")
    
    # -----------------------------------------------------------
    # 라벨 점수 합산 (중복 방지 로직)
    # -----------------------------------------------------------
    if has_three_rider_label and score < 80:
        score += 80.0
        risk_factors.append("3인 이상 탑승 라벨 감지 (+80점)")
    elif has_two_rider_label and score < 50:
        score += 50.0
        risk_factors.append("2인 탑승 라벨 감지 (+50점)")


    # 점수 범위 제한 (0 ~ 100)
    score = float(np.clip(score, 0.0, float(settings.RISK_SCORE_MAX)))
    level, level_kor = _risk_level_from_score(score)

    return RiskDetail(
        risk_score=score,
        risk_level=level,
        risk_level_kor=level_kor,
        risk_factors=risk_factors,
    )


# =========================================================================
# 이미지 분석 실행
# =========================================================================
def analyze_image(image_bytes: bytes) -> DetectionResult:
    _ensure_model_loaded()
    image = _open_image(image_bytes)

    detections = []

    try:
        results_yolo = _yolo_model.predict(
            source=image, 
            verbose=False, 
            conf=0.05, 
            imgsz=1024, 
            agnostic_nms=True 
        )
        if results_yolo:
            detections.extend(_parse_obb_result(results_yolo[0]))

        if _person_model:
            results_person = _person_model.predict(
                source=image, 
                verbose=False, 
                conf=0.10,   
                imgsz=960, 
                classes=[0], 
                agnostic_nms=True
            )
            if results_person:
                person_dets = _parse_obb_result(results_person[0])
                for d in person_dets:
                    d.label = 'rider'
                detections.extend(person_dets)

    except Exception as e:
        logger.error(f"AI 추론 오류: {e}")
        if not detections: detections = []

    risk = _calculate_risk(detections, image)

    print(f"\n[🔍 AI 결과] 객체: {[d.label for d in detections]}")
    print(f"[🔍 AI 결과] 점수: {risk.risk_score}점\n")

    return DetectionResult(detections=detections, risk=risk)
