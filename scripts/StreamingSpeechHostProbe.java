// Host-only: compiled app Kotlin classes + SDK 0.1.5 JNI built for the host, never an Android latency test.
package com.aistudio.altermind.voice;

import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.*;
import kotlin.Unit;
import kotlin.coroutines.*;
import kotlin.jvm.functions.Function1;

public final class StreamingSpeechHostProbe {
    private static final Continuation<Unit> CONTINUATION = new Continuation<>() {
        public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
        public void resumeWith(Object result) { throw new AssertionError("Unexpected suspension"); }
    };
    private static float[] audio(String file) throws Exception {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(Paths.get(file).toFile())) {
            if (input.getFormat().getSampleRate() != 16000 || input.getFormat().getChannels() != 1)
                throw new AssertionError("Expected mono 16 kHz PCM");
            ShortBuffer samples = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            float[] result = new float[samples.remaining()];
            for (int i=0; i<result.length; i++) result[i] = samples.get()/32768f;
            return result;
        }
    }
    public static void main(String[] args) throws Exception {
        float[] english = audio(args[2]), hindi = audio(args[3]);
        try (MoonshineSpeech model = MoonshineSpeech.Companion.open(args[0])) {
            model.setKeyterms(Arrays.asList("ALTER", "BRACE"));
            for (int run=0; run<2; run++) {
                AtomicInteger offset = new AtomicInteger();
                Function1<Continuation<? super float[]>, Object> read = ignored -> {
                    int start = offset.getAndAdd(1600);
                    return start >= english.length ? null : Arrays.copyOfRange(english,start,Math.min(start+1600,english.length));
                };
                String[] latest = {""};
                model.stream(read, text -> { latest[0] = text; return Unit.INSTANCE; }, CONTINUATION);
                if (!latest[0].toLowerCase(Locale.ROOT).contains("portrait")) throw new AssertionError(latest[0]);
                System.out.println("Moonshine fresh stream "+run+": "+latest[0]);
            }
            if (!model.decode(english).toLowerCase(Locale.ROOT).contains("portrait")) throw new AssertionError("Saved decode failed");
        }
        NemotronNative nativeModel = NemotronNative.INSTANCE;
        long handle = nativeModel.load(args[1]);
        if (handle == 0) throw new AssertionError("Nemotron load failed");
        try {
            nativeModel.prepare(handle);
            try {
                nativeModel.begin(handle,"not-a-language",2);
                throw new AssertionError("Missing native language error");
            } catch (IllegalStateException expected) {
                if (!expected.getMessage().contains("unsupported language")) throw expected;
                System.out.println("Native failure reason PASS: "+expected.getMessage());
            } finally { nativeModel.end(handle); }
            for (float[] pcm : new float[][]{english,hindi}) for (String language : new String[]{"en-US","auto","hi-IN"}) {
                nativeModel.prepare(handle);
                if (!nativeModel.begin(handle,language,2)) throw new AssertionError("begin "+language);
                try {
                    for (int i=0; i<pcm.length; i+=1600)
                        if (nativeModel.feed(handle,Arrays.copyOfRange(pcm,i,Math.min(i+1600,pcm.length))) == null)
                            throw new AssertionError("feed "+language);
                    byte[] text = nativeModel.finish(handle);
                    if (text == null || new String(text,java.nio.charset.StandardCharsets.UTF_8).isBlank())
                        throw new AssertionError("Empty result "+language);
                    System.out.println("Nemotron "+language+": "+new String(text,java.nio.charset.StandardCharsets.UTF_8));
                } finally { nativeModel.end(handle); }
            }
        } finally { nativeModel.close(handle); }
        System.out.println("PASS: actual Kotlin/JNI Moonshine streaming/reuse/saved decoding and Nemotron language modes/errors");
    }
}
