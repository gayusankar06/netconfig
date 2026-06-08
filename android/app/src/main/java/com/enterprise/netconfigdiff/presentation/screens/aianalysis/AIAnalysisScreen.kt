package com.enterprise.netconfigdiff.presentation.screens.aianalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.netconfigdiff.domain.model.ReviewDetail
import com.enterprise.netconfigdiff.domain.repository.ReviewRepository
import com.enterprise.netconfigdiff.presentation.components.RiskBadge
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel
@HiltViewModel
class AIAnalysisViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val _reviewDetail = MutableStateFlow<ReviewDetail?>(null)
    val reviewDetail: StateFlow<ReviewDetail?> = _reviewDetail.asStateFlow()

    fun loadAnalysis(reviewId: String) {
        viewModelScope.launch {
            reviewRepository.refreshReviewDetail(reviewId)
            reviewRepository.getReviewDetail(reviewId).collectLatest { detail ->
                _reviewDetail.value = detail
            }
        }
    }
}

// Composable View
@Composable
fun AIAnalysisScreen(
    reviewId: String,
    viewModel: AIAnalysisViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val detail by viewModel.reviewDetail.collectAsState()

    LaunchedEffect(reviewId) {
        viewModel.loadAnalysis(reviewId)
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("AI Security Analysis") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        }
    ) { paddingValues ->
        if (detail == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            val review = detail!!.review
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Overall Risk Score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            RiskBadge(riskLevel = review.overallRiskLevel)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { (review.overallRiskScore ?: 0f) / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PrimaryColor,
                            trackColor = Color(0xFFE5E7EB)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score: ${review.overallRiskScore ?: 0f} / 100.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Executive Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = detail!!.aiSummary ?: "AI analysis completed. Overall config risk scored as ${review.overallRiskLevel}. Local rule-based check shows score of ${review.overallRiskScore ?: 0.0}.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Security Recommendation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recommendation: ${detail!!.aiRecommendation ?: 'PENDING'}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = "Suggested Remediation Guidance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryColor)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "1. Restrict open source ranges to private subnet IP allocations.", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "2. Enforce transport level encryption and restrict ingress rules.", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
