package com.almeidajr.molda.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.data.LoadResult
import com.almeidajr.molda.data.asLoadResult
import com.almeidajr.molda.profile.domain.ProfileId
import com.almeidajr.molda.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One list of the stored profiles, Perfis' or a picker's, and the deletions made from it. */
@HiltViewModel
class ProfileListViewModel @Inject constructor(private val repository: ProfileRepository) :
    ViewModel() {
    private val mutableState = MutableStateFlow(ProfileListState())
    val state: StateFlow<ProfileListState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    /** Follows the stored profiles; after a load failure, calling it again is the retry. */
    fun load() {
        if (mutableState.value.load is LoadResult.Success) return
        // A retry while the first emission is pending replaces that collector.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.profiles().asLoadResult().collect { load ->
                mutableState.update { it.copy(load = load) }
            }
        }
    }

    /** Deletes profile [id] unless this list is busy; then it is [ProfileListState.deleted]. */
    fun delete(id: ProfileId) {
        val current = mutableState.value
        val profiles = (current.load as? LoadResult.Success)?.value.orEmpty()
        if (!current.canChangeProfiles || profiles.none { it.id == id }) return
        mutableState.update { it.copy(deletion = ProfileDeletion.Running(id)) }
        viewModelScope.launch {
            val result = repository.delete(id)
            mutableState.update { it.deletedWith(id, result) }
        }
    }

    /** Forgets the deletion once the list's entry has shown it. */
    fun deletionShown() = mutableState.update { it.copy(deleted = null) }
}
