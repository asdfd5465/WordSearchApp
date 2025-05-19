package dict.nick.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dict.nick.data.model.WordDetail
import dict.nick.data.repository.DictionaryRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DictionaryRepository(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")

    private val _wordPredictions = MutableStateFlow<List<String>>(emptyList())
    val wordPredictions: StateFlow<List<String>> = _wordPredictions.asStateFlow()

    private val _selectedWordDetail = MutableStateFlow<WordDetail?>(null)
    val selectedWordDetail: StateFlow<WordDetail?> = _selectedWordDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // Add a debounce to avoid too many prediction calls
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length > 1) { // Start predictions after 1 char
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
            _selectedWordDetail.value = null // Clear previous detail
            try {
                val detail = repository.getWordDetail(word)
                _selectedWordDetail.value = detail
            } catch (e: Exception) {
                // Handle error, e.g., log it or update a UI error state
                _selectedWordDetail.value = null
            } finally {
                _isLoadingDetail.value = false
            }
        }
    }
}
