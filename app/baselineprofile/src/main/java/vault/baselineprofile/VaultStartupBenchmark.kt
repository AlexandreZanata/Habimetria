package vault.baselineprofile

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Reference-device gate for the U11 cold-start budget (release APK,
// median timeToInitialDisplayMs <= 2000ms read from the benchmark
// report). Compiles on Linux; executes only on the reference device
// (device + release gate, never CI):
//   ./gradlew :composeApp:assembleRelease
//   ./gradlew :baselineprofile:connectedAndroidTest \
//     -Pandroid.testInstrumentationRunnerArguments.class=vault.baselineprofile.VaultStartupBenchmark
@RunWith(AndroidJUnit4::class)
class VaultStartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStart() = rule.measureRepeated(
        packageName = "vault.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Cofre de Vistoria")), 5_000)
    }
}
