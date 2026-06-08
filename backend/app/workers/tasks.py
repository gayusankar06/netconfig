import asyncio
import uuid
from datetime import datetime
from celery import shared_task
from sqlalchemy.future import select
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker

from app.config import settings
from app.database import Base
from app.models.review import Review
from app.models.diff_change import DiffChange
from app.models.compliance import ComplianceFinding
from app.models.workflow_step import WorkflowStep
from app.models.audit import AuditLog
from app.services.diff_engine import ConfigDiffEngine
from app.services.compliance_engine import ComplianceEngine
from app.services.ollama_service import OllamaService
from app.services.parser_service import ParserService
from app.services.report_service import ReportService
from app.utils.logger import logger

# Helper async session factory for Celery workers
async_engine = create_async_engine(settings.DATABASE_URL, pool_pre_ping=True)
AsyncSessionLocal = async_sessionmaker(bind=async_engine, class_=AsyncSession, expire_on_commit=False)

async def _execute_analysis_pipeline(review_id: str):
    logger.info(f"Starting async analysis pipeline for review: {review_id}")
    async with AsyncSessionLocal() as session:
        # Retrieve review
        result = await session.execute(select(Review).filter(Review.id == uuid.UUID(review_id)))
        review: Review = result.scalars().first()
        if not review:
            logger.error(f"Review not found: {review_id}")
            return
        
        try:
            review.status = "UNDER_ANALYSIS"
            await session.commit()
            
            # Read files
            with open(review.old_config_path, "r", encoding="utf-8") as f:
                old_content = f.read()
            with open(review.new_config_path, "r", encoding="utf-8") as f:
                new_content = f.read()
            
            # Parse configs
            old_dict = ParserService.parse_config(old_content, review.config_type)
            new_dict = ParserService.parse_config(new_content, review.config_type)
            
            # Run diff
            diff_engine = ConfigDiffEngine()
            changes: List[DiffChange] = diff_engine.compute_diff(old_dict, new_dict)
            
            # Save diff changes
            for idx, c in enumerate(changes):
                c.review_id = review.id
                c.order_index = idx
                session.add(c)
            await session.commit()
            
            # Log audit
            audit_diff = AuditLog(
                event_type="DIFF_CREATED",
                event_description=f"Generated {len(changes)} configuration changes for review: {review.title}",
                review_id=review.id,
                payload={"changes_count": len(changes)}
            )
            session.add(audit_diff)
            
            # Run compliance
            compliance_engine = ComplianceEngine()
            findings: List[ComplianceFinding] = compliance_engine.validate(changes, review.compliance_frameworks)
            
            failed_count = 0
            for f in findings:
                f.review_id = review.id
                session.add(f)
                if f.status == "FAIL":
                    failed_count += 1
            await session.commit()
            
            # Calculate compliance score
            total_checks = len(findings)
            compliance_score = ((total_checks - failed_count) / total_checks * 100.0) if total_checks > 0 else 100.0
            review.compliance_score = compliance_score
            
            # Log audit compliance
            audit_comp = AuditLog(
                event_type="COMPLIANCE_CHECKED",
                event_description=f"Compliance check completed with score: {compliance_score:.1f}%",
                review_id=review.id,
                payload={"total_checks": total_checks, "failed_checks": failed_count}
            )
            session.add(audit_comp)
            await session.commit()
            
            # Run Ollama AI analysis
            ollama = OllamaService()
            ai_data = await ollama.analyze_changes(changes, review.config_type)
            await ollama.close()
            
            # Update review with AI results
            review.overall_risk_level = ai_data.get("overall_risk_level", "UNKNOWN")
            review.overall_risk_score = ai_data.get("overall_risk_score", 0.0)
            review.ai_summary = ai_data.get("executive_summary", "")
            review.ai_recommendation = ai_data.get("ai_recommendation", "ESCALATE")
            
            # Save AI audit
            audit_ai = AuditLog(
                event_type="ANALYSIS_COMPLETED",
                event_description=f"AI analysis completed. Risk Level: {review.overall_risk_level}, Score: {review.overall_risk_score}",
                review_id=review.id,
                payload={"risk_level": review.overall_risk_level, "risk_score": review.overall_risk_score}
            )
            session.add(audit_ai)
            
            # Update changes with explanations from AI
            explanations = ai_data.get("change_explanations", {})
            for c in changes:
                c.ai_explanation = explanations.get(c.field_path)
                session.add(c)
                
            # Workflow Rules engine for status transition
            if review.overall_risk_level == "LOW" and review.auto_approve_if_low_risk:
                review.status = "APPROVED"
                review.completed_at = datetime.utcnow()
                step = WorkflowStep(
                    review_id=review.id,
                    step_number=1,
                    status="APPROVED",
                    actor_name="System",
                    actor_role="Automation Engine",
                    comment="System automatically approved review as risk level is LOW."
                )
                session.add(step)
            elif review.overall_risk_level == "CRITICAL":
                review.status = "ESCALATED"
                step = WorkflowStep(
                    review_id=review.id,
                    step_number=1,
                    status="ESCALATED",
                    actor_name="System",
                    actor_role="Automation Engine",
                    comment="System automatically escalated review due to CRITICAL risk levels."
                )
                session.add(step)
            else:
                review.status = "PENDING_REVIEW"
                step = WorkflowStep(
                    review_id=review.id,
                    step_number=1,
                    status="PENDING_REVIEW",
                    actor_name="System",
                    actor_role="Automation Engine",
                    comment="Awaiting review by authorized engineering staff."
                )
                session.add(step)
                
            await session.commit()
            
            # Generate static report file pre-cache
            report_service = ReportService()
            report_service.generate_pdf(review, os.path.join(settings.REPORT_OUTPUT_DIR, f"{review.id}.pdf"))
            
            logger.info(f"Pipeline completed successfully for review: {review_id}")
            
        except Exception as e:
            logger.exception(f"Failed to process analysis pipeline for review: {review_id}")
            review.status = "FAILED"
            audit_fail = AuditLog(
                event_type="ANALYSIS_COMPLETED",
                event_description=f"Analysis pipeline crashed: {str(e)}",
                review_id=review.id
            )
            session.add(audit_fail)
            await session.commit()

@shared_task(name="app.workers.tasks.run_analysis_pipeline_task")
def run_analysis_pipeline_task(review_id: str):
    asyncio.run(_execute_analysis_pipeline(review_id))
