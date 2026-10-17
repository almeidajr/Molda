plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.almeidajr.molda"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.almeidajr.molda"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        checkAllWarnings = true
        checkTestSources = true
        // Dependabot owns update notices; available releases must not change CI results.
        disable += setOf("GradleDependency", "NewerVersionAvailable")
        // Opt-in library check: accessors for file-private Kotlin helpers are trivial and R8 inlines them.
        disable += "SyntheticAccessor"
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        // K2 reports unused variables and similar IDE-level diagnostics only with -Wextra.
        extraWarnings = true
        freeCompilerArgs.add("-Xreturn-value-checker=check")
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    failOnSeverity = dev.detekt.gradle.extensions.FailOnSeverity.Info
    config.setFrom(rootProject.file("config/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}

dependencies {
    implementation(libs.rive.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.adaptive.navigation3)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.window.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
