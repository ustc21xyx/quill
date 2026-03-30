package com.quill.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quill.data.repository.ModelRepository
import com.quill.data.repository.PersonaRepository
import com.quill.domain.model.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    modelRepository: ModelRepository,
    private val personaRepository: PersonaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("quill_settings", Context.MODE_PRIVATE)

    val defaultModelName: StateFlow<String?> = modelRepository.getDefault()
        .map { it?.displayName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val personas: StateFlow<List<Persona>> = personaRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contextMessageCount = MutableStateFlow(prefs.getInt("context_message_count", 20))
    val contextMessageCount: StateFlow<Int> = _contextMessageCount.asStateFlow()

    private val _languageCode = MutableStateFlow(prefs.getString("language", "system") ?: "system")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    fun updateContextMessageCount(count: Int) {
        val clamped = count.coerceIn(1, 100)
        _contextMessageCount.value = clamped
        prefs.edit().putInt("context_message_count", clamped).apply()
    }

    fun updateLanguage(code: String) {
        _languageCode.value = code
        prefs.edit().putString("language", code).apply()
        val locales = if (code == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun deletePersona(id: String) {
        viewModelScope.launch { personaRepository.delete(id) }
    }

    fun setDefaultPersona(id: String) {
        viewModelScope.launch { personaRepository.setDefault(id) }
    }

    companion object {
        fun getContextMessageCount(context: Context): Int {
            return context.getSharedPreferences("quill_settings", Context.MODE_PRIVATE)
                .getInt("context_message_count", 20)
        }
    }
}
