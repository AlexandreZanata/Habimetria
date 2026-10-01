package vault.ui

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import okio.FileSystem
import vault.api.VaultApi
import vault.api.appFilesDir
import vault.api.platformEngine
import vault.local.SyncMode
import vault.local.uploadsEnabled
import vault.queue.CaptureStore
import vault.queue.UploadPump
import java.util.concurrent.TimeUnit

// UploadWorker drains the queue off the UI thread with CONNECTED
// constraint and exponential backoff (mirrors Backoff in shared).
class UploadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val baseUrl = inputData.getString(KEY_BASE_URL) ?: return Result.failure()
        val fs = FileSystem.SYSTEM
        val api = VaultApi(baseUrl, platformEngine())
        val store = CaptureStore(fs, appFilesDir() + "/queue")
        UploadPump(store, api, fs).drain()
        return if (store.pending().isEmpty()) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_BASE_URL = "base_url"
        private const val UNIQUE = "vault-upload"

        // L1 gate: LOCAL_ONLY never schedules network work.
        fun scheduleIfAllowed(context: Context, baseUrl: String, mode: SyncMode) {
            if (!uploadsEnabled(mode)) return
            schedule(context, baseUrl)
        }

        fun schedule(context: Context, baseUrl: String) {
            val req = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(
                    androidx.work.Data.Builder().putString(KEY_BASE_URL, baseUrl).build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
