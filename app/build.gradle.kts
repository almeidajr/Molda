plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
        // The release workflow passes these from the tag; local builds are never beta builds or releases.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "0.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only the release workflow has the key (docs/adr/0001); elsewhere release builds stay unsigned.
        providers.environmentVariable("RELEASE_KEYSTORE_FILE").orNull?.let { keystore ->
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.window.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
