package com.offlinedictionary.pro // Your package name

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileOutputStream
import java.io.IOException

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_NAME = "dictionary.db"
        private const val DATABASE_VERSION = 1 // Increment if schema changes (e.g., adding is_favorite)
        private val DB_PATH = "/data/data/${BuildConfig.APPLICATION_ID}/databases/"
        private const val TAG = "DatabaseHelper"

        // Table and Column Names
        private const val TABLE_WORDS = "words"
        private const val COLUMN_WORD_TEXT = "word_text"
        private const val COLUMN_IS_FAVORITE = "is_favorite" // New column

        private const val TABLE_DEFINITIONS = "definitions"
        // ... other definition columns
    }

    init {
        copyDatabaseFromAssets()
    }

    // ... copyDatabaseFromAssets() and checkDataBase() remain the same

    override fun onCreate(db: SQLiteDatabase?) {
        Log.i(TAG, "onCreate called - this should not happen if DB is copied correctly.")
        // If you reach here, it means the DB wasn't copied and SQLite is creating it.
        // You'd need to define table creation SQL here, but our primary path is copying.
        // For safety, you could include the table creation SQL your Python script uses.
        // Example (ensure this matches your Python script's table creation EXACTLY):
        /*
        val sqlCreateWordsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_WORDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WORD_TEXT TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0
            );
            """
        val sqlCreateDefinitionsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_DEFINITIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WORD_TEXT TEXT NOT NULL,
                part_of_speech TEXT,
                definition_text TEXT,
                examples_json TEXT,
                synonyms_json TEXT,
                antonyms_json TEXT,
                FOREIGN KEY ($COLUMN_WORD_TEXT) REFERENCES $TABLE_WORDS ($COLUMN_WORD_TEXT)
            );
            """
        db?.execSQL(sqlCreateWordsTable)
        db?.execSQL(sqlCreateDefinitionsTable)
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_word_text ON $TABLE_WORDS ($COLUMN_WORD_TEXT);")
        db?.execSQL("CREATE INDEX IF NOT EXISTS idx_definition_word_text ON $TABLE_DEFINITIONS ($COLUMN_WORD_TEXT);")
        */
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "onUpgrade called from version $oldVersion to $newVersion")
        // Handle DB schema migrations. For a simple addition like is_favorite,
        // if you ship a new DB, you might just delete the old one and recopy.
        // Or, if oldVersion < NEW_VERSION_WHERE_FAVORITE_ADDED:
        // db?.execSQL("ALTER TABLE $TABLE_WORDS ADD COLUMN $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0;")
        // For simplicity now, if you update the DB in assets, increment DATABASE_VERSION,
        // and the copy logic might need adjustment to handle overwriting if version changed.
        // Or, just uninstall/reinstall the app to get the new DB.
    }

    // ... getWordSuggestions, getWordDefinition, wordExists remain similar but ensure they use constants

    fun getWordSuggestions(query: String, limit: Int = 10): List<String> {
        val suggestions = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_WORD_TEXT FROM $TABLE_WORDS WHERE $COLUMN_WORD_TEXT LIKE ? ORDER BY $COLUMN_WORD_TEXT LIMIT ?",
            arrayOf("$query%", limit.toString())
        )
        // ... (rest of the method is the same) ...
        try { /* ... */ } catch (e: Exception) { /* ... */ } finally { cursor.close() }
        return suggestions
    }

    fun getWordDefinition(word: String): WordDefinitionEntry? {
        // ... (same logic, just ensure column names are correct if you used constants for them) ...
        val db = this.readableDatabase
        var wordEntry: WordDefinitionEntry? = null
        val definitionsList = mutableListOf<DefinitionDetail>()

        val cursor = db.rawQuery(
            "SELECT part_of_speech, definition_text, examples_json, synonyms_json, antonyms_json FROM $TABLE_DEFINITIONS WHERE $COLUMN_WORD_TEXT = ?",
            arrayOf(word)
        )
        // ... (rest of the parsing logic is the same) ...
        try { /* ... */ } catch (e: Exception) { /* ... */ } finally { cursor.close() }
        return wordEntry
    }

     fun wordExists(word: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_WORDS WHERE $COLUMN_WORD_TEXT = ? LIMIT 1",
            arrayOf(word)
        )
        // ... (rest of the method is the same) ...
        var exists = false; try { /* ... */ } catch (e: Exception) { /* ... */ } finally { cursor.close() }; return exists
    }


    // --- New Favorite Methods ---
    fun isWordFavorite(word: String): Boolean {
        val db = this.readableDatabase
        var isFavorite = false
        val cursor = db.rawQuery(
            "SELECT $COLUMN_IS_FAVORITE FROM $TABLE_WORDS WHERE $COLUMN_WORD_TEXT = ?",
            arrayOf(word)
        )
        try {
            if (cursor.moveToFirst()) {
                val isFavoriteColumnIndex = cursor.getColumnIndex(COLUMN_IS_FAVORITE)
                if (isFavoriteColumnIndex != -1) {
                    isFavorite = cursor.getInt(isFavoriteColumnIndex) == 1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if word '$word' is favorite", e)
        } finally {
            cursor.close()
        }
        return isFavorite
    }

    fun setWordFavoriteStatus(word: String, isFavorite: Boolean): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_IS_FAVORITE, if (isFavorite) 1 else 0)
        var success = false
        try {
            val rowsAffected = db.update(TABLE_WORDS, values, "$COLUMN_WORD_TEXT = ?", arrayOf(word))
            success = rowsAffected > 0
            if (success) {
                Log.d(TAG, "Word '$word' favorite status set to $isFavorite")
            } else {
                Log.w(TAG, "Failed to update favorite status for word '$word' (word might not exist in 'words' table or status was already set)")
                 // It's possible the word isn't in the words table if your dictionary.json processing
                 // didn't add all searchable words there. Or the word key in definitions isn't in words.
                 // For robustness, you might want to ensure the word exists in 'words' table before setting favorite.
                 // However, if it was searched and definitions found, it should exist.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting favorite status for word '$word'", e)
        }
        // db.close() // SQLiteOpenHelper handles this
        return success
    }

    fun getFavoriteWords(): List<String> {
        val favoriteWords = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_WORD_TEXT FROM $TABLE_WORDS WHERE $COLUMN_IS_FAVORITE = 1 ORDER BY $COLUMN_WORD_TEXT",
            null
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    val wordTextColumnIndex = cursor.getColumnIndex(COLUMN_WORD_TEXT)
                    if (wordTextColumnIndex != -1) {
                        favoriteWords.add(cursor.getString(wordTextColumnIndex))
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorite words", e)
        } finally {
            cursor.close()
        }
        Log.d(TAG, "Fetched favorite words: $favoriteWords")
        return favoriteWords
    }
}
