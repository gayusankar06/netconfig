import json
import os
from datetime import datetime
from typing import Dict, Any, List
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch, mm
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether)
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT
from app.models.review import Review

RISK_COLORS = {
    "LOW": colors.HexColor("#1B8A2C"),
    "MEDIUM": colors.HexColor("#D97706"),
    "HIGH": colors.HexColor("#D95D0A"),
    "CRITICAL": colors.HexColor("#C62828"),
    "UNKNOWN": colors.HexColor("#4B5563")
}

class ReportService:
    def generate_pdf(self, review: Review, output_path: str) -> str:
        """
        Generates an enterprise-grade PDF report using ReportLab.
        """
        # Ensure output directory exists
        dir_name = os.path.dirname(output_path)
        if dir_name:
            os.makedirs(dir_name, exist_ok=True)

        doc = SimpleDocTemplate(
            output_path,
            pagesize=A4,
            rightMargin=20*mm,
            leftMargin=20*mm,
            topMargin=25*mm,
            bottomMargin=20*mm
        )

        styles = getSampleStyleSheet()

        # Custom styles
        title_style = ParagraphStyle(
            "DocTitle",
            parent=styles["Title"],
            fontName="Helvetica-Bold",
            fontSize=24,
            leading=28,
            textColor=colors.HexColor("#1565C0"),
            alignment=TA_LEFT,
            spaceAfter=15
        )

        h1_style = ParagraphStyle(
            "SectionH1",
            parent=styles["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=16,
            leading=20,
            textColor=colors.HexColor("#1565C0"),
            spaceBefore=15,
            spaceAfter=10,
            keepWithNext=True
        )

        body_style = ParagraphStyle(
            "BodyTextCustom",
            parent=styles["Normal"],
            fontName="Helvetica",
            fontSize=10,
            leading=14,
            textColor=colors.HexColor("#374151"),
            spaceAfter=8
        )

        meta_label_style = ParagraphStyle(
            "MetaLabel",
            parent=styles["Normal"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=14,
            textColor=colors.HexColor("#1F2937")
        )

        story = []

        # 1. Header / Cover Block
        story.append(Paragraph("AI Network Config Diff Review Report", title_style))
        
        # Meta Table
        created_by_str = str(review.created_by)
        meta_data = [
            [Paragraph("Review Title:", meta_label_style), Paragraph(review.title, body_style)],
            [Paragraph("Review ID:", meta_label_style), Paragraph(str(review.id), body_style)],
            [Paragraph("Config Type:", meta_label_style), Paragraph(review.config_type, body_style)],
            [Paragraph("Cloud Provider:", meta_label_style), Paragraph(review.cloud_provider, body_style)],
            [Paragraph("Generated At:", meta_label_style), Paragraph(datetime.now().strftime("%Y-%m-%d %H:%M:%S"), body_style)],
            [Paragraph("Created By:", meta_label_style), Paragraph(created_by_str, body_style)]
        ]
        meta_table = Table(meta_data, colWidths=[130, 350])
        meta_table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 4),
            ('TOPPADDING', (0,0), (-1,-1), 4),
            ('LEFTPADDING', (0,0), (-1,-1), 0),
            ('RIGHTPADDING', (0,0), (-1,-1), 0),
        ]))
        story.append(meta_table)
        story.append(Spacer(1, 15))

        # Risk Summary Banner Table
        risk_color = RISK_COLORS.get(review.overall_risk_level, colors.gray)
        risk_label_style = ParagraphStyle(
            "RiskLabel",
            fontName="Helvetica-Bold",
            fontSize=12,
            textColor=colors.white,
            alignment=TA_CENTER
        )
        risk_banner_data = [
            [
                Paragraph(f"OVERALL RISK: {review.overall_risk_level} (Score: {review.overall_risk_score or 0.0:.1f})", risk_label_style),
                Paragraph(f"COMPLIANCE SCORE: {review.compliance_score or 0.0:.1f}%", risk_label_style)
            ]
        ]
        risk_banner_table = Table(risk_banner_data, colWidths=[240, 240])
        risk_banner_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), risk_color),
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 10),
            ('TOPPADDING', (0,0), (-1,-1), 10),
        ]))
        story.append(risk_banner_table)
        story.append(Spacer(1, 20))

        # 2. Executive Summary / AI recommendations
        story.append(Paragraph("Executive Summary", h1_style))
        summary_text = review.ai_summary or "No executive summary available."
        story.append(Paragraph(summary_text, body_style))
        story.append(Spacer(1, 15))

        story.append(Paragraph("AI Recommendations & Decisions", h1_style))
        rec_text = f"<b>Recommendation:</b> {review.ai_recommendation or 'PENDING'}<br/>" \
                   f"<b>Workflow Status:</b> {review.status}"
        story.append(Paragraph(rec_text, body_style))
        story.append(Spacer(1, 20))

        # 3. Changes Table
        story.append(Paragraph(f"Configuration Changes ({len(review.diff_changes)} total)", h1_style))
        if review.diff_changes:
            table_header_style = ParagraphStyle("THeader", fontName="Helvetica-Bold", fontSize=9, leading=11, textColor=colors.white)
            table_body_style = ParagraphStyle("TBody", fontName="Helvetica", fontSize=8, leading=10)
            
            changes_data = [[
                Paragraph("Field", table_header_style),
                Paragraph("Type", table_header_style),
                Paragraph("Old Value", table_header_style),
                Paragraph("New Value", table_header_style),
                Paragraph("Risk", table_header_style)
            ]]

            for c in review.diff_changes:
                changes_data.append([
                    Paragraph(c.field_name[:30], table_body_style),
                    Paragraph(c.change_type, table_body_style),
                    Paragraph(str(c.old_value or "")[:40], table_body_style),
                    Paragraph(str(c.new_value or "")[:40], table_body_style),
                    Paragraph(f"<font color='{RISK_COLORS.get(c.risk_level, colors.black).hexval()}'><b>{c.risk_level}</b></font>", table_body_style)
                ])

            changes_table = Table(changes_data, colWidths=[100, 50, 140, 140, 50])
            changes_table.setStyle(TableStyle([
                ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1565C0")),
                ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#E5E7EB")),
                ('VALIGN', (0,0), (-1,-1), 'TOP'),
                ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F9FAFB")]),
                ('BOTTOMPADDING', (0,0), (-1,-1), 6),
                ('TOPPADDING', (0,0), (-1,-1), 6),
            ]))
            story.append(changes_table)
        else:
            story.append(Paragraph("No changes detected in configuration files.", body_style))
        story.append(Spacer(1, 20))

        # 4. Compliance Findings
        story.append(Paragraph("Compliance Findings", h1_style))
        if review.compliance_findings:
            comp_header_style = ParagraphStyle("CHeader", fontName="Helvetica-Bold", fontSize=9, leading=11, textColor=colors.white)
            comp_body_style = ParagraphStyle("CBody", fontName="Helvetica", fontSize=8, leading=10)
            
            comp_data = [[
                Paragraph("Framework", comp_header_style),
                Paragraph("Control ID", comp_header_style),
                Paragraph("Control Name", comp_header_style),
                Paragraph("Status", comp_header_style),
                Paragraph("Severity", comp_header_style)
            ]]

            for f in review.compliance_findings:
                status_color = colors.HexColor("#1B8A2C") if f.status == "PASS" else colors.HexColor("#C62828")
                comp_data.append([
                    Paragraph(f.framework, comp_body_style),
                    Paragraph(f.control_id, comp_body_style),
                    Paragraph(f.control_name, comp_body_style),
                    Paragraph(f"<font color='{status_color.hexval()}'><b>{f.status}</b></font>", comp_body_style),
                    Paragraph(f.severity, comp_body_style)
                ])

            comp_table = Table(comp_data, colWidths=[70, 70, 200, 70, 70])
            comp_table.setStyle(TableStyle([
                ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1565C0")),
                ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#E5E7EB")),
                ('VALIGN', (0,0), (-1,-1), 'TOP'),
                ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F9FAFB")]),
                ('BOTTOMPADDING', (0,0), (-1,-1), 6),
                ('TOPPADDING', (0,0), (-1,-1), 6),
            ]))
            story.append(comp_table)
        else:
            story.append(Paragraph("No compliance checks run or findings created.", body_style))
        story.append(Spacer(1, 20))

        # 5. Approval Workflow timeline
        story.append(Paragraph("Approval Workflow History", h1_style))
        if review.workflow_steps:
            wf_header_style = ParagraphStyle("WHeader", fontName="Helvetica-Bold", fontSize=9, leading=11, textColor=colors.white)
            wf_body_style = ParagraphStyle("WBody", fontName="Helvetica", fontSize=8, leading=10)
            
            wf_data = [[
                Paragraph("Step", wf_header_style),
                Paragraph("Status", wf_header_style),
                Paragraph("Actor", wf_header_style),
                Paragraph("Date", wf_header_style),
                Paragraph("Comment", wf_header_style)
            ]]

            for s in review.workflow_steps:
                wf_data.append([
                    Paragraph(str(s.step_number), wf_body_style),
                    Paragraph(s.status, wf_body_style),
                    Paragraph(f"{s.actor_name or '-'} ({s.actor_role or '-'})", wf_body_style),
                    Paragraph(s.created_at.strftime("%Y-%m-%d"), wf_body_style),
                    Paragraph(s.comment or "-", wf_body_style)
                ])

            wf_table = Table(wf_data, colWidths=[40, 80, 130, 80, 150])
            wf_table.setStyle(TableStyle([
                ('BACKGROUND', (0,0), (-1,0), colors.HexColor("#1565C0")),
                ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#E5E7EB")),
                ('VALIGN', (0,0), (-1,-1), 'TOP'),
                ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F9FAFB")]),
                ('BOTTOMPADDING', (0,0), (-1,-1), 6),
                ('TOPPADDING', (0,0), (-1,-1), 6),
            ]))
            story.append(wf_table)
        else:
            story.append(Paragraph("No workflow steps recorded yet.", body_style))

        def add_header_footer(canvas, doc):
            canvas.saveState()
            canvas.setFillColor(colors.HexColor("#1565C0"))
            canvas.setFont("Helvetica-Bold", 8)
            canvas.drawString(20*mm, 287*mm, "AI Network Config Diff Reviewer — CONFIDENTIAL")
            canvas.setStrokeColor(colors.HexColor("#1565C0"))
            canvas.setLineWidth(0.5)
            canvas.line(20*mm, 285*mm, 190*mm, 285*mm)
            
            canvas.setFont("Helvetica", 8)
            canvas.setFillColor(colors.HexColor("#4B5563"))
            canvas.drawString(20*mm, 10*mm, datetime.now().strftime("%Y-%m-%d"))
            canvas.drawRightString(190*mm, 10*mm, f"Page {doc.page}")
            canvas.restoreState()

        doc.build(story, onFirstPage=add_header_footer, onLaterPages=add_header_footer)
        return output_path

    def generate_markdown(self, review: Review) -> str:
        """
        Generates a developer-friendly Markdown report.
        """
        lines = [
            f"# Network Config Review Report",
            f"",
            f"**Review Title:** {review.title}",
            f"**Review ID:** `{review.id}`",
            f"**Config Type:** {review.config_type}",
            f"**Cloud Provider:** {review.cloud_provider}",
            f"**Generated:** {datetime.now().isoformat()}Z",
            f"**Ticket ID:** {review.ticket_id or 'N/A'}",
            f"",
            f"---",
            f"",
            f"## Risk Assessment",
            f"",
            f"| Attribute | Value |",
            f"|-----------|-------|",
            f"| Overall Risk Level | **{review.overall_risk_level}** |",
            f"| Risk Score | {review.overall_risk_score or 0.0:.1f} / 100 |",
            f"| Compliance Score | {review.compliance_score or 0.0:.1f}% |",
            f"| AI Recommendation | **{review.ai_recommendation or 'PENDING'}** |",
            f"| Approval Status | {review.status} |",
            f"",
            f"---",
            f"",
            f"## Executive Summary",
            f"",
            f"{review.ai_summary or '_AI summary not available._'}",
            f"",
            f"---",
            f"",
            f"## Configuration Changes ({len(review.diff_changes)} total)",
            f"",
            f"| # | Field | Change Type | Old Value | New Value | Risk |",
            f"|---|-------|-------------|-----------|-----------|------|",
        ]

        for i, c in enumerate(review.diff_changes, 1):
            old_v = (c.old_value or "_N/A_")[:60]
            new_v = (c.new_value or "_N/A_")[:60]
            lines.append(f"| {i} | `{c.field_name[:40]}` | {c.change_type} | {old_v} | {new_v} | **{c.risk_level}** |")

        lines.extend([
            f"",
            f"---",
            f"",
            f"## Compliance Findings",
            f""
        ])

        for framework in ["CIS", "NIST", "PCI_DSS", "CUSTOM"]:
            fw_findings = [f for f in review.compliance_findings if f.framework == framework]
            if fw_findings:
                lines.append(f"### {framework} Controls")
                lines.append(f"")
                for f in fw_findings:
                    status_emoji = "✅" if f.status == "PASS" else "❌"
                    lines.append(f"- {status_emoji} **{f.control_id}** — {f.control_name}")
                    if f.status != "PASS":
                        lines.append(f"  - **Finding:** {f.finding_description}")
                        lines.append(f"  - **Remediation:** {f.remediation_guidance}")
                lines.append(f"")

        lines.extend([
            f"---",
            f"",
            f"## Approval Workflow",
            f"",
            f"| Step | Status | Actor | Timestamp | Comment |",
            f"|------|--------|-------|-----------|---------|",
        ])

        for s in review.workflow_steps:
            lines.append(f"| {s.step_number} | {s.status} | {s.actor_name or '-'} ({s.actor_role or '-'}) | {s.created_at.strftime('%Y-%m-%d %H:%M:%S')} | {s.comment or '-'} |")

        lines.extend([
            f"",
            f"---",
            f"",
            f"_Report generated by AI Network Config Diff Reviewer v1.0 — Powered by Ollama + Llama 3_"
        ])

        return "\n".join(lines)

    def generate_json(self, review: Review) -> Dict[str, Any]:
        """
        Generates a machine-readable JSON export.
        """
        return {
            "schema_version": "1.0",
            "generated_at": datetime.now().isoformat() + "Z",
            "review": {
                "id": str(review.id),
                "title": review.title,
                "config_type": review.config_type,
                "cloud_provider": review.cloud_provider,
                "ticket_id": review.ticket_id,
                "status": review.status,
                "overall_risk_level": review.overall_risk_level,
                "overall_risk_score": review.overall_risk_score,
                "compliance_score": review.compliance_score,
                "ai_recommendation": review.ai_recommendation,
                "ai_summary": review.ai_summary,
                "compliance_frameworks": review.compliance_frameworks,
                "created_at": review.created_at.isoformat() + "Z",
                "completed_at": review.completed_at.isoformat() + "Z" if review.completed_at else None,
            },
            "diff_changes": [
                {
                    "field_path": c.field_path,
                    "field_name": c.field_name,
                    "change_type": c.change_type,
                    "old_value": c.old_value,
                    "new_value": c.new_value,
                    "risk_level": c.risk_level,
                    "risk_score": c.risk_score,
                    "ai_explanation": c.ai_explanation,
                    "cis_control_ref": c.cis_control_ref,
                    "nist_control_ref": c.nist_control_ref
                }
                for c in review.diff_changes
            ],
            "compliance_findings": [
                {
                    "framework": f.framework,
                    "control_id": f.control_id,
                    "control_name": f.control_name,
                    "status": f.status,
                    "severity": f.severity,
                    "finding": f.finding_description,
                    "remediation": f.remediation_guidance
                }
                for f in review.compliance_findings
            ],
            "workflow": [
                {
                    "step": s.step_number,
                    "status": s.status,
                    "actor": s.actor_name,
                    "role": s.actor_role,
                    "comment": s.comment,
                    "timestamp": s.created_at.isoformat() + "Z"
                }
                for s in review.workflow_steps
            ]
        }
