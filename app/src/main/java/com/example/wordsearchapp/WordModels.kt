package com.example.wordsearchapp

// Simplified model for DictionaryAPI.dev response
// The API returns a List<ApiEntry>

data class ApiEntry(
    val word: String?,
    val phonetic: String?,
    val meanings: List<Meaning>?
)

data class Meaning(
    val partOfSpeech: String?,
    val definitions: List<DefinitionDetail>?
)

data class DefinitionDetail(
    val definition: String?,
    val example: String?
    // We can add synonyms, antonyms if needed
)
