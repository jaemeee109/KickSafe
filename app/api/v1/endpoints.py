# api/v1/endpoints.py
# ------------------------------------------
# KickSafe-Ai FastAPI 엔드포인트 정의
# - /api/v1/health          : 헬스 체크
# - /api/v1/detect/image    : 이미지 업로드 → OBB + 위험도 분석
# ------------------------------------------

from fastapi import APIRouter, UploadFile, File, HTTPException
from starlette import status

from app.schemas import DetectionResult
from app.services.roboflow_service import analyze_image

router = APIRouter()


@router.get("/health")
async def health_check():
    """
    단순 헬스 체크 엔드포인트
    """
    return {"status": "ok", "service": "kicksafe-ai"}


@router.post(
    "/detect/image",
    response_model=DetectionResult,
    status_code=status.HTTP_200_OK,
)
async def detect_from_image(file: UploadFile = File(...)):
    """
    이미지 1장을 업로드 받아 YOLOv8-OBB 추론 + 위험도 분석 결과를 반환하는 엔드포인트

    요청:
      - multipart/form-data
        - file: 이미지 파일(jpg, png 등)

    응답:
      - DetectionResult (detections + risk)
    """
    if file.content_type is None or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이미지 파일만 업로드 가능합니다.",
        )

    image_bytes = await file.read()
    return analyze_image(image_bytes)
