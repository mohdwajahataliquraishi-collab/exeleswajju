package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Input
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SideCalculatorDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        color = Midnight800,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        shadowElevation = 16.dp,
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("side_calculator_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACCOUNTING CALCULATOR",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Box
            Surface(
                color = Midnight900,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Midnight700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = uiState.calcExpression.ifBlank { "0" },
                        color = Slate400,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹ ${uiState.calcResult}",
                        color = CyanBright,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "Insert to Selected Cell" Primary Action
            Button(
                onClick = { viewModel.handleCalcInput("INSERT_TO_CELL") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("insert_calc_result_to_cell_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Input,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Insert Result Into Cell",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keypad Grid
            val keys = listOf(
                listOf("C", "(", ")", "/"),
                listOf("7", "8", "9", "*"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "DEL", "=")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { k ->
                            val isOp = k in listOf("/", "*", "-", "+", "=")
                            val isAction = k in listOf("C", "DEL", "(", ")")

                            Surface(
                                color = when {
                                    k == "=" -> EmeraldDark
                                    isOp -> VioletPrimary
                                    isAction -> Midnight700
                                    else -> Midnight600
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { viewModel.handleCalcInput(k) }
                                    .testTag("calc_key_$k")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = k,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calculation History
            Text(
                text = "CALCULATION HISTORY",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (uiState.calcHistory.isEmpty()) {
                    item {
                        Text(
                            text = "No calculations yet",
                            color = Slate500,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(uiState.calcHistory) { hist ->
                        Surface(
                            color = Midnight900,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = hist,
                                color = Slate200,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
