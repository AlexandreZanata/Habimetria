package vault.ui

// ServerConfig is the single place for the API origin.
// Device test builds point at the LAN backend; emulator uses 10.0.2.2.
object ServerConfig {
    const val DEFAULT_BASE_URL = "http://172.19.2.11:8080"
}
