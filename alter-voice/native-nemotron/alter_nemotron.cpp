#include <jni.h>
#include <atomic>
#include <cstring>
#include <memory>
#include <mutex>
#include <string>
#include <vector>
#include "transcribe.h"
#include "transcribe/parakeet.h"

struct SpeechEngine {
    transcribe_model *model = nullptr;
    transcribe_session *session = nullptr;
    std::atomic<bool> cancelled{false};
    ~SpeechEngine() { transcribe_session_free(session); transcribe_model_free(model); }
};
static std::once_flag log_once;
extern "C" JNIEXPORT jlong JNICALL Java_com_aistudio_altermind_voice_NemotronNative_load(JNIEnv *env, jobject, jstring path) {
    try {
        std::call_once(log_once, [] { transcribe_log_set([](transcribe_log_level, const char *, void *) {}, nullptr); });
        std::unique_ptr<SpeechEngine> engine(new SpeechEngine());
        const char *name = env->GetStringUTFChars(path, nullptr);
        transcribe_model_load_params params; transcribe_model_load_params_init(&params);
        params.backend = TRANSCRIBE_BACKEND_CPU;
        auto status = transcribe_model_load_file(name, &params, &engine->model);
        env->ReleaseStringUTFChars(path, name);
        if (status != TRANSCRIBE_OK) return 0;
        transcribe_capabilities caps; transcribe_capabilities_init(&caps);
        if (transcribe_model_get_capabilities(engine->model, &caps) != TRANSCRIBE_OK || !caps.supports_streaming) return 0;
        return reinterpret_cast<jlong>(engine.release());
    } catch (...) { return 0; }
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_NemotronNative_prepare(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<SpeechEngine *>(handle)->cancelled.store(false);
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_NemotronNative_cancel(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<SpeechEngine *>(handle)->cancelled.store(true);
}
extern "C" JNIEXPORT jboolean JNICALL Java_com_aistudio_altermind_voice_NemotronNative_begin(JNIEnv *env, jobject, jlong handle, jstring language, jint threads) {
    try {
        auto *engine = reinterpret_cast<SpeechEngine *>(handle); if (!engine || engine->cancelled.load()) return false;
        transcribe_session_free(engine->session); engine->session = nullptr;
        transcribe_session_params params; transcribe_session_params_init(&params);
        params.n_threads = threads > 0 && threads <= 4 ? threads : 4;
        if (transcribe_session_init(engine->model, &params, &engine->session) != TRANSCRIBE_OK) return false;
        transcribe_set_abort_callback(engine->session, [](void *ctx) { return static_cast<SpeechEngine *>(ctx)->cancelled.load(); }, engine);
        const char *code = env->GetStringUTFChars(language, nullptr);
        std::string hint(code); env->ReleaseStringUTFChars(language, code);
        transcribe_run_params run; transcribe_run_params_init(&run);
        run.language = hint == "auto" ? nullptr : hint.c_str(); run.timestamps = TRANSCRIBE_TIMESTAMPS_NONE;
        transcribe_stream_params stream; transcribe_stream_params_init(&stream);
        stream.commit_policy = TRANSCRIBE_STREAM_COMMIT_ON_FINALIZE;
        transcribe_parakeet_stream_ext profile; transcribe_parakeet_stream_ext_init(&profile);
        profile.att_context_right = 3; // 320 ms cache-aware profile, validated by model metadata.
        stream.family = &profile.ext;
        return !engine->cancelled.load() && transcribe_stream_begin(engine->session, &run, &stream) == TRANSCRIBE_OK;
    } catch (...) { return false; }
}
static jbyteArray snapshot(JNIEnv *env, SpeechEngine *engine) {
    if (engine->cancelled.load()) return nullptr;
    transcribe_stream_text text; transcribe_stream_text_init(&text);
    if (transcribe_stream_get_text(engine->session, &text) != TRANSCRIBE_OK || text.full_text_bytes > 200000) return nullptr;
    auto out = env->NewByteArray(static_cast<jsize>(text.full_text_bytes));
    if (text.full_text_bytes) env->SetByteArrayRegion(out, 0, text.full_text_bytes, reinterpret_cast<const jbyte *>(text.full_text));
    return out;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_aistudio_altermind_voice_NemotronNative_feed(JNIEnv *env, jobject, jlong handle, jfloatArray samples) {
    try {
        auto *engine = reinterpret_cast<SpeechEngine *>(handle); if (!engine || !engine->session || engine->cancelled.load()) return nullptr;
        const int n = env->GetArrayLength(samples); if (n < 1 || n > 16000) return nullptr;
        std::vector<float> pcm(n); env->GetFloatArrayRegion(samples, 0, n, pcm.data());
        if (transcribe_stream_feed(engine->session, pcm.data(), n, nullptr) != TRANSCRIBE_OK) return nullptr;
        return snapshot(env, engine);
    } catch (...) { return nullptr; }
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_aistudio_altermind_voice_NemotronNative_finish(JNIEnv *env, jobject, jlong handle) {
    try {
        auto *engine = reinterpret_cast<SpeechEngine *>(handle); if (!engine || !engine->session || engine->cancelled.load()) return nullptr;
        if (transcribe_stream_finalize(engine->session, nullptr) != TRANSCRIBE_OK) return nullptr;
        return snapshot(env, engine);
    } catch (...) { return nullptr; }
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_NemotronNative_end(JNIEnv *, jobject, jlong handle) {
    auto *engine = reinterpret_cast<SpeechEngine *>(handle); if (!engine) return;
    transcribe_session_free(engine->session); engine->session = nullptr;
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_NemotronNative_close(JNIEnv *, jobject, jlong handle) {
    delete reinterpret_cast<SpeechEngine *>(handle);
}

// Parakeet's offline TDT decoder shares the pinned CPU runtime, not Nemotron's streaming session.
extern "C" JNIEXPORT jlong JNICALL Java_com_aistudio_altermind_voice_ParakeetTdtNative_load(JNIEnv *env, jobject, jstring path) {
    try {
        std::call_once(log_once, [] { transcribe_log_set([](transcribe_log_level, const char *, void *) {}, nullptr); });
        std::unique_ptr<SpeechEngine> engine(new SpeechEngine());
        const char *name = env->GetStringUTFChars(path, nullptr);
        transcribe_model_load_params params; transcribe_model_load_params_init(&params);
        params.backend = TRANSCRIBE_BACKEND_CPU;
        auto status = transcribe_model_load_file(name, &params, &engine->model);
        env->ReleaseStringUTFChars(path, name);
        if (status != TRANSCRIBE_OK || std::strcmp(transcribe_model_arch_string(engine->model), "parakeet") != 0) return 0;
        return reinterpret_cast<jlong>(engine.release());
    } catch (...) { return 0; }
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_aistudio_altermind_voice_ParakeetTdtNative_transcribe(JNIEnv *env, jobject, jlong handle, jfloatArray samples, jint threads) {
    try {
        auto *engine = reinterpret_cast<SpeechEngine *>(handle);
        if (!engine || engine->cancelled.load()) return nullptr;
        const int n = env->GetArrayLength(samples);
        if (n < 1 || n > 20 * 16000) return nullptr;
        transcribe_session_params params; transcribe_session_params_init(&params);
        params.n_threads = threads > 0 && threads <= 4 ? threads : 2;
        transcribe_session *raw = nullptr;
        if (transcribe_session_init(engine->model, &params, &raw) != TRANSCRIBE_OK) return nullptr;
        std::unique_ptr<transcribe_session, decltype(&transcribe_session_free)> session(raw, transcribe_session_free);
        transcribe_set_abort_callback(session.get(), [](void *ctx) { return static_cast<SpeechEngine *>(ctx)->cancelled.load(); }, engine);
        std::vector<float> pcm(n); env->GetFloatArrayRegion(samples, 0, n, pcm.data());
        transcribe_run_params run; transcribe_run_params_init(&run);
        run.timestamps = TRANSCRIBE_TIMESTAMPS_NONE;
        if (engine->cancelled.load() || transcribe_run(session.get(), pcm.data(), n, &run) != TRANSCRIBE_OK || engine->cancelled.load()) return nullptr;
        const char *text = transcribe_full_text(session.get());
        if (!text) return nullptr;
        const size_t bytes = std::strlen(text);
        if (bytes > 200000) return nullptr;
        auto out = env->NewByteArray(static_cast<jsize>(bytes));
        if (out && bytes) env->SetByteArrayRegion(out, 0, bytes, reinterpret_cast<const jbyte *>(text));
        return out;
    } catch (...) { return nullptr; }
}
