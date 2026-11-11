# Isolated Android account storage probe

`AccountStorageRunner.kt` uses a random probe namespace/key under noBackupFilesDir.
It never opens main account state, local collection, photos or the user's DB.
Checks Keystore non-exportability, ciphertext absence of the synthetic secret,
restart persistence, atomic competing consumes, durable pending state, corrupt
record denial, explicit erase and subsequent reuse. Only safe PASS/failure codes
and device model/OS are printed; probe credentials/key are removed.

Build `:composeApp:assembleDebug :composeApp:assembleDebugAndroidTest
-Pvault.testRunner=vault.account.AccountStorageRunner` (one command), install
both APKs in place and run `adb shell am instrument -w
vault.app.test/vault.account.AccountStorageRunner`. The Gradle property selects
the test APK runner; the default remains PdfDeviceRunner. This is automated evidence,
not owner device/locked-screen/accessibility or release acceptance.
