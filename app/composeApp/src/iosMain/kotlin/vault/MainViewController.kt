package vault

import androidx.compose.ui.window.ComposeUIViewController
import vault.ui.App

// Entry point embedded by iosApp's ContentView.
fun MainViewController() = ComposeUIViewController { App() }
