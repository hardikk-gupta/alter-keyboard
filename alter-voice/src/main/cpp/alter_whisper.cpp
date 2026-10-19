#include <jni.h>
#include <atomic>
#include <string>
#include <vector>
#include "whisper.h"
struct SpeechContext { whisper_context * model; std::atomic<bool> cancelled{false}; };
extern "C" JNIEXPORT jlong JNICALL Java_com_aistudio_altermind_voice_WhisperNative_load(JNIEnv *env, jobject, jstring path) {
    const char *filename = env->GetStringUTFChars(path, nullptr);
    auto params = whisper_context_default_params(); params.use_gpu = false;
    auto *model = whisper_init_from_file_with_params(filename, params);
    env->ReleaseStringUTFChars(path, filename);
    return model ? reinterpret_cast<jlong>(new SpeechContext{model}) : 0;
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_WhisperNative_prepare(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<SpeechContext *>(handle)->cancelled.store(false);
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_WhisperNative_cancel(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<SpeechContext *>(handle)->cancelled.store(true);
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_WhisperNative_close(JNIEnv *, jobject, jlong handle) {
    auto *ctx = reinterpret_cast<SpeechContext *>(handle); if (!ctx) return;
    whisper_free(ctx->model); delete ctx;
}
extern "C" JNIEXPORT jstring JNICALL Java_com_aistudio_altermind_voice_WhisperNative_transcribe(JNIEnv *env, jobject, jlong handle, jfloatArray samples, jstring language, jint threads) {
    auto *ctx = reinterpret_cast<SpeechContext *>(handle); if (!ctx) return nullptr;
    const int length = env->GetArrayLength(samples);
    std::vector<float> pcm(length); env->GetFloatArrayRegion(samples, 0, length, pcm.data());
    const char *lang = env->GetStringUTFChars(language, nullptr);
    std::string code(lang); env->ReleaseStringUTFChars(language, lang);
    auto params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = threads; params.translate = false; params.no_context = true;
    params.no_timestamps = true; params.print_realtime = false; params.print_progress = false;
    params.print_timestamps = false; params.print_special = false; params.suppress_blank = true;
    params.language = code.c_str(); params.detect_language = false; // "auto" still detects language before transcribing.
    params.abort_callback = [](void *data) { return reinterpret_cast<SpeechContext *>(data)->cancelled.load(); };
    params.abort_callback_user_data = ctx;
    if (whisper_full(ctx->model, params, pcm.data(), length) != 0 || ctx->cancelled.load()) return nullptr;
    std::string result;
    for (int i = 0; i < whisper_full_n_segments(ctx->model); ++i) {
        // Drop segments explicitly classified as no speech by Whisper.
        if (whisper_full_get_segment_no_speech_prob(ctx->model, i) > 0.6f) continue;
        result += whisper_full_get_segment_text(ctx->model, i);
    }
    return env->NewStringUTF(result.c_str());
}
