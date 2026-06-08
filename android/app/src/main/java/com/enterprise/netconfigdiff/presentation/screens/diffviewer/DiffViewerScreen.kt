package com.enterprise.netconfigdiff.presentation.screens.diffviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
import com.enterprise.netconfigdiff.domain.model.DiffChange
import com.enterprise.netconfigdiff.domain.repository.ReviewRepository
import com.enterprise.netconfigdiff.presentation.components.DiffLineItem
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
class DiffViewerViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val _changes = MutableStateFlow<List<DiffChange>>(emptyList())
    val changes: StateFlow<List<DiffChange>> = _changes.asStateFlow()

    fun loadChanges(reviewId: String) {
        viewModelScope.launch {
            reviewRepository.refreshReviewDetail(reviewId)
            reviewRepository.getReviewDetail(reviewId).collectLatest { detail ->
                if (detail != null) {
                    _changes.value = detail.diffChanges
                }
            }
        }
    }
}

// Composable View
@Composable
fun DiffViewerScreen(
    reviewId: String,
    viewModel: DiffViewerViewModel = hiltViewModel(),
    onNavigateToAnalysis: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToApproval: () -> Unit,
    onNavigateToReports: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val changes by viewModel.changes.collectAsState()

    LaunchedEffect(reviewId) {
        viewModel.loadChanges(reviewId)
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Configuration Differences") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAnalysis,
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Analytics, contentDescription = "View AI Analysis")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            val tabs = listOf("ADDED", "REMOVED", "MODIFIED", "SIDE-BY-SIDE")
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Quick navigation panel to compliance or reports
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onNavigateToCompliance, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                    Text("Compliance", color = Color.White, fontSize = 12.sp)
                }
                Button(onClick = onNavigateToApproval, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                    Text("Approval", color = Color.White, fontSize = 12.sp)
                }
                Button(onClick = onNavigateToReports, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                    Text("Reports", color = Color.White, fontSize = 12.sp)
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> { // ADDED
                    val added = changes.filter { it.changeType.uppercase() == "ADDED" }
                    DiffList(changesList = added)
                }
                1 -> { // REMOVED
                    val removed = changes.filter { it.changeType.uppercase() == "REMOVED" }
                    DiffList(changesList = removed)
                }
                2 -> { // MODIFIED
                    val modified = changes.filter { it.changeType.uppercase() == "MODIFIED" }
                    DiffList(changesList = modified)
                }
                3 -> { // SIDE-BY-SIDE
                    SideBySideView(changes = changes)
                }
            }
        }
    }
}

@Composable
fun DiffList(changesList: List<DiffChange>) {
    if (changesList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No changes of this type detected.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(changesList) { change ->
                DiffLineItem(
                    fieldName = change.fieldName,
                    oldValue = change.oldValue,
                    newValue = change.newValue,
                    changeType = change.changeType,
                    riskLevel = change.riskLevel,
                    aiExplanation = change.aiExplanation
                )
            }
        }
    }
}

@Composable
fun SideBySideView(changes: List<DiffChange>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFE5E7EB)).padding(8.dp)) {
                Text("OLD SETTING", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = Color.Gray)
                Text("NEW SETTING", modifier = Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        
        items(changes) { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${c.fieldName}: ${c.oldValue ?: "N/A"}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${c.fieldName}: ${c.newValue ?: "N/A"}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

// Re-declare dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
