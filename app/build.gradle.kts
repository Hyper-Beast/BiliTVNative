plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

val supportedAbis = setOf("armeabi-v7a", "arm64-v8a")
val targetAbi = providers.gradleProperty("targetAbi").orNull?.trim()?.takeIf { it.isNotEmpty() }

require(targetAbi == null || targetAbi in supportedAbis) {
  "Unsupported targetAbi=$targetAbi. Supported values: ${supportedAbis.joinToString()}"
}

android {
  namespace = "com.kirin.bilitv"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.kirin.bilitv"
    minSdk = 23
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"

    ndk {
      abiFilters.clear()
      abiFilters += targetAbi?.let(::listOf) ?: supportedAbis.toList()
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

  debugImplementation(libs.compose.ui.tooling)
}
