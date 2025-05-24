package com.offlinedictionary.pro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    private lateinit var favoriteButton: ImageButton
    private lateinit var mainToolbar: MaterialToolbar

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var suggestionsAdapter: ArrayAdapter<String>

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentWordToSpeak: String? = null
    private var currentSearchedWord: String? = null
    private var currentWordIsFavorite: Boolean = false

    private lateinit var favoritesActivityLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val EXTRA_SEARCH_WORD = "extra_search_word_from_favorites" // Made more specific
    }

    private val TAG = "WordSearchAppMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        mainToolbar = findViewById(R.id.mainToolbar)
        setSupportActionBar(mainToolbar)
        // Remove default title provided by label, we set it via app:title or programmatically if needed
        supportActionBar?.setDisplayShowTitleEnabled(true) // Ensure title is shown if set in XML

        // Make the toolbar title clickable to go to "home" (welcome state)
        mainToolbar.setOnClickListener {
            // Check if we are already in the welcome state to avoid unnecessary actions
            if (welcomeMessageContainer.visibility != View.VISIBLE) {
                showWelcomeState()
                wordAutoCompleteTextView.setText("", false) // Clear search input
                // Optionally, clear focus and hide keyboard
                hideKeyboardAndClearFocus()
            }
        }


        dbHelper = DatabaseHelper(this)
        tts = TextToSpeech(this, this)

        wordAutoCompleteTextView = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        definitionsContainer = findViewById(R.id.definitionsContainer)
        resultsScrollView = findViewById(R.id.resultsScrollView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
        welcomeMessageContainer = findViewById(R.id.welcomeMessageContainer)
        searchedWordContainer = findViewById(R.id.searchedWordContainer)
        searchedWordTextView = findViewById(R.id.searchedWordTextView)
        ttsButton = findViewById(R.id.ttsButton)
        favoriteButton = findViewById(R.id.favoriteButton)

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
            currentSearchedWord?.let { speakWord(it) }
        }

        favoriteButton.setOnClickListener {
            currentSearchedWord?.let { word ->
                toggleFavoriteStatus(word)
            }
        }

        favoritesActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val wordFromFavorites = result.data?.getStringExtra(EXTRA_SEARCH_WORD)
                if (wordFromFavorites != null) {
                    Log.d(TAG, "Received word from FavoritesActivity: $wordFromFavorites")
                    wordAutoCompleteTextView.setText(wordFromFavorites, false)
                    performSearch(wordFromFavorites)
                } else if (result.data?.getBooleanExtra(FavoritesActivity.RESULT_FAVORITES_MODIFIED, false) == true) {
                    Log.d(TAG, "Favorites list modified, updating current word's favorite icon if visible.")
                    currentSearchedWord?.let {
                        // Re-check favorite status from DB as it might have changed
                        val isFavoriteNow = dbHelper.isWordFavorite(it)
                        updateFavoriteIcon(isFavoriteNow)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                favoritesActivityLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showWelcomeState() {
        currentSearchedWord = null
        welcomeMessageContainer.visibility = View.VISIBLE
        searchedWordContainer.visibility = View.GONE
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        Log.d(TAG, "UI State: Welcome")
    }

    private fun showResultsState(searchedWord: String) {
        currentSearchedWord = searchedWord
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        lifecycleScope.launch { // Fetch favorite status from DB
            val isFavorite = withContext(Dispatchers.IO) { dbHelper.isWordFavorite(searchedWord) }
            updateFavoriteIcon(isFavorite)
        }
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        Log.d(TAG, "UI State: Results for $searchedWord")
    }

    private fun showNoResultsState(searchedWord: String) {
        currentSearchedWord = searchedWord
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        lifecycleScope.launch { // Fetch favorite status from DB
            val isFavorite = withContext(Dispatchers.IO) { dbHelper.isWordFavorite(searchedWord) }
            updateFavoriteIcon(isFavorite)
        }
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
        Log.d(TAG, "UI State: No Results for $searchedWord")
    }
    
    private fun showSearchingState(searchedWord: String) {
        currentSearchedWord = searchedWord
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        lifecycleScope.launch { // Fetch favorite status from DB
            val isFavorite = withContext(Dispatchers.IO) { dbHelper.isWordFavorite(searchedWord) }
            updateFavoriteIcon(isFavorite)
        }
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        definitionsContainer.removeAllViews()
        Log.d(TAG, "UI State: Searching for $searchedWord")
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
                    if (query.isEmpty() && definitionsContainer.childCount == 0 && noResultsTextView.visibility == View.GONE) {
                        // If input is cleared and no results/welcome shown, show welcome
                        // This condition might need refinement based on desired UX
                    }
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
            // If search is empty and results were shown, revert to welcome state
            if (resultsScrollView.visibility == View.VISIBLE || noResultsTextView.visibility == View.VISIBLE) {
                showWelcomeState()
            }
            return
        }

        showSearchingState(word)

        lifecycleScope.launch {
            Log.d(TAG, "Coroutine launched for DB query for '$word'")
            val wordEntryResult = withContext(Dispatchers.IO) {
                dbHelper.getWordDefinition(word)
            }
            Log.d(TAG, "DB query finished for '$word', result found: ${wordEntryResult != null}")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && wordEntryResult.definitions != null && wordEntryResult.definitions.isNotEmpty()) {
                    displayDefinitions(wordEntryResult.definitions)
                    showResultsState(word)
                } else {
                    showNoResultsState(word)
                }
                wordAutoCompleteTextView.setText("", false)
            } else {
                Log.w(TAG, "performSearch: Not updating UI, activity no longer started. Word: '$word'")
            }
        }
    }

    private fun displayDefinitions(definitions: List<DefinitionDetail>) {
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
            } ?: run { examplesContainer.visibility = View.GONE }

            defDetail.synonyms?.takeIf { it.isNotEmpty() }?.let { syns ->
                synonymsContainer.visibility = View.VISIBLE
                synonymsTV.text = syns.joinToString(", ")
            } ?: run { synonymsContainer.visibility = View.GONE }

            defDetail.antonyms?.takeIf { it.isNotEmpty() }?.let { ants ->
                antonymsContainer.visibility = View.VISIBLE
                antonymsTV.text = ants.joinToString(", ")
            } ?: run { antonymsContainer.visibility = View.GONE }
            definitionsContainer.addView(itemView)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS: The Language specified is not supported!")
                Toast.makeText(this, getString(R.string.tts_language_not_supported), Toast.LENGTH_SHORT).show()
                ttsReady = false
            } else {
                Log.i(TAG, "TTS Initialization Successful.")
                ttsReady = true
                currentWordToSpeak?.let {
                    if(searchedWordContainer.visibility == View.VISIBLE && it == searchedWordTextView.text.toString().lowercase(Locale.getDefault())) {
                        // Speak only if the word currently shown is the one we intended to speak
                       // speakWord(it) // Auto-speak can be annoying, let user click
                    }
                }
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
    }

    private fun toggleFavoriteStatus(word: String) {
        val newFavoriteStatus = !currentWordIsFavorite
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                dbHelper.setWordFavoriteStatus(word, newFavoriteStatus)
            }
            if (success) {
                currentWordIsFavorite = newFavoriteStatus
                updateFavoriteIcon(newFavoriteStatus)
                val message = if (newFavoriteStatus) getString(R.string.word_favorited_message, word)
                              else getString(R.string.word_unfavorited_message, word)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Failed to update favorite.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        currentWordIsFavorite = isFavorite
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            favoriteButton.contentDescription = getString(R.string.remove_from_favorites_description)
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_outline)
            favoriteButton.contentDescription = getString(R.string.add_to_favorites_description)
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

    override fun onDestroy() {
        if (tts != null) {
            Log.d(TAG, "TTS Shutting down.")
            tts!!.stop()
            tts!!.shutdown()
            tts = null // Good practice to nullify
        }
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }
}
