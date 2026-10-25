package com.almeidajr.molda.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.update.domain.UpdateCheck
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The update on offer, checked when the entry opens and again on [check]. */
@HiltViewModel
internal class UpdateCheckViewModel @Inject constructor(private val repository: UpdateRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow<UpdateCheck?>(null)

    /** The last check's outcome; null while checking. */
    val state: StateFlow<UpdateCheck?> = mutableState.asStateFlow()

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
}
