package dict.nick

import android.app.Application
import android.util.Log

class DictionaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("DictionaryApp", "Application onCreate")
        // Initialize global components if needed (e.g., DI, Logging)
    }
}
