package com.alter.localai.v1;
import android.os.ParcelFileDescriptor;
import com.alter.localai.v1.ILocalAiCallback;
/** Every transaction MUST authenticate Binder.getCallingUid(), including cancellation. */
oneway interface ILocalAiProvider {
    void handshake(String helloJson, ILocalAiCallback callback);
    void inventory(String requestJson, ILocalAiCallback callback);
    void submit(String requestJson, in ParcelFileDescriptor input, in ParcelFileDescriptor output, ILocalAiCallback callback);
    void cancel(String requestId);
}
