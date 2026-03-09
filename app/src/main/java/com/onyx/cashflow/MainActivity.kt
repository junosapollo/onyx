package com.onyx.cashflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onyx.cashflow.ui.screens.AddTransactionScreen
import com.onyx.cashflow.ui.screens.CategoriesScreen
import com.onyx.cashflow.ui.screens.DashboardScreen
import com.onyx.cashflow.ui.screens.PendingScreen
import com.onyx.cashflow.ui.theme.CashFlowTheme
import com.onyx.cashflow.viewmodel.CategoryViewModel
import com.onyx.cashflow.viewmodel.DashboardViewModel
import com.onyx.cashflow.viewmodel.BalanceGapViewModel
import com.onyx.cashflow.viewmodel.PendingViewModel
import com.onyx.cashflow.viewmodel.TransactionViewModel

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Categories : Screen("categories")
    data object Pending : Screen("pending")
    data object AddTransaction : Screen("add_transaction")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — app works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermissionIfNeeded()
        setContent {
            CashFlowTheme {
                CashFlowApp()
            }
        }
    }

    private fun requestSmsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }
    }
}

@Composable
fun CashFlowApp() {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val transactionViewModel: TransactionViewModel = viewModel()
    val categoryViewModel: CategoryViewModel = viewModel()
    val pendingViewModel: PendingViewModel = viewModel()
    val balanceGapViewModel: BalanceGapViewModel = viewModel()

    val pendingCount by pendingViewModel.pendingCount.collectAsState()

    val navItems = listOf(
        BottomNavItem(Screen.Dashboard, "DASH", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        BottomNavItem(Screen.Pending, "PENDING", Icons.Filled.Sms, Icons.Outlined.Sms),
        BottomNavItem(Screen.Categories, "CATEGORIES", Icons.Filled.Category, Icons.Outlined.Category)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Dashboard.route,
        Screen.Categories.route,
        Screen.Pending.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                if (item.screen == Screen.Pending && pendingCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(
                                                    "$pendingCount",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                }
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    balanceGapViewModel = balanceGapViewModel,
                    onAddTransaction = {
                        transactionViewModel.resetForm()
                        navController.navigate(Screen.AddTransaction.route)
                    }
                )
            }

            composable(Screen.Pending.route) {
                PendingScreen(viewModel = pendingViewModel)
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(viewModel = categoryViewModel)
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    viewModel = transactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
