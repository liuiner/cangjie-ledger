package com.cangjie.ledger

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.cangjie.ledger.ui.AppViewModel
import com.cangjie.ledger.ui.LedgerApp
import com.cangjie.ledger.ui.theme.CangJieTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val app by lazy { application as LedgerApplication }
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory(app) }
    private val openAddRequests = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD, false)) openAddRequests.value = 1
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val addRequest by openAddRequests.collectAsStateWithLifecycle()
            CangJieTheme(
                themeMode = state.settings.themeMode,
                dynamicColor = state.settings.dynamicColor,
            ) {
                LedgerApp(
                    viewModel = viewModel,
                    openAddRequest = addRequest,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD, false)) {
            openAddRequests.value += 1
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD = "open_add"
    }
}

