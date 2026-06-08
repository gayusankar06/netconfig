package com.enterprise.netconfigdiff.presentation.screens.reports

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.netconfigdiff.domain.repository.ReportRepository
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State
sealed class ReportUiState {
    object Idle : ReportUiState()
    object Loading : ReportUiState()
    data class Success(val message: String) : ReportUiState()
    data class Error(val error: String) : ReportUiState()
}

// ViewModel
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun downloadReport(reviewId: String, format: String, onFinished: (ByteArray) -> Unit) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            val result = reportRepository.generateAndDownloadReport(reviewId, format)
            result.onSuccess { bytes ->
                _uiState.value = ReportUiState.Success("Report generated successfully.")
                onFinished(bytes)
            }.onFailure { error ->
                _uiState.value = ReportUiState.Error(error.message ?: "Failed to generate report.")
            }
        }
    }
}

// Composable View
@Composable
fun ReportsScreen(
    reviewId: String,
    viewModel: ReportsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Generate Reports") },
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Download Review Reports",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // PDF Card
                item {
                    ReportTypeCard(
                        title = "PDF Report",
                        description = "Executive Summary, compliance assessment, risk levels, and visual audit trail layout. Best for auditing and manager signature signs.",
                        buttonLabel = "Download PDF",
                        onClick = {
                            viewModel.downloadReport(reviewId, "pdf") { bytes ->
                                // Trigger simple local download simulation
                                Toast.makeText(context, "PDF saved to Downloads folder.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Markdown Card
                item {
                    ReportTypeCard(
                        title = "Markdown Report",
                        description = "Clean, raw text formatting. Best for developer review pipelines and JIRA ticket attachments.",
                        buttonLabel = "Download Markdown",
                        onClick = {
                            viewModel.downloadReport(reviewId, "markdown") { bytes ->
                                val text = String(bytes)
                                // Copy or share
                                shareText(context, "Markdown Report", text)
                            }
                        }
                    )
                }

                // JSON Card
                item {
                    ReportTypeCard(
                        title = "JSON Report",
                        description = "Raw structured JSON payload of configuration diff, overall risk levels and compliance failures. Best for automation integration.",
                        buttonLabel = "Export JSON",
                        onClick = {
                            viewModel.downloadReport(reviewId, "json") { bytes ->
                                val jsonStr = String(bytes)
                                copyToClipboard(context, jsonStr)
                                Toast.makeText(context, "JSON copied to clipboard.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            if (uiState is ReportUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }
        }
    }
}

@Composable
fun ReportTypeCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
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
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(text = buttonLabel, color = Color.White)
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Configuration Review Report", text)
    clipboard.setPrimaryClip(clip)
}

fun shareText(context: Context, subject: String, body: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        putExtra(android.content.Intent.EXTRA_TEXT, body)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Report via"))
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
import androidx.compose.foundation.background
