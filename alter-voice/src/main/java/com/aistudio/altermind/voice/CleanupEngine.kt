package com.aistudio.altermind.voice

import android.content.Context
import android.os.Build
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * ALTER's LiteRT runtime, reduced to the dictation editor. One LiteRT engine at a time; LiteRT 0.18 has a reported
 * recurrent-state reuse issue for LFM2, so every cleanup chunk gets a fresh engine that is released afterwards.
 */
internal class CleanupEngine(private val context: Context, private val store: ModelStore) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    val supported: Boolean get() = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }

    private fun loadLocked(spec: ModelSpec) {
        require(supported) { "Dictation cleanup needs a 64-bit Android device." }
        require(spec.modelType == ModelType.TEXT_CLEANUP)
        require(store.installed(spec)) { "The cleanup model is not installed. Download it in ALTER voice settings." }
        closeLocked()
        val candidate = Engine(EngineConfig(modelPath = store.file(spec).absolutePath, backend = Backend.CPU(),
            maxNumTokens = minOf(spec.contextTokens, 4096), cacheDir = cache(spec)))
        try {
            candidate.initialize()
            engine = candidate
        } catch (e: OutOfMemoryError) {
            throw IllegalStateException("Not enough memory for dictation cleanup.", e)
        } catch (e: LinkageError) {
            throw IllegalStateException("The cleanup runtime is incompatible with this device.", e)
        } catch (e: Exception) {
            if (candidate.isInitialized()) candidate.close()
            throw IllegalStateException("The cleanup model could not start on this device.", e)
        }
    }

    /** Dictation never queues behind another cleanup: a busy engine yields nothing and the transcript stays as is. */
    fun generate(spec: ModelSpec, request: CleanupRequest) = flow {
        if (!mutex.tryLock()) return@flow
        try {
            require(request.message.length <= 4000) { "Cleanup supports up to 4,000 characters per chunk." }
            store.verify(spec)
            loadLocked(spec)
            val history = request.examples.flatMap { (raw, clean) -> listOf(Message.user(raw), Message.model(clean)) }
            val active = engine!!.createConversation(ConversationConfig(
                systemInstruction = Contents.of(request.instruction), initialMessages = history,
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                thinkingConfig = ThinkingConfig(enableThinking = false),
                extraContext = mapOf("enable_thinking" to false)))
            try {
                active.sendMessageAsync(Contents.of(listOf<Content>(Content.Text(request.message))), maxOutputToken = 512,
                    extraContext = mapOf("enable_thinking" to false)).collect { chunk ->
                    val text = chunk.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
                    if (text.isNotEmpty()) emit(text)
                }
            } finally {
                withContext(NonCancellable) { active.cancelProcess(); active.close() }
            }
        } finally {
            try { closeLocked() } finally { mutex.unlock() }
        }
    }.flowOn(Dispatchers.IO)

    private fun cache(spec: ModelSpec): String = java.io.File(context.cacheDir, "alter_voice/${spec.id}/${spec.version}").apply { mkdirs() }.absolutePath
    private fun closeLocked() {
        engine?.close()
        engine = null
    }
}
