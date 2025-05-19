package dict.nick.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// For wordnet_word_list.json
@Serializable
data class WordList(
    val words: List<String>
)

// For wordnet_dictionary.json (Value part of the Map<String, WordDetail>)
@Serializable
data class WordDetail(
    val word: String,
    val definitions: List<DefinitionEntry>
)

@Serializable
data class DefinitionEntry(
    @SerialName("part_of_speech")
    val partOfSpeech: String,
    val definition: String,
    val examples: List<String>? = emptyList(), // Make optional if they can be missing
    val synonyms: List<String>? = emptyList(),
    val antonyms: List<String>? = emptyList()
)
