package dict.nick.ui.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "theme_prefs")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = getApplication<Application>().applicationContext.dataStore

    companion object {
        val IS_DARK_THEME_KEY = booleanPreferencesKey("is_dark_theme")
    }

    val isDarkTheme = dataStore.data
        .map { preferences ->
            preferences[IS_DARK_THEME_KEY] ?: false // Default to false (light theme) or system theme
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Or Lazily
            initialValue = false // Initial default before datastore loads
        )

    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { settings ->
                val currentTheme = settings[IS_DARK_THEME_KEY] ?: false
                settings[IS_DARK_THEME_KEY] = !currentTheme
            }
        }
    }
}
