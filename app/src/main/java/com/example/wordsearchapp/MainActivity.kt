package com.example.wordsearchapp

import android.os.Bundle
// import android.view.inputmethod.EditorInfo // No longer needed for Test A
// import android.widget.Button // No longer needed for Test A
// import android.widget.EditText // No longer needed for Test A
import android.widget.TextView
import android.widget.Toast // Still used if performSearch were called directly (though not for Test A)
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Still used if performSearch were called directly
import kotlinx.coroutines.launch // Still used if performSearch were called directly

class MainActivity : AppCompatActivity() {

    // --- FOR TEST A: Comment out declarations for EditText and Button ---
    // private lateinit var wordEditText: EditText
    // private lateinit var searchButton: Button
    private lateinit var meaningTextView: TextView // Keep this, as it's used to display info

    // Keep dictionaryApiService if performSearch logic is still present,
    // even if not called from UI for Test A.
    private val dictionaryApiService = DictionaryApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- FOR TEST A: Comment out findViewById and listeners for EditText and Button ---
        // The user already had these lines (25-41) commented, which is correct for Test A.

        // wordEditText = findViewById(R.id.wordEditText)
        // searchButton = findViewById(R.id.searchButton)
        meaningTextView = findViewById(R.id.meaningTextView) // Initialize meaningTextView

        // searchButton.setOnClickListener {
        //  performSearch()
        // }

        // wordEditText.setOnEditorActionListener { _, actionId, _ ->
        //  if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        //    performSearch()
        //    true
        //  } else {
        //    false
        //  }
        // }
        // The closing curly brace for onCreate was commented out in your paste, ensure it's present:
    } // <- Make sure this closing brace for onCreate is NOT commented

    // --- FOR TEST A: Modify performSearch so it doesn't rely on wordEditText ---
    // Since performSearch is not called by UI interactions in Test A,
    // we just need to ensure it compiles.
    private fun performSearch() {
        // val word = wordEditText.text.toString().trim() // << CRITICAL: Comment this line out as wordEditText is not initialized/declared for Test A

        // For Test A, since wordEditText is not used, the concept of a 'word' from UI is gone.
        // We can make this function do nothing or use a dummy word if we absolutely needed to test its internal logic.
        // For now, let's make it simply return to avoid compilation errors and further execution.
        return // << ADD THIS to stop further execution for Test A

        // The original logic below will not be reached due to the 'return' statement above.
        // This prevents errors related to 'word' being uninitialized or wordEditText not existing.
        /*
        if (word.isEmpty()) { // 'word' would be undefined here without the modification above
            Toast.makeText(this, getString(R.string.error_no_word), Toast.LENGTH_SHORT).show()
            return
        }

        meaningTextView.text = "Searching..." // Provide immediate feedback

        lifecycleScope.launch {
            val result = dictionaryApiService.getMeaning(word) // 'word' would be problematic
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
        */
    }
}
