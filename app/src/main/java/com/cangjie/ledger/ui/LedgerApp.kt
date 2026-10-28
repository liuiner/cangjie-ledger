package com.cangjie.ledger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cangjie.ledger.ui.books.BooksScreen
import com.cangjie.ledger.ui.budget.BudgetScreen
import com.cangjie.ledger.ui.data.DataScreen
import com.cangjie.ledger.ui.home.HomeScreen
import com.cangjie.ledger.ui.record.RecordEditorScreen
import com.cangjie.ledger.ui.recurring.RecurringScreen
import com.cangjie.ledger.ui.search.SearchScreen
import com.cangjie.ledger.ui.settings.SettingsScreen
import com.cangjie.ledger.ui.stats.StatsScreen

private object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val BUDGET = "budget"
    const val SETTINGS = "settings"
    const val ADD = "record/new"
    const val EDIT = "record/{recordId}"
    const val SEARCH = "search"
    const val BOOKS = "books"
    const val RECURRING = "recurring"
    const val DATA = "data"

    fun edit(recordId: Long) = "record/$recordId"
}

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(Routes.HOME, "账单", Icons.Rounded.ReceiptLong),
    TopDestination(Routes.STATS, "统计", Icons.Rounded.Insights),
    TopDestination(Routes.BUDGET, "预算", Icons.Rounded.PieChart),
    TopDestination(Routes.SETTINGS, "设置", Icons.Rounded.Settings),
)

@Composable
fun LedgerApp(
    viewModel: AppViewModel,
    openAddRequest: Int,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = topDestinations.any { it.route == currentRoute }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(value)
        viewModel.clearMessage()
    }
    LaunchedEffect(openAddRequest) {
        if (openAddRequest > 0 && currentRoute != Routes.ADD) {
            navController.navigate(Routes.ADD)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topDestinations.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("记一笔") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        state = state,
                        onOpenRecord = { navController.navigate(Routes.edit(it)) },
                        onSearch = { navController.navigate(Routes.SEARCH) },
                        onManageBooks = { navController.navigate(Routes.BOOKS) },
                        onSelectBook = viewModel::setDefaultBook,
                    )
                }
                composable(Routes.STATS) {
                    StatsScreen(
                        state = state,
                        onOpenRecord = { navController.navigate(Routes.edit(it)) },
                    )
                }
                composable(Routes.BUDGET) {
                    BudgetScreen(
                        state = state,
                        onSaveBudget = viewModel::saveBudget,
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = state,
                        viewModel = viewModel,
                        onOpenBooks = { navController.navigate(Routes.BOOKS) },
                        onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                        onOpenData = { navController.navigate(Routes.DATA) },
                    )
                }
                composable(Routes.ADD) {
                    RecordEditorScreen(
                        state = state,
                        recordId = null,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.EDIT,
                    arguments = listOf(navArgument("recordId") { type = androidx.navigation.NavType.LongType }),
                ) { entry ->
                    RecordEditorScreen(
                        state = state,
                        recordId = entry.arguments?.getLong("recordId"),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onOpenRecord = { navController.navigate(Routes.edit(it)) },
                    )
                }
                composable(Routes.BOOKS) {
                    BooksScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.RECURRING) {
                    RecurringScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.DATA) {
                    DataScreen(
                        state = state,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

