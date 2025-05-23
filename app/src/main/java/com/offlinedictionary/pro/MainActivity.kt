package com.offlinedictionary.pro // Your package name

// ... other imports ...
import android.app.Activity
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.view.Menu // For Toolbar Menu
import android.view.MenuItem // For Toolbar Menu
import androidx.activity.result.ActivityResultLauncher // For Activity Result
import androidx.activity.result.contract.ActivityResultContracts // For Activity Result
import com.google.android.material.appbar.MaterialToolbar // For Toolbar

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // ... (existing private lateinit vars) ...
    private lateinit var favoriteButton: ImageButton // Added
    private lateinit var mainToolbar: MaterialToolbar // Added

    private var currentSearchedWord: String? = null // To keep track of the displayed word
    private var currentWordIsFavorite: Boolean = false

    // ActivityResultLauncher for FavoritesActivity
    private lateinit var favoritesActivityLauncher: ActivityResultLauncher<Intent>


    companion object {
        const val EXTRA_SEARCH_WORD = "extra_search_word"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate called")

        mainToolbar = findViewById(R.id.mainToolbar)
        setSupportActionBar(mainToolbar)

        dbHelper = DatabaseHelper(this)
        tts = TextToSpeech(this, this)

        // ... (findViewById for other views as before) ...
        wordAutoCompleteTextView = findViewById(R.id.wordEditText)
        searchButton = findViewById(R.id.searchButton)
        definitionsContainer = findViewById(R.id.definitionsContainer)
        resultsScrollView = findViewById(R.id.resultsScrollView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
        welcomeMessageContainer = findViewById(R.id.welcomeMessageContainer)
        searchedWordContainer = findViewById(R.id.searchedWordContainer)
        searchedWordTextView = findViewById(R.id.searchedWordTextView)
        ttsButton = findViewById(R.id.ttsButton)
        favoriteButton = findViewById(R.id.favoriteButton) // Initialize new button

        showWelcomeState()
        setupAutoComplete()
        // ... (button listeners as before) ...
        searchButton.setOnClickListener { /* ... */ }
        wordAutoCompleteTextView.setOnEditorActionListener { /* ... */ }
        wordAutoCompleteTextView.setOnItemClickListener { /* ... */ }
        ttsButton.setOnClickListener {
            currentSearchedWord?.let { speakWord(it) } // Use currentSearchedWord
        }

        favoriteButton.setOnClickListener {
            currentSearchedWord?.let { word ->
                toggleFavoriteStatus(word)
            }
        }

        // Register for Activity Result from FavoritesActivity
        favoritesActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(EXTRA_SEARCH_WORD)?.let { wordFromFavorites ->
                    // User clicked a word in favorites, search for it
                    wordAutoCompleteTextView.setText(wordFromFavorites, false) // Populate search bar
                    performSearch(wordFromFavorites)
                }
                // Check if favorites were modified to update heart icon if current word was affected
                if (result.data?.getBooleanExtra(FavoritesActivity.RESULT_FAVORITES_MODIFIED, false) == true) {
                    currentSearchedWord?.let { updateFavoriteIcon(dbHelper.isWordFavorite(it)) }
                }
            }
        }
    }

    // --- Toolbar Menu ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorites -> {
                val intent = Intent(this, FavoritesActivity::class.java)
                favoritesActivityLauncher.launch(intent) // Use launcher
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // --- UI State Management ---
    // showWelcomeState(), showResultsState(), showNoResultsState(), showSearchingState()
    // In these methods, when searchedWordContainer becomes VISIBLE, update the favorite icon status.

    private fun showWelcomeState() {
        currentSearchedWord = null // No word is active
        // ... (rest of the visibility logic)
        welcomeMessageContainer.visibility = View.VISIBLE
        searchedWordContainer.visibility = View.GONE
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
    }

    private fun showResultsState(searchedWord: String) {
        currentSearchedWord = searchedWord // Store the current word
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        updateFavoriteIcon(dbHelper.isWordFavorite(searchedWord)) // Update heart
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.VISIBLE
        noResultsTextView.visibility = View.GONE
    }

    private fun showNoResultsState(searchedWord: String) {
        currentSearchedWord = searchedWord
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        updateFavoriteIcon(dbHelper.isWordFavorite(searchedWord)) // Update heart
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.VISIBLE
    }

    private fun showSearchingState(searchedWord: String) {
        currentSearchedWord = searchedWord
        welcomeMessageContainer.visibility = View.GONE
        searchedWordContainer.visibility = View.VISIBLE
        searchedWordTextView.text = searchedWord.uppercase(Locale.getDefault())
        updateFavoriteIcon(dbHelper.isWordFavorite(searchedWord)) // Update heart on search start
        currentWordToSpeak = searchedWord
        resultsScrollView.visibility = View.GONE
        noResultsTextView.visibility = View.GONE
        definitionsContainer.removeAllViews()
    }


    // ... performSearch() needs to call the new state methods
    private fun performSearch(wordToSearch: String) {
        val word = wordToSearch.trim()
        // ... (hideKeyboard, empty check)
        hideKeyboardAndClearFocus()
        if (word.isEmpty()) { /* ... */ return }

        showSearchingState(word) // Set UI for searching this word

        lifecycleScope.launch {
            val wordEntryResult = withContext(Dispatchers.IO) { /* dbHelper.getWordDefinition(word) */ }
            
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (wordEntryResult != null && /* ... definitions not empty ... */) {
                    displayDefinitions(wordEntryResult.definitions!!)
                    showResultsState(word) // Update UI state after results
                } else {
                    showNoResultsState(word) // Update UI state for no results
                }
                wordAutoCompleteTextView.setText("", false)
            } else { /* ... */ }
        }
    }
    // ... displayDefinitions() remains the same
    // ... TTS methods (onInit, speakWord) remain the same
    // ... hideKeyboardAndClearFocus() remains the same
    // ... lifecycle methods (onStart, onResume, onPause, onStop, onDestroy) remain the same, ensure TTS shutdown in onDestroy

    // --- Favorite Logic ---
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
        currentWordIsFavorite = isFavorite // Keep track of current state
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            favoriteButton.contentDescription = getString(R.string.remove_from_favorites_description)
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_outline)
            favoriteButton.contentDescription = getString(R.string.add_to_favorites_description)
        }
    }
}
