
package com.offlinedictionary.pro

import com.google.gson.annotations.SerializedName // Still needed if you use it elsewhere, but not for DB direct mapping

// This is what we'll construct from the database query result
data class WordDefinitionEntry(
    val word: String?, // The word string itself
    val definitions: List<DefinitionDetail>?
    // Removed: val pronunciation: String?,
    // Removed: val etymology: String?
)

data class DefinitionDetail(
    // @SerializedName("part_of_speech") // Not strictly needed if column name matches or handled in query
    val partOfSpeech: String?, // Assuming column name is part_of_speech
    val definition: String?,   // Assuming column name is definition_text
    val examples: List<String>?,
    val synonyms: List<String>?,
    val antonyms: List<String>?
)

// WordListContainer is no longer needed as list.json is not used directly
