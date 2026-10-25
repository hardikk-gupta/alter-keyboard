#include <jni.h>
#include <atomic>
#include <algorithm>
#include <memory>
#include <string>
#include <vector>
#include "llama.h"
struct Handy { llama_model *model; std::atomic<bool> cancelled{false}; };
extern "C" JNIEXPORT jlong JNICALL Java_com_aistudio_altermind_voice_HandyNative_load(JNIEnv *env, jobject, jstring path) {
    try {
    llama_log_set([](ggml_log_level, const char *, void *) {}, nullptr); // No transcript/runtime logging.
    llama_backend_init();
    const char *name = env->GetStringUTFChars(path, nullptr);
    auto params = llama_model_default_params(); params.n_gpu_layers = 0;
    auto *model = llama_model_load_from_file(name, params);
    env->ReleaseStringUTFChars(path, name);
    return model ? reinterpret_cast<jlong>(new Handy{model}) : 0;
    } catch (...) { return 0; }
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_HandyNative_prepare(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<Handy *>(handle)->cancelled.store(false);
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_HandyNative_cancel(JNIEnv *, jobject, jlong handle) {
    if (handle) reinterpret_cast<Handy *>(handle)->cancelled.store(true);
}
extern "C" JNIEXPORT void JNICALL Java_com_aistudio_altermind_voice_HandyNative_close(JNIEnv *, jobject, jlong handle) {
    auto *h = reinterpret_cast<Handy *>(handle); if (!h) return;
    llama_model_free(h->model); delete h;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_aistudio_altermind_voice_HandyNative_edit(JNIEnv *env, jobject, jlong handle, jbyteArray raw, jint limit) {
    try {
    auto *h = reinterpret_cast<Handy *>(handle); if (!h || limit < 1 || limit > 768) return nullptr;
    std::string input(env->GetArrayLength(raw), '\0');
    env->GetByteArrayRegion(raw, 0, input.size(), reinterpret_cast<jbyte *>(input.data()));
    const auto *vocab = llama_model_get_vocab(h->model);
    std::vector<llama_token> tokens;
    auto add = [&](const std::string &text, bool special) {
        int count = -llama_tokenize(vocab, text.data(), text.size(), nullptr, 0, false, special);
        if (count < 0 || count > 2048) return false;
        std::vector<llama_token> part(count);
        count = llama_tokenize(vocab, text.data(), text.size(), part.data(), part.size(), false, special);
        if (count < 0) return false;
        tokens.insert(tokens.end(), part.begin(), part.begin() + count); return true;
    };
    // Exact Handy chatml framing: explicit empty system turn; transcript special tokens stay literal.
    if (!add("<|im_start|>system\n<|im_end|>\n<|im_start|>user\n", true) || !add(input, false) ||
        !add("<|im_end|>\n<|im_start|>assistant\n", true) || tokens.size() + limit > 2048) return nullptr;
    auto params = llama_context_default_params();
    params.n_ctx = 2048; params.n_batch = 512; params.n_ubatch = 512;
    params.n_threads = 2; params.n_threads_batch = 2;
    // Fresh context per edit: no recurrent state/history may leak between dictations.
    std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(h->model, params), llama_free);
    if (!ctx) return nullptr;
    llama_set_abort_callback(ctx.get(), [](void *p) { return static_cast<Handy *>(p)->cancelled.load(); }, h);
    for (size_t offset = 0; offset < tokens.size(); offset += 512) {
        auto batch = llama_batch_get_one(tokens.data() + offset, std::min<size_t>(512, tokens.size() - offset));
        if (h->cancelled.load() || llama_decode(ctx.get(), batch)) return nullptr;
    }
    std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(llama_sampler_init_greedy(), llama_sampler_free);
    std::string result;
    for (int i = 0; i < limit; ++i) {
        if (h->cancelled.load()) return nullptr;
        auto token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
        if (llama_vocab_is_eog(vocab, token)) {
            auto output = env->NewByteArray(result.size());
            env->SetByteArrayRegion(output, 0, result.size(), reinterpret_cast<const jbyte *>(result.data()));
            return output;
        }
        std::vector<char> piece(64);
        int count = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
        if (count < 0) { piece.resize(-count); count = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false); }
        if (count < 0) return nullptr;
        result.append(piece.data(), count);
        if (result.size() > 6000) return nullptr;
        auto batch = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx.get(), batch)) return nullptr;
    }
    return nullptr; // Never publish an output truncated by the token budget.
    } catch (...) { return nullptr; }
}
