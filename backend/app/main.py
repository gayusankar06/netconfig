from contextlib import asynccontextmanager
from fastapi import FastAPI, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.v1.router import api_router
from app.database import init_db
from app.config import settings
from app.utils.logger import logger

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting AI Network Config Diff Reviewer API...")
    # Initialize database tables asynchronously
    await init_db()
    yield
    logger.info("Shutting down...")

app = FastAPI(
    title="AI Network Config Diff Reviewer",
    description="Enterprise-grade network configuration change review platform",
    version="1.0.0",
    docs_url="/api/docs" if settings.ENVIRONMENT != "production" else None,
    redoc_url="/api/redoc" if settings.ENVIRONMENT != "production" else None,
    lifespan=lifespan
)

# Security Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
)

app.add_middleware(
    TrustedHostMiddleware, 
    allowed_hosts=settings.ALLOWED_HOSTS
)

# Prometheus metrics
Instrumentator().instrument(app).expose(app, endpoint="/metrics")

# API Routing
app.include_router(api_router, prefix="/api/v1")

@app.get("/api/v1/health", status_code=status.HTTP_200_OK)
async def health_check():
    return {"status": "healthy", "timestamp": lifespan}
