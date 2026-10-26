package com.almeidajr.molda.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateDownload
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The update on offer, checked when the entry opens and again on [check], and its download. */
@HiltViewModel
internal class UpdateCheckViewModel @Inject constructor(private val repository: UpdateRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow<UpdateCheck?>(null)

    /** The last check's outcome; null while checking. */
    val state: StateFlow<UpdateCheck?> = mutableState.asStateFlow()

    /** The offered update's download while collected, null when no update is on offer. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val download: StateFlow<UpdateDownload?> = mutableState
        .flatMapLatest { check ->
            if (check is UpdateCheck.Available) repository.download(check.update) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    private var checkJob: Job? = null

    init {
        check()
    }

    /** Checks again; ignored while a check is running. */
    fun check() {
        if (checkJob?.isActive == true) return
        mutableState.value = null
        checkJob = viewModelScope.launch { mutableState.value = repository.check() }
    }

    /** Starts downloading the offered update, or again after it failed. */
    fun startDownload() {
        val check = mutableState.value as? UpdateCheck.Available ?: return
        viewModelScope.launch { repository.startDownload(check.update) }
    }
}
