// File Path: app/src/main/java/com/offlinedictionary/pro/FavoritesActivity.kt
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
import androidx.lifecycle.lifecycleScope // Changed from GlobalScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyFavoritesTextView: TextView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var dbHelper: DatabaseHelper
    private val TAG = "FavoritesActivity"

    companion object {
        // const val REQUEST_CODE_FAVORITES = 1001 // Not needed with ActivityResultLauncher
        const val RESULT_FAVORITES_MODIFIED_FLAG = "favorites_modified_flag" // Changed key
    }
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
                Log.d(TAG, "Favorite item clicked: $word")
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.EXTRA_SEARCH_WORD_FROM_FAVORITES, word)
                // Also indicate if favorites were modified, though clicking an item doesn't modify them here
                // But if a remove action happened before, we want MainActivity to know
                if (favoritesWereModifiedInThisSession) {
                    resultIntent.putExtra(RESULT_FAVORITES_MODIFIED_FLAG, true)
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
        val itemDecorator = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        // You might want to customize the decorator's drawable if the default is too prominent
        // itemDecorator.setDrawable(ContextCompat.getDrawable(this, R.drawable.your_custom_divider)!!)
        favoritesRecyclerView.addItemDecoration(itemDecorator)
    }

    private fun loadFavoriteWords() {
        Log.d(TAG, "Loading favorite words...")
        lifecycleScope.launch { // Use lifecycleScope
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
        lifecycleScope.launch { // Use lifecycleScope
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Ensure we set the result correctly when back is pressed
        if (favoritesWereModifiedInThisSession) {
            val resultIntent = Intent()
            resultIntent.putExtra(RESULT_FAVORITES_MODIFIED_FLAG, true)
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        super.onBackPressed()
    }


    // Adapter for the RecyclerView
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
}
