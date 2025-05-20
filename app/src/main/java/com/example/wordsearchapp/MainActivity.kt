package com.example.wordsearchapp // Use your actual package name

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView // Changed from EditText for suggestions
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // private lateinit var wordEditText: EditText // Changed to AutoCompleteTextView
    private lateinit var wordAutoCompleteTextView: AutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var meaningTextView: TextView

    // private val dictionaryApiService = DictionaryApiService() // REMOVE THIS
    private lateinit var dbHelper: DatabaseHelper // ADD THIS
    private lateinit var suggestionsAdapter: ArrayAdapter<String>


    private val TAG = "WordSearchAppMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this) // Initialize before using

        wordAutoCompleteTextView = findViewById(R.id.wordEditText) // Ensure ID in XML is wordEditText
        searchButton = findViewById(R.id.searchButton)
        meaningTextView = findViewById(R.id.meaningTextView)

        setupAutoComplete()

        searchButton.setOnClickListener {
            Log.d(TAG, "Search button clicked")
            val wordToSearch = wordAutoCompleteTextView.text.toString()
            performSearch(wordToSearch)
        }

        wordAutoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.d(TAG, "IME action search triggered")
                val wordToSearch = wordAutoCompleteTextView.text.toString()
                performSearch(wordToSearch)
                true
            } else {
                false
            }
        }
        // Listener for when a suggestion is clicked
        wordAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedWord = parent.getItemAtPosition(position) as String
            Log.d(TAG, "Suggestion selected: $selectedWord")
            performSearch(selectedWord) // Search when a suggestion is clicked
        }
    }

    private fun setupAutoComplete() {
        suggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        wordAutoCompleteTextView.setAdapter(suggestionsAdapter)
        wordAutoCompleteTextView.threshold = 1 // Start showing suggestions after 1 character

        wordAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= wordAutoCompleteTextView.threshold) {
                    // Use a coroutine to fetch suggestions off the main thread
                    lifecycleScope.launch {
                        val suggestions = withContext(Dispatchers.IO) {
                            dbHelper.getWordSuggestions(query)
                        }
                        // Update adapter on the main thread
                        suggestionsAdapter.clear()
                        suggestionsAdapter.addAll(suggestions)
                        suggestionsAdapter.filter.filter(null) // To display all fetched suggestions
                        // OR if you want Android's filtering: suggestionsAdapter.filter.filter(query)
                        Log.d(TAG, "Fetched suggestions for '$query': $suggestions")
                    }
                } else {
                    suggestionsAdapter.clear()
                    suggestionsAdapter.notifyDataSetChanged()
                }
            }
        })
    }


    private fun performSearch(wordToSearch: String) {
        val word = wordToSearch.trim()
        Log.d(TAG, "performSearch called with word: '$word'")

        hideKeyboardAndClearFocus()

        if (word.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_word), Toast.LENGTH_SHORT).show()
            return
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            meaningTextView.text = "Searching..."
        } else {
            Log.w(TAG, "performSearch: Not updating 'Searching...' text, activity not started.")
        }

        lifecycleScope.launch {
            Log.d(TAG, "Coroutine launched for DB query")
            // Perform DB query on a background thread
            val wordEntryResult = withContext(Dispatchers.IO) {
                if (!dbHelper.wordExists(word)) { // Optional: explicit validation
                    Log.d(TAG, "Word '$word' does not exist in 'words' table according to dbHelper.wordExists.")
                    // Fallback or directly try to get definition, as definition query also checks word_text
                }
                dbHelper.getWordDefinition(word)
            }
            Log.d(TAG, "DB query finished for '$word', result found: ${wordEntryResult != null}")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && wordEntryResult.definitions != null && wordEntryResult.definitions.isNotEmpty()) {
                    val formattedResult = StringBuilder()
                    // The word itself is available from wordEntryResult.word
                    // formattedResult.append("Word: ${wordEntryResult.word ?: "N/A"}\n") // Already known

                    wordEntryResult.definitions.forEach { defDetail ->
                        formattedResult.append("\nPart of Speech: ${defDetail.partOfSpeech ?: "N/A"}\n")
                        formattedResult.append("  Definition: ${defDetail.definition ?: "N/A"}\n")
                        defDetail.examples?.takeIf { it.isNotEmpty() }?.let { exs ->
                            formattedResult.append("  Examples:\n")
                            exs.forEach { ex -> formattedResult.append("    - $ex\n") }
                        }
                        defDetail.synonyms?.takeIf { it.isNotEmpty() }?.let { syns ->
                            formattedResult.append("  Synonyms: ${syns.joinToString(", ")}\n")
                        }
                        defDetail.antonyms?.takeIf { it.isNotEmpty() }?.let { ants ->
                            formattedResult.append("  Antonyms: ${ants.joinToString(", ")}\n")
                        }
                    }
                    meaningTextView.text = formattedResult.toString()
                    Log.d(TAG, "Successfully displayed meaning for '$word' from DB")
                } else {
                    meaningTextView.text = getString(R.string.no_definition_found)
                    Log.d(TAG, "No definition found for '$word' in DB")
                }
            } else {
                Log.w(TAG, "performSearch: Not updating UI with result, activity no longer started. Word: '$word', Found: ${wordEntryResult != null}")
            }
        }
    }

    private fun hideKeyboardAndClearFocus() {
        Log.d(TAG, "Attempting to hide keyboard and clear focus")
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        var focusedView = currentFocus
        if (focusedView == null) {
            focusedView = window.decorView
        }
        focusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
            Log.d(TAG, "Keyboard hidden and focus cleared for view: $it")
        } ?: run {
            Log.d(TAG, "No view currently has focus to hide keyboard from or clear.")
        }
        if (::wordAutoCompleteTextView.isInitialized && wordAutoCompleteTextView != focusedView) {
            wordAutoCompleteTextView.clearFocus()
        }
    }

    // Lifecycle methods (onStart, onResume, onPause, onStop, onDestroy) remain the same for logging
    override fun onStart() {
        val startTime = System.currentTimeMillis()
        super.onStart()
        Log.d(TAG, "onStart called")
        Log.d(TAG, "onStart finished in ${System.currentTimeMillis() - startTime}ms")
    }

    override fun onResume() {
        val startTime = System.currentTimeMillis()
        super.onResume()
        Log.d(TAG, "onResume called")
        Log.d(TAG, "onResume finished in ${System.currentTimeMillis() - startTime}ms")
    }

    override fun onPause() {
        val startTime = System.currentTimeMillis()
        super.onPause()
        Log.d(TAG, "onPause called")
        Log.d(TAG, "onPause finished in ${System.currentTimeMillis() - startTime}ms")
    }

    override fun onStop() {
        val startTime = System.currentTimeMillis()
        super.onStop()
        Log.d(TAG, "onStop called")
        // hideKeyboardAndClearFocus() // Keep this commented based on previous findings
        Log.d(TAG, "onStop (hideKeyboardAndClearFocus was commented out for this test in previous version)")
        Log.d(TAG, "onStop finished in ${System.currentTimeMillis() - startTime}ms")
    }

    override fun onDestroy() {
        val startTime = System.currentTimeMillis()
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // dbHelper.close() // SQLiteOpenHelper handles this
        Log.d(TAG, "onDestroy finished in ${System.currentTimeMillis() - startTime}ms")
    }
}
