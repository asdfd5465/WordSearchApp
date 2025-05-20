package com.example.wordsearchapp // Or your new package name: com.newcompany.newapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
// import android.widget.AutoCompleteTextView // Still commented out, which is fine
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.view.isVisible // This import was present in your pasted code, but not used. Keep if needed elsewhere, or remove.
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView // Keep this one
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// REMOVE: import com.google.android.material.textfield.MaterialAutoCompleteTextView (this was the duplicate)

class MainActivity : AppCompatActivity() {

    private lateinit var wordAutoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var searchButton: Button
    private lateinit var definitionsContainer: LinearLayout
    private lateinit var resultsScrollView: ScrollView
    private lateinit var noResultsTextView: TextView

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var suggestionsAdapter: ArrayAdapter<String>

    private val TAG = "WordSearchAppMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure your package name is correct here if you changed it.
        // If MainActivity is in com.newcompany.newapp, then R should be found.
        // If MainActivity is still in com.example.wordsearchapp BUT your namespace in build.gradle
        // is com.newcompany.newapp, you'd need to import com.newcompany.newapp.R
        setTheme(R.style.Theme_WordSearchApp) 
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        dbHelper = DatabaseHelper(this)

        wordAutoCompleteTextView = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        definitionsContainer = findViewById(R.id.definitionsContainer)
        resultsScrollView = findViewById(R.id.resultsScrollView)
        noResultsTextView = findViewById(R.id.noResultsTextView)

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
    }

    private fun setupAutoComplete() {
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
            return
        }

        definitionsContainer.removeAllViews()
        noResultsTextView.visibility = View.GONE
        
        lifecycleScope.launch {
            Log.d(TAG, "Coroutine launched for DB query for '$word'")
            val wordEntryResult = withContext(Dispatchers.IO) {
                dbHelper.getWordDefinition(word)
            }
            Log.d(TAG, "DB query finished for '$word', result found: ${wordEntryResult != null}")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && wordEntryResult.definitions != null && wordEntryResult.definitions.isNotEmpty()) {
                    displayDefinitions(wordEntryResult.definitions)
                    resultsScrollView.visibility = View.VISIBLE
                    noResultsTextView.visibility = View.GONE
                    Log.d(TAG, "Successfully displayed meaning for '$word' from DB")
                } else {
                    definitionsContainer.removeAllViews()
                    resultsScrollView.visibility = View.GONE
                    noResultsTextView.visibility = View.VISIBLE
                    Log.d(TAG, "No definition found for '$word' in DB")
                }
            } else {
                Log.w(TAG, "performSearch: Not updating UI, activity no longer started. Word: '$word'")
            }
        }
    }

    private fun displayDefinitions(definitions: List<DefinitionDetail>) {
        definitionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (defDetail in definitions) {
            val itemView = inflater.inflate(R.layout.item_definition, definitionsContainer, false)

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

    override fun onStart() { super.onStart(); Log.d(TAG, "onStart called") }
    override fun onResume() { super.onResume(); Log.d(TAG, "onResume called") }
    override fun onPause() { super.onPause(); Log.d(TAG, "onPause called") }
    override fun onStop() { super.onStop(); Log.d(TAG, "onStop called") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy called") }
}
