package dict.nick.data.repository

import android.content.Context
import dict.nick.data.model.WordDetail
import dict.nick.data.model.WordList
import dict.nick.utils.loadJsonFromAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DictionaryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var fullWordList: List<String>? = null
    private var dictionary: Map<String, WordDetail>? = null

    suspend fun getWordList(): List<String> {
        if (fullWordList == null) {
            fullWordList = withContext(Dispatchers.IO) {
                val jsonString = context.loadJsonFromAsset("wordnet_word_list.json")
                jsonString?.let { json.decodeFromString<WordList>(it).words } ?: emptyList()
            }
        }
        return fullWordList ?: emptyList()
    }

    suspend fun getDictionary(): Map<String, WordDetail> {
        if (dictionary == null) {
            dictionary = withContext(Dispatchers.IO) {
                val jsonString = context.loadJsonFromAsset("wordnet_dictionary.json")
                jsonString?.let { json.decodeFromString<Map<String, WordDetail>>(it) } ?: emptyMap()
            }
        }
        return dictionary ?: emptyMap()
    }

    suspend fun getWordDetail(word: String): WordDetail? {
        return getDictionary()[word.lowercase()] // Ensure lookup is also lowercase
    }

    suspend fun getWordPredictions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return getWordList().filter { it.startsWith(lowerQuery) }.take(10) // Limit predictions
    }
}
