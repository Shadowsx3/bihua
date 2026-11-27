import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
}

val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "com.bassmd.bihua"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bassmd.bihua"
        minSdk = 29
        targetSdk = 36
        versionCode = 23
        versionName = "1.1.2"
        ndk {
            // The tablet and phone are arm64. Dropping emulator ABIs keeps the
            // local model runtime from doubling the APK size.
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Keep native debug symbols out of the APK but available as a
            // separate Play Console upload for crash analysis.
            ndk {
                debugSymbolLevel = "FULL"
            }
            // A personal release build is signed with the upload key when
            // keystore.properties exists, otherwise with the debug key so
            // `assembleRelease` still produces an installable APK.
            signingConfig = if (keystoreFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += "db"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        // The Compose lint bundled for the KMP mix reports valid `remember`
        // blocks that return a collection as Unit. Keep the rest of the checks.
        disable += "RememberReturnType"
        // The bundled LiveData detector crashes on the Kotlin 2.2 analysis API.
        // Bihua has no LiveData; keep the check disabled until AGP is upgraded.
        disable += "NullSafeMutableLiveData"
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }

    // The iOS app links this framework from Xcode. Android keeps its
    // application plugin and source layout; the port starts here.
    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "BihuaKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okhttp)
            implementation(libs.okhttp.tls)
            implementation(libs.androidx.work.ktx)
            implementation(libs.androidx.window)
            implementation(libs.mlkit.digital.ink)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.foundation)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.androidx.ui)
            implementation(libs.androidx.ui.graphics)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.androidx.material3)
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation("androidx.compose.ui:ui-test-junit4")
        }

        // Compose Multiplatform artifacts are only on the iOS targets for the
        // P0 spike, so the Android dependency graph is unchanged.
        iosMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

// Android-only debug dependencies stay on the AGP configuration; the KMP
// variant source sets do not create an androidDebug container in this setup.
dependencies {
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
