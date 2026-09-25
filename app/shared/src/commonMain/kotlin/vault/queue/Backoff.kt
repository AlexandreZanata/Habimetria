package vault.queue

// Backoff is exponential with a cap: 2s, 4s, 8s … 5min.
// Pure math, unit-tested. WorkManager mirrors it natively.
object Backoff {
    const val BASE_MS = 2_000L
    const val CAP_MS = 300_000L
    const val MAX_ATTEMPTS = 8

    fun delayMs(attempt: Int): Long {
        var d = BASE_MS
        repeat(attempt.coerceAtLeast(0)) { d = (d * 2).coerceAtMost(CAP_MS) }
        return d
    }
}
