package dict.nick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dict.nick.data.model.WordDetail // Ensure this import is present
import dict.nick.data.repository.DictionaryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class) // For .debounce()
class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DictionaryRepository(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    // No public setter for searchQuery from outside, only through updateSearchQuery

    private val _wordPredictions = MutableStateFlow<List<String>>(emptyList())
    val wordPredictions: StateFlow<List<String>> = _wordPredictions.asStateFlow()

    private val _selectedWordDetail = MutableStateFlow<WordDetail?>(null)
    val selectedWordDetail: StateFlow<WordDetail?> = _selectedWordDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Only emit if no new value for 300ms
                .distinctUntilChanged() // Only emit if the value has actually changed
                .collectLatest { query -> // Collect the latest value, cancelling previous collections
                    if (query.length > 1) { // Start predictions after 1 character
                        _wordPredictions.value = repository.getWordPredictions(query)
                    } else {
                        _wordPredictions.value = emptyList()
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearPredictions() {
        _wordPredictions.value = emptyList()
    }

    fun fetchWordDetail(word: String) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _selectedWordDetail.value = null // Clear previous detail before fetching new one
            try {
                val detail = repository.getWordDetail(word)
                _selectedWordDetail.value = detail
            } catch (e: Exception) {
                // Consider logging the exception e.g. Log.e("DictionaryVM", "Error fetching word", e)
                _selectedWordDetail.value = null // Ensure it's null on error
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }

    // Function specifically for populating data in @Preview composables
    fun setPreviewData(wordForPreview: String, detail: WordDetail?) {
        // This function is intended for use in @Preview composables.
        // It bypasses actual data fetching for UI preview purposes.
        // You might add checks if this should only run in debug/preview builds if necessary.
        _searchQuery.value = wordForPreview // Optionally set search query for consistent preview
        _selectedWordDetail.value = detail
        _isLoadingDetail.value = false
        _wordPredictions.value = emptyList() // Clear predictions for detail view preview
    }
}