package dict.nick.utils

import android.content.Context
import java.io.IOException
import java.io.InputStream

fun Context.loadJsonFromAsset(fileName: String): String? {
    return try {
        val inputStream: InputStream = assets.open(fileName)
        inputStream.bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace() // Log the error
        null
    }
}
