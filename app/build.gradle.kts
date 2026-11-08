plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    useLibrary("android.car")
    namespace = "com.example.hyundiaboysranger"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.hyundiaboysranger"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "IS_DEMO", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("demo") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            matchingFallbacks += listOf("debug")
            buildConfigField("boolean", "IS_DEMO", "true")
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi.helper)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.gson) { isTransitive = false }
    testImplementation(libs.error.prone.annotations)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
