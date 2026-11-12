plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hank.kustom"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hank.kustom"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Hammerhead Karoo Extensions SDK
    implementation("io.hammerhead:karoo-ext:1.1.9")

    // Dhaval2404 Material ColorPicker library
    implementation("com.github.Dhaval2404:ColorPicker:2.3")

    // Preferences DataStore for live mid-ride configuration updates in Kustom
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // ViewModel & Lifecycle Compose integration for the Kustom configuration UI
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.ow2.asm:asm:9.7")
    testImplementation("org.ow2.asm:asm-commons:9.7")
    testImplementation("org.ow2.asm:asm-tree:9.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.github.nikosavola.karoo-ext-testing:karoo-ext-testing:6f13bcb")
    testImplementation("com.github.nikosavola.karoo-ext-testing:karoo-ext-testing-robolectric:6f13bcb")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}