package com.enterprise.netconfigdiff.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.enterprise.netconfigdiff.domain.model.DashboardStats
import com.enterprise.netconfigdiff.domain.model.Review
import com.enterprise.netconfigdiff.domain.repository.ReviewRepository
import com.enterprise.netconfigdiff.presentation.components.ApprovalStatusChip
import com.enterprise.netconfigdiff.presentation.components.RiskBadge
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats(0, 0, 0, 0, 0))
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        // Collect database reviews
        viewModelScope.launch {
            reviewRepository.getReviews().collectLatest { list ->
                _reviews.value = list
                
                // Summarize stats locally
                val total = list.size
                val open = list.count { it.status in listOf("DRAFT", "UNDER_ANALYSIS", "PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED") }
                val high = list.count { it.overallRiskLevel in listOf("CRITICAL", "HIGH") }
                val compliance = list.count { (it.complianceScore ?: 100f) < 100f }
                val pending = list.count { it.status in listOf("PENDING_REVIEW", "PENDING_APPROVAL", "ESCALATED") }
                _stats.value = DashboardStats(total, open, high, compliance, pending)
            }
        }

        // Trigger network refresh and setup 30 seconds loop
        viewModelScope.launch {
            while (true) {
                reviewRepository.refreshReviews()
                delay(30000L) // 30 seconds refresh
            }
        }
    }
}

// Composable View
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToUpload: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToReview: (String) -> Unit
) {
    val reviews by viewModel.reviews.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onNavigateToAudit) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Audit Logs")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor, titleContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; onNavigateToUpload() },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Upload") },
                    label = { Text("Upload") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; onNavigateToAudit() },
                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                    label = { Text("Audit") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToUpload,
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Review")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                // Summary Cards Horizontal Row
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(title = "Total Reviews", count = stats.totalReviews, color = Color(0xFF1565C0))
                    StatCard(title = "Open Reviews", count = stats.openReviews, color = Color(0xFFFFF3E0), countColor = Color(0xFFD97706))
                    StatCard(title = "High Risk Findings", count = stats.highRiskFindings, color = Color(0xFFFBE9E7), countColor = Color(0xFFC62828))
                    StatCard(title = "Compliance Violations", count = stats.complianceViolations, color = Color(0xFFFFF8E1), countColor = Color(0xFFF57F17))
                    StatCard(title = "Pending Approvals", count = stats.pendingApprovals, color = Color(0xFFF3E5F5), countColor = Color(0xFF8E24AA))
                }
            }

            item {
                Text(
                    text = "Recent Reviews",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            if (reviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No reviews found. Click '+' to start.", color = Color.Gray)
                    }
                }
            } else {
                items(reviews.take(10)) { review ->
                    ReviewListItem(review = review, onClick = { onNavigateToReview(review.id) })
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: Int, color: Color, countColor: Color = Color.White) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val labelColor = if (color == Color(0xFF1565C0)) Color.White.copy(alpha = 0.8f) else Color.DarkGray
            Text(text = title, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
            val finalCountColor = if (color == Color(0xFF1565C0)) Color.White else countColor
            Text(text = count.toString(), fontSize = 28.sp, color = finalCountColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReviewListItem(review: Review, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                RiskBadge(riskLevel = review.overallRiskLevel)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Type: ${review.configType} | Provider: ${review.cloudProvider}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.createdAt.split("T").first(),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                ApprovalStatusChip(status = review.status)
            }
        }
    }
}

// Dummy/placeholder annotation class just to solve compilation if OptIn is not defined
annotation class OptInExperimentalMaterial3Api
