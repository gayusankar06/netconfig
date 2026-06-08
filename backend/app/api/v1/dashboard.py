from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import func

from app.database import get_db
from app.models.user import User
from app.models.review import Review
from app.models.diff_change import DiffChange
from app.models.compliance import ComplianceFinding
from app.schemas.review import DashboardStatsOut
from app.services.auth_service import get_current_active_user

router = APIRouter()

@router.get("", response_model=DashboardStatsOut)
async def get_dashboard_stats(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    # Total reviews
    total_reviews_res = await db.execute(select(func.count(Review.id)))
    total_reviews = total_reviews_res.scalar() or 0

    # Open reviews count
    open_status = ["DRAFT", "UNDER_ANALYSIS", "PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED"]
    open_reviews_res = await db.execute(select(func.count(Review.id)).filter(Review.status.in_(open_status)))
    open_reviews = open_reviews_res.scalar() or 0

    # High-risk findings count
    high_risk_res = await db.execute(select(func.count(DiffChange.id)).filter(DiffChange.risk_level.in_(["CRITICAL", "HIGH"])))
    high_risk_findings = high_risk_res.scalar() or 0

    # Compliance violations count
    violations_res = await db.execute(select(func.count(ComplianceFinding.id)).filter(ComplianceFinding.status == "FAIL"))
    compliance_violations = violations_res.scalar() or 0

    # Pending approvals
    pending_status = ["PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED"]
    pending_approvals_res = await db.execute(select(func.count(Review.id)).filter(Review.status.in_(pending_status)))
    pending_approvals = pending_approvals_res.scalar() or 0

    return {
        "total_reviews": total_reviews,
        "open_reviews": open_reviews,
        "high_risk_findings": high_risk_findings,
        "compliance_violations": compliance_violations,
        "pending_approvals": pending_approvals
    }
