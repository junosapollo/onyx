package com.onyx.cashflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onyx.cashflow.data.TransactionType
import com.onyx.cashflow.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.date
    )

    LaunchedEffect(formState.saved) {
        if (formState.saved) {
            onNavigateBack()
            viewModel.resetForm()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                    showDatePicker = false
                }) { Text("OK", letterSpacing = 1.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", letterSpacing = 1.sp)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ADD TRANSACTION",
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Type toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val selected = formState.type == type
                    val color = if (type == TransactionType.EXPENSE)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.secondary

                    OutlinedButton(
                        onClick = { viewModel.updateType(type) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) color else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            if (type == TransactionType.EXPENSE) Icons.Default.ArrowDownward
                            else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            type.name.uppercase(),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("AMOUNT (₹)", letterSpacing = 1.sp) },
                leadingIcon = {
                    Text(
                        "₹",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                isError = formState.error?.contains("amount", ignoreCase = true) == true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Category selection
            Text(
                "CATEGORY",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val selected = formState.categoryId == category.id
                    val catColor = Color(category.color)

                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateCategory(category.id) },
                        label = {
                            Text(
                                category.name.uppercase(),
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor.copy(alpha = 0.15f),
                            selectedLabelColor = catColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selected) catColor else MaterialTheme.colorScheme.outline,
                            selectedBorderColor = catColor,
                            borderWidth = if (selected) 2.dp else 1.dp,
                            selectedBorderWidth = 2.dp,
                            enabled = true,
                            selected = selected
                        )
                    )
                }
            }

            // Note
            OutlinedTextField(
                value = formState.note,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("NOTE (OPTIONAL)", letterSpacing = 1.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Notes,
                        "Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // Date
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        "Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "DATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Text(
                            dateFormat.format(Date(formState.date)).uppercase(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error
            formState.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SAVE TRANSACTION",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}
