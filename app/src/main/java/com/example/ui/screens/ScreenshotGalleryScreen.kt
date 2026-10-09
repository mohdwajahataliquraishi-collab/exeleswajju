package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.GeneratedScreenshot
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotGalleryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var fullPreviewScreenshot by remember { mutableStateOf<GeneratedScreenshot?>(null) }

    // System Back Handler
    BackHandler {
        viewModel.navigateTo(AppScreen.WORKSPACE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MONTHLY SCREENSHOT GALLERY",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${uiState.workbook.monthYear} • Individual Accounting Statements",
                            color = CyanBright,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.WORKSPACE) },
                        modifier = Modifier.testTag("gallery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Workspace",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openScreenshotGallery() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Screenshots",
                            tint = CyanBright
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Midnight800)
            )
        },
        bottomBar = {
            Surface(
                color = Midnight800,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Excel Button
                    OutlinedButton(
                        onClick = { viewModel.exportToExcel(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gallery_export_excel_button")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Primary Share Selected Button
                    val selectedCount = uiState.selectedScreenshotNames.size
                    Button(
                        onClick = { viewModel.shareSelectedScreenshots(context) },
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldDark,
                            disabledContainerColor = Slate700
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("share_selected_screenshots_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedCount > 0) "Share Selected ($selectedCount)" else "Select Items",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        containerColor = IvoryWhite
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Selection Controls Header: Select All / Deselect All + Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.selectAllScreenshots() },
                        modifier = Modifier.testTag("select_all_button")
                    ) {
                        Text("Select All", color = VioletPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("|", color = Slate400)
                    TextButton(
                        onClick = { viewModel.deselectAllScreenshots() },
                        modifier = Modifier.testTag("deselect_all_button")
                    ) {
                        Text("Deselect All", color = Slate500, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }

                Surface(
                    color = Midnight700,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${uiState.selectedScreenshotNames.size} of ${uiState.generatedScreenshots.size} Selected",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.isGeneratingScreenshots) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VioletPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Generating individual bitmap accounting statements...",
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (uiState.generatedScreenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No accounting boxes found in this month.",
                            color = Slate700,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create client boxes in the spreadsheet first (e.g. 'Salman ka box banao').",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.generatedScreenshots, key = { it.personName }) { item ->
                        val isSelected = uiState.selectedScreenshotNames.contains(item.personName)
                        ScreenshotCard(
                            item = item,
                            isSelected = isSelected,
                            onToggleSelection = { viewModel.toggleScreenshotSelection(item.personName) },
                            onPreviewClick = { fullPreviewScreenshot = item }
                        )
                    }
                }
            }
        }
    }

    // High-Resolution Preview Dialog
    fullPreviewScreenshot?.let { previewItem ->
        Dialog(onDismissRequest = { fullPreviewScreenshot = null }) {
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = previewItem.personName.uppercase(Locale.ROOT),
                                fontWeight = FontWeight.Bold,
                                color = Midnight900,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${previewItem.monthYear} • Statement Preview",
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { fullPreviewScreenshot = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Image(
                        bitmap = previewItem.bitmap.asImageBitmap(),
                        contentDescription = "Full Preview for ${previewItem.personName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.shareSelectedScreenshots(context)
                            fullPreviewScreenshot = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = PureWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share to WhatsApp / Boss", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotCard(
    item: GeneratedScreenshot,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EmeraldDark else Slate200
        ),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .testTag("screenshot_card_${item.personName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldDark,
                    uncheckedColor = Slate400
                ),
                modifier = Modifier.testTag("checkbox_${item.personName}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Real Bitmap Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Slate300, RoundedCornerShape(6.dp))
                    .clickable { onPreviewClick() }
            ) {
                Image(
                    bitmap = item.bitmap.asImageBitmap(),
                    contentDescription = "Thumbnail for ${item.personName}",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.personName,
                        fontWeight = FontWeight.Bold,
                        color = Midnight900,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = EmeraldLight,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✓ Real PNG",
                            color = EmeraldDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.monthYear} • Statement",
                    color = Slate500,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Charbi: ₹${formatAmount(item.totalCharbi)}",
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Jama: ₹${formatAmount(item.totalJama)}",
                        color = EmeraldDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Baki: ₹${formatAmount(item.currentBalance)}",
                        color = Midnight900,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Preview icon button
            IconButton(
                onClick = onPreviewClick,
                modifier = Modifier.testTag("preview_button_${item.personName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Preview",
                    tint = VioletPrimary
                )
            }
        }
    }
}

private fun formatAmount(amt: Double): String {
    return if (amt % 1.0 == 0.0) String.format(Locale.US, "%,d", amt.toLong()) else String.format(Locale.US, "%,.2f", amt)
}
