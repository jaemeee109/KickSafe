# main.py
# ------------------------------------------
# uvicorn으로 실행될 FastAPI 애플리케이션의 진입점입니다.
# (SpringBoot의 @SpringBootApplication 클래스와 비슷한 역할)
# ------------------------------------------

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings
from app.api.v1.endpoints import router as api_v1_router

settings = get_settings()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version="1.0.0",
)

# CORS 설정 (필요 시 도메인 제한 가능)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 나중에 SpringBoot/React 도메인으로 제한 가능
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# /api/v1 이하 라우팅
app.include_router(api_v1_router, prefix=settings.API_V1_PREFIX)


@app.get("/")
async def root():
    """
    간단한 루트 엔드포인트
    """
    return {"message": "KickSafe AI Service is running"}
