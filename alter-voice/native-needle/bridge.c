#include <jni.h>
#include <dlfcn.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define JNI(name) Java_com_aistudio_altermind_voice_NeedleNative_##name

/* SpeechModels serializes all calls. The upstream API owns a single model and
   has no unload function, so each session owns a dynamic-library reference. */
typedef struct {
    void *lib;
    int (*feed)(const float *, int, const char *, const char *, char *, int);
    int (*stop)(char *, int);
    const char *(*error)(void);
    char output[262144];
} Session;

static void fail(JNIEnv *env, const char *message) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), message);
}

JNIEXPORT jlong JNICALL JNI(open)(JNIEnv *env, jobject obj, jbyteArray weights) {
    Session *s = calloc(1, sizeof(*s));
    if (!s || !(s->lib = dlopen("libalter_needle_engine.so", RTLD_NOW | RTLD_LOCAL))) {
        free(s);
        fail(env, "Could not open the ARM64 Needle engine.");
        return 0;
    }
    int (*load)(const unsigned char *, unsigned long long) = dlsym(s->lib, "needle_load");
    int (*models)(void) = dlsym(s->lib, "needle_models");
    s->feed = dlsym(s->lib, "needle_stream_transcribe_process");
    s->stop = dlsym(s->lib, "needle_stream_transcribe_stop");
    s->error = dlsym(s->lib, "needle_last_error");
    if (!load || !models || !s->feed || !s->stop || !s->error) {
        fail(env, "The Needle speech API is unavailable.");
    } else {
        jbyte *data = (*env)->GetByteArrayElements(env, weights, NULL);
        if (data) {
            int code = load((unsigned char *)data, (*env)->GetArrayLength(env, weights));
            (*env)->ReleaseByteArrayElements(env, weights, data, JNI_ABORT);
            if (code >= 0 && (models() & 2)) return (intptr_t)s;
            fail(env, "Needle could not load the verified speech checkpoint.");
        }
    }
    dlclose(s->lib);
    free(s);
    return 0;
}

JNIEXPORT jbyteArray JNICALL JNI(step)(JNIEnv *env, jobject obj, jlong handle, jfloatArray audio, jboolean hindi) {
    Session *s = (Session *)(intptr_t)handle;
    int code;
    if (audio) {
        jfloat *pcm = (*env)->GetFloatArrayElements(env, audio, NULL);
        if (!pcm) return NULL;
        code = s->feed(pcm, (*env)->GetArrayLength(env, audio), hindi ? "hi" : NULL,
                       NULL, s->output, sizeof(s->output));
        (*env)->ReleaseFloatArrayElements(env, audio, pcm, JNI_ABORT);
    } else {
        code = s->stop(s->output, sizeof(s->output));
    }
    if (code < 0) {
        fail(env, s->error());
        return NULL;
    }
    size_t size = strnlen(s->output, sizeof(s->output));
    jbyteArray bytes = (*env)->NewByteArray(env, size);
    if (bytes) (*env)->SetByteArrayRegion(env, bytes, 0, size, (const jbyte *)s->output);
    return bytes;
}

JNIEXPORT void JNICALL JNI(close)(JNIEnv *env, jobject obj, jlong handle) {
    Session *s = (Session *)(intptr_t)handle;
    dlclose(s->lib);
    free(s);
}
