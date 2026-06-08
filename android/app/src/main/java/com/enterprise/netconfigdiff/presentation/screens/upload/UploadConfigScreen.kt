package com.enterprise.netconfigdiff.presentation.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
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
import com.enterprise.netconfigdiff.data.remote.api.NetConfigApiService
import com.enterprise.netconfigdiff.data.remote.dto.CreateDiffRequestDto
import com.enterprise.netconfigdiff.domain.repository.UploadRepository
import com.enterprise.netconfigdiff.presentation.components.LoadingOverlay
import com.enterprise.netconfigdiff.presentation.theme.PrimaryColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// UI State
sealed class UploadUiState {
    object Step1 : UploadUiState()
    object Step2 : UploadUiState()
    data class Submitting(val message: String) : UploadUiState()
    data class Success(val reviewId: String) : UploadUiState()
    data class Error(val error: String) : UploadUiState()
}

// ViewModel
@HiltViewModel
class UploadConfigViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
    private val apiService: NetConfigApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Step1)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun setStep(step: Int) {
        _uiState.value = if (step == 1) UploadUiState.Step1 else UploadUiState.Step2
    }

    fun submitReview(
        oldFile: File,
        newFile: File,
        configType: String,
        cloudProvider: String,
        frameworks: List<String>,
        title: String,
        ticketId: String?,
        description: String?,
        autoApprove: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = UploadUiState.Submitting("Uploading files...")
            
            val uploadResult = uploadRepository.uploadConfigurationFiles(oldFile, newFile, configType, cloudProvider)
            uploadResult.onSuccess { uploadId ->
                _uiState.value = UploadUiState.Submitting("Starting analysis pipeline...")
                val diffRequest = CreateDiffRequestDto(
                    uploadId = uploadId,
                    complianceFrameworks = frameworks,
                    title = title,
                    ticketId = ticketId,
                    description = description,
                    autoApprove = autoApprove
                )
                try {
                    val response = apiService.createDiff(diffRequest)
                    if (response.isSuccessful && response.body() != null) {
                        val reviewId = response.body()!!.reviewId
                        _uiState.value = UploadUiState.Submitting("Analyzing changes...")
                        
                        // Poll status
                        var status = "UNDER_ANALYSIS"
                        while (status == "UNDER_ANALYSIS" || status == "DRAFT") {
                            delay(2000L)
                            val statusResponse = apiService.getReviewStatus(reviewId)
                            if (statusResponse.isSuccessful && statusResponse.body() != null) {
                                status = statusResponse.body()!!.status
                            }
                        }
                        
                        if (status == "FAILED") {
                            _uiState.value = UploadUiState.Error("Analysis pipeline failed. Verify configuration contents.")
                        } else {
                            _uiState.value = UploadUiState.Success(reviewId)
                        }
                    } else {
                        _uiState.value = UploadUiState.Error("Failed to initiate configuration review job.")
                    }
                } catch (e: Exception) {
                    _uiState.value = UploadUiState.Error(e.message ?: "Network error during analysis run.")
                }
            }.onFailure { error ->
                _uiState.value = UploadUiState.Error(error.message ?: "Failed to upload configuration files.")
            }
        }
    }
}

// Composable View
@Composable
fun UploadConfigScreen(
    viewModel: UploadConfigViewModel = hiltViewModel(),
    onNavigateToDiff: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var oldFileUri by remember { mutableStateOf<Uri?>(null) }
    var newFileUri by remember { mutableStateOf<Uri?>(null) }
    var oldFileName by remember { mutableStateOf("No file chosen") }
    var newFileName by remember { mutableStateOf("No file chosen") }

    var configType by remember { mutableStateOf("AWS_SECURITY_GROUP") }
    var cloudProvider by remember { mutableStateOf("AWS") }

    var title by remember { mutableStateOf("") }
    var ticketId by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var autoApprove by remember { mutableStateOf(false) }

    val selectedFrameworks = remember { mutableStateListOf("CIS") }

    val oldPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            oldFileUri = uri
            oldFileName = uri.path?.substringAfterLast("/") ?: "selected_file"
        }
    }
    val newPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            newFileUri = uri
            newFileName = uri.path?.substringAfterLast("/") ?: "selected_file"
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UploadUiState.Success) {
            onNavigateToDiff((uiState as UploadUiState.Success).reviewId)
        }
    }

    Scaffold(
        topBar = {
            OptInExperimentalMaterial3Api {
                TopAppBar(
                    title = { Text("New Configuration Review") },
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
            when (uiState) {
                is UploadUiState.Step1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Step 1: Upload configuration files", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Old file
                        FileSelectorCard(title = "Old Config File", fileName = oldFileName, onClick = { oldPicker.launch("*/*") })
                        Spacer(modifier = Modifier.height(16.dp))

                        // New file
                        FileSelectorCard(title = "New Config File", fileName = newFileName, onClick = { newPicker.launch("*/*") })
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Select Configuration Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val configTypes = listOf("AWS_SECURITY_GROUP", "AWS_NACL", "AWS_ROUTE_TABLE", "AWS_VPN", "GCP_FIREWALL", "GCP_ROUTES", "AZURE_NSG", "TERRAFORM_IAC", "GENERIC_JSON", "GENERIC_YAML")
                        var showConfigDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = configType,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { showConfigDropdown = true },
                                trailingIcon = {
                                    Text("▼", modifier = Modifier.clickable { showConfigDropdown = true })
                                }
                            )
                            DropdownMenu(expanded = showConfigDropdown, onDismissRequest = { showConfigDropdown = false }) {
                                configTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { configType = type; showConfigDropdown = false })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.setStep(2) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text("Next", color = Color.White)
                        }
                    }
                }
                is UploadUiState.Step2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Step 2: Configure Review Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Review Title (required)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ticketId,
                            onValueChange = { ticketId = it },
                            label = { Text("Change Ticket / JIRA ID (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Text("Select Compliance Frameworks", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val frameworks = listOf("CIS", "NIST", "PCI-DSS", "Custom Policy")
                        frameworks.forEach { fw ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (selectedFrameworks.contains(fw)) selectedFrameworks.remove(fw) else selectedFrameworks.add(fw)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedFrameworks.contains(fw),
                                    onCheckedChange = {
                                        if (it) selectedFrameworks.add(fw) else selectedFrameworks.remove(fw)
                                    }
                                )
                                Text(text = fw)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(checked = autoApprove, onCheckedChange = { autoApprove = it })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Auto-Approve if Risk Level is LOW")
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { viewModel.setStep(1) }, modifier = Modifier.weight(1f)) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (title.isNotEmpty()) {
                                        // Write dummy files if Uris are null for simulation/testing
                                        val oldFile = createTempFileFromUri(context, oldFileUri, "old_sg_temp.json", "{}")
                                        val newFile = createTempFileFromUri(context, newFileUri, "new_sg_temp.json", "{}")
                                        
                                        viewModel.submitReview(
                                            oldFile, newFile, configType, cloudProvider,
                                            selectedFrameworks.toList(), title, ticketId, description, autoApprove
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                enabled = title.isNotEmpty()
                            ) {
                                Text("Submit", color = Color.White)
                            }
                        }
                    }
                }
                is UploadUiState.Submitting -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryColor)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text((uiState as UploadUiState.Submitting).message, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                is UploadUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error Running Review", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text((uiState as UploadUiState.Error).error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.setStep(1) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                            Text("Try Again", color = Color.White)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun FileSelectorCard(title: String, fileName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = fileName, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

fun createTempFileFromUri(context: android.content.Context, uri: Uri?, defaultName: String, fallbackContent: String): File {
    val file = File(context.cacheDir, defaultName)
    if (uri == null) {
        FileOutputStream(file).use { it.write(fallbackContent.toByteArray()) }
        return file
    }
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}
