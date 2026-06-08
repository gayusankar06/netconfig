import json
import re
import ipaddress
from deepdiff import DeepDiff
from typing import Any, Dict, List, Tuple
from app.models.diff_change import DiffChange

class ConfigDiffEngine:
    SENSITIVE_PORTS = {22, 3389, 3306, 5432, 6379, 9200, 27017, 5984, 6443, 8080, 8443}
    CRITICAL_CIDRS = {"0.0.0.0/0", "::/0"}
    INTERNAL_CIDR_PREFIXES = (
        "10.", "172.16.", "172.17.", "172.18.", "172.19.", "172.20.",
        "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
        "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "192.168."
    )

    def compute_diff(self, old_config: Dict[str, Any], new_config: Dict[str, Any]) -> List[DiffChange]:
        """
        Computes structural diff between two configuration dictionaries.
        Returns a list of unpersisted DiffChange objects with risk scores.
        """
        changes: List[DiffChange] = []
        diff = DeepDiff(old_config, new_config, ignore_order=True, verbose_level=2)

        # 1. Process added items
        for path, value in diff.get("dictionary_item_added", {}).items():
            change = self._process_change(path, None, value, "ADDED")
            changes.append(change)

        # 2. Process removed items
        for path, value in diff.get("dictionary_item_removed", {}).items():
            change = self._process_change(path, value, None, "REMOVED")
            changes.append(change)

        # 3. Process modified values
        for path, change_info in diff.get("values_changed", {}).items():
            change = self._process_change(
                path,
                change_info.get("old_value"),
                change_info.get("new_value"),
                "MODIFIED"
            )
            changes.append(change)

        # 4. Process iterable additions
        for path, items in diff.get("iterable_item_added", {}).items():
            for item in (items if isinstance(items, list) else [items]):
                change = self._process_change(path, None, item, "ADDED")
                changes.append(change)

        # 5. Process iterable removals
        for path, items in diff.get("iterable_item_removed", {}).items():
            for item in (items if isinstance(items, list) else [items]):
                change = self._process_change(path, item, None, "REMOVED")
                changes.append(change)

        # Sort changes by risk score descending
        return sorted(changes, key=lambda c: c.risk_score, reverse=True)

    def _process_change(self, path: str, old_value: Any, new_value: Any, change_type: str) -> DiffChange:
        """
        Classifies a change, extracts name and details, and computes its risk score.
        """
        field_name = self._extract_field_name(path)
        risk_level, risk_score = self._compute_risk(field_name, old_value, new_value, change_type)

        return DiffChange(
            field_path=str(path),
            field_name=field_name,
            old_value=str(old_value) if old_value is not None else None,
            new_value=str(new_value) if new_value is not None else None,
            change_type=change_type,
            risk_level=risk_level,
            risk_score=risk_score,
            affected_resource=self._extract_resource_name(path)
        )

    def _compute_risk(self, field_name: str, old_value: Any, new_value: Any, change_type: str) -> Tuple[str, float]:
        """
        Rule-based risk scoring logic. Returns (risk_level, risk_score).
        """
        field_lower = field_name.lower()
        new_val_str = str(new_value).lower() if new_value is not None else ""
        old_val_str = str(old_value).lower() if old_value is not None else ""

        # CRITICAL: Exposure of sensitive ports to public internet
        is_port_field = any(k in field_lower for k in ["port", "from_port", "to_port", "destination_port"])
        if is_port_field:
            try:
                # Strip port values if ranges like "22-22"
                port_str = new_val_str.split("-")[0].strip() if new_val_str else "0"
                if port_str.isdigit():
                    port = int(port_str)
                    if port in self.SENSITIVE_PORTS:
                        # Check if exposed to public
                        if any(cidr in new_val_str or cidr in field_lower for cidr in self.CRITICAL_CIDRS):
                            return "CRITICAL", 98.0
                        return "HIGH", 75.0
            except ValueError:
                pass

        # CRITICAL: Exposure to public internet
        if any(cidr in new_val_str for cidr in self.CRITICAL_CIDRS):
            # Check if this changes from internal or is added
            if not old_val_str or not any(cidr in old_val_str for cidr in self.CRITICAL_CIDRS):
                if change_type == "ADDED":
                    return "CRITICAL", 90.0
                return "CRITICAL", 95.0

        # HIGH: Any CIDR expansion (e.g. from /24 to /16)
        is_cidr_field = any(k in field_lower for k in ["cidr", "source_range", "ip_range", "prefix"])
        if is_cidr_field and old_val_str and new_val_str:
            if self._is_cidr_expansion(old_val_str, new_val_str):
                return "HIGH", 70.0

        # HIGH: Protocol changes to wildcard (e.g. TCP -> ALL)
        if "protocol" in field_lower:
            wildcards = ["-1", "all", "*", "any"]
            if any(w in new_val_str for w in wildcards) and not any(w in old_val_str for w in wildcards):
                return "HIGH", 72.0

        # HIGH: VPN/encryption modifications
        if any(k in field_lower for k in ["vpn", "encryption", "psk", "ike", "tunnel"]):
            return "HIGH", 68.0

        # MEDIUM: Route changes
        if any(k in field_lower for k in ["route", "next_hop", "gateway"]):
            return "MEDIUM", 45.0

        # MEDIUM: Firewall/Security group policy changes
        if any(k in field_lower for k in ["rule", "policy", "acl", "firewall"]):
            return "MEDIUM", 40.0

        # LOW: Tag, label, description, name updates
        if any(k in field_lower for k in ["tag", "label", "name", "description", "comment"]):
            return "LOW", 10.0

        # Default low fallback
        return "LOW", 20.0

    def _is_cidr_expansion(self, old_cidr: str, new_cidr: str) -> bool:
        """
        Determines if the new CIDR block is broader than the old CIDR block.
        """
        try:
            old_net = ipaddress.ip_network(old_cidr, strict=False)
            new_net = ipaddress.ip_network(new_cidr, strict=False)
            return new_net.prefixlen < old_net.prefixlen
        except ValueError:
            return False

    def _extract_field_name(self, path: str) -> str:
        """
        Converts a DeepDiff path string (e.g. "root['rules'][0]['cidr']") to a clean dotted string or field name.
        """
        cleaned = re.sub(r"root\[", "", str(path))
        cleaned = re.sub(r"\]\[", ".", cleaned)
        cleaned = re.sub(r"[\[\]']", "", cleaned)
        return cleaned

    def _extract_resource_name(self, path: str) -> str:
        """
        Extracts resource identifier from path string.
        """
        parts = str(path).split("[")
        if len(parts) > 1:
            return parts[1].strip("']\"")
        return "Unknown Resource"
