package vault.api

import android.content.Context

// AppContext holds the Android application context for shared code.
object AppContext {
    lateinit var app: Context
}

actual fun appFilesDir(): String = AppContext.app.filesDir.resolve("vault").absolutePath
