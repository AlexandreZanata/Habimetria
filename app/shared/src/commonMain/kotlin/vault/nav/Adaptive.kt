package vault.nav

// Adaptive policy shared by all platforms: breakpoints, content caps and
// grid columns derive from width only, so Android, iOS, tablets,
// foldables and multi-window behave identically. Units are dp integers;
// UI layers convert to Dp.
enum class WindowClass {
    COMPACT, MEDIUM, EXPANDED
}

fun windowClassFor(maxWidthDp: Int): WindowClass = when {
    maxWidthDp < 600 -> WindowClass.COMPACT
    maxWidthDp < 840 -> WindowClass.MEDIUM
    else -> WindowClass.EXPANDED
}

// Max content width per class; -1 means unbounded (phone portrait).
fun maxContentWidthDp(windowClass: WindowClass): Int = when (windowClass) {
    WindowClass.COMPACT -> -1
    WindowClass.MEDIUM -> 720
    WindowClass.EXPANDED -> 960
}

fun roomsColumns(windowClass: WindowClass): Int = when (windowClass) {
    WindowClass.COMPACT -> 1
    WindowClass.MEDIUM -> 2
    WindowClass.EXPANDED -> 3
}

// Wide layouts (landscape, tablet, foldable open) place preview and
// controls side by side; compact keeps the single column.
fun useSideBySide(windowClass: WindowClass): Boolean = windowClass != WindowClass.COMPACT
