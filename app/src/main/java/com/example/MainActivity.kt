package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ScreenshotGalleryScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                // Audio permission launcher for speech recognition
                val audioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.voiceManager.startListening()
                    }
                }

                LaunchedEffect(Unit) {
                    val hasRecordAudio = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasRecordAudio) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                Surface(
                    color = IvoryWhite,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (uiState.currentScreen) {
                        AppScreen.WORKSPACE -> {
                            WorkspaceScreen(viewModel = viewModel)
                        }
                        AppScreen.SCREENSHOT_GALLERY -> {
                            ScreenshotGalleryScreen(viewModel = viewModel)
                        }
                        AppScreen.MONTHLY_ARCHIVE -> {
                            WorkspaceScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
