package com.enterprise.netconfigdiff.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.enterprise.netconfigdiff.presentation.screens.splash.SplashScreen
import com.enterprise.netconfigdiff.presentation.screens.login.LoginScreen
import com.enterprise.netconfigdiff.presentation.screens.dashboard.DashboardScreen
import com.enterprise.netconfigdiff.presentation.screens.upload.UploadConfigScreen
import com.enterprise.netconfigdiff.presentation.screens.diffviewer.DiffViewerScreen
import com.enterprise.netconfigdiff.presentation.screens.aianalysis.AIAnalysisScreen
import com.enterprise.netconfigdiff.presentation.screens.compliance.ComplianceReportScreen
import com.enterprise.netconfigdiff.presentation.screens.approval.ApprovalWorkflowScreen
import com.enterprise.netconfigdiff.presentation.screens.reports.ReportsScreen
import com.enterprise.netconfigdiff.presentation.screens.auditlogs.AuditLogsScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object UploadConfig : Screen("upload_config")
    object DiffViewer : Screen("diff_viewer/{reviewId}") {
        fun passReviewId(reviewId: String) = "diff_viewer/$reviewId"
    }
    object AIAnalysis : Screen("ai_analysis/{reviewId}") {
        fun passReviewId(reviewId: String) = "ai_analysis/$reviewId"
    }
    object ComplianceReport : Screen("compliance_report/{reviewId}") {
        fun passReviewId(reviewId: String) = "compliance_report/$reviewId"
    }
    object ApprovalWorkflow : Screen("approval_workflow/{reviewId}") {
        fun passReviewId(reviewId: String) = "approval_workflow/$reviewId"
    }
    object Reports : Screen("reports/{reviewId}") {
        fun passReviewId(reviewId: String) = "reports/$reviewId"
    }
    object AuditLogs : Screen("audit_logs")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToUpload = { navController.navigate(Screen.UploadConfig.route) },
                onNavigateToAudit = { navController.navigate(Screen.AuditLogs.route) },
                onNavigateToReview = { reviewId -> navController.navigate(Screen.DiffViewer.passReviewId(reviewId)) }
            )
        }
        composable(route = Screen.UploadConfig.route) {
            UploadConfigScreen(
                onNavigateToDiff = { reviewId ->
                    navController.navigate(Screen.DiffViewer.passReviewId(reviewId)) {
                        popUpTo(Screen.UploadConfig.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.DiffViewer.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            DiffViewerScreen(
                reviewId = reviewId,
                onNavigateToAnalysis = { navController.navigate(Screen.AIAnalysis.passReviewId(reviewId)) },
                onNavigateToCompliance = { navController.navigate(Screen.ComplianceReport.passReviewId(reviewId)) },
                onNavigateToApproval = { navController.navigate(Screen.ApprovalWorkflow.passReviewId(reviewId)) },
                onNavigateToReports = { navController.navigate(Screen.Reports.passReviewId(reviewId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AIAnalysis.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            AIAnalysisScreen(reviewId = reviewId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.ComplianceReport.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            ComplianceReportScreen(reviewId = reviewId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.ApprovalWorkflow.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            ApprovalWorkflowScreen(reviewId = reviewId, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Reports.route,
            arguments = listOf(navArgument("reviewId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId") ?: ""
            ReportsScreen(reviewId = reviewId, onBack = { navController.popBackStack() })
        }
        composable(route = Screen.AuditLogs.route) {
            AuditLogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
