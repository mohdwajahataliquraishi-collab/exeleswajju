package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuditIssue
import com.example.ui.viewmodel.IssueType
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MonthValidationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val issues = uiState.validationIssues

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("month_validation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (issues.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (issues.isEmpty()) EmeraldGreen else GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MONTH-END VALIDATION",
                                color = Midnight900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = uiState.workbook.monthYear,
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (issues.isEmpty()) {
                    // Success State: 100% verified
                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "100% Mathematically Verified",
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All dates, formulas, charbi entries, and balances match perfectly without discrepancies.",
                                color = Slate700,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Issues List
                    Text(
                        text = "Found ${issues.size} items requiring attention:",
                        color = Slate700,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(issues) { issue ->
                            AuditIssueCard(issue = issue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss", color = Slate700)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.openScreenshotGallery()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        modifier = Modifier.weight(1.5f).testTag("finalize_and_open_gallery_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Gallery", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditIssueCard(issue: AuditIssue) {
    Surface(
        color = GridHeaderBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = when (issue.type) {
                    IssueType.CALCULATION_DISCREPANCY -> Color(0xFFDC2626)
                    else -> GoldAccent
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${issue.personName}: ${issue.type.name.replace("_", " ")}",
                    fontWeight = FontWeight.Bold,
                    color = Midnight900,
                    fontSize = 12.sp
                )
                Text(
                    text = issue.message,
                    color = Slate700,
                    fontSize = 11.sp
                )
            }
        }
    }
}
