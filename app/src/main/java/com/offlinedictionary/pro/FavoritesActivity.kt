package com.offlinedictionary.pro // Your package name

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
import android.widget.Toast // Add this
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope // Or switch to lifecycleScope/viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyFavoritesTextView: TextView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var dbHelper: DatabaseHelper
    private val TAG = "FavoritesActivity"

    companion object {
        const val REQUEST_CODE_FAVORITES = 1001
        const val RESULT_FAVORITES_MODIFIED = "favorites_modified"
    }
    private var favoritesModified = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val toolbar: MaterialToolbar = findViewById(R.id.favoritesToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show Up button
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
                // When a favorite word is clicked, send it back to MainActivity to display
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.EXTRA_SEARCH_WORD, word)
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Close FavoritesActivity
            },
            onRemoveFavoriteClick = { word ->
                removeWordFromFavorites(word)
            }
        )
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesRecyclerView.adapter = favoritesAdapter
        favoritesRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
    }

    private fun loadFavoriteWords() {
        Log.d(TAG, "Loading favorite words...")
        // Use a coroutine to load from DB off the main thread
        GlobalScope.launch(Dispatchers.Main) { // For UI updates
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
                favoritesModified = true // Mark that changes were made
                loadFavoriteWords() // Refresh the list
            } else {
                Toast.makeText(this@FavoritesActivity, "Could not remove favorite.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed() // Or finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        if (favoritesModified) {
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_FAVORITES_MODIFIED, true)
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        super.finish()
    }
}

// Adapter for the RecyclerView
class FavoritesAdapter(
    private val onItemClick: (String) -> Unit,
    private val onRemoveFavoriteClick: (String) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    private var words: List<String> = emptyList()

    fun submitList(newWords: List<String>) {
        words = newWords
        notifyDataSetChanged() // Simple notification, consider DiffUtil for larger lists
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
        private val onItemClick: (String) -> Unit,
        private val onRemoveFavoriteClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val wordTextView: TextView = itemView.findViewById(R.id.favoriteWordTextView)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeFavoriteButton)

        fun bind(word: String) {
            wordTextView.text = word
            itemView.setOnClickListener { onItemClick(word) }
            removeButton.setOnClickListener { onRemoveFavoriteClick(word) }
        }
    }
}
