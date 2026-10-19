import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Properties
import java.util.TimeZone
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ALTER Keyboard: FUTO Keyboard (org.futo.inputmethod.latin) with ALTER's voice pack and pill on top.
// ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc. See MODIFICATIONS.md.
plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("org.jetbrains.kotlin.plugin.serialization")
}

// Each build is versioned from its source commit time (minutes since 2026-01-01 UTC), so a newer build always
// installs as an update. Without git (AI Studio, source zips) the build time is used, so builds never downgrade.
val commitStamp: List<String> = if (file("${rootDir}/.git").exists()) {
  runCatching {
    providers.exec {
      isIgnoreExitValue = true
      commandLine("git", "log", "-1", "--format=%ct %h")
    }.standardOutput.asText.get().trim().split(" ").filter { it.isNotBlank() }
  }.getOrDefault(emptyList())
} else {
  emptyList()
}
val commitSeconds = commitStamp.getOrNull(0)?.toLongOrNull() ?: (System.currentTimeMillis() / 1000L)
val alterVersionCode = ((commitSeconds - 1767225600L) / 60L).toInt().coerceAtLeast(2)
val alterVersionName = "1.0." + SimpleDateFormat("yyMMdd.HHmm").apply { timeZone = TimeZone.getTimeZone("UTC") }
  .format(Date(commitSeconds * 1000L)) + (commitStamp.getOrNull(1)?.let { "-$it" } ?: "-local")

// One shared key signs every ALTER build so updates install over each other (docs/SIGNING.md). It comes from the
// ALTER_SIGNING_KEYSTORE_B64 environment variable when set, else the git-ignored debug.keystore in the repo root.
val sharedSigningKeystore: File? = System.getenv("ALTER_SIGNING_KEYSTORE_B64")?.takeIf { it.isNotBlank() }?.let { encoded ->
  layout.buildDirectory.file("signing/alter-debug.keystore").get().asFile.apply {
    parentFile.mkdirs(); writeBytes(Base64.getMimeDecoder().decode(encoded.trim()))
  }
} ?: file("${rootDir}/debug.keystore").takeIf { it.exists() }
if (sharedSigningKeystore == null) {
  logger.warn("No shared ALTER signing key (ALTER_SIGNING_KEYSTORE_B64 or debug.keystore); signing with FUTO's test key. This APK will not install over ALTER builds.")
}

apply(from = "translations.gradle")
@Suppress("UNCHECKED_CAST")
val translationsWithoutEngValues = extra["translationsWithoutEngValues"] as groovy.lang.Closure<File>

val python = if (System.getProperty("os.name").lowercase().contains("windows")) "python" else "python3"

// Generates KeyboardTextsTable.java from the layout texts before every build.
val updateLocales = tasks.register<Exec>("updateLocales") {
  workingDir = file("tools/make-keyboard-text-py")
  commandLine(python, "src/generate.py")
}

tasks.register("updateContributors") {
  val script = file("tools/contributors.py")
  inputs.file(script)
  outputs.file(file("java/src/org/futo/inputmethod/latin/uix/settings/pages/credits/Contributors.kt"))
  doLast {
    providers.exec {
      environment("GITHUB_TOKEN", System.getenv("GITHUB_TOKEN") ?: "")
      commandLine("python3", script.absolutePath)
    }.result.get()
  }
}

tasks.named("preBuild") { dependsOn(updateLocales) }

val crashReportPropertiesFile = rootProject.file("crashreporting.properties")
val crashReportProperties = Properties().apply {
  if (crashReportPropertiesFile.exists()) FileInputStream(crashReportPropertiesFile).use { load(it) }
  else logger.lifecycle("crashreporting.properties not found, crash reporting will be disabled")
}

android {
  namespace = "org.futo.inputmethod.latin"
  compileSdk = 35
  ndkVersion = "28.2.13676358"

  useLibrary("android.test.runner")
  useLibrary("android.test.base")
  useLibrary("android.test.mock")

  defaultConfig {
    applicationId = "com.aistudio.altermind.keyboard"
    minSdk = 24
    targetSdk = 35
    versionCode = alterVersionCode
    versionName = alterVersionName
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = false
  }

  bundle {
    language { enableSplit = true }
  }

  signingConfigs {
    getByName("debug") {
      if (sharedSigningKeystore != null) {
        storeFile = sharedSigningKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      } else {
        storeFile = file("java/shared.keystore")
      }
      enableV2Signing = true
      enableV3Signing = true
    }
  }

  val keystorePropertiesFile = rootProject.file("keystore.properties")
  val releaseSigning = if (keystorePropertiesFile.exists()) {
    val keystoreProperties = Properties().apply { FileInputStream(keystorePropertiesFile).use { load(it) } }
    signingConfigs.create("release") {
      keyAlias = keystoreProperties["keyAlias"] as String
      keyPassword = keystoreProperties["keyPassword"] as String
      storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
      storePassword = keystoreProperties["storePassword"] as String
    }
  } else signingConfigs.getByName("debug")

  buildTypes {
    getByName("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("debug")
    }
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = releaseSigning
    }
    all {
      if (crashReportPropertiesFile.exists()) {
        buildConfigField("boolean", "ENABLE_ACRA", crashReportProperties["acraEnabled"] as String)
        buildConfigField("String", "ACRA_URL", crashReportProperties["acraUrl"] as String)
        buildConfigField("String", "ACRA_USER", crashReportProperties["acraUser"] as String)
        buildConfigField("String", "ACRA_PASSWORD", crashReportProperties["acraPassword"] as String)
      } else {
        buildConfigField("boolean", "ENABLE_ACRA", "false")
        buildConfigField("String", "ACRA_URL", "\"\"")
        buildConfigField("String", "ACRA_USER", "\"\"")
        buildConfigField("String", "ACRA_PASSWORD", "\"\"")
      }
      // FUTO's payment features stay (FUTO Source First License); do not remove them.
      buildConfigField("String", "FUTOPAY_URL", "\"https://pay2.futo.org/checkout/polar/futo-keyboard/futo-keyboard-voiceinput/checkout-ready?success=redirect-to-organization-page\"")
      buildConfigField("String", "FUTOPAY_PRICE", "\"~\$6.99\"")
      buildConfigField("String", "GOOGLEPAY_URL", "\"https://play.google.com/store/apps/details?id=org.futo.keyboardpayment\"")
      buildConfigField("String", "GOOGLEPAY_PRICE", "\"~\$11.99\"")
    }
  }

  // Only FUTO's "stable" flavor is built; "unstable" and "playstore" were removed.
  flavorDimensions += "buildType"
  productFlavors {
    create("stable") {
      dimension = "buildType"
      isDefault = true
      buildConfigField("boolean", "IS_PLAYSTORE_BUILD", "false")
      // ALTER Keyboard must never offer FUTO's APK as an update.
      buildConfigField("boolean", "UPDATE_CHECKING", "false")
      buildConfigField("boolean", "UPDATE_CHECKING_NETWORK", "false")
      buildConfigField("String", "BRANCH", "\"main\"")
      buildConfigField("String", "PAYMENT_PRICE", "FUTOPAY_PRICE")
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  sourceSets {
    getByName("main") {
      res.setSrcDirs(listOf(
        "java/res",
        translationsWithoutEngValues.call("translations/core"),
        translationsWithoutEngValues.call("translations/core-ign"),
        "java/res-large"
      ))
      java.setSrcDirs(listOf("common/src", "java/src"))
      manifest.srcFile("java/AndroidManifest.xml")
      assets.setSrcDirs(listOf("java/assets"))
    }
    getByName("stable") {
      java.setSrcDirs(listOf("common/src", "java/src", "java/stable/java"))
      manifest.srcFile("java/stable/AndroidManifest.xml")
    }
  }

  lint {
    checkReleaseBuilds = false
  }

  androidResources {
    noCompress += "dict"
  }

  packaging {
    jniLibs { useLegacyPackaging = true }
  }

  externalNativeBuild {
    cmake { path = file("native/jni/CMakeLists.txt") }
  }

  buildFeatures {
    compose = true
    viewBinding = true
    mlModelBinding = true
    buildConfig = true
  }
}

kotlin {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
}

dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.lifecycle:lifecycle-runtime:2.8.4")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
  implementation("androidx.activity:activity-compose:1.9.1")
  implementation(platform("androidx.compose:compose-bom:2025.06.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.navigation:navigation-compose:2.10.0-alpha02")

  implementation("com.google.code.findbugs:jsr305:3.0.2")

  implementation("androidx.datastore:datastore-preferences:1.1.7")
  implementation("androidx.autofill:autofill:1.1.0")
  implementation("androidx.window:window:1.3.0")

  "stableImplementation"("ch.acra:acra-mail:5.11.1")
  "stableImplementation"("ch.acra:acra-dialog:5.11.1")

  implementation("sh.calvin.reorderable:reorderable:3.0.0")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.7.1")

  implementation("com.akuleshov7:ktoml-core:0.7.1")

  implementation(project(":voiceinput-shared"))
  implementation(project(":alter-voice"))

  implementation("com.charleskorn.kaml:kaml:0.61.0")

  // For MOZC
  implementation("com.google.protobuf:protobuf-javalite:3.8.0")
  implementation("com.google.guava:guava:33.4.8-android")
  implementation(mapOf("name" to "mozc-release", "ext" to "aar"))

  // For RIME
  implementation(files("libs/rime-release.aar"))

  implementation(files("libs/futo-swipe-release.aar"))

  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  testImplementation("junit:junit:4.13.2")
}
