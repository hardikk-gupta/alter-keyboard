# Handy Editor 350M Q4_K_M

Fine-tune/conversion: MagicNoThief/handy-editor-lfm2.5-350m
https://huggingface.co/MagicNoThief/handy-editor-lfm2.5-350m
Pinned revision: f1f01e16ab10143f51ffb0fb180ac715dcef964e
File: handy-editor-350m-Q4_K_M.gguf (229,315,072 bytes)
SHA-256: eace6ae31325690abe7a731b02b6d6b92ca9ca8b479b94fe1a6f6bfbefb41356
Base: LiquidAI/LFM2.5-350M, LFM Open License v1.0 (full terms alongside this file).
English-trained specialized editor, not a verified Hinglish model. Model-card scores/desktop latency are author
claims, not ALTER Android benchmarks. ChatML framing must include an explicit empty system turn for llama.cpp;
no ALTER system prompt, example history, memories or tools are attached to this model.

Training-data credit from the model card: MagicNoThief/handy-dictation-editing (CC-BY-4.0), with attribution to
Google Research disfl_qa, nyralabs/disfluency_speech_english and amaai-lab/DisfluencySpeech.
No training dataset or weights are bundled in the app. Weights download only on explicit user action.

Handy is a text cleanup editor, independent of the selected ASR (Whisper/Oriserve, Moonshine
or experimental Nemotron). Its license does not cover the separately licensed Nemotron
model. See [native provenance](../alter-voice/native-handy/SOURCE.md) and
[the experiment guide](../docs/HANDY_EXPERIMENT.md).
