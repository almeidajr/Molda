package com.almeidajr.molda.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almeidajr.molda.profile.domain.MeasurementProfile
import com.almeidajr.molda.profile.domain.ProfileMeasurements
import com.almeidajr.molda.profile.domain.ProfileRepository
import com.almeidajr.molda.skirt.ui.parseMeasurement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileField(val label: String) {
    Name("Nome ou apelido"),
    Waist("Cintura do perfil (cm)"),
    Date("Data da medida (dd/mm/aaaa)"),
    Notes("Observações (opcional)"),
}

data class ProfilesState(
    val profiles: List<MeasurementProfile> = emptyList(),
    val inputs: Map<ProfileField, String> = ProfileField.entries.associateWith { "" },
    val errors: Map<ProfileField, String> = emptyMap(),
    val isCreating: Boolean = false,
    val editingId: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val savedMessage: String? = null,
)

class ProfilesViewModel(
    private val repository: ProfileRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ProfilesState(
            inputs = ProfileField.entries.associateWith { savedState[it.name] ?: "" },
            isCreating = savedState["isCreating"] ?: false,
            editingId = savedState["editingId"],
        ),
    )
    val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!mutableState.value.isLoading && mutableState.value.loadError == null) return
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val profiles = repository.list()
                mutableState.update { it.copy(profiles = profiles.toList(), isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Não foi possível carregar os perfis. " +
                            "Os dados não foram apagados. Tente novamente.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        (repository as? AutoCloseable)?.close()
    }

    fun startCreating() {
        if (!canMutate()) return
        if (mutableState.value.editingId != null) {
            ProfileField.entries.forEach { savedState[it.name] = "" }
            mutableState.update {
                it.copy(inputs = ProfileField.entries.associateWith { "" }, errors = emptyMap())
            }
        }
        savedState["isCreating"] = true
        savedState["editingId"] = null
        mutableState.update {
            it.copy(isCreating = true, editingId = null, savedMessage = null, saveError = null)
        }
    }

    fun startEditing(id: Long) {
        if (!canMutate()) return
        val profile = mutableState.value.profiles.find { it.id == id } ?: return
        val measurements = profile.measurements
        val inputs = mapOf(
            ProfileField.Name to measurements.name,
            ProfileField.Waist to profileWaistText(measurements.waist),
            ProfileField.Date to measurements.measurementDate.format(ProfileDateFormat),
            ProfileField.Notes to measurements.notes,
        )
        inputs.forEach { (field, text) -> savedState[field.name] = text }
        savedState["isCreating"] = true
        savedState["editingId"] = id
        mutableState.update {
            it.copy(
                inputs = inputs,
                isCreating = true,
                editingId = id,
                errors = emptyMap(),
                savedMessage = null,
                saveError = null,
            )
        }
    }

    fun edit(field: ProfileField, text: String) {
        if (mutableState.value.isSaving) return
        savedState[field.name] = text
        mutableState.update {
            it.copy(
                inputs = it.inputs + (field to text),
                errors = it.errors - field,
                saveError = null,
            )
        }
    }

    fun save() {
        val current = mutableState.value
        if (!canMutate() || !current.isCreating) return
        val waist = parseMeasurement(current.inputs.getValue(ProfileField.Waist))
        val date = parseProfileDate(current.inputs.getValue(ProfileField.Date))
        val errors = profileErrors(current.inputs, waist, date)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errors = errors) }
            return
        }
        val measurements = ProfileMeasurements(
            current.inputs.getValue(ProfileField.Name).trim(),
            requireNotNull(waist),
            requireNotNull(date),
            current.inputs.getValue(ProfileField.Notes),
        )
        mutableState.update { it.copy(isSaving = true, saveError = null, savedMessage = null) }
        viewModelScope.launch {
            try {
                val profile = current.editingId?.let { id ->
                    MeasurementProfile(id, measurements).also { repository.update(it) }
                } ?: repository.create(measurements)
                clearSavedForm()
                mutableState.update {
                    it.copy(
                        profiles = if (current.editingId == null) {
                            it.profiles + profile
                        } else {
                            it.profiles.map { saved ->
                                if (saved.id ==
                                    profile.id
                                ) {
                                    profile
                                } else {
                                    saved
                                }
                            }
                        },
                        inputs = ProfileField.entries.associateWith { "" },
                        isCreating = false,
                        editingId = null,
                        isSaving = false,
                        errors = emptyMap(),
                        savedMessage = "Perfil salvo neste aparelho.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Não foi possível salvar o perfil. " +
                            "Confira os dados e tente novamente.",
                    )
                }
            }
        }
    }

    fun delete(id: Long) {
        if (!canMutate() || mutableState.value.profiles.none { it.id == id }) return
        mutableState.update { it.copy(isSaving = true, saveError = null, savedMessage = null) }
        viewModelScope.launch {
            try {
                repository.delete(id)
                if (mutableState.value.editingId == id) {
                    clearSavedForm()
                    mutableState.update {
                        it.copy(
                            inputs = ProfileField.entries.associateWith { "" },
                            isCreating = false,
                            editingId = null,
                            errors = emptyMap(),
                        )
                    }
                }
                mutableState.update {
                    it.copy(
                        profiles = it.profiles.filterNot { profile -> profile.id == id },
                        isSaving = false,
                        savedMessage = "Perfil excluído deste aparelho.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Não foi possível excluir o perfil. " +
                            "Os dados não foram apagados. Tente novamente.",
                    )
                }
            }
        }
    }

    private fun canMutate(): Boolean = mutableState.value.let {
        !it.isLoading && !it.isSaving && it.loadError == null
    }

    private fun clearSavedForm() {
        ProfileField.entries.forEach { savedState[it.name] = "" }
        savedState["isCreating"] = false
        savedState["editingId"] = null
    }
}

internal val ProfileDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(
    "dd/MM/uuuu",
).withResolverStyle(ResolverStyle.STRICT)

private fun parseProfileDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim(), ProfileDateFormat)
} catch (_: DateTimeParseException) {
    null
}

private fun profileErrors(
    inputs: Map<ProfileField, String>,
    waist: Double?,
    date: LocalDate?,
): Map<ProfileField, String> = buildMap {
    if (inputs.getValue(ProfileField.Name).isBlank()) {
        put(ProfileField.Name, "Informe um nome ou apelido.")
    }
    if (waist == null || waist <= 0) {
        put(ProfileField.Waist, "Informe uma cintura positiva e finita, como 80,5.")
    }
    if (date == null) {
        put(ProfileField.Date, "Informe uma data válida no formato dd/mm/aaaa.")
    }
}
