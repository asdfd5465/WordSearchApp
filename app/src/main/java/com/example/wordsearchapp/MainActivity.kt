package com.example.wordsearchapp

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var wordEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var meaningTextView: TextView

    private val dictionaryApiService = DictionaryApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wordEditText = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        meaningTextView = findViewById(R.id.meaningTextView)

        searchButton.setOnClickListener {
            performSearch()
        }

        wordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val word = wordEditText.text.toString().trim()
        if (word.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_word), Toast.LENGTH_SHORT).show()
            return
        }

        meaningTextView.text = "Searching..." // Provide immediate feedback

        lifecycleScope.launch {
            val result = dictionaryApiService.getMeaning(word)
            result.fold(
                onSuccess = { entries ->
                    if (entries.isEmpty()) {
                        meaningTextView.text = getString(R.string.no_definition_found)
                    } else {
                        // --- START OF MODIFIED SECTION ---
                        val firstEntry = entries.firstOrNull()
                        if (firstEntry == null || firstEntry.meanings.isNullOrEmpty()) {
                            meaningTextView.text = getString(R.string.no_definition_found)
                            return@fold
                        }

                        val firstMeaning = firstEntry.meanings.firstOrNull()
                        if (firstMeaning == null || firstMeaning.definitions.isNullOrEmpty()) {
                            meaningTextView.text = getString(R.string.no_definition_found)
                            return@fold
                        }

                        val firstDefinitionDetail = firstMeaning.definitions.firstOrNull()
                        if (firstDefinitionDetail == null || firstDefinitionDetail.definition.isNullOrBlank()) {
                            meaningTextView.text = getString(R.string.no_definition_found)
                            return@fold
                        }

                        val formattedResult = StringBuilder()
                        formattedResult.append("Word: ${firstEntry.word ?: word.replaceFirstChar { it.titlecase() }}\n\n") // Use original input if API word is null

                        if (!firstMeaning.partOfSpeech.isNullOrBlank()) {
                            formattedResult.append("Type: ${firstMeaning.partOfSpeech}\n\n")
                        }

                        formattedResult.append("Meaning: ${firstDefinitionDetail.definition}\n\n")

                        if (!firstDefinitionDetail.example.isNullOrBlank()) {
                            formattedResult.append("Example: ${firstDefinitionDetail.example}\n")
                        }
                        // --- END OF MODIFIED SECTION ---
                        meaningTextView.text = formattedResult.toString().trim() // trim() to remove trailing newlines if example is missing
                    }
                },
                onFailure = {
                    meaningTextView.text = getString(R.string.error_fetching_meaning)
                    // For debugging, you might want to log the error:
                    // android.util.Log.e("MainActivity", "API Error", it)
                }
            )
        }
    }
}
