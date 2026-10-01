package vault.queue

class LocalQueueCorruptException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
