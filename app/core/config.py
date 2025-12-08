# core/config.py
# ------------------------------------------
# KickSafe-Ai 프로젝트 공통 설정 모듈
# - 환경변수(.env) 로드
# - YOLO 모델 경로, API Prefix, 위험도 상수 등
# ------------------------------------------

import os
from functools import lru_cache

from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()


class Settings:

    # 전체 FastAPI 서비스에서 공통으로 사용할 설정 값 모음


    # 프로젝트 정보
    PROJECT_NAME: str = "KickSafe AI Service"
    API_V1_PREFIX: str = "/api/v1"

    # YOLOv8-OBB 모델 경로
    # - Roboflow에서 받은 OBB 데이터셋으로 학습한 best.pt 위치를 지정
    # - 예: models/kicksafe_yolov8n_obb_best.pt
    YOLO_MODEL_PATH: str = os.getenv(
        "YOLO_MODEL_PATH",
        "models/kicksafe_yolov8_obb_best.pt",
    )

    # 위험 점수 최대값
    RISK_SCORE_MAX: int = 100

    # 향후 필요 시(예: 로그 레벨, GPU 사용 여부 등) 추가 가능
    USE_GPU: bool = os.getenv("USE_GPU", "false").lower() == "true"


@lru_cache
def get_settings() -> Settings:

   # Settings 인스턴스를 애플리케이션 전체에서 1개만 사용하도록 캐시

    return Settings()
