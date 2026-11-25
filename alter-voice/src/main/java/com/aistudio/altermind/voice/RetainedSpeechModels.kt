package com.aistudio.altermind.voice

internal const val APEX_HINGLISH_MODEL = "whisper-oriserve-hinglish-apex-q8"
internal const val UNIFIED_MODEL = "parakeet-unified-en-560ms"
internal const val AKSHAR_MODEL = "akshar-v4"
internal val retainedSpeechIds = setOf(UNIFIED_MODEL, AKSHAR_MODEL, "shruti-hindi", APEX_HINGLISH_MODEL, "parakeet-110m-ctc-int8", "parakeet-110m-tdt-fp16",
    "nemotron-3.5-streaming-q4", "moonshine-en-medium")
internal fun retiredRecording(model: String) = model != DEVICE_SPEECH && model !in retainedSpeechIds
internal fun speechRecoveryMessage(model: String): String = if (retiredRecording(model))
    "This recording used a removed model. Select a remaining model and retry processing, or discard it. Your recording is saved."
    else "Select $model again to resume this recording, or discard it."
