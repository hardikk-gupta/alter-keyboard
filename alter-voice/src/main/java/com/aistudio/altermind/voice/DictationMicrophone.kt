package com.aistudio.altermind.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build

/** PCM capture is independently testable; a loaded model does not prove microphone access. */
internal interface DictationMicrophone {
    fun start()
    fun read(buffer: ShortArray): Int
    fun stop()
    fun close()
}

internal class AndroidDictationMicrophone(private val context: Context) : DictationMicrophone {
    private var record: AudioRecord? = null
    override fun start() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw SecurityException("Microphone permission was removed. Allow microphone access in Android's app permissions, then try again.")
        check(!context.getSystemService(AudioManager::class.java).isMicrophoneMute) {
            "Microphone access is off. Turn it on in Android's Quick Settings, then try again."
        }
        val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "This phone cannot record mono 16 kHz audio." }
        val builder = AudioRecord.Builder()
            // Match the MIC source used by ALTER's working recorder, rather than OEM speech preprocessing.
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimum * 2, 12800))
        if (Build.VERSION.SDK_INT >= 30) builder.setPrivacySensitive(true)
        record = builder.build()
        val microphone = record!!
        check(microphone.state == AudioRecord.STATE_INITIALIZED) { "Could not open the microphone." }
        microphone.startRecording()
        check(microphone.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Android did not start the microphone. Close other recorders and try again." }
    }
    override fun read(buffer: ShortArray): Int {
        val microphone = checkNotNull(record)
        if (Build.VERSION.SDK_INT >= 29) check(microphone.activeRecordingConfiguration?.isClientSilenced != true) {
            "Android is silencing ALTER Keyboard's microphone. Turn on system microphone access and close other voice or recording apps."
        }
        // A blocked native read must not trap Stop/cancellation; the controller waits between empty reads.
        return microphone.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING)
    }
    override fun stop() { record?.let { if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop() } }
    override fun close() { record?.release(); record = null }
}

internal interface DictationSpeech {
    val selected: String?
    fun checkInstalled()
    suspend fun ensureReady()
    suspend fun stream(owner: Any, read: suspend () -> FloatArray?, update: (String) -> Unit): Boolean
    suspend fun transcribe(audio: FloatArray, owner: Any): String
    fun cancel(owner: Any)
}
