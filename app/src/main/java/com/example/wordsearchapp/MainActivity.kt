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

        //wordEditText = findViewById(R.id.wordEditText) > Line 25 to 41 commented
        //searchButton = findViewById(R.id.searchButton)
        meaningTextView = findViewById(R.id.meaningTextView)

        //searchButton.setOnClickListener {
          //  performSearch()
        //}

        //wordEditText.setOnEditorActionListener { _, actionId, _ ->
          //  if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            //    performSearch()
              //  true
            //} else {
              //  false
            //}
        //}
    //}

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
                    if (entries.isEmpty() || entries.first().meanings.isNullOrEmpty()) {
                        meaningTextView.text = getString(R.string.no_definition_found)
                    } else {
                        // Let's format the output a bit
                        val formattedResult = StringBuilder()
                        entries.forEachIndexed { entryIndex, entry ->
                            if (entryIndex > 0) formattedResult.append("\n\n---\n\n")
                            formattedResult.append("Word: ${entry.word ?: "N/A"}\n")
                            if (!entry.phonetic.isNullOrBlank()) {
                                formattedResult.append("Phonetic: ${entry.phonetic}\n")
                            }
                            entry.meanings?.forEach { meaning ->
                                formattedResult.append("\nPart of Speech: ${meaning.partOfSpeech ?: "N/A"}\n")
                                meaning.definitions?.forEachIndexed { defIndex, defDetail ->
                                    formattedResult.append("  ${defIndex + 1}. ${defDetail.definition ?: "N/A"}\n")
                                    if (!defDetail.example.isNullOrBlank()) {
                                        formattedResult.append("     Example: ${defDetail.example}\n")
                                    }
                                }
                            }
                        }
                        meaningTextView.text = formattedResult.toString()
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
