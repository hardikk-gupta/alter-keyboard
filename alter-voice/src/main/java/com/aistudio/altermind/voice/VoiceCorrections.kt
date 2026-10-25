package com.aistudio.altermind.voice

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** Explicit local corrections only; no editor monitoring, audio retention or model training. */
class VoiceCorrections(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "voice-corrections.json"))
    fun read(): Map<String, String> = synchronized(lock) { if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) emptyMap() else {
        val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        json.keys().asSequence().associateWith { json.getString(it) }
    } }
    fun save(from: String, to: String) = synchronized(lock) {
        require(validVoiceCorrection(from, to)) { "Use two different single words, without numbers or negations." }
        val entries = read().toMutableMap()
        require(entries.size < 100 || entries.containsKey(from.lowercase(java.util.Locale.ROOT))) { "Delete a correction before adding more." }
        entries[from.lowercase(java.util.Locale.ROOT)] = to
        write(entries)
    }
    fun delete(from: String) = synchronized(lock) { write(read() - from) }
    companion object { private val lock = Any() }
    private fun write(entries: Map<String, String>) {
        val output = file.startWrite()
        try { output.write(JSONObject(entries).toString().toByteArray()); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }
}
