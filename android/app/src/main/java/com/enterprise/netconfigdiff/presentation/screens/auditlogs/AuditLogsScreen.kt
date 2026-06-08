package com.enterprise.netconfigdiff.presentation.screens.auditlogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.enterprise.netconfigdiff.domain.model.AuditLog
import com.enterprise.netconfigdiff.domain.repository.AuditRepository
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
class AuditLogsViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {
    private val _logs = MutableStateFlow<List<AuditLog>>(emptyList())
    val logs: StateFlow<List<AuditLog>> = _logs.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total.asStateFlow()

    init {
        // Collect database logs
        viewModelScope.launch {
            auditRepository.getAuditLogs(null, null).collectLatest { list ->
                _logs.value = list
            }
        }
        
        // Refresh initially
        refreshLogs(1, 50, null, null)
    }

    fun refreshLogs(page: Int, size: Int, filter: String?, search: String?) {
        viewModelScope.launch {
            val result = auditRepository.refreshAuditLogs(page, size, filter, search)
            result.onSuccess { totalCount ->
                _total.value = totalCount
            }
        }
    }
}

// Composable View
@Composable
fun AuditLogsScreen(
    viewModel: AuditLogsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val logs by viewModel.logs.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(search, selectedFilter) {
        viewModel.refreshLogs(1, 50, if (selectedFilter == "ALL") null else selectedFilter, if (search.isEmpty()) null else search)
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Audit Trail Logs") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search logs by description...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Horizontal Row
            val scrollState = rememberScrollState()
            val filters = listOf("ALL", "AUTH_LOGIN", "AUTH_LOGOUT", "CONFIG_UPLOAD", "DIFF_CREATED", "COMPLIANCE_CHECKED", "ANALYSIS_COMPLETED", "REPORT_GENERATED")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.replace("AUTH_", "").replace("CONFIG_", "").replace("_", " "), fontSize = 11.sp) }
                    )
                }
            }

            // Logs list
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No logs match filters.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        AuditLogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(text = log.eventType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryColor)
                Text(text = log.createdAt.split("T").first(), fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.eventDescription, fontSize = 12.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
            val userText = if (!log.userEmail.isNullOrEmpty()) "User: ${log.userEmail} (${log.userRole})" else "Actor: System"
            Text(text = userText, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
