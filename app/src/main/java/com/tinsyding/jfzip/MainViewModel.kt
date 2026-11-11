package com.tinsyding.jfzip

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinsyding.jfzip.data.SettingsRepository
import com.tinsyding.jfzip.domain.ZipBuilder
import com.tinsyding.jfzip.domain.archiveFileName
import com.tinsyding.jfzip.domain.datePrefix
import com.tinsyding.jfzip.util.imageUris
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class ReadyArchive(
    val file: File,
    val name: String,
    val size: Long,
    val count: Int,
)

sealed interface BuildState {
    data object Idle : BuildState
    data class Building(val done: Int, val total: Int) : BuildState
    data class Ready(val archive: ReadyArchive) : BuildState
}

data class UiState(
    val uris: List<Uri> = emptyList(),
    val name: String = "",
    val nameEdited: Boolean = false,
    val defaultName: String = "",
    val prefix: String = datePrefix(),
    val build: BuildState = BuildState.Idle,
    val settingsOpen: Boolean = false,
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val resolver = application.contentResolver

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var buildJob: Job? = null
    private var settingsSaveJob: Job? = null
    private var settingsTouched = false

    init {
        clearExports()
        viewModelScope.launch {
            val savedName = settingsRepository.name.first()
            if (!settingsTouched) {
                _state.update { current ->
                    current.copy(
                        defaultName = savedName,
                        name = if (current.nameEdited) current.name else savedName,
                    )
                }
            }
        }
    }

    fun handleShareIntent(intent: Intent?) {
        val incoming = intent.imageUris()
        if (incoming.isEmpty()) return

        buildJob?.cancel()
        clearExports()
        _state.update { current ->
            current.copy(
                uris = incoming,
                name = current.defaultName,
                nameEdited = false,
                build = BuildState.Idle,
                settingsOpen = false,
                message = null,
            )
        }
    }

    fun refreshDate() {
        val today = datePrefix()
        _state.update { current ->
            if (current.build is BuildState.Ready || current.prefix == today) {
                current
            } else {
                current.copy(prefix = today)
            }
        }
    }

    fun addUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        buildJob?.cancel()
        _state.update { current ->
            current.copy(
                uris = (current.uris + uris).distinct(),
                build = BuildState.Idle,
                message = null,
            )
        }
    }

    fun removeUri(uri: Uri) {
        buildJob?.cancel()
        _state.update { current ->
            current.copy(
                uris = current.uris - uri,
                build = BuildState.Idle,
            )
        }
    }

    fun clearUris() {
        buildJob?.cancel()
        clearExports()
        _state.update { current ->
            current.copy(
                uris = emptyList(),
                build = BuildState.Idle,
                message = null,
            )
        }
    }

    fun setName(value: String) {
        _state.update { current ->
            current.copy(name = value, nameEdited = true)
        }
    }

    fun setDefaultName(value: String) {
        settingsTouched = true
        _state.update { current ->
            current.copy(
                defaultName = value,
                name = if (current.nameEdited) current.name else value,
            )
        }
        settingsSaveJob?.cancel()
        settingsSaveJob = viewModelScope.launch {
            delay(200)
            settingsRepository.setName(value)
        }
    }

    fun openSettings() {
        _state.update { it.copy(settingsOpen = true) }
    }

    fun closeSettings() {
        settingsSaveJob?.cancel()
        if (settingsTouched) {
            val value = _state.value.defaultName
            viewModelScope.launch {
                settingsRepository.setName(value)
            }
        }
        _state.update { it.copy(settingsOpen = false) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun generate() {
        val snapshot = _state.value
        if (snapshot.uris.isEmpty() || snapshot.build is BuildState.Building) return

        buildJob?.cancel()
        val prefix = datePrefix()
        val fileName = archiveFileName(prefix, snapshot.name)

        buildJob = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    prefix = prefix,
                    build = BuildState.Building(0, snapshot.uris.size),
                    message = null,
                )
            }

            val directory = File(getApplication<Application>().cacheDir, "exports").apply {
                mkdirs()
            }
            directory.listFiles()?.forEach { it.delete() }

            val output = File(directory, fileName)
            try {
                ZipBuilder.build(resolver, snapshot.uris, prefix, output) { done ->
                    _state.update { current ->
                        val building = current.build
                        if (building is BuildState.Building) {
                            current.copy(build = building.copy(done = done))
                        } else {
                            current
                        }
                    }
                }

                if (!isActive) {
                    output.delete()
                    return@launch
                }

                _state.update {
                    it.copy(
                        build = BuildState.Ready(
                            ReadyArchive(
                                file = output,
                                name = fileName,
                                size = output.length(),
                                count = snapshot.uris.size,
                            ),
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                output.delete()
                throw cancelled
            } catch (error: Throwable) {
                output.delete()
                _state.update {
                    it.copy(build = BuildState.Idle, message = "生成失败")
                }
            }
        }
    }

    private fun clearExports() {
        File(getApplication<Application>().cacheDir, "exports")
            .listFiles()
            ?.forEach { it.delete() }
    }
}
