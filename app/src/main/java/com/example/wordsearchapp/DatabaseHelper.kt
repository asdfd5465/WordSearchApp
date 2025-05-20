package com.example.wordsearchapp // Use your actual package name

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

    private val gson = Gson() // For parsing JSON strings from DB

    companion object {
        private const val DATABASE_NAME = "dictionary.db"
        private const val DATABASE_VERSION = 1
        private val DB_PATH = "/data/data/${BuildConfig.APPLICATION_ID}/databases/" // Path to app's private DB storage
        private const val TAG = "DatabaseHelper"
    }

    init {
        copyDatabaseFromAssets()
    }

    private fun copyDatabaseFromAssets() {
        val dbExist = checkDataBase()
        if (dbExist) {
            Log.i(TAG, "Database already exists.")
        } else {
            this.readableDatabase // Creates an empty DB in the default system path
            this.close() // Close it so we can overwrite it
            try {
                Log.i(TAG, "Database does not exist. Copying from assets...")
                val myInput = context.assets.open(DATABASE_NAME)
                val outFileName = DB_PATH + DATABASE_NAME

                // Ensure the directory exists
                val dbDir = java.io.File(DB_PATH)
                if (!dbDir.exists()) {
                    dbDir.mkdirs()
                }

                val myOutput = FileOutputStream(outFileName)
                val buffer = ByteArray(1024)
                var length: Int
                while (myInput.read(buffer).also { length = it } > 0) {
                    myOutput.write(buffer, 0, length)
                }
                myOutput.flush()
                myOutput.close()
                myInput.close()
                Log.i(TAG, "Database copied successfully from assets to $outFileName")
            } catch (e: IOException) {
                Log.e(TAG, "Error copying database from assets", e)
                throw Error("Error copying database") // Propagate error
            }
        }
    }

    private fun checkDataBase(): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            val myPath = DB_PATH + DATABASE_NAME
            val dbFile = java.io.File(myPath)
            if (dbFile.exists()) {
                checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Database does't exist yet (or error opening): ${e.message}")
        }
        checkDB?.close()
        return checkDB != null
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Tables are already created in the pre-populated DB from assets.
        // This method is called if the database did not exist and had to be created by SQLiteOpenHelper.
        // Since we copy a pre-existing DB, this might not be strictly necessary
        // unless copyDatabaseFromAssets() fails and SQLiteOpenHelper tries to create one.
        Log.i(TAG, "onCreate called (should ideally not happen if DB is copied from assets)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades here if you release new versions
        // with a different DB structure. For now, we can leave it empty or log.
        Log.i(TAG, "onUpgrade called from version $oldVersion to $newVersion")
        // Example: db?.execSQL("DROP TABLE IF EXISTS words")
        // onCreate(db)
    }

    // --- Query Methods ---

    fun getWordSuggestions(query: String, limit: Int = 10): List<String> {
        val suggestions = mutableListOf<String>()
        val db = this.readableDatabase
        // Using COLLATE NOCASE in table creation handles case-insensitivity.
        // If not, use `UPPER(word_text) LIKE UPPER(?)`
        val cursor = db.rawQuery(
            "SELECT word_text FROM words WHERE word_text LIKE ? ORDER BY word_text LIMIT ?",
            arrayOf("$query%", limit.toString())
        )

        try {
            if (cursor.moveToFirst()) {
                do {
                    val wordTextColumnIndex = cursor.getColumnIndex("word_text")
                    if (wordTextColumnIndex != -1) {
                        suggestions.add(cursor.getString(wordTextColumnIndex))
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting word suggestions", e)
        } finally {
            cursor.close()
            // db.close() // SQLiteOpenHelper manages DB closing
        }
        Log.d(TAG, "Suggestions for '$query': $suggestions")
        return suggestions
    }

    fun getWordDefinition(word: String): WordDefinitionEntry? {
        val db = this.readableDatabase
        var wordEntry: WordDefinitionEntry? = null
        val definitionsList = mutableListOf<DefinitionDetail>()

        // Using COLLATE NOCASE in table creation handles case-insensitivity.
        // If not, use `UPPER(word_text) LIKE UPPER(?)`
        val cursor = db.rawQuery(
            "SELECT part_of_speech, definition_text, examples_json, synonyms_json, antonyms_json FROM definitions WHERE word_text = ?",
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
            // db.close() // SQLiteOpenHelper manages DB closing
        }
        Log.d(TAG, "Definition for '$word': ${wordEntry != null}")
        return wordEntry
    }

    fun wordExists(word: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM words WHERE word_text = ? LIMIT 1",
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
}
