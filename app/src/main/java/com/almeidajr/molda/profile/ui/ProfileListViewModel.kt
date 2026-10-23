package com.almeidajr.molda.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One list of the stored profiles, Perfis' or a picker's, and the deletions made from it. */
@HiltViewModel
class ProfileListViewModel @Inject constructor(private val repository: ProfileRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(ProfilesState())
    val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Follows the stored profiles; after a load failure, calling it again is the retry. */
    fun load() {
        val current = mutableState.value
        if (!current.isLoading && current.loadError == null) return
        mutableState.update { it.loading() }
        // A retry while the first emission is pending replaces that collector.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.profiles()
                .map { Result.success(it) }
                .catch { failure -> emit(Result.failure(failure)) }
                .collect { result -> mutableState.update { it.loadedWith(result) } }
        }
    }

    /** Deletes profile [id] unless this list is busy; then it is [ProfilesState.deleted]. */
    fun delete(id: ProfileId) {
        val current = mutableState.value
        if (!current.canMutate || current.profiles.none { it.id == id }) return
        mutableState.update { it.mutating(deleteId = id) }
        viewModelScope.launch {
            val result = repository.delete(id)
            mutableState.update { it.deletedWith(id, result) }
        }
    }

    /** Forgets the deletion once the list's entry has shown it. */
    fun deletionShown() = mutableState.update { it.copy(deleted = null) }
}
