package vault.api

import io.ktor.client.engine.HttpClientEngineFactory

// Platform HTTP engine: CIO on Android/JVM, Darwin on iOS.
expect fun platformEngine(): HttpClientEngineFactory<*>
