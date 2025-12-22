# app/services/roboflow_service.py
# ------------------------------------------
# [긴급 수정] 라벨 명칭 불일치 수정 (electric_scooter 추가)
# ------------------------------------------

import io # 바이트 데이터를 파일처럼 다루기 위한 io 모듈
import logging # 로그 출력을 위한 모듈
from typing import List, Tuple # List, Tuple 사용

import numpy as np # 수치 연산 사용
from PIL import Image # 이미지 로드/변환(Pillow)
from fastapi import HTTPException # FastAPI에서 에러 응답(예외) 만들기
from starlette import status # HTTP 상태코드
from ultralytics import YOLO # YOLO 모델

from app.core.config import get_settings  # 환경설정
from app.schemas import OrientedBBox, RiskDetail, DetectionResult  # 응답/데이터 스키마

logger = logging.getLogger(__name__) # 이 파일 전용 로거 생성
settings = get_settings() # .env/설정값을 읽어서 settings에 저장

# -----------------------------
# 1. 모델 로드
# -----------------------------
try:  # OBB 모델 파일 경로 로그로 남김
    logger.info(f"Loading YOLOv8-OBB model from: {settings.YOLO_MODEL_PATH}")
      # 설정된 경로에서 OBB 모델 로드
    _yolo_model = YOLO(settings.YOLO_MODEL_PATH)
except Exception as e:
    # 모델 로드 실패 시 에러 로그 남기고 None 처리
    logger.error(f"YOLOv8-OBB 모델 로드 실패: {e}")
    _yolo_model = None

try:
    # 사람(person) 탐지를 위해 기본 모델 로드
    logger.info("Loading YOLOv8 person model (COCO): yolov8n.pt")
    _person_model = YOLO("yolov8n.pt")
except Exception as e:
    # person 모델은 보조 모델임 warning 처리하고 None으로 두기
    logger.warning(f"Person 모델 로드 실패: {e}")
    _person_model = None


def _ensure_model_loaded():
    # 필수 모델(OBB)이 로드됐는지 확인, 아니라면 500 에러로 응답
    if _yolo_model is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, # 500 서버 오류
            detail="AI 모델 로드 실패", # 에러 상세 메시지
        )


# 업로드된 이미지 바이트를 PIL 이미지로 열고 RGB로 변환
def _open_image(image_bytes: bytes) -> Image.Image:
    try:
        # bytes -> BytesIO -> Image.open -> RGB 변환
        return Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as e:
        # 이미지 파싱 실패 시 400으로 응답
        raise HTTPException(status_code=400, detail="이미지 로드 실패")


def _parse_obb_result(result) -> List[OrientedBBox]:
    # YOLO 결과에서 OBB 또는 일반 박스를 읽어 OrientedBBox 리스트로 변환
    detections: List[OrientedBBox] = []
    names = result.names  # 클래스 인덱스 -> 라벨명 매핑(dict)

    
    # 1. OBB (회전 박스) 결과 처리
    obb = getattr(result, "obb", None) # result.obb가 있으면 가져오고 없으면 None
    if obb is not None and hasattr(obb, "xyxyxyxy"):  # OBB 좌표가 존재하는지 확인
        xyxyxyxy = obb.xyxyxyxy.cpu().numpy() # 좌표를 CPU numpy 배열로 변환
        confs = obb.conf.cpu().numpy()  # confidence 배열
        clses = obb.cls.cpu().numpy().astype(int)  # 클래스 인덱스 배열(int)

        for i in range(len(xyxyxyxy)): # 각 탐지 박스마다 반복
            conf = float(confs[i])  # 해당 박스 confidence
            cls_idx = int(clses[i]) # 해당 박스 클래스 인덱스
            label = names.get(cls_idx, str(cls_idx)) # 인덱스를 라벨명으로 변환(없으면 숫자)


            # 중요 라벨
            target_labels = [
                'rider', 'human', 'person', 
                'electric-scooter', 'electric_scooter', 'scooter', 'kickboard', 'patin'
            ]

            if label in target_labels: # 중요 라벨이면
                if conf < 0.15: continue # conf가 0.15 미만이면 버림
            else: # 그 외 라벨이면
                if conf < 0.05: continue # conf가 0.05 미만이면 버림

            coords = np.array(xyxyxyxy[i]).reshape(-1).tolist()  # 좌표를 1차원 리스트로 변환
            if len(coords) == 8: # 총 8개인지 확인
                x1, y1, x2, y2, x3, y3, x4, y4 = coords # 4개 꼭짓점 좌표 분해
                detections.append(
                    # OrientedBBox 스키마 형태로 저장
                    OrientedBBox(label=label, confidence=conf, x1=x1, y1=y1, x2=x2, y2=y2, x3=x3, y3=y3, x4=x4, y4=y4)
                )
        return detections # OBB 처리 결과 반환

    # 2. 일반 Box (수평 박스) 결과 처리 (=> OBB가 없을 때 대비)
    boxes = getattr(result, "boxes", None) # result.boxes가 있으면 가져옴
    if boxes is not None and hasattr(boxes, "xyxy"):  # 수평 박스 좌표 존재 확인
        xyxy = boxes.xyxy.cpu().numpy() # (x1,y1,x2,y2) 형태 좌표
        confs = boxes.conf.cpu().numpy()  # confidence
        clses = boxes.cls.cpu().numpy().astype(int) # 클래스 인덱스
        for i in range(len(xyxy)):  # 각 박스 반복
            conf = float(confs[i])  # confidence
            cls_idx = int(clses[i]) # 클래스 인덱스
            label = names.get(cls_idx, str(cls_idx)) # 라벨명

            target_labels = [
                'rider', 'human', 'person', 
                'electric-scooter', 'electric_scooter', 'scooter', 'kickboard'
            ]
            # 중요 라벨 목록(수평 박스 버전)
            if label in target_labels:
                if conf < 0.15: continue
            else:
                if conf < 0.05: continue

            # 수평 박스를 4개 꼭짓점(OBB 형태)로 변환해서 저장
            x1, y1, x2, y2 = xyxy[i].tolist()
            detections.append(
                OrientedBBox(label=label, confidence=conf, x1=x1, y1=y1, x2=x2, y2=y1, x3=x2, y3=y2, x4=x1, y4=y2)
            )

    return detections  # 박스 처리 결과 반환(없으면 빈 리스트)


# 위험 점수(score)를 위험 등급(영문/한글)으로 변환
def _risk_level_from_score(score: float) -> Tuple[str, str]:
    if score < 25: return "LOW", "안전" # 0~24
    if score < 71: return "MEDIUM", "주의" # 25~70
    if score < 90: return "HIGH", "위험" # 71~89
    return "CRITICAL", "매우 위험" # 90~100


# =========================================================================
# 킥보드 vs 사람 비율 계산 로직
# =========================================================================
def _calculate_risk(detections: List[OrientedBBox], image: Image.Image) -> RiskDetail:
    if not detections: # 탐지 결과가 아예 없으면 (점수0, 등급 LOW, '안전', 요인없음)
        return RiskDetail(risk_score=0.0, risk_level="LOW", risk_level_kor="안전", risk_factors=[])

    score = 0.0 # 위험 점수 누적 변수
    risk_factors: List[str] = [] # 위험 요인 메시지 리스트
    
    # 1. 객체 수 세기
    rider_count = 0 # 사람 수
    scooter_count = 0 # 킥보드 수
    has_no_helmet = False  # 헬멧 미착용 점수 중복 부여 방지용

    # 재학습 라벨 감지 여부 (2인/3인 탑승 라벨이 따로 있을 때)
    has_two_rider_label = False
    has_three_rider_label = False

    for det in detections: # 탐지 결과 하나씩 검사
        label = det.label.lower()  # 라벨을 소문자로 통일해서 비교


        # A. 사람(rider) 카운트
        if label in ['rider', 'human', 'person']:
            rider_count += 1
            
        # B. 킥보드(scooter) 카운트 (언더바 포함!)
        elif label in ['electric-scooter', 'electric_scooter', 'scooter', 'kickboard', 'patin']:
            scooter_count += 1

        # C. 헬멧 미착용 체크
        elif 'no_helmet' in label:
            if not has_no_helmet: # 이미 반영했으면 중복 점수 부여하지 않음
                score += 20.0
                risk_factors.append("헬멧 미착용 감지 (+20점)")
                has_no_helmet = True # 중복 방지 플래그 ON
        
        # D. 재학습된 라벨
        elif label in ['two', 'two_riders', '2']:
             if not has_two_rider_label:
                has_two_rider_label = True # 2인 라벨 감지
        elif label in ['three', 'three_riders', '3', 'multi']:
             if not has_three_rider_label:
                has_three_rider_label = True  # 3인 이상 라벨 감지

    # -----------------------------------------------------------
    # [논리 판단] 킥보드 수와 사람 수 비교
    # -----------------------------------------------------------
    if scooter_count > 0:  # 킥보드가 최소 1대라도 있을 때만 판단

        # 1. 킥보드 1대에 사람 2명 -> 2인 탑승
        if scooter_count == 1 and rider_count == 2:
            # 라벨이 없어도 숫자로 판단
            if not has_two_rider_label:
                score += 50.0 # 2인 탑승은 +50점
                risk_factors.append("2인 탑승 의심 (킥보드 1대 / 사람 2명) (+50점)")

        # 2. 킥보드 1대에 사람 3명 이상 -> 다인 탑승
        elif scooter_count == 1 and rider_count >= 3:
            if not has_three_rider_label:
                score += 80.0 # 3인 이상 탑승은 +80점
                risk_factors.append(f"3인 이상 탑승 의심 ({rider_count}명) (+80점)")

        # 3. 킥보드 수와 사람 수가 같음 -> 1인 1기기 (안전)
        elif scooter_count == rider_count:
             risk_factors.append(f"1인 1기기 탑승 추정 (기기 {scooter_count}대 / 사람 {rider_count}명)")

        # 4. 사람이 더 많음 (일반화)
        elif rider_count > scooter_count:
             # 라벨 점수 받은 적 없으면 점수 부여
             if score < 50:
                 extra = (rider_count - scooter_count) * 30.0 # 초과 인원당 30점
                 score += extra
                 risk_factors.append(f"초과 인원 감지 (+{extra}점)")
    
    # -----------------------------------------------------------
    # 라벨 점수 합산 (중복 방지 로직)
    # -----------------------------------------------------------
    # 3인 라벨이 감지됐는데 아직 80점 미만이면 80점으로 올려줌
    if has_three_rider_label and score < 80:
        score += 80.0
        risk_factors.append("3인 이상 탑승 라벨 감지 (+80점)")
  # 2인 라벨이 감지됐는데 아직 50점 미만이면 50점으로 올려줌
    elif has_two_rider_label and score < 50:
        score += 50.0
        risk_factors.append("2인 탑승 라벨 감지 (+50점)")


    # 점수 범위 제한 (0 ~ 100)
    score = float(np.clip(score, 0.0, float(settings.RISK_SCORE_MAX)))  # 0~100 범위로 자르기
    level, level_kor = _risk_level_from_score(score) # 점수 -> 등급 변환

    return RiskDetail(
        risk_score=score, # 최종 점수
        risk_level=level,# 영문 등급
        risk_level_kor=level_kor, # 한글 등급
        risk_factors=risk_factors, # 요인 리스트
    )


# =========================================================================
# 이미지 분석 실행
# =========================================================================
def analyze_image(image_bytes: bytes) -> DetectionResult:
    _ensure_model_loaded() # OBB 모델이 로드됐는지 확인(없으면 500 에러)
    image = _open_image(image_bytes) # bytes를 PIL 이미지로 열기

    detections = []  # 최종 탐지 결과를 담을 리스트

    try: # 1) OBB 모델로 추론
        results_yolo = _yolo_model.predict(
            source=image, # 입력이미지
            verbose=False, # 로그 출력 최소화
            conf=0.05, # 내부 thereshold (추론 단계 필터)
            imgsz=1024, # 입력 이미지 리사이즈 크기
            agnostic_nms=True # 클래스 무시 NMS(중복 제거 방식)
        )
        if results_yolo: # 결과가 있으면 첫 결과를 파싱해서 detections에 추가
            detections.extend(_parse_obb_result(results_yolo[0]))

        # 2) 사람 보조 모델이 있으면, 사람만 별도 추론해서 rider로 합침
        if _person_model:
            results_person = _person_model.predict(
                source=image, 
                verbose=False, 
                conf=0.10,   
                imgsz=960, 
                classes=[0],  # COCO에서 person 클래스(0)만 탐지
                agnostic_nms=True # NMS 방식
            )
            if results_person: # 결과가 있으면
                person_dets = _parse_obb_result(results_person[0])  # person 결과도 동일 파서로 변환
                for d in person_dets: # 사람 라벨을 rider로 통일
                    d.label = 'rider'
                detections.extend(person_dets) # 사람 탐지 결과를 전체 detections에 합침

    except Exception as e:  # 추론 중 에러가 나면 로그 기록
        logger.error(f"AI 추론 오류: {e}")  # detections가 없을 경우 빈 리스트 유지(안전 처리)
        if not detections: detections = []

    risk = _calculate_risk(detections, image)  # 탐지 결과로 위험도 계산

    print(f"\n[ AI 결과] 객체: {[d.label for d in detections]}")
    print(f"[ AI 결과] 점수: {risk.risk_score}점\n")

    return DetectionResult(detections=detections, risk=risk) # 최종 응답(탐지 + 위험도) 반환
