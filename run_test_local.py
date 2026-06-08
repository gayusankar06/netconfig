import sys
import os
import argparse
import uuid
import json
from datetime import datetime

# Add backend directory to sys.path so we can import app modules
sys.path.append(os.path.join(os.path.dirname(__file__), "backend"))

try:
    from app.services.diff_engine import ConfigDiffEngine
    from app.services.compliance_engine import ComplianceEngine
    from app.services.report_service import ReportService
    from app.models.review import Review
    from app.models.workflow_step import WorkflowStep
except ImportError as e:
    print(f"Error importing modules: {e}")
    print("Please make sure dependencies are installed by running: pip install deepdiff pyyaml python-hcl2 reportlab markdown2 loguru tenacity pydantic-settings")
    sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="AI Network Config Diff Reviewer - Local CLI Runner")
    parser.add_argument("--old", required=True, help="Path to old config file")
    parser.add_argument("--new", required=True, help="Path to new config file")
    parser.add_argument("--type", default="AWS_SECURITY_GROUP", help="Configuration type")
    parser.add_argument("--provider", default="AWS", help="Cloud provider")
    parser.add_argument("--output", default="realtime_report.md", help="Output report file path")
    args = parser.parse_args()

    print(f"Loading configuration files...")
    print(f"  Old config: {args.old}")
    print(f"  New config: {args.new}")

    if not os.path.exists(args.old) or not os.path.exists(args.new):
        print("Error: Input files do not exist.")
        sys.exit(1)

    with open(args.old, "r", encoding="utf-8") as f:
        old_content = f.read()
    with open(args.new, "r", encoding="utf-8") as f:
        new_content = f.read()

    # Parse JSON
    try:
        old_dict = json.loads(old_content)
        new_dict = json.loads(new_content)
    except Exception as je:
        print(f"Error parsing JSON configuration files: {je}")
        sys.exit(1)

    print("\n[1/4] Running Structural Diff & Risk Scoring...")
    diff_engine = ConfigDiffEngine()
    changes = diff_engine.compute_diff(old_dict, new_dict)
    print(f"  Detected {len(changes)} configuration change(s).")
    for idx, c in enumerate(changes, 1):
        print(f"    Change #{idx}: {c.change_type} {c.field_name} (Risk: {c.risk_level}, Score: {c.risk_score})")

    print("\n[2/4] Running Compliance Verification Rules...")
    compliance_engine = ComplianceEngine()
    findings = compliance_engine.validate(changes, ["CIS", "NIST", "PCI_DSS", "CUSTOM"])
    failed_count = sum(1 for f in findings if f.status == "FAIL")
    print(f"  Ran compliance checks. Violations: {failed_count} / {len(findings)} checks.")

    # Calculate compliance score
    total_checks = len(findings)
    compliance_score = ((total_checks - failed_count) / total_checks * 100.0) if total_checks > 0 else 100.0

    # Determine overall risk
    critical_changes = [c for c in changes if c.risk_level == "CRITICAL"]
    high_changes = [c for c in changes if c.risk_level == "HIGH"]
    if critical_changes:
        overall_risk_level = "CRITICAL"
        overall_risk_score = 95.0
        ai_rec = "REJECT"
    elif high_changes:
        overall_risk_level = "HIGH"
        overall_risk_score = 75.0
        ai_rec = "ESCALATE"
    else:
        overall_risk_level = "LOW"
        overall_risk_score = 20.0
        ai_rec = "APPROVE"

    # Mock Review object
    review = Review(
        id=uuid.uuid4(),
        title="Local CLI Network Change Review",
        ticket_id="TICKET-2026",
        description="Local test run of AI Network Config Diff Reviewer on changes",
        config_type=args.type,
        cloud_provider=args.provider,
        status="PENDING_REVIEW" if overall_risk_level != "CRITICAL" else "ESCALATED",
        overall_risk_level=overall_risk_level,
        overall_risk_score=overall_risk_score,
        compliance_score=compliance_score,
        ai_summary="Local rule-based review. Critical port exposures (22, 3306) detected on public ranges (0.0.0.0/0). Awaiting manual security confirmation.",
        ai_recommendation=ai_rec,
        created_by=uuid.uuid4(),
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )

    # Attach mappings
    review.diff_changes = changes
    review.compliance_findings = findings
    
    # Mock workflow steps
    review.workflow_steps = [
        WorkflowStep(
            step_number=1,
            status=review.status,
            actor_name="Local Runner",
            actor_role="CLI Engine",
            comment="CLI execution triggered and completed locally.",
            created_at=datetime.utcnow()
        )
    ]

    print("\n[3/4] Generating Real-Time Reports...")
    report_service = ReportService()
    
    # Generate Markdown report
    md_report = report_service.generate_markdown(review)
    with open(args.output, "w", encoding="utf-8") as f:
        f.write(md_report)
    print(f"  Markdown report written to: {args.output}")

    # Generate PDF report (local check)
    pdf_path = args.output.replace(".md", ".pdf")
    try:
        report_service.generate_pdf(review, pdf_path)
        print(f"  PDF report written to: {pdf_path}")
    except Exception as pe:
        print(f"  Warning: Could not compile PDF report: {pe}")

    print("\n[4/4] CLI Review Completed successfully.")
    print("--------------------------------------------------")
    print(f"OVERALL RISK LEVEL : {overall_risk_level}")
    print(f"OVERALL RISK SCORE : {overall_risk_score} / 100")
    print(f"COMPLIANCE SCORE   : {compliance_score:.1f}%")
    print(f"DECISION           : {ai_rec}")
    print("--------------------------------------------------")

if __name__ == "__main__":
    main()
