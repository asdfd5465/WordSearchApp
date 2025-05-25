package com.offlinedictionary.pro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.activity.OnBackPressedCallback
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
    private var currentSearchedWordForUI: String? = null
    private var currentWordIsFavorite: Boolean = false

    private lateinit var favoritesActivityLauncher: ActivityResultLauncher<Intent>

    private var doubleBackToExitPressedOnce = false

    companion object {
        const val EXTRA_SEARCH_WORD = "extra_search_word_from_favorites"
        const val RESULT_FAVORITES_MODIFIED_FLAG = "favorites_modified_flag"
    }

    private val TAG = "WordSearchAppMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        mainToolbar = findViewById(R.id.mainToolbar)
        setSupportActionBar(mainToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        mainToolbar.setOnClickListener {
            if (welcomeMessageContainer.visibility != View.VISIBLE) {
                showWelcomeState()
                // wordAutoCompleteTextView is cleared in showWelcomeState
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
            Log.d(TAG, "Search button clicked via GO")
            val wordToSearch = wordAutoCompleteTextView.text.toString()
            performSearch(wordToSearch)
        }

        wordAutoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.d(TAG, "IME action search triggered")
                val wordToSearch = wordAutoCompleteTextView.text.toString()
                performSearch(wordToSearch)
                true
            } else { false }
        }
        wordAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedWord = parent.getItemAtPosition(position) as String
            Log.d(TAG, "Suggestion selected: $selectedWord")
            wordAutoCompleteTextView.setText(selectedWord, false)
            performSearch(selectedWord)
        }

        ttsButton.setOnClickListener {
            currentSearchedWordForUI?.let { speakWord(it) }
        }

        favoriteButton.setOnClickListener {
            currentSearchedWordForUI?.let { word ->
                toggleFavoriteStatus(word)
            }
        }

        favoritesActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "ActivityResult from Favorites: resultCode = ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val wordFromFavorites = result.data?.getStringExtra(EXTRA_SEARCH_WORD)
                val favoritesModified = result.data?.getBooleanExtra(RESULT_FAVORITES_MODIFIED_FLAG, false) ?: false

                if (wordFromFavorites != null) {
                    Log.i(TAG, "Received word from FavoritesActivity: '$wordFromFavorites', performing search.")
                    wordAutoCompleteTextView.setText(wordFromFavorites, false) // Set the text in search box
                    performSearch(wordFromFavorites)
                } else if (favoritesModified) {
                    Log.i(TAG, "Favorites list modified, updating current word's favorite icon if visible.")
                    currentSearchedWordForUI?.let { currentWord ->
                        lifecycleScope.launch {
                            val isFavoriteNow = withContext(Dispatchers.IO) { dbHelper.isWordFavorite(currentWord) }
                            updateFavoriteIcon(isFavoriteNow)
                        }
                    }
                } else {
                    Log.d(TAG, "ActivityResult: RESULT_OK but no specific action data.")
                }
            } else {
                Log.d(TAG, "ActivityResult: Not RESULT_OK (resultCode = ${result.resultCode})")
            }
        }

        // Setup custom back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back pressed. Welcome visible: ${welcomeMessageContainer.visibility == View.VISIBLE}")
                if (resultsScrollView.visibility == View.VISIBLE ||
                    noResultsTextView.visibility == View.VISIBLE ||
                    (searchedWordContainer.visibility == View.VISIBLE && welcomeMessageContainer.visibility != View.VISIBLE) ) {
                    Log.d(TAG, "Back press: Reverting to welcome state.")
                    showWelcomeState()
                    // wordAutoCompleteTextView cleared in showWelcomeState
                    hideKeyboardAndClearFocus()
                } else {
                    if (doubleBackToExitPressedOnce) {
                        Log.d(TAG, "Back press: Second press, exiting.")
                        finishAffinity()
                        return
                    }
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(this@MainActivity, "Press BACK again to exit", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Back press: First press, showing exit toast.")
                    Handler(Looper.getMainLooper()).postDelayed({
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            }
        })
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
        currentSearchedWordForUI = null
        if(::wordAutoCompleteTextView.isInitialized) { // Check if initialized before accessing
            wordAutoCompleteTextView.setText("", false)
        }
        welcomeMessageContainer.visibility = View.VISIBLE
        searchedWordContainer.visibility = View.GONE
        resultsScrollView.visibility = View.GONE
        definitionsContainer.removeAllViews()
        noResultsTextView.visibility = View.GONE
        Log.d(TAG, "UI State: Welcome")
    }

    private fun updateUiForNewWordSearch(searchedWord: String) {
        currentSearchedWordForUI = searchedWord
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        currentWordToSpeak = searchedWord
        lifecycleScope.launch {
            val isFavorite = withContext(Dispatchers.IO) { dbHelper.isWordFavorite(searchedWord) }
            updateFavoriteIcon(isFavorite)
        }
    }

    private fun showResultsState() { // Parameter 'searchedWord' removed, uses currentSearchedWordForUI
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        // searchedWordTextView and favoriteIcon updated by updateUiForNewWordSearch (called in showSearchingState)
        resultsScrollView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
        Log.d(TAG, "UI State: Results shown for $currentSearchedWordForUI")
    }

    private fun showNoResultsState() { // Parameter 'searchedWord' removed
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        // searchedWordTextView and favoriteIcon updated by updateUiForNewWordSearch (called in showSearchingState)
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
        Log.d(TAG, "UI State: No Results for $currentSearchedWordForUI")
    }

    private fun showSearchingState(wordBeingSearched: String) {
        updateUiForNewWordSearch(wordBeingSearched)
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        definitionsContainer.removeAllViews()
        Log.d(TAG, "UI State: Searching for $wordBeingSearched")
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
                        val suggestions = withContext(Dispatchers.IO) { dbHelper.getWordSuggestions(query) }
                        suggestionsAdapter.clear()
                        suggestionsAdapter.addAll(suggestions)
                        suggestionsAdapter.filter.filter(null)
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
        Log.i(TAG, "performSearch called with word: '$word'")
        hideKeyboardAndClearFocus()

        if (word.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_word), Toast.LENGTH_SHORT).show()
            if (searchedWordContainer.visibility == View.VISIBLE) {
                showWelcomeState()
            }
            return
        }

        showSearchingState(word)

        lifecycleScope.launch {
            Log.d(TAG, "performSearch: Coroutine launched for DB query for '$word'")
            val wordEntryResult = withContext(Dispatchers.IO) {
                dbHelper.getWordDefinition(word)
            }
            Log.d(TAG, "performSearch: DB query finished for '$word', result found: ${wordEntryResult != null}")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && wordEntryResult.definitions != null && wordEntryResult.definitions.isNotEmpty()) {
                    displayDefinitions(wordEntryResult.definitions)
                    showResultsState()
                } else {
                    showNoResultsState()
                }
                // Removed wordAutoCompleteTextView.setText("", false) to keep searched word visible
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
            if (!ttsReady) Toast.makeText(this, getString(R.string.tts_init_failed), Toast.LENGTH_SHORT).show()
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
        val focusedView = currentFocus ?: window.decorView
        inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
        // focusedView.clearFocus() // This might cause issues if called too aggressively.
                                  // AutoCompleteTextView might lose focus naturally.
        Log.d(TAG, "Keyboard hidden.")
        if (::wordAutoCompleteTextView.isInitialized) {
             wordAutoCompleteTextView.clearFocus() // Specifically clear focus from our input
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
            tts!!.
