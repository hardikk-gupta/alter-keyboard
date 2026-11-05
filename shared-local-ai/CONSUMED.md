# Keyboard contract import

Source: [Alter AI PR #64](https://github.com/hardikk-gupta/ALTER-AI-code-base/pull/64),
commit `13b3fcd3802849751dc12793ee32a05ab1c9768d`, branch `codex/shared-local-ai`.
Protocol **1.0**, Binder package `com.alter.localai.v1`. The PR is not assumed merged.

The complete [v1 directory](v1/README.md) is copied verbatim, including its checksum manifest.
Verify with `cd shared-local-ai/v1 && sha256sum -c SHA256SUMS` (four entries).
`alter-voice` compiles that directory's AIDL directly; there is no second interface copy or dependency release.
The source handoff and baseline were reviewed at the same pinned revision:

- [CONTRACT_HANDOFF.md](https://github.com/hardikk-gupta/ALTER-AI-code-base/blob/13b3fcd3802849751dc12793ee32a05ab1c9768d/docs/compatibility/CONTRACT_HANDOFF.md)
- [BASELINE.md](https://github.com/hardikk-gupta/ALTER-AI-code-base/blob/13b3fcd3802849751dc12793ee32a05ab1c9768d/docs/compatibility/BASELINE.md)

Implemented in this step: AIDL build integration and inactive, package-targeted discovery/current-signer/UID
checks. No service is exported or bound, no setting is enabled and no model is served remotely.
Only matching current signer sets are accepted; no independent deployment pins are configured. Signing
history does not grant trust. This does not prove actual installed companion certificates match.

Model fingerprint generation is blocked on the [serializer clarification](https://github.com/hardikk-gupta/ALTER-AI-code-base/pull/64#issuecomment-6669304540).
Do not infer artifact compatibility from display names or invent an encoding. Update this provenance only
when explicitly consuming a new immutable contract revision; preserve that revision's entire directory.
See [Keyboard integration status](../docs/SHARED_LOCAL_AI.md) for adapter work and device acceptance still required.
