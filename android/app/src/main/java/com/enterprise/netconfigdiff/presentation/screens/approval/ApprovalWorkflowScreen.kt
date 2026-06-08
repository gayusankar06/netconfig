package com.enterprise.netconfigdiff.presentation.screens.approval

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.enterprise.netconfigdiff.domain.model.WorkflowStep
import com.enterprise.netconfigdiff.domain.repository.ReviewRepository
import com.enterprise.netconfigdiff.presentation.components.ApprovalStatusChip
import com.enterprise.netconfigdiff.presentation.components.LoadingOverlay
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State
sealed class ApprovalUiState {
    object Loading : ApprovalUiState()
    data class Content(val detail: ReviewDetail, val steps: List<WorkflowStep>) : ApprovalUiState()
    data class Error(val error: String) : ApprovalUiState()
}

// ViewModel
@HiltViewModel
class ApprovalWorkflowViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ApprovalUiState>(ApprovalUiState.Loading)
    val uiState: StateFlow<ApprovalUiState> = _uiState.asStateFlow()

    fun loadWorkflow(reviewId: String) {
        viewModelScope.launch {
            _uiState.value = ApprovalUiState.Loading
            reviewRepository.refreshReviewDetail(reviewId)
            reviewRepository.getReviewDetail(reviewId).collectLatest { detail ->
                if (detail != null) {
                    _uiState.value = ApprovalUiState.Content(detail, detail.workflowSteps)
                }
            }
        }
    }

    fun submitAction(reviewId: String, actionType: String, comment: String) {
        viewModelScope.launch {
            _uiState.value = ApprovalUiState.Loading
            val result = when (actionType) {
                "APPROVE" -> reviewRepository.approveReview(reviewId, comment)
                "REJECT" -> reviewRepository.rejectReview(reviewId, comment)
                else -> reviewRepository.escalateReview(reviewId, comment)
            }
            result.onSuccess {
                loadWorkflow(reviewId)
            }.onFailure { error ->
                _uiState.value = ApprovalUiState.Error(error.message ?: "Failed to perform approval action.")
            }
        }
    }
}

// Composable View
@Composable
fun ApprovalWorkflowScreen(
    reviewId: String,
    viewModel: ApprovalWorkflowViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(reviewId) {
        viewModel.loadWorkflow(reviewId)
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Approval Workflow") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ApprovalUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
                is ApprovalUiState.Content -> {
                    val review = state.detail.review
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Status banner
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Current Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Updated: ${review.updatedAt.split("T").first()}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    ApprovalStatusChip(status = review.status)
                                }
                            }
                        }

                        // Workflow Timeline Component
                        item {
                            Text(text = "Approval Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryColor)
                        }

                        if (state.steps.isEmpty()) {
                            item {
                                Text("No progress recorded in workflow timeline.", fontSize = 13.sp, color = Color.Gray)
                            }
                        } else {
                            items(state.steps) { step ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Step ${step.stepNumber}: ${step.status}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = step.createdAt.split("T").first(), fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Actor: ${step.actorName ?: "System"} (${step.actorRole ?: "Automation Engine"})", fontSize = 12.sp)
                                        if (!step.comment.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "Comment: ${step.comment}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }

                        // Reviewer Actions Section
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Reviewer Comments & Decision", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                label = { Text("Comment (max 500 characters)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.submitAction(review.id, "APPROVE", comment); comment = "" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8A2C)),
                                    enabled = comment.isNotEmpty()
                                ) {
                                    Text("Approve", color = Color.White, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.submitAction(review.id, "REJECT", comment); comment = "" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    enabled = comment.isNotEmpty()
                                ) {
                                    Text("Reject", color = Color.White, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.submitAction(review.id, "ESCALATE", comment); comment = "" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    enabled = comment.isNotEmpty()
                                ) {
                                    Text("Escalate", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                is ApprovalUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Action Failed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.error)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.loadWorkflow(reviewId) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                            Text("Try Again", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
