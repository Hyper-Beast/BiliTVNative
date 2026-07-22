plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

val releaseAbis = setOf("armeabi-v7a", "arm64-v8a")
val emulatorValidationAbis = setOf("x86", "x86_64")
val targetAbi = providers.gradleProperty("targetAbi").orNull?.trim()?.takeIf { it.isNotEmpty() }
// x86/x86_64 is opt-in for emulator validation only; default app builds stay on release ARM ABIs.
val emulatorValidationAbi = providers.gradleProperty("emulatorValidationAbi").orNull.toBoolean()
val supportedAbis = releaseAbis + if (emulatorValidationAbi) emulatorValidationAbis else emptySet()

require(targetAbi == null || targetAbi in supportedAbis) {
  "Unsupported targetAbi=$targetAbi. Supported values: ${supportedAbis.joinToString()}"
}
require(!emulatorValidationAbi || targetAbi in emulatorValidationAbis) {
  "emulatorValidationAbi=true must be paired with targetAbi=${emulatorValidationAbis.joinToString("/")}."
}

android {
  namespace = "com.kirin.bilitv"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.kirin.bilitv"
    minSdk = 23
    targetSdk = 36
    versionCode = 101
    versionName = "1.0.1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters.clear()
      abiFilters += targetAbi?.let(::listOf) ?: releaseAbis.toList()
    }

  }

  buildTypes {
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
    }
    release {
      signingConfig = signingConfigs.getByName("debug")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  buildFeatures {
    compose = true
  }

  androidResources {
    localeFilters += listOf("zh", "zh-rHK", "zh-rTW")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging {
    resources {
      excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "/META-INF/**/LICENSE.txt",
      )
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  implementation(platform(libs.compose.bom))

  implementation(libs.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.backdrop)
  implementation(libs.compose.foundation)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.coil.compose)
  implementation(libs.coroutines.android)
  implementation(libs.danmaku.render.engine)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.media3.datasource.okhttp)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.exoplayer.dash)
  implementation(libs.media3.ui)
  implementation(libs.okhttp)
  implementation(libs.okhttp.brotli)
  implementation(libs.opencc4j)
  implementation(libs.tv.material)
  implementation(libs.zxing.core)

  testImplementation(libs.junit4)

  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.compose.ui.test.junit4)

  debugImplementation(libs.compose.ui.test.manifest)
  debugImplementation(libs.compose.ui.tooling)
}
