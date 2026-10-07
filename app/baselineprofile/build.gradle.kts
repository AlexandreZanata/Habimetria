plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "vault.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":composeApp"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation(libs.benchmark.macro)
    implementation(libs.uiautomator)
    implementation(libs.test.junit)
    implementation(libs.test.ext.junit)
}
