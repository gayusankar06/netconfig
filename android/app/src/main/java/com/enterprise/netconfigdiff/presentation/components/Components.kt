package com.enterprise.netconfigdiff.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enterprise.netconfigdiff.presentation.theme.*

@Composable
fun RiskBadge(riskLevel: String) {
    val (bgColor, textColor) = when (riskLevel.uppercase()) {
        "LOW" -> Color(0xFFE8F5E9) to Color(0xFF1B8A2C)
        "MEDIUM" -> Color(0xFFFFF3E0) to Color(0xFFD97706)
        "HIGH" -> Color(0xFFFBE9E7) to Color(0xFFD95D0A)
        "CRITICAL" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
    }

    val modifier = if (riskLevel.uppercase() == "CRITICAL") {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Modifier
            .alpha(alpha)
            .border(1.dp, textColor, RoundedCornerShape(4.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = riskLevel,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiffLineItem(
    fieldName: String,
    oldValue: String?,
    newValue: String?,
    changeType: String,
    riskLevel: String,
    aiExplanation: String?,
    onExpandClick: () -> Unit = {}
) {
    val borderColor = when (changeType.uppercase()) {
        "ADDED" -> Color(0xFF1B8A2C)
        "REMOVED" -> Color(0xFFC62828)
        "MODIFIED" -> Color(0xFFD97706)
        else -> Color.Gray
    }

    val symbol = when (changeType.uppercase()) {
        "ADDED" -> "+"
        "REMOVED" -> "-"
        "MODIFIED" -> "~"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbol,
                color = borderColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fieldName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (changeType.uppercase() == "MODIFIED") {
                    Text(
                        text = "Change: ${oldValue ?: "N/A"} -> ${newValue ?: "N/A"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else if (changeType.uppercase() == "ADDED") {
                    Text(
                        text = "Added: ${newValue ?: "N/A"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        text = "Removed: ${oldValue ?: "N/A"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (!aiExplanation.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AI: $aiExplanation",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            RiskBadge(riskLevel = riskLevel)
        }
    }
}

@Composable
fun ComplianceFindingCard(
    framework: String,
    controlId: String,
    controlName: String,
    status: String,
    severity: String,
    description: String,
    remediation: String?
) {
    val statusColor = if (status.uppercase() == "PASS") Color(0xFF1B8A2C) else Color(0xFFC62828)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$framework: $controlId",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor,
                    fontSize = 14.sp
                )

                Text(
                    text = status,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = controlName,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            if (!remediation.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Remediation: $remediation",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ErrorColor
                )
            }
        }
    }
}

@Composable
fun ApprovalStatusChip(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "APPROVED" -> Color(0xFFE8F5E9) to Color(0xFF1B8A2C)
        "REJECTED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "ESCALATED" -> Color(0xFFFFF3E0) to Color(0xFFD97706)
        "PENDING_REVIEW" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        else -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingOverlay(isVisible: Boolean) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryColor)
        }
    }
}
