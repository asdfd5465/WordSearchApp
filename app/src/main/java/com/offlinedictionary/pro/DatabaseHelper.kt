package com.offlinedictionary.pro

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileOutputStream
import java.io.IOException
import java.io.File // Added import for java.io.File

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_NAME = "dictionary.db"
        private const val DATABASE_VERSION = 1
        // Corrected DB_PATH to use context.applicationInfo.dataDir for robustness
        private fun getDbPath(context: Context): String =
            context.applicationInfo.dataDir + "/databases/"

        private const val TAG = "DatabaseHelper"

        // Table and Column Names
        private const val TABLE_WORDS = "words"
        private const val COLUMN_WORD_TEXT = "word_text"
        private const val COLUMN_IS_FAVORITE = "is_favorite"

        private const val TABLE_DEFINITIONS = "definitions"
    }

    init {
        // Ensure the database is copied when the helper is initialized
        copyDatabaseFromAssets()
    }

    @Synchronized // Ensure only one thread copies the DB
    private fun copyDatabaseFromAssets() {
        val dbPathWithDbName = getDbPath(context) + DATABASE_NAME
        if (checkDataBase(dbPathWithDbName)) {
            Log.i(TAG, "Database already exists at $dbPathWithDbName.")
            return
        }

        // Ensure the databases directory exists.
        val dbDir = File(getDbPath(context))
        if (!dbDir.exists()) {
            if (!dbDir.mkdirs()) {
                Log.e(TAG, "Failed to create database directory: ${dbDir.absolutePath}")
                throw IOException("Failed to create database directory: ${dbDir.absolutePath}")
            }
        }
        
        // By calling this method, an empty database will be created into the default system path
        // of your application so we are gonna be able to overwrite that database with our database.
        this.readableDatabase 
        this.close() // Close the empty DB so we can overwrite. IMPORTANT!

        try {
            Log.i(TAG, "Database does not exist. Copying from assets to $dbPathWithDbName...")
            val myInput = context.assets.open(DATABASE_NAME)
            val myOutput = FileOutputStream(dbPathWithDbName)
            val buffer = ByteArray(1024)
            var length: Int
            while (myInput.read(buffer).also { length = it } > 0) {
                myOutput.write(buffer, 0, length)
            }
            myOutput.flush()
            myOutput.close()
            myInput.close()
            Log.i(TAG, "Database copied successfully from assets.")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying database from assets", e)
            throw Error("Error copying database: ${e.message}")
        }
    }

    private fun checkDataBase(dbPathWithDbName: String): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            val dbFile = File(dbPathWithDbName)
            if (dbFile.exists()) {
                checkDB = SQLiteDatabase.openDatabase(dbPathWithDbName, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Database doesn't exist yet or error opening for check: ${e.message}")
        }
        checkDB?.close()
        return checkDB != null
    }


    override fun onCreate(db: SQLiteDatabase?) {
        Log.i(TAG, "onCreate called - this should not happen if DB is copied correctly.")
        // If for some reason copy fails and SQLiteOpenHelper creates the DB,
        // you might want to log an error or attempt to recopy.
        // The tables are expected to be in the copied DB.
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "onUpgrade called from version $oldVersion to $newVersion. " +
                   "Current implementation will require app reinstall for DB schema changes if asset DB is updated.")
        // For a simple app with a pre-populated DB from assets, a common strategy for
        // schema changes is to increment DATABASE_VERSION, and then in copyDatabaseFromAssets,
        // you might add logic to delete the old DB if the version in assets is newer.
        // Or, for simplicity, instruct users to reinstall if the DB schema changes significantly.
        // If you were creating tables here, you'd drop and recreate them:
        // db?.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        // db?.execSQL("DROP TABLE IF EXISTS $TABLE_DEFINITIONS")
        // onCreate(db)
    }

    // --- Query Methods ---
    // (getWordSuggestions, getWordDefinition, wordExists, isWordFavorite, setWordFavoriteStatus, getFavoriteWords)
    // Ensure these methods are correctly defined as provided in the previous response.
    // I'll re-paste them here for completeness.

    fun getWordSuggestions(query: String, limit: Int = 10): List<String> {
        val suggestions = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_WORD_TEXT FROM $TABLE_WORDS WHERE $COLUMN_WORD_TEXT LIKE ? ORDER BY $COLUMN_WORD_TEXT LIMIT ?",
            arrayOf("$query%", limit.toString())
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    val wordTextColumnIndex = cursor.getColumnIndex(COLUMN_WORD_TEXT)
                    if (wordTextColumnIndex != -1) {
                        suggestions.add(cursor.getString(wordTextColumnIndex))
                    } else {
                        Log.w(TAG, "Column '$COLUMN_WORD_TEXT' not found in words table for suggestions.")
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting word suggestions for '$query'", e)
        } finally {
            cursor.close()
        }
        Log.d(TAG, "Suggestions for '$query': $suggestions")
        return suggestions
    }

    fun getWordDefinition(word: String): WordDefinitionEntry? {
        val db = this.readableDatabase
        var wordEntry: WordDefinitionEntry? = null
        val definitionsList = mutableListOf<DefinitionDetail>()

        val cursor = db.rawQuery(
            "SELECT part_of_speech, definition_text, examples_json, synonyms_json, antonyms_json FROM $TABLE_DEFINITIONS WHERE $COLUMN_WORD_TEXT = ?",
            arrayOf(word)
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    val posColumnIndex = cursor.getColumnIndex("part_of_speech")
                    val defTextColumnIndex = cursor.getColumnIndex("definition_text")
                    val examplesJsonColumnIndex = cursor.getColumnIndex("examples_json")
                    val synonymsJsonColumnIndex = cursor.getColumnIndex("synonyms_json")
                    val antonymsJsonColumnIndex = cursor.getColumnIndex("antonyms_json")

                    val partOfSpeech = if (posColumnIndex != -1) cursor.getString(posColumnIndex) else null
                    val definitionText = if (defTextColumnIndex != -1) cursor.getString(defTextColumnIndex) else null

                    val examplesJson = if (examplesJsonColumnIndex != -1) cursor.getString(examplesJsonColumnIndex) else "[]"
                    val synonymsJson = if (synonymsJsonColumnIndex != -1) cursor.getString(synonymsJsonColumnIndex) else "[]"
                    val antonymsJson = if (antonymsJsonColumnIndex != -1) cursor.getString(antonymsJsonColumnIndex) else "[]"

                    val listType = object : TypeToken<List<String>>() {}.type
                    val examples: List<String> = gson.fromJson(examplesJson, listType)
                    val synonyms: List<String> = gson.fromJson(synonymsJson, listType)
                    val antonyms: List<String> = gson.fromJson(antonymsJson, listType)

                    definitionsList.add(
                        DefinitionDetail(
                            partOfSpeech = partOfSpeech,
                            definition = definitionText,
                            examples = examples,
                            synonyms = synonyms,
                            antonyms = antonyms
                        )
                    )
                } while (cursor.moveToNext())

                if (definitionsList.isNotEmpty()) {
                    wordEntry = WordDefinitionEntry(word = word, definitions = definitionsList)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting word definition for '$word'", e)
        } finally {
            cursor.close()
        }
        Log.d(TAG, "Definition for '$word': ${wordEntry != null}")
        return wordEntry
    }

     fun wordExists(word: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_WORDS WHERE $COLUMN_WORD_TEXT = ? LIMIT 1",
            arrayOf(word)
        )
        var exists = false
        try {
            exists = cursor.count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if word exists: '$word'", e)
        } finally {
            cursor.close()
        }
        Log.d(TAG, "Word '$word' exists: $exists")
        return exists
    }

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
                } else {
                     Log.w(TAG, "Column '$COLUMN_IS_FAVORITE' not found in words table.")
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
                Log.w(TAG, "Failed to update favorite status for word '$word' (word might not exist or status unchanged)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting favorite status for word '$word'", e)
        }
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
                    } else {
                        Log.w(TAG, "Column '$COLUMN_WORD_TEXT' not found in words table for favorites.")
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
