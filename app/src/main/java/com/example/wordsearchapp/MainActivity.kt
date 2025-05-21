package com.offlinedictionary.pro

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale // For TTS

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var wordAutoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var definitionsContainer: LinearLayout
    private lateinit var resultsScrollView: ScrollView
    private lateinit var noResultsTextView: TextView
    private lateinit var welcomeMessageContainer: LinearLayout
    private lateinit var searchedWordContainer: LinearLayout
    private lateinit var searchedWordTextView: TextView
    private lateinit var ttsButton: ImageButton

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var suggestionsAdapter: ArrayAdapter<String>

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentWordToSpeak: String? = null

    private val TAG = "WordSearchAppMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setTheme(R.style.Theme_WordSearchApp) // Usually set in Manifest
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        dbHelper = DatabaseHelper(this)
        tts = TextToSpeech(this, this) // Initialize TTS

        wordAutoCompleteTextView = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        definitionsContainer = findViewById(R.id.definitionsContainer)
        resultsScrollView = findViewById(R.id.resultsScrollView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
        welcomeMessageContainer = findViewById(R.id.welcomeMessageContainer)
        searchedWordContainer = findViewById(R.id.searchedWordContainer)
        searchedWordTextView = findViewById(R.id.searchedWordTextView)
        ttsButton = findViewById(R.id.ttsButton)

        // Initial UI state: show welcome message
        showWelcomeState()

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
        wordAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedWord = parent.getItemAtPosition(position) as String
            Log.d(TAG, "Suggestion selected: $selectedWord")
            wordAutoCompleteTextView.setText(selectedWord, false)
            performSearch(selectedWord)
        }

        ttsButton.setOnClickListener {
            currentWordToSpeak?.let { speakWord(it) }
        }
    }

    private fun showWelcomeState() {
        welcomeMessageContainer.visibility = View.VISIBLE
        searchedWordContainer.visibility = View.GONE
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
    }

    private fun showResultsState() {
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        resultsScrollView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
    }

    private fun showNoResultsState(searchedWord: String) {
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE // Still show the searched word
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        currentWordToSpeak = searchedWord // Allow TTS for the word even if no defs
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }
    
    private fun showSearchingState(searchedWord: String) {
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.GONE // Hide old results
        noResultsTextView.visibility = View.GONE // Hide no results message
        definitionsContainer.removeAllViews() // Clear previous results
        // You could add a ProgressBar here dynamically if needed
    }


    private fun setupAutoComplete() {
        // ... (same as before)
        suggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        wordAutoCompleteTextView.setAdapter(suggestionsAdapter)
        wordAutoCompleteTextView.threshold = 1

        wordAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= wordAutoCompleteTextView.threshold) {
                    lifecycleScope.launch {
                        val suggestions = withContext(Dispatchers.IO) {
                            dbHelper.getWordSuggestions(query)
                        }
                        suggestionsAdapter.clear()
                        suggestionsAdapter.addAll(suggestions)
                        suggestionsAdapter.filter.filter(null)
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
            // Optionally revert to welcome state if input is cleared after a search
            // if (definitionsContainer.childCount > 0 || noResultsTextView.isVisible) {
            //     showWelcomeState()
            // }
            return
        }

        showSearchingState(word) // Update UI to show searched word and "searching" state

        lifecycleScope.launch {
            Log.d(TAG, "Coroutine launched for DB query for '$word'")
            val wordEntryResult = withContext(Dispatchers.IO) {
                dbHelper.getWordDefinition(word)
            }
            Log.d(TAG, "DB query finished for '$word', result found: ${wordEntryResult != null}")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && wordEntryResult.definitions != null && wordEntryResult.definitions.isNotEmpty()) {
                    displayDefinitions(wordEntryResult.definitions)
                    showResultsState() // Ensure correct containers are visible
                    Log.d(TAG, "Successfully displayed meaning for '$word' from DB")
                } else {
                    showNoResultsState(word)
                    Log.d(TAG, "No definition found for '$word' in DB")
                }
                // Reset AutoCompleteTextView after search
                wordAutoCompleteTextView.setText("", false) // Clear text, don't filter
                // wordAutoCompleteTextView.hint = getString(R.string.hint_enter_word) // If you want to reset hint
            } else {
                Log.w(TAG, "performSearch: Not updating UI, activity no longer started. Word: '$word'")
            }
        }
    }

    private fun displayDefinitions(definitions: List<DefinitionDetail>) {
        // definitionsContainer is already cleared in showSearchingState or by showResultsState logic
        val inflater = LayoutInflater.from(this)

        for (defDetail in definitions) {
            val itemView = inflater.inflate(R.layout.item_definition, definitionsContainer, false)
            // ... (same population logic as before for itemView)
            val partOfSpeechTV = itemView.findViewById<TextView>(R.id.itemPartOfSpeechTextView)
            val definitionTV = itemView.findViewById<TextView>(R.id.itemDefinitionTextView)
            val examplesContainer = itemView.findViewById<LinearLayout>(R.id.itemExamplesContainer)
            val examplesTV = itemView.findViewById<TextView>(R.id.itemExamplesTextView)
            val synonymsContainer = itemView.findViewById<LinearLayout>(R.id.itemSynonymsContainer)
            val synonymsTV = itemView.findViewById<TextView>(R.id.itemSynonymsTextView)
            val antonymsContainer = itemView.findViewById<LinearLayout>(R.id.itemAntonymsContainer)
            val antonymsTV = itemView.findViewById<TextView>(R.id.itemAntonymsTextView)

            partOfSpeechTV.text = defDetail.partOfSpeech?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "N/A"
            definitionTV.text = defDetail.definition ?: "No definition text."

            defDetail.examples?.takeIf { it.isNotEmpty() }?.let { exs ->
                examplesContainer.visibility = View.VISIBLE
                examplesTV.text = exs.joinToString(separator = "\n") { "- $it" }
            } ?: run {
                examplesContainer.visibility = View.GONE
            }

            defDetail.synonyms?.takeIf { it.isNotEmpty() }?.let { syns ->
                synonymsContainer.visibility = View.VISIBLE
                synonymsTV.text = syns.joinToString(", ")
            } ?: run {
                synonymsContainer.visibility = View.GONE
            }

            defDetail.antonyms?.takeIf { it.isNotEmpty() }?.let { ants ->
                antonymsContainer.visibility = View.VISIBLE
                antonymsTV.text = ants.joinToString(", ")
            } ?: run {
                antonymsContainer.visibility = View.GONE
            }
            definitionsContainer.addView(itemView)
        }
    }

    // --- TextToSpeech Implementation ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US) // Set language, e.g., US English
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS: The Language specified is not supported!")
                Toast.makeText(this, getString(R.string.tts_language_not_supported), Toast.LENGTH_SHORT).show()
                ttsReady = false
            } else {
                Log.i(TAG, "TTS Initialization Successful.")
                ttsReady = true
                // If a word was searched before TTS was ready, speak it now
                currentWordToSpeak?.let { if(searchedWordContainer.visibility == View.VISIBLE) speakWord(it) }
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
            Toast.makeText(this, getString(R.string.tts_init_failed), Toast.LENGTH_SHORT).show()
            ttsReady = false
        }
    }

    private fun speakWord(word: String) {
        if (ttsReady && tts != null) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.i(TAG, "TTS: Speaking '$word'")
        } else {
            Log.e(TAG, "TTS: Not ready or null, cannot speak '$word'")
            if(!ttsReady) Toast.makeText(this, getString(R.string.tts_init_failed), Toast.LENGTH_SHORT).show()
        }
        currentWordToSpeak = word // Store it in case TTS wasn't ready on first call
    }


    private fun hideKeyboardAndClearFocus() {
        // ... (same as before)
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

    // Lifecycle methods
    override fun onStart() { super.onStart(); Log.d(TAG, "onStart called") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume called") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause called") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop called") }

    override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            Log.d(TAG, "TTS Shutting down.")
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }
}
