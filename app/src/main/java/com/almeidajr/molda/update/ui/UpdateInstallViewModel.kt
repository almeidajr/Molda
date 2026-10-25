package com.almeidajr.molda.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.update.domain.Update
import com.almeidajr.molda.update.domain.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * The app's one way to install a downloaded update, owned by the activity so that every place
 * offering the install (About, the startup notice) goes through the same checks first.
 */
@HiltViewModel
internal class UpdateInstallViewModel @Inject constructor(
    private val repository: UpdateRepository,
) : ViewModel() {
    /** Installs [update], which the system installer asks the person to confirm. */
    fun install(update: Update) {
        viewModelScope.launch { repository.install(update) }
    }
}
