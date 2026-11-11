package com.tinsyding.jfzip

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinsyding.jfzip.ui.JFZipTheme
import com.tinsyding.jfzip.ui.MainScreen
import com.tinsyding.jfzip.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            viewModel.handleShareIntent(intent)
        }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            JFZipTheme {
                BackHandler(enabled = state.settingsOpen) {
                    viewModel.closeSettings()
                }

                if (state.settingsOpen) {
                    SettingsScreen(
                        name = state.defaultName,
                        prefix = state.prefix,
                        onNameChange = viewModel::setDefaultName,
                        onBack = viewModel::closeSettings,
                    )
                } else {
                    MainScreen(
                        state = state,
                        onAddUris = viewModel::addUris,
                        onRemoveUri = viewModel::removeUri,
                        onClearUris = viewModel::clearUris,
                        onNameChange = viewModel::setName,
                        onGenerate = viewModel::generate,
                        onOpenSettings = viewModel::openSettings,
                        onMessageShown = viewModel::dismissMessage,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDate()
    }
}
