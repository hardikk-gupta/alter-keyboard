package com.alter.localai.v1;
/** Small JSON control events only. Inference data uses the supplied descriptor pipes. */
oneway interface ILocalAiCallback {
    void onEvent(String eventJson);
}
