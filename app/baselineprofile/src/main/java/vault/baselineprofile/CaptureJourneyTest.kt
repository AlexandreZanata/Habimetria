package vault.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Critical user journey for AOT compilation: cold start, Home render,
// address typing, start flow. Generation needs a rooted/userdebug
// device or emulator (M5 device pass):
//   ./gradlew :composeApp:generateBaselineProfile
@RunWith(AndroidJUnit4::class)
class CaptureJourneyTest {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect("vault-startup", includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Protect this tenancy")), 5_000)
    }
}
