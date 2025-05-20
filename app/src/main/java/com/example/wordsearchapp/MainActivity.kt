package com.example.wordsearchapp

import android.content.Context
import android.os.Bundle
import android.util.Log // For logging
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var wordEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var meaningTextView: TextView

    private val dictionaryApiService = DictionaryApiService()
    private val TAG = "WordSearchAppMainActivity" // Tag for logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        wordEditText = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        meaningTextView = findViewById(R.id.meaningTextView)

        searchButton.setOnClickListener {
            Log.d(TAG, "Search button clicked")
            performSearch()
        }

        wordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.d(TAG, "IME action search triggered")
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val word = wordEditText.text.toString().trim()
        Log.d(TAG, "performSearch called with word: '$word'")

        // Hide keyboard and clear focus
        hideKeyboardAndClearFocus()

        if (word.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_word), Toast.LENGTH_SHORT).show()
            return
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            meaningTextView.text = "Searching..." // Provide immediate feedback only if started
        } else {
            Log.w(TAG, "performSearch: Not updating 'Searching...' text, activity not started.")
        }


        lifecycleScope.launch {
            Log.d(TAG, "Coroutine launched for API call")
            val result = dictionaryApiService.getMeaning(word)
            Log.d(TAG, "API call finished, result success: ${result.isSuccess}")

            // Only update UI if the Activity is at least in STARTED state (visible)
            // Using RESUMED is even safer for UI updates that should only happen when fully interactive.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                result.fold(
                    onSuccess = { entries ->
                        if (entries.isEmpty() || entries.first().meanings.isNullOrEmpty()) {
                            meaningTextView.text = getString(R.string.no_definition_found)
                            Log.d(TAG, "No definition found for '$word'")
                        } else {
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
                            Log.d(TAG, "Successfully displayed meaning for '$word'")
                        }
                    },
                    onFailure = { exception ->
                        meaningTextView.text = getString(R.string.error_fetching_meaning)
                        Log.e(TAG, "Error fetching meaning for '$word'", exception)
                    }
                )
            } else {
                Log.w(TAG, "performSearch: Not updating UI with result, activity no longer started. Word: '$word', Success: ${result.isSuccess}")
            }
        }
    }

    private fun hideKeyboardAndClearFocus() {
        Log.d(TAG, "Attempting to hide keyboard and clear focus")
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var focusedView = currentFocus
        if (focusedView == null) {
            // If no view has focus, decor view might be the one to use token from
            focusedView = window.decorView
        }
        focusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus() // Clear focus from the currently focused view
             Log.d(TAG, "Keyboard hidden and focus cleared for view: $it")
        } ?: run {
            Log.d(TAG, "No view currently has focus to hide keyboard from or clear.")
        }
        // Additionally, specifically try to clear focus from wordEditText if it's not the current focus
        // but might still hold some state.
        if (::wordEditText.isInitialized && wordEditText != focusedView) {
            wordEditText.clearFocus()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        // It's good practice to also try to hide keyboard here if activity is losing focus
        // hideKeyboardAndClearFocus() // Consider if this causes issues with quick app switching
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called - attempting to hide keyboard and clear focus")
        // This is a crucial place to ensure resources are released
        hideKeyboardAndClearFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }
}
