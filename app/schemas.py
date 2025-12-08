# schemas.py
# ------------------------------------------
# FastAPI 요청/응답 Body 정의 (Pydantic 모델)
# - YOLOv8 OBB 추론 결과
# - KickSafe 위험도 점수/등급 정보
# ------------------------------------------

from typing import List

from pydantic import BaseModel, Field


class OrientedBBox(BaseModel):
    """
    YOLOv8-OBB 결과 1개(객체 1개)에 대한 정보
    - label: 클래스 이름 (예: "no_helmet", "two_riders")
    - confidence: 검출 신뢰도 (0.0 ~ 1.0)
    - x1~x4, y1~y4: 이미지 내 꼭짓점 좌표 (픽셀 단위, 좌상단 원점 기준)
    """

    label: str = Field(..., description="감지된 객체 클래스 이름")
    confidence: float = Field(..., ge=0.0, le=1.0, description="신뢰도 (0~1 사이)")

    x1: float = Field(..., description="꼭짓점1 X 좌표")
    y1: float = Field(..., description="꼭짓점1 Y 좌표")
    x2: float = Field(..., description="꼭짓점2 X 좌표")
    y2: float = Field(..., description="꼭짓점2 Y 좌표")
    x3: float = Field(..., description="꼭짓점3 X 좌표")
    y3: float = Field(..., description="꼭짓점3 Y 좌표")
    x4: float = Field(..., description="꼭짓점4 X 좌표")
    y4: float = Field(..., description="꼭짓점4 Y 좌표")


class RiskDetail(BaseModel):
    """
    KickSafe에서 사용할 위험 점수 / 등급 정보
    - risk_score: 0 ~ 100
    - risk_level: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
    - risk_level_kor: "안전" | "주의" | "위험" | "매우 위험"
    - risk_factors: 위험 점수에 기여한 요인 리스트
    """

    risk_score: float = Field(..., ge=0.0, le=100.0, description="위험 점수 (0~100)")
    risk_level: str = Field(..., description="영문 위험 등급")
    risk_level_kor: str = Field(..., description="한글 위험 등급")
    risk_factors: List[str] = Field(default_factory=list, description="위험 요인 설명 목록")


class DetectionResult(BaseModel):
    """
    이미지 1장에 대한 전체 분석 결과
    - detections: YOLOv8-OBB로 검출된 객체 리스트
    - risk: KickSafe 위험도 정보
    """

    detections: List[OrientedBBox] = Field(default_factory=list)
    risk: RiskDetail
