import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ALTER's voice pack: on-device speech (Whisper, Moonshine, Android on-device recognition) and the optional
// LFM2.5 cleanup editor. Model weights are never bundled; they download on demand from the pinned catalog.
plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.aistudio.altermind.voice"
  compileSdk = 35

  // libalter_whisper.so is prebuilt into src/main/jniLibs from native/ (scripts/build_native_libs.sh):
  // Google AI Studio has no NDK or CMake.
  defaultConfig {
    minSdk = 26
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  buildFeatures { compose = true }
  testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

dependencies {
  api(project(":alter-agent"))
  implementation(files("libs/sherpa-onnx-2.0.1.jar"))
  implementation("org.apache.commons:commons-compress:1.27.1")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation(platform("androidx.compose:compose-bom:2025.06.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("ai.moonshine:moonshine-voice:0.1.5")
  implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
