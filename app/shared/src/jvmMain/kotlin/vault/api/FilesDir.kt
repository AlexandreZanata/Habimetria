package vault.api

import java.io.File

actual fun appFilesDir(): String =
    File(System.getProperty("java.io.tmpdir"), "vault").absolutePath
