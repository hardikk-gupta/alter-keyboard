# CHANGES.md — ALTER Keyboard Changelog

Append-only record of changes and checks executed for each PR.

---

## PR 1: Import, Scaffolding & Warm-up (In Progress)

### Completed Warm-up Steps:
- Configured application metadata and resource identity to **ALTER Keyboard**.
- Established agent workflow constraints and guidelines in `AGENTS.md`.
- Added attribution and FUTO Source First License compliance notice in `MODIFICATIONS.md`.
- Documented complete design system specifications in `docs/DESIGN.md`.
- Documented signing configuration, shared key requirements, and verification steps in `docs/SIGNING.md`.
- Created `scripts/signing_certificate.sha1` with target SHA-1 fingerprint.
- Initialized core ALTER Design System color tokens (`PitchBlack`, `SlateGrayCard`, `RaisedSurface`, `AlterYellow`, `ErrorRed`, `OrbInk`, etc.) in theme configuration.

### Checks:
- [x] Application name synced across metadata and resources
- [x] Shared signing certificate SHA-1 fingerprint recorded
- [x] License and attribution notice created
- [x] Compilation & Robolectric unit tests pass (`BUILD SUCCESSFUL`)
- [x] PR 1 scaffolding ready for local CLI checkout and submodule initialization
