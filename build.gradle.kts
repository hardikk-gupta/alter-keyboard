import org.gradle.api.artifacts.transform.*
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

// Top-level build file. Plugin versions are declared once here; LiteRT-LM 0.18.0 requires Kotlin 2.4.
plugins {
  id("com.android.application") version "8.10.1" apply false
  id("com.android.library") version "8.10.1" apply false
  id("org.jetbrains.kotlin.android") version "2.4.0" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
}

// Moonshine 0.1.5 includes a reduced ORT 1.23.2 build. Use the full, matching Maven
// runtime for both Moonshine and IndicXlit; never let packaging pick an arbitrary copy.
abstract class ExternalMoonshineOrt : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputArtifact: Provider<FileSystemLocation>
    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (input.name != "moonshine-voice-0.1.5.aar") { outputs.file(input); return }
        val output = outputs.file("moonshine-voice-0.1.5-external-ort.aar")
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream()).use { out ->
                zip.entries().asSequence().filterNot { it.name.matches(Regex("jni/[^/]+/libonnxruntime\\.so")) }.forEach { entry ->
                    out.putNextEntry(ZipEntry(entry.name).apply { time = 0 })
                    if (!entry.isDirectory) zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }
}
val externalOrt = Attribute.of("alter.external-onnx-runtime", Boolean::class.javaObjectType)
subprojects {
    dependencies {
        artifactTypes.maybeCreate("aar").attributes.attribute(externalOrt, false)
        registerTransform(ExternalMoonshineOrt::class) {
            from.attribute(Attribute.of("artifactType", String::class.java), "aar").attribute(externalOrt, false)
            to.attribute(Attribute.of("artifactType", String::class.java), "aar").attribute(externalOrt, true)
        }
    }
    configurations.configureEach { attributes.attribute(externalOrt, true) }
}
