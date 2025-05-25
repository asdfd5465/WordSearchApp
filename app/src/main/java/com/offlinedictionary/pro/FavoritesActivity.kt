package com.offlinedictionary.pro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope // Using GlobalScope for simplicity in adapter, consider other scopes
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyFavoritesTextView: TextView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var dbHelper: DatabaseHelper
    private val TAG = "FavoritesActivity"

    private var favoritesWereModifiedInThisSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val toolbar: MaterialToolbar = findViewById(R.id.favoritesToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        dbHelper = DatabaseHelper(this)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyFavoritesTextView = findViewById(R.id.emptyFavoritesTextView)

        setupRecyclerView()
        loadFavoriteWords()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onItemClick = { word ->
                Log.i(TAG, "Favorite item clicked: '$word'. Sending back to MainActivity.")
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.Companion.EXTRA_SEARCH_WORD, word)
                if (favoritesWereModifiedInThisSession) { // Also pass modification if it happened before click
                    resultIntent.putExtra(MainActivity.Companion.RESULT_FAVORITES_MODIFIED_FLAG, true)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            },
            onRemoveFavoriteClick = { word ->
                removeWordFromFavorites(word)
            }
        )
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesRecyclerView.adapter = favoritesAdapter
        val decoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        favoritesRecyclerView.addItemDecoration(decoration)
    }

    private fun loadFavoriteWords() {
        Log.d(TAG, "Loading favorite words...")
        GlobalScope.launch(Dispatchers.Main) {
            val favWords = withContext(Dispatchers.IO) {
                dbHelper.getFavoriteWords()
            }
            if (favWords.isEmpty()) {
                favoritesRecyclerView.visibility = View.GONE
                emptyFavoritesTextView.visibility = View.VISIBLE
                Log.d(TAG, "No favorite words found.")
            } else {
                favoritesRecyclerView.visibility = View.VISIBLE
                emptyFavoritesTextView.visibility = View.GONE
                favoritesAdapter.submitList(favWords)
                Log.d(TAG, "Displaying ${favWords.size} favorite words.")
            }
        }
    }

    private fun removeWordFromFavorites(word: String) {
        Log.d(TAG, "Attempting to remove '$word' from favorites.")
        GlobalScope.launch(Dispatchers.Main) {
            val success = withContext(Dispatchers.IO) {
                dbHelper.setWordFavoriteStatus(word, false)
            }
            if (success) {
                Toast.makeText(this@FavoritesActivity, getString(R.string.word_unfavorited_message, word), Toast.LENGTH_SHORT).show()
                favoritesWereModifiedInThisSession = true
                loadFavoriteWords()
            } else {
                Toast.makeText(this@FavoritesActivity, "Could not remove favorite.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("This method is deprecated in API level 33 (Tiramisu) and higher. Use OnBackPressedDispatcher directly.")
    override fun onBackPressed() {
        Log.d(TAG, "Back pressed in FavoritesActivity")
        prepareResultAndFinish()
        // Note: We don't call super.onBackPressed() here because prepareResultAndFinish() calls super.finish()
    }

    private fun prepareResultAndFinish() {
        Log.d(TAG, "Preparing result. Favorites modified: $favoritesWereModifiedInThisSession")
        if (favoritesWereModifiedInThisSession) {
            val resultIntent = Intent()
            resultIntent.putExtra(MainActivity.Companion.RESULT_FAVORITES_MODIFIED_FLAG, true)
            setResult(Activity.RESULT_OK, resultIntent)
            Log.d(TAG, "Set RESULT_OK with modification flag.")
        } else {
            // If no modifications, and no item was clicked to set a result, set RESULT_CANCELED
            // Checking if a result was already set (e.g., by onItemClick)
            if (resultCode == Activity.RESULT_CANCELED) { // Check current result code if necessary
                 Log.d(TAG, "Set RESULT_CANCELED as no modifications were made and no item clicked.")
                 setResult(Activity.RESULT_CANCELED)
            } else {
                Log.d(TAG, "Result already set (likely by item click), not overriding.")
            }
        }
        super.finish() // Call super.finish() to ensure proper activity closing
    }


    class FavoritesAdapter(
        private val onItemClick: (String) -> Unit,
        private val onRemoveFavoriteClick: (String) -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

        private var words: List<String> = emptyList()

        fun submitList(newWords: List<String>) {
            words = newWords
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_word, parent, false)
            return FavoriteViewHolder(view, onItemClick, onRemoveFavoriteClick)
        }

        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            holder.bind(words[position])
        }

        override fun getItemCount(): Int = words.size

        class FavoriteViewHolder(
            itemView: View,
            private val onItemClickLambda: (String) -> Unit,
            private val onRemoveFavoriteClickLambda: (String) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            private val wordTextView: TextView = itemView.findViewById(R.id.favoriteWordTextView)
            private val removeButton: ImageButton = itemView.findViewById(R.id.removeFavoriteButton)

            fun bind(word: String) {
                wordTextView.text = word
                itemView.setOnClickListener {
                    Log.d("FavoritesAdapter", "Item clicked: $word")
                    onItemClickLambda(word)
                }
                removeButton.setOnClickListener {
                    Log.d("FavoritesAdapter", "Remove clicked for: $word")
                    onRemoveFavoriteClickLambda(word)
                }
            }
        }
    }
}
