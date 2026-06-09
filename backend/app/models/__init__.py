from app.database import Base
from app.models.user import User, Role
from app.models.review import Review, ReviewStatus, RiskLevel

# Export models safely, using try/except for the optional ones so it doesn't crash if they are partially implemented
try:
    from app.models.config_file import ConfigFile
except ImportError:
    ConfigFile = None

try:
    from app.models.diff_result import DiffResult
except ImportError:
    DiffResult = None

try:
    from app.models.analysis_result import AnalysisResult
except ImportError:
    AnalysisResult = None

try:
    from app.models.compliance_result import ComplianceResult
except ImportError:
    ComplianceResult = None

try:
    from app.models.audit_log import AuditLog
except ImportError:
    AuditLog = None

__all__ = ["Base", "User", "Role", "Review", "ReviewStatus", "RiskLevel", "ConfigFile", "DiffResult", "AnalysisResult", "ComplianceResult", "AuditLog"]
