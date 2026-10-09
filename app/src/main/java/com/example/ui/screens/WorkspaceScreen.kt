package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.GridBackground
import com.example.ui.theme.Midnight900
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                TopBar(viewModel = viewModel)
            },
            bottomBar = {
                BottomWorksheetTabs(viewModel = viewModel)
            },
            containerColor = GridBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isExpandedScreen) {
                    // Expanded screen (Tablet / Desktop): Voice on left, Spreadsheet in centre, Design on right
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Voice Quick Controls
                        Surface(
                            color = Midnight900,
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                VoiceFloatingControl(viewModel = viewModel)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Voice Active", color = Color.White)
                            }
                        }

                        // Center Spreadsheet
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            FormulaBar(viewModel = viewModel)
                            SpreadsheetGrid(
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Right Design Studio / Calculator Pane
                        Surface(
                            color = Midnight900,
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                        ) {
                            DesignStudioDrawer(
                                viewModel = viewModel,
                                onClose = { viewModel.toggleDesignStudio() }
                            )
                        }
                    }
                } else {
                    // Mobile layout: Spreadsheet is primary workspace!
                    Column(modifier = Modifier.fillMaxSize()) {
                        FormulaBar(viewModel = viewModel)
                        SpreadsheetGrid(
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Floating Voice Button in Bottom Right Corner above tabs
                    VoiceFloatingControl(
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                    )

                    // Expandable Side Calculator Drawer (Mobile)
                    AnimatedVisibility(
                        visible = uiState.isCalculatorOpen,
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        SideCalculatorDrawer(
                            viewModel = viewModel,
                            onClose = { viewModel.toggleCalculator() }
                        )
                    }

                    // Expandable Design Studio Drawer (Mobile)
                    AnimatedVisibility(
                        visible = uiState.isDesignStudioOpen,
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        DesignStudioDrawer(
                            viewModel = viewModel,
                            onClose = { viewModel.toggleDesignStudio() }
                        )
                    }
                }

                // Voice Assistant Bottom Sheet Dialog
                if (uiState.isVoiceDialogOpen) {
                    VoiceAssistantDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.toggleVoiceDialog(false) }
                    )
                }

                // Month-End Validation Dialog
                if (uiState.isValidationDialogOpen) {
                    MonthValidationDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissValidationDialog() }
                    )
                }
            }
        }
    }
}
