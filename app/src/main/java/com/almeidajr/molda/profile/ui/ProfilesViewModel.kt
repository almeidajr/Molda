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
        savedState["isCreating"] = true
        mutableState.update { it.copy(isCreating = true, savedMessage = null) }
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
        if (current.isLoading || current.isSaving || current.loadError != null) return
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
                val profile = repository.create(measurements)
                ProfileField.entries.forEach { savedState[it.name] = "" }
                savedState["isCreating"] = false
                mutableState.update {
                    it.copy(
                        profiles = it.profiles + profile,
                        inputs = ProfileField.entries.associateWith { "" },
                        isCreating = false,
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
