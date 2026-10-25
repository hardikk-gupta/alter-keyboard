package com.aistudio.altermind.voice

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.UUID

/** One private, file-backed temporary record: atomic metadata plus append-only 16 kHz PCM.
 * Lives in noBackupFilesDir; removed only after accepted insertion or explicit discard, never on keyboard hide.
 * Audio is kept outside metadata so appending a chunk never rewrites minutes of audio.
 */
internal class RecordingDraftStore(private val directory: File) {
    data class Draft(val id: String, val model: String, val prefix: String, val partial: String,
        val completed: String?, val samples: Int, val raw: String? = null) {
        val hasContent get() = samples > 0 || prefix.isNotBlank() || partial.isNotBlank() || !completed.isNullOrBlank()
        val deviceText get() = listOf(prefix, partial).filter { it.isNotBlank() }.joinToString(" ")
    }
    private val metadata get() = File(directory, "draft.properties")
    private val audio get() = File(directory, "recording.pcm")

    fun load(): Draft? = synchronized(lock) {
        if (!metadata.exists()) return@synchronized null
        check(metadata.length() <= 1_048_576) { "Saved recording metadata is too large." }
        val p = Properties().apply { metadata.inputStream().use { load(it) } }
        check(audio.length() <= MAX_SAMPLES * 2L + 1) { "Saved recording exceeds the fifteen-minute limit." }
        Draft(checkNotNull(p.getProperty("id")), checkNotNull(p.getProperty("model")),
            p.getProperty("prefix", ""), p.getProperty("partial", ""), p.getProperty("completed"),
            (audio.length() / 2).toInt(), p.getProperty("raw"))
    }

    fun begin(model: String): Draft = synchronized(lock) {
        check(load()?.hasContent != true) { "Resume or discard the saved recording before starting another." }
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create private recording storage." }
        FileOutputStream(audio).use { it.fd.sync() }
        Draft(UUID.randomUUID().toString(), model, "", "", null, 0).also { write(it) }
    }

    fun append(id: String, buffer: ShortArray, count: Int): Boolean = synchronized(lock) {
        require(count in 0..buffer.size)
        val draft = load()?.takeIf { it.id == id } ?: return@synchronized false
        check(draft.samples + count <= MAX_SAMPLES) { "Recording reached the fifteen-minute limit." }
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) { bytes[i * 2] = buffer[i].toByte(); bytes[i * 2 + 1] = (buffer[i].toInt() shr 8).toByte() }
        RandomAccessFile(audio, "rw").use {
            // A killed process may leave an incomplete final sample; keep every complete sample.
            it.setLength(draft.samples * 2L); it.seek(it.length()); it.write(bytes)
            if (draft.samples / 16000 != (draft.samples + count) / 16000) it.fd.sync()
        }
        true
    }

    fun replay(id: String, consume: (FloatArray) -> Unit) = synchronized(lock) {
        val draft = checkNotNull(load()?.takeIf { it.id == id }) { "The saved recording is no longer available." }
        audio.inputStream().use { stream ->
            var remaining = draft.samples * 2
            val buffer = ByteArray(3200)
            while (remaining > 0) {
                val count = minOf(buffer.size, remaining)
                var read = 0
                while (read < count) {
                    val next = stream.read(buffer, read, count - read)
                    check(next > 0) { "The saved audio ended unexpectedly." }
                    read += next
                }
                consume(pcm16Samples(buffer.copyOf(count)))
                remaining -= count
            }
        }
    }

    fun resume(id: String): Draft = synchronized(lock) {
        val draft = checkNotNull(load()?.takeIf { it.id == id })
        draft.copy(prefix = draft.deviceText, partial = "", completed = null).also { write(it) }
    }

    fun partial(id: String, text: String) = update(id) { it.copy(partial = text) }
    fun raw(id: String, text: String) = update(id) { it.copy(raw = text) }
    fun complete(id: String, text: String) = update(id) { it.copy(completed = text) }
    private fun update(id: String, change: (Draft) -> Draft) = synchronized(lock) {
        load()?.takeIf { it.id == id }?.let { write(change(it)) }
        Unit
    }

    fun sync() = synchronized(lock) {
        if (audio.exists()) RandomAccessFile(audio, "rw").use { it.fd.sync() }
    }

    /** An old controller cannot delete a newer recording. null is only for explicitly discarding an unreadable draft. */
    fun clear(id: String?) = synchronized(lock) {
        if (id == null || load()?.id == id) {
            check(!metadata.exists() || metadata.delete()) { "Cannot discard the saved recording." }
            check(!audio.exists() || audio.delete()) { "Cannot discard the saved audio." }
            File(directory, "draft.new").delete()
        }
    }

    private fun write(draft: Draft) {
        val next = File(directory, "draft.new")
        FileOutputStream(next).use { stream ->
            Properties().apply {
                setProperty("id", draft.id); setProperty("model", draft.model)
                setProperty("prefix", draft.prefix); setProperty("partial", draft.partial)
                draft.completed?.let { setProperty("completed", it) }
                draft.raw?.let { setProperty("raw", it) }
            }.store(stream, null)
            check(stream.channel.size() <= 1_048_576) { "Saved transcript exceeds the temporary draft limit." }
            stream.fd.sync()
        }
        Files.move(next.toPath(), metadata.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    companion object {
        const val MAX_SAMPLES = 900 * 16000
        private val lock = Any()
    }
}
