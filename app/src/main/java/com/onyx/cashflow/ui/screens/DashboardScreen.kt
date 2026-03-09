package com.onyx.cashflow.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onyx.cashflow.data.BalanceGap
import com.onyx.cashflow.data.Category
import com.onyx.cashflow.data.CategoryTotal
import com.onyx.cashflow.data.Transaction
import com.onyx.cashflow.data.TransactionType
import com.onyx.cashflow.viewmodel.BalanceGapViewModel
import com.onyx.cashflow.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    balanceGapViewModel: BalanceGapViewModel,
    onAddTransaction: () -> Unit
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val editingTransaction by viewModel.editingTransaction.collectAsState()
    val unresolvedGaps by balanceGapViewModel.unresolvedGaps.collectAsState()

    // State for the resolve-gap dialog
    var resolvingGap by remember { mutableStateOf<BalanceGap?>(null) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
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

    // Resolve gap dialog
    resolvingGap?.let { gap ->
        ResolveGapDialog(
            gap = gap,
            categories = categories,
            currencyFormat = currencyFormat,
            onDismiss = { resolvingGap = null },
            onResolve = { categoryId, note ->
                balanceGapViewModel.resolveWithTransaction(gap, categoryId, note)
                resolvingGap = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month selector
            item {
                MonthSelector(
                    monthLabel = monthFormat.format(selectedMonth.time),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Spent",
                        amount = currencyFormat.format(totalExpenses),
                        color = MaterialTheme.colorScheme.error
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Earned",
                        amount = currencyFormat.format(totalIncome),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Balance card
            item {
                val balance = totalIncome - totalExpenses
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Balance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            currencyFormat.format(balance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Balance gap alerts
            if (unresolvedGaps.isNotEmpty()) {
                item {
                    Text(
                        "⚠\uFE0F Balance Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(unresolvedGaps, key = { it.id }) { gap ->
                    BalanceGapCard(
                        gap = gap,
                        currencyFormat = currencyFormat,
                        onAddTransaction = { resolvingGap = gap },
                        onDismiss = { balanceGapViewModel.dismissGap(gap.id) }
                    )
                }
            }

            // Pie chart
            if (categoryTotals.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Spending by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(16.dp))
                            DonutChart(
                                data = categoryTotals,
                                total = totalExpenses,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            categoryTotals.forEach { ct ->
                                CategoryLegendItem(
                                    name = ct.categoryName ?: "Unknown",
                                    color = Color(ct.categoryColor ?: 0xFF78909C),
                                    amount = currencyFormat.format(ct.total),
                                    percentage = if (totalExpenses > 0) (ct.total / totalExpenses * 100).toInt() else 0
                                )
                            }
                        }
                    }
                }
            }

            // Recent transactions header
            item {
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                "Tap + to add your first expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            items(recentTransactions, key = { it.id }) { transaction ->
                val category = categories.find { it.id == transaction.categoryId }
                TransactionItem(
                    transaction = transaction,
                    category = category,
                    currencyFormat = currencyFormat,
                    onClick = { viewModel.startEditCategory(transaction) }
                )
            }

            // Bottom spacer for FAB
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun MonthSelector(
    monthLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, "Previous month")
        }
        Text(
            monthLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, "Next month")
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DonutChart(
    data: List<CategoryTotal>,
    total: Double,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "chartAnimation"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 40.dp.toPx()
        val radius = (minOf(size.width, size.height) - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        val rect = Size(radius * 2, radius * 2)
        val topLeft = Offset(center.x - radius, center.y - radius)

        var startAngle = -90f
        data.forEach { ct ->
            val sweep = if (total > 0) (ct.total / total * 360f * animationProgress).toFloat() else 0f
            drawArc(
                color = Color(ct.categoryColor ?: 0xFF78909C),
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryLegendItem(
    name: String,
    color: Color,
    amount: String,
    percentage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$percentage%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    currencyFormat: NumberFormat,
    onClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isExpense = transaction.type == TransactionType.EXPENSE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Color(category?.color ?: 0xFF78909C).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color(category?.color ?: 0xFF78909C),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Text(
                "${if (isExpense) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary
            )
        }
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
        title = { Text("Change Category") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    val isSelected = category.id == currentCategoryId
                    val catColor = Color(category.color)

                    Surface(
                        onClick = { onSelectCategory(category.id) },
                        shape = RoundedCornerShape(12.dp),
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
                                style = MaterialTheme.typography.bodyLarge,
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BalanceGapCard(
    gap: BalanceGap,
    currencyFormat: NumberFormat,
    onAddTransaction: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isExpense = gap.gapType == TransactionType.EXPENSE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Gap alert",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isExpense) "Missed Expense" else "Missed Income",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "A/c ••••${gap.accountId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${if (isExpense) "-" else "+"}${currencyFormat.format(gap.gapAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Detected ${dateFormat.format(Date(gap.detectedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onAddTransaction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Transaction")
                }
            }
        }
    }
}

@Composable
private fun ResolveGapDialog(
    gap: BalanceGap,
    categories: List<Category>,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onResolve: (categoryId: Long?, note: String) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    val isExpense = gap.gapType == TransactionType.EXPENSE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (isExpense) "Add Missed Expense" else "Add Missed Income")
                Text(
                    currencyFormat.format(gap.gapAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column {
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val isSelected = category.id == selectedCategoryId
                        val catColor = Color(category.color)

                        Surface(
                            onClick = { selectedCategoryId = category.id },
                            shape = RoundedCornerShape(12.dp),
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve(selectedCategoryId, note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
