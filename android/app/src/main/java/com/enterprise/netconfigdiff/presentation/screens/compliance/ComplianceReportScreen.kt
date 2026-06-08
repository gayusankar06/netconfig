package com.enterprise.netconfigdiff.presentation.screens.compliance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.enterprise.netconfigdiff.domain.model.ComplianceFinding
import com.enterprise.netconfigdiff.domain.repository.ComplianceRepository
import com.enterprise.netconfigdiff.presentation.components.ComplianceFindingCard
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
class ComplianceReportViewModel @Inject constructor(
    private val complianceRepository: ComplianceRepository
) : ViewModel() {
    private val _findings = MutableStateFlow<List<ComplianceFinding>>(emptyList())
    val findings: StateFlow<List<ComplianceFinding>> = _findings.asStateFlow()

    fun loadFindings(reviewId: String) {
        viewModelScope.launch {
            complianceRepository.triggerCompliance(reviewId)
            complianceRepository.getComplianceFindings(reviewId).collectLatest { list ->
                _findings.value = list
            }
        }
    }
}

// Composable View
@Composable
fun ComplianceReportScreen(
    reviewId: String,
    viewModel: ComplianceReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val findings by viewModel.findings.collectAsState()

    LaunchedEffect(reviewId) {
        viewModel.loadFindings(reviewId)
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Compliance Report") },
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
        if (findings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No compliance findings generated yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Framework Assessment Findings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(findings) { f ->
                    ComplianceFindingCard(
                        framework = f.framework,
                        controlId = f.controlId,
                        controlName = f.controlName,
                        status = f.status,
                        severity = f.severity,
                        description = f.findingDescription,
                        remediation = f.remediationGuidance
                    )
                }
            }
        }
    }
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
