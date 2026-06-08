package com.enterprise.netconfigdiff.presentation.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.enterprise.netconfigdiff.data.remote.interceptors.TokenManager
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    tokenManager: TokenManager? = null, # Inject or fallback
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000L) // Duration: 2000ms minimum
        val token = tokenManager?.getAccessToken()
        // For testing, since we don't have Hilt running right now, we can default to navigate to Login
        // which matches specs if token is invalid or missing.
        if (!token.isNullOrEmpty()) {
            onNavigateToDashboard()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AI Network Config Reviewer",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
