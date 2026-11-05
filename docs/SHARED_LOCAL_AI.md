# Optional ecosystem AI: Keyboard inspection and dependency

Inspected November 6, 2026 (client date). **Inspection complete; cross-app implementation is blocked on the
Alter AI agent's exact contract revision. No IPC, service discovery, sharing toggle or remote inference is
implemented by this change.** This is an integration assessment and acceptance checklist, not a competing protocol.

## Source baselines and contract search

| Repository | Inspected main commit | Identity from source |
| --- | --- | --- |
| Keyboard | `f5b05d1bc` (PR #32 merged) | `com.aistudio.altermind.keyboard` |
| Alter AI | `99964498359c115dbd49c6a1bd3e291768a35a50` | `com.aistudio.altermind.ai` |
| Fitdrobe | `fdeff851b4499ecb3ba9c123be119686c47a73dd` | `com.aistudio.fitdrobe.wkbvca` |

Keyboard's previous handoff described PR #32 as open. Source/GitHub inspection confirms it is merged;
there were no open Keyboard or Alter AI PRs at inspection. The local Alter AI checkout is an older clean
snapshot, so companion facts below were checked against remote main read-only. No companion files were edited.
Fitdrobe's remote tree has no AGENTS.md; its source was inspected without creating a local product checkout.

The authoritative shared-local-AI contract was not found in Alter AI's main tree, CURRENT_HANDOFF.md,
open PR list, local workspace/shared locations, or the published candidate shared-work branches
`codex/shared-design-and-handoff`, `codex/shared-river-and-notes` and `claude/serene-curie-g5j25x`.
The published branch list was enumerated, but this is not a claim to have audited every historical branch.
The exact contract SHA/path or PR was requested from the user; none was supplied during this inspection.

**Contract revision: unavailable.** Before implementing, retrieve the Alter AI agent's immutable commit SHA,
contract path, protocol/version rules, reproducible distribution files and trust policy. Record their source hashes
here. Do not infer an interface from class names, create another AIDL schema or export a placeholder service.
A contract found later must be verified against current source; branch names alone are not a revision pin.

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
   disconnect from local delete. Coordinate remote downloads only through the owner's contract-defined operation.
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

Alter AI agent must publish: exact versioned AIDL/schema/distribution revision, component discovery/handshake rules,
artifact-set/runtime identity, transport bounds, caller/provider certificate policy, request/session/cancel/death lifecycle,
queue/priority/delete-use semantics, delegation/deduplication controls, and stable errors/fallback rules. This Keyboard
inspection deliberately supplies no alternative wire contract. Then implement its provider/client sides and coordinator
without bypassing Local/Gemini selection or the local engine lock.

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
IDs/runtime declarations and manifest review. No runtime code changed, no APK built/published, and no cross-app tests
were run. Trust rejection/protocol mismatch/provider selection/cancellation/timeout/death/isolation tests must accompany
the actual adapters after the contract is available. Host or one-app results cannot establish ecosystem completion.
