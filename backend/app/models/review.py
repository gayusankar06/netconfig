import uuid
from datetime import datetime
from typing import List, Optional
from sqlalchemy import String, Float, Boolean, DateTime, Text, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, ARRAY
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database import Base

class Review(Base):
    __tablename__ = "reviews"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    ticket_id: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    config_type: Mapped[str] = mapped_column(String(100), nullable=False) # e.g. AWS_SECURITY_GROUP, GCP_FIREWALL
    cloud_provider: Mapped[str] = mapped_column(String(50), nullable=False) # AWS | GCP | AZURE | MULTI_CLOUD
    status: Mapped[str] = mapped_column(String(50), nullable=False, default="DRAFT") # DRAFT | PENDING_REVIEW | APPROVED | REJECTED | ESCALATED | CLOSED | UNDER_ANALYSIS | PENDING_APPROVAL | FAILED
    overall_risk_level: Mapped[str] = mapped_column(String(50), nullable=False, default="UNKNOWN") # LOW | MEDIUM | HIGH | CRITICAL | UNKNOWN
    overall_risk_score: Mapped[Optional[float]] = mapped_column(Float, nullable=True) # 0.0 - 100.0
    compliance_score: Mapped[Optional[float]] = mapped_column(Float, nullable=True) # 0.0 - 100.0
    ai_summary: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    ai_recommendation: Mapped[Optional[str]] = mapped_column(String(50), nullable=True) # APPROVE | REJECT | ESCALATE
    auto_approve_if_low_risk: Mapped[bool] = mapped_column(Boolean, default=False)
    notify_manager: Mapped[bool] = mapped_column(Boolean, default=True)
    old_config_path: Mapped[str] = mapped_column(String(500), nullable=False)
    new_config_path: Mapped[str] = mapped_column(String(500), nullable=False)
    compliance_frameworks: Mapped[List[str]] = mapped_column(ARRAY(String), nullable=False, default=list)

    created_by: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    assigned_reviewer_id: Mapped[Optional[uuid.UUID]] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, index=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    completed_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)

    # Relationships
    creator = relationship("User", foreign_keys=[created_by])
    assigned_reviewer = relationship("User", foreign_keys=[assigned_reviewer_id])
    diff_changes = relationship("DiffChange", back_populates="review", cascade="all, delete-orphan")
    compliance_findings = relationship("ComplianceFinding", back_populates="review", cascade="all, delete-orphan")
    workflow_steps = relationship("WorkflowStep", back_populates="review", cascade="all, delete-orphan")
