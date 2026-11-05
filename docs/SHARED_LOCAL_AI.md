# Optional ecosystem AI: Keyboard inspection and dependency

Inspected November 6, 2026 (client date). **Contract imported; only discovery/trust groundwork is
implemented. Cross-app inference, opt-in settings and coordinator routing are not active.**

## Baselines and pinned contract

| Repository | Inspected source commit | Identity from source |
| --- | --- | --- |
| Keyboard main | `f5b05d1bc` (PR #32 merged) | `com.aistudio.altermind.keyboard` |
| Alter AI baseline | `99964498359c115dbd49c6a1bd3e291768a35a50` | `com.aistudio.altermind.ai` |
| Fitdrobe baseline | `fdeff851b4499ecb3ba9c123be119686c47a73dd` | `com.aistudio.fitdrobe.wkbvca` |
| Alter AI contract | `13b3fcd3802849751dc12793ee32a05ab1c9768d` (PR #64) | Protocol 1.0 |

The user supplied the unmerged publication revision after the initial inspection. All four SHA256SUMS
entries verify. [Provenance](../shared-local-ai/CONSUMED.md) records the exact revision and reviewed
CONTRACT_HANDOFF/BASELINE files. The complete [normative contract](../shared-local-ai/v1/README.md)
is unchanged and compiles directly as alter-voice's AIDL source. No companion repository is edited.

### Compatibility report and implementation boundary

- All eight Moonshine Medium files match the inspected Alter catalog. The declared `moonshine / 0.1.5 /
  offline-16khz-v1` contract is offline ASR, not native streaming; preserve current local streaming.
- Qwen3 1.7B is not Alter's Qwen 0.6B. Runtime equality alone cannot authorize reuse. Parakeet formats differ.
- Fingerprint serialization needs an exact string escape policy/reference hashes. Equivalent JSON can encode
  slashes and Unicode differently. [Reported to the contract owner](https://github.com/hardikk-gupta/ALTER-AI-code-base/pull/64#issuecomment-6669304540).
  No modelKey generation, model inventory advertisement or remote download suppression is implemented until
  that clarification is pinned. Do not alter the imported protocol to resolve it locally.
- `PeerTrust` and `InstalledPeerDiscovery` implement the inactive security boundary: query only the exact
  ecosystem packages, reject ambiguous/unavailable services, check current installed signer sets and exclusive
  package UIDs, and provide a Binder caller check for each future endpoint. Android API26/27 uses current
  signatures; API28+ uses `apkContentsSigners`, never history. No deployment pins are configured.
- Manifest queries add only Alter AI and Fitdrobe visibility. There is no exported service, automatic bind,
  model/file access, ecosystem preference or routing change in this step. Discovery candidates are not
  initialized/compatible providers until a successful future handshake.
- Reserved memory/tools remain unavailable, including through a coordinator. Dictation never gains them.

Next: consume the owner's canonicalizer clarification at an exact revision; implement descriptors plus
strict version/JSON negotiation, explicit opt-in/initialization gates and bounded provider/client queues.
Use existing engine locks and request-scoped ownership, scoped PCM/text pipes, cancellation/timeouts/death
cleanup and safe session-boundary routing. Verify local fallback and duplicate-download checks without
changing selections or consuming audio twice. Only then expose a service/settings and test with companions.

## Actual Keyboard boundary

- `alter-voice/.../ModelCatalog.kt` and `assets/alter_voice_models.json`: pinned artifact identity, version,
  filename, size, SHA, type, capabilities and backend. Display names are not compatibility keys.
- `ModelStore.kt`: private `noBackupFilesDir/models/<type>/<id>/<version>`, verified partial download rename,
  atomic metadata and verification before native loading. Stored filesystem paths are private implementation details.
- `ModelDownloads.kt`, `ModelTransfer.kt`, `ModelOperations.kt`: resumable transfers, exact activation checks,
  per-model download/delete mutexes. These locks are process-local, not cross-app coordination.
- `SpeechModels.kt`: one selected downloaded recognizer, serialized load/decode/release, native cancellation;
  Parakeet CTC/TDT, Nemotron, Moonshine Medium and newly retained Oriserve Hinglish Apex. Apex uses the
  feature-selected Whisper CPU bridges; no external Hindi router/transliterator is active.
- `VoicePack.kt`: **Qwen3 1.7B cleanup**, not the superseded LFM-only snapshot. `LIVE_CLEANUP=false`;
  cleanup happens after recording, includes Hindi when enabled, and skips eligible simple phrases.
- `CleanupEngine.kt`: LiteRT-LM Android 0.18.0 CPU, `tryLock` busy fallback, fresh engine per cleanup chunk,
  4,000-character request bound, 512 output-token limit and cancellation/release. It is not a generic chat service.
- `AlterDictationAgent`: caller-owned dictation instructions, examples, deterministic Backtrack and validation.
  Errors/timeouts retain source text. Dictated text never requests tools or memory; the module has no such access.
- `DictationController`: capture is independent of warm-up, journals PCM privately, uses session/setup ownership,
  preserves paused/error drafts and final clipboard/insertion behavior. Android recognition retains recognized text
  rather than source PCM. `UixManager`/voice action preserve private-field restrictions and InputConnection insertion.
- Manifest currently exposes the IME under `BIND_INPUT_METHOD`, not a shared inference endpoint. There is no
  ecosystem permission, package discovery or provider/client Binder contract to reuse as-is.

## Companion capabilities and catalog overlap

Alter AI already has Local/Gemini selection, a pinned model store, LiteRT local generation/embedding,
Whisper/Moonshine speech and bounded approved-memory/tool mechanisms. Its existing exported speech,
accessibility or widget services are not generic model-sharing services. `AgentMemory.kt` versions derived
vectors by model revision, filters active passages and validates embeddings; `AlterAgent.kt` validates allowed
tool proposals. Neither is an approved cross-app retrieval/wardrobe API yet. Keep provider choice, backup and
memory validation authoritative; do not route failed local work to Gemini.

The eight Moonshine Medium assets in both catalogs have identical **filename, revision, size and full SHA-256**:
`adapter.ort`, `cross_kv.ort`, `decoder_kv.ort`, `encoder.ort`, `frontend.model.ort`, `frontend.weights.ort`,
`streaming_config.json`, `tokenizer.bin`. Both declare Moonshine SDK 0.1.5. This is a concrete candidate for
an owner-process speech path after contract negotiation; it is not verified end-to-end compatibility.
The tokenizer also matches other Moonshine size catalogs, which does not make those whole models equivalent.

Keyboard Qwen3 1.7B INT8 (`e4059962…`) is not Alter AI's catalog Qwen 0.6B (`7900eb4e…`); shared branding,
LiteRT-LM 0.18.0 and a `.litertlm` extension do not make them interchangeable. The inspected catalog comparison
found no matching cleanup artifact. Never silently substitute another editor and call it duplicate-download reuse.

Fitdrobe exposes `SegmentationEngine.generateMask` and `MetadataSuggestionEngine.suggestMetadata`.
The inspected local segmentation implementation draws a rounded rectangle; metadata uses aspect ratio and sampled
colors. **No Gemma inference or learned automatic segmentation is established by that code.** Room v1 owns wardrobe
items/outfits; `WardrobeRepository`/`OutfitRepository` expose existing item/outfit reads and flows. The item editor calls those interfaces through its view model; Settings exposes local-suggestion and cloud-generation
opt-ins, not a demonstrated pinned-model inventory. These are small future adapter points. The inspected tree does not establish a pinned local model downloader equivalent to
Keyboard's. Preserve its current image editing, repositories, data and heuristic fallbacks; do not create a chat agent,
claim a style-reference collection, or freeze its evolving v1 to provide this integration.

## Smallest implementation seams after the contract is pinned

1. Wrap `SpeechModels` and the cleanup generation callback, retaining local engines and the dictation policy.
   Choose a provider once before a request/session; never switch mid-recording/insertion or replay consumed audio twice.
   Prefer the matching eight-file speech installation; cleanup reuse requires a separately negotiated exact artifact.
2. Consult negotiated inventory before `ModelDownloads.start`, without changing the local model selection, moving files,
   deleting duplicates or blocking standalone downloads when providers are absent. Keep UI status concise and distinguish
   disconnect from local delete. v1 has no remote download command: preserve explicit local downloads and reconcile races without deleting duplicates.
3. A provider adapter must enter existing model-use/setup and engine locks; do not open a second independent engine
   or call raw internal cleanup with chat permissions. Inventory contains artifact facts, never private paths or drafts.
   Hosting a compatible engine must not export FUTO-derived policy code to other repositories.
4. Pin a successful Alter AI coordinator handshake between sessions. It may delegate to the owning peer without file
   migration. Contract-defined request IDs, loop/hop controls, deadlines and bounded retries must prevent duplication.
   Peer resource hosting does not grant coordinator, memory or tool authority.
5. Authenticate both endpoints using installed certificates and Binder caller UID in every entry point, including
   cancel/inventory operations. Scan only the named ecosystem packages. Reject unknown callers/versions/models before
   work, and invalidate discovery after package changes/resume/death. No shared-UID or broad installed-app scan.
6. Use contract-defined descriptor/stream transport for audio or larger payloads; never put recordings/images/weights
   in large Binder parcels. Preserve cancellation, private draft ownership and single-engine priority under disconnect.
   Do not automatically resubmit partially executed work to a different peer without contract-defined deduplication.

These are required adapter constraints, not invented protocol fields/service names or code already implemented.
Do not add sharing status UI until there is real negotiated availability. Standalone remains the current execution path.

## Signing/trust finding

Keyboard declares the expected shared certificate SHA-1 in `scripts/signing_certificate.sha1` and uses the shared
keystore configuration. Alter AI also has shared/release/debug configuration; Fitdrobe references its own debug/upload
keystore paths. Configuration, matching passwords or package names do **not** prove installed signer compatibility.
No device APK signing-certificate set, approved SHA-256 trust pins or rotation policy was verified here. The existing
Keyboard CI can generate a one-off key when its shared secret is absent. Do not change identities/keys or trust that
fallback to make sharing work. Device certificate evidence and the contract owner's authentication policy are prerequisites;
sharing must fail closed without them, while standalone remains usable.

## Contract-owner and companion pickup

Alter AI has published the AIDL/schema/distribution at the pinned revision above. The contract-owner
pickup is now the canonicalizer clarification and its provider/client/coordinator runtime, which the publication
handoff explicitly marks unimplemented. Preserve Local/Gemini selection and the local engine lock. Do not
infer a working coordinator from the presence of contract files. Keyboard's later adapter steps need the
same versioned model identity and real handshake/cancellation support to validate interoperability.

Fitdrobe owner must adapt its existing AI interfaces and settings to the same pinned contract, retain its standalone
heuristics/Room data, and expose only consent-scoped read-only wardrobe search/item/outfit reads with bounded image
URI/descriptor grants. Existing repository methods are not an exported service or permission implementation.
Alter AI must consume these through its validated tool mechanism. Scoped memory retrieval, provenance/revisions,
revocation/deletion handling and embedding-version isolation require companion work; Keyboard must neither request
nor host those capabilities. Never collect keystrokes, private fields, recordings or drafts into shared memory.

## Acceptance matrix (not executed)

- Each app alone; Keyboard typing, private fields, local download/selection, dictation and recovery unchanged.
- Both peer installation orders; simultaneous compatible download requests and duplicate files preserved.
- Alter AI installed but unlaunched/stopped, uninitialized, wrong signer, incompatible, busy or disconnected:
  standalone/peer operation continues; no promise that installation automatically starts a service.
- Coordinator becomes ready during capture/generation: route only the next safe session; no audio loss or double insertion.
- Provider death/uninstall, cancellation, timeout, queue saturation, concurrent jobs and deletion while in use:
  release resources, retain draft, bound retries, offer local download if no compatible provider remains.
- Reject mismatched checksums/companions/tokenizers/runtime/version and hostile UID/signers; do not leak paths or data.
- No keyboard memory/tool scope, including prompt-injected tool requests and private input; no database merging.
- Upgrade each signed app over installed data; check certificate rotation, selections, Room/backup and model ownership.

Inspection checks: current Git/PR status and source review; eight-component catalog equality; companion capabilities,
IDs/runtime declarations and manifest review. This step adds trust/discovery code and compiles the pinned AIDL;
actual build/test outcomes are recorded in CHANGES.md. No APK is published and no device/cross-app tests were run.
Protocol mismatch/model mismatch/provider selection/cancellation/timeout/death/isolation tests remain required
with the request adapters. Host or one-app results cannot establish ecosystem completion.
