package com.onyx.cashflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onyx.cashflow.data.Category
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import com.onyx.cashflow.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddTransaction: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val editingTransaction by viewModel.editingTransaction.collectAsState()

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Category edit dialog
    editingTransaction?.let { transaction ->
        EditCategoryDialog(
            currentCategoryId = transaction.categoryId,
            categories = categories,
            onDismiss = viewModel::dismissEditCategory,
            onSelectCategory = viewModel::updateTransactionCategory
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp), // padding for FAB
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "a",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Text(
                        "Hey, apolloJ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        letterSpacing = 1.sp
                    )

                    IconButton(onClick = { /* TODO settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Cards - Horizontal Scroll
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SummaryCard(
                            title = "Today's\nTransactions",
                            amount = currencyFormat.format(0.0) // Placeholder logic
                        )
                    }
                    item {
                        SummaryCard(
                            title = "Yesterday's\nTransactions",
                            amount = currencyFormat.format(0.0) // Placeholder logic
                        )
                    }
                    item {
                        SummaryCard(
                            title = "Mar\nTransactions",
                            amount = currencyFormat.format(totalExpenses) // Using expense total for now
                        )
                    }
                }
            }

            // Transactions Header
            item {
                Text(
                    "All Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // Green accent
                    modifier = Modifier.padding(horizontal = 20.dp, top = 8.dp),
                    letterSpacing = 0.5.sp
                )
            }

            if (recentTransactions.isEmpty()) {
                item {
                    // Empty state matching the reference image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                // Crossed Wallet Graphic
                                Icon(
                                    Icons.Default.Wallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                                // Green accent element behind or overriding
                                Icon(
                                    Icons.Default.Add, // Placeholder for the green flash in the wallet
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .offset(x = (-16).dp, y = (-16).dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                // Cross icon
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .offset(x = 24.dp, y = (-24).dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            
                            Text(
                                "zero Transactions",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { transaction ->
                    val category = categories.find { it.id == transaction.categoryId }
                    TransactionItemRow(
                        transaction = transaction,
                        category = category,
                        currencyFormat = currencyFormat,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { viewModel.startEditCategory(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String
) {
    // Outer rounded card
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Calendar icon placeholder (top left)
            Icon(
                Icons.Default.Add, // Placeholder for calendar icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            // Title
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            // Inner dark amount box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    amount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TransactionItemRow(
    transaction: Transaction,
    category: Category?,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isExpense = transaction.type == TransactionType.EXPENSE

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(category?.color ?: 0xFF424242).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = Color(category?.color ?: 0xFFAAAAAA),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                category?.name ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (transaction.note.isNotBlank()) {
                Text(
                    transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                dateFormat.format(Date(transaction.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Text(
            "${if (isExpense) "-" else "+"}${currencyFormat.format(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EditCategoryDialog(
    currentCategoryId: Long?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSelectCategory: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Category")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    val isSelected = category.id == currentCategoryId
                    val catColor = Color(category.color)

                    Surface(
                        onClick = { onSelectCategory(category.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) catColor.copy(alpha = 0.15f)
                        else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
