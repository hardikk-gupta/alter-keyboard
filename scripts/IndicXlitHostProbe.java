// Host-only probe of the app's compiled Kotlin decoder with real ONNX weights. No phone latency claim.
import ai.onnxruntime.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import kotlin.coroutines.*;
import kotlinx.coroutines.*;

public final class IndicXlitHostProbe {
    public static void main(String[] args) throws Exception {
        Path directory = Paths.get(args[0]);
        List<String> src = Files.readAllLines(directory.resolve("source.txt"));
        List<String> tgt = Files.readAllLines(directory.resolve("target.txt"));
        Map<String, Integer> source = new HashMap<>();
        for (int i = 0; i < src.size(); i++) source.put(src.get(i), i);
        OrtEnvironment env = OrtEnvironment.getEnvironment();
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2); options.setInterOpNumThreads(1);
            OrtSession encoder = env.createSession(directory.resolve("encoder.onnx").toString(), options);
            OrtSession decoder = env.createSession(directory.resolve("decoder.onnx").toString(), options);
            Class<?> type = Class.forName("com.aistudio.altermind.voice.IndicXlit");
            Constructor<?> constructor = type.getDeclaredConstructor(OrtSession.class, OrtSession.class, Map.class, List.class);
            constructor.setAccessible(true);
            try (AutoCloseable engine = (AutoCloseable) constructor.newInstance(encoder, decoder, source, tgt)) {
                Method convert = type.getMethod("convert", String.class, Continuation.class);
                Continuation<String> continuation = new Continuation<>() {
                    public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
                    public void resumeWith(Object value) { throw new AssertionError("Unexpected suspension"); }
                };
                String input = "Baby मैं घर जा रहा हूँ, then I'll call you. API_v2 Résumé १२३।";
                String expected = "Baby main ghar ja raha hun, then I'll call you. API_v2 Résumé 123.";
                long start = System.nanoTime();
                Object result = convert.invoke(engine, input, continuation);
                if (!expected.equals(result)) throw new AssertionError(result);
                long first = System.nanoTime() - start;
                start = System.nanoTime();
                if (!expected.equals(convert.invoke(engine, input, continuation))) throw new AssertionError("Cache changed output");
                System.out.printf("Real Kotlin/ORT sentence + cache PASS; host first %.0f ms, cached %.0f ms%n", first/1e6, (System.nanoTime()-start)/1e6);
                if (!"main buss ghar".equals(convert.invoke(engine, "मैं बस घर", continuation))) throw new AssertionError("Dropped word");
                Object requested = convert.invoke(engine, "बेबी चाहिए कॉल यू आइल देन", continuation);
                if (!"baby chahiye call you ail dena".equals(requested))
                    throw new AssertionError("Requested spellings: " + requested);
                CompletableJob cancelled = JobKt.Job((Job) null);
                cancelled.cancel(new java.util.concurrent.CancellationException("probe"));
                Continuation<String> stopped = new Continuation<>() {
                    public CoroutineContext getContext() { return cancelled; }
                    public void resumeWith(Object value) { throw new AssertionError("Unexpected suspension"); }
                };
                try { convert.invoke(engine, input, stopped); throw new AssertionError("Ignored cancellation"); }
                catch (InvocationTargetException e) {
                    if (!(e.getCause() instanceof java.util.concurrent.CancellationException)) throw e;
                }
                try { convert.invoke(engine, "क".repeat(65), continuation); throw new AssertionError("Truncated long word"); }
                catch (InvocationTargetException e) {
                    if (!(e.getCause() instanceof IllegalArgumentException)) throw e;
                }
                System.out.println("Preserved extra word, cancellation and bounded-word error PASS");
            }
        }
    }
}
