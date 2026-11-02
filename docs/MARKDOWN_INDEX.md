# Repository Markdown inventory

Reviewed: November 1, 2026. Scope: every Git-tracked `.md`/`.MD` file, plus the new canonical handoff files
included in this documentation change. Keep this inventory complete whenever a Markdown file is added,
removed or changes role. Read [AGENTS.md](../AGENTS.md) and [agent instructions](../AGENT_INSTRUCTIONS.md).

Review **all** Markdown after each logical code/config/planning change. Update every affected first-party
document immediately, with that change, in the same commit/PR. An unchanged file must be intentionally
unaffected/preserved; do not modify license wording or upstream history just to make every file produce a diff.
The classifications below record that decision. New task-specific audit details belong in PLAN/HANDOFF/CHANGES.

Current guides describe this branch. Main/PR state and runtime checks are dated snapshots in
[HANDOFF.md](../HANDOFF.md); an open PR is not a merged release or a verified phone fix.

Inventoried Markdown files: **46**.

| File | Role/status | November 1 review |
| --- | --- | --- |
| [AGENTS.md](../AGENTS.md) | Current | Canonical repository constraints and immediate documentation rule |
| [AGENT_INSTRUCTIONS.md](../AGENT_INSTRUCTIONS.md) | Current | Coding-agent execution/documentation checklist; supplements AGENTS |
| [CHANGES.md](../CHANGES.md) | Append-only history | New entry added; prior results kept as dated evidence |
| [HANDOFF.md](../HANDOFF.md) | Current snapshot | Main/PR state, implementation, historical evidence, blockers and next actions |
| [LICENSE.md](../LICENSE.md) | Legal/vendor, preserved | Reviewed scope; copyright, legal and bundled dependency notices intentionally unchanged |
| [MODIFICATIONS.md](../MODIFICATIONS.md) | Current + provenance | FUTO modifications and current feature scope; retired component facts identified |
| [PLAN.md](../PLAN.md) | Current plan | Task scope, intended/completed changes and remaining acceptance work |
| [README.md](../README.md) | Current | Entry point, setup, supported features and handoff links |
| [alter-agent/README.md](../alter-agent/README.md) | Current | Text-only agent boundary; LFM only; no retired routing/editor claims |
| [alter-voice/native-handy/SOURCE.md](../alter-voice/native-handy/SOURCE.md) | Archived provenance | Handy excluded from APK; source/license/rebuild facts retained |
| [alter-voice/native-handy/llama/vendor/hash/rotate-bits/LICENSE.md](../alter-voice/native-handy/llama/vendor/hash/rotate-bits/LICENSE.md) | Legal/vendor, preserved | Reviewed scope; copyright, legal and bundled dependency notices intentionally unchanged |
| [alter-voice/native-nemotron/SOURCE.md](../alter-voice/native-nemotron/SOURCE.md) | Current provenance | Active native adapter/source pin, rebuild and diagnostic behavior |
| [alter-voice/native-nemotron/transcribe/THIRD-PARTY-LICENSES.md](../alter-voice/native-nemotron/transcribe/THIRD-PARTY-LICENSES.md) | Legal/vendor, preserved | Reviewed scope; copyright, legal and bundled dependency notices intentionally unchanged |
| [alter-voice/native/SOURCE.md](../alter-voice/native/SOURCE.md) | Archived provenance | Whisper/Oriserve bridge excluded from APK; original source/license facts retained |
| [app/libs/README.md](../app/libs/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/src/main/assets/futo-swipe/LICENSE.md](../app/src/main/assets/futo-swipe/LICENSE.md) | Legal/vendor, preserved | Reviewed scope; copyright, legal and bundled dependency notices intentionally unchanged |
| [app/src/main/assets/futo-swipe/README.md](../app/src/main/assets/futo-swipe/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/src/main/assets/layouts/LayoutSpec.md](../app/src/main/assets/layouts/LayoutSpec.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/src/main/assets/layouts/README.md](../app/src/main/assets/layouts/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/src/main/assets/themes/README.md](../app/src/main/assets/themes/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/src/main/res-large/README.md](../app/src/main/res-large/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/tools/make-keyboard-text-py/README.md](../app/tools/make-keyboard-text-py/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [app/translations/README.md](../app/translations/README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [docs/AUTOMATIC_LANGUAGE_ROUTING.md](../docs/AUTOMATIC_LANGUAGE_ROUTING.md) | Retired experiment | Existing retirement banner retained; former behavior is historical, not active setup |
| [docs/DESIGN.md](../docs/DESIGN.md) | Current | Compact voice UI, shared spacing, visible errors and preserved upstream behavior |
| [docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md) | Current | Portable build/native/test setup; no local-script dependency |
| [docs/FUTO_README.md](../docs/FUTO_README.md) | Upstream/reference, preserved | Reviewed scope; typing/layout/theme/translation/library reference unaffected by ALTER voice changes |
| [docs/HANDY_EXPERIMENT.md](../docs/HANDY_EXPERIMENT.md) | Retired experiment | Existing retirement banner retained; former behavior is historical, not active setup |
| [docs/INDICXLIT_TRANSLITERATION.md](../docs/INDICXLIT_TRANSLITERATION.md) | Retired experiment | Existing retirement banner retained; former behavior is historical, not active setup |
| [docs/INDIC_HINDI_PIPELINE.md](../docs/INDIC_HINDI_PIPELINE.md) | Retired experiment | Existing retirement banner retained; former behavior is historical, not active setup |
| [docs/MARKDOWN_INDEX.md](../docs/MARKDOWN_INDEX.md) | Current inventory | Every tracked Markdown path; classifications and review record |
| [docs/NEMOTRON_EXPERIMENT.md](../docs/NEMOTRON_EXPERIMENT.md) | Current + historical tests | Native language modes, current selection UI; PR #23 results dated as history |
| [docs/PARAKEET_EXPERIMENT.md](../docs/PARAKEET_EXPERIMENT.md) | Current + historical tests | English-only CTC/TDT; obsolete Hindi route removed; PR #26 evidence kept |
| [docs/SIGNING.md](../docs/SIGNING.md) | Current, reviewed unchanged | Shared-key/update constraints and CI one-off-key limitation remain accurate |
| [docs/SITEMAP.md](../docs/SITEMAP.md) | Current | Navigation, voice flow and supported model controls |
| [docs/STREAMING_RECOGNIZER_CHECKS.md](../docs/STREAMING_RECOGNIZER_CHECKS.md) | Current branch checks | PR #31 adapter/probe details; host success is not device acceptance |
| [docs/VOICE_MODELS.md](../docs/VOICE_MODELS.md) | Current | Retained recognizers, LFM conditions, upgrade/recovery and provider-tone limits |
| [docs/dictation-cleanup-instructions.md](../docs/dictation-cleanup-instructions.md) | Reference + current status | Designer prompt reference preserved; implementation/LFM/bypass limits stated |
| [licenses/Automatic-Routing-PROVENANCE.md](../licenses/Automatic-Routing-PROVENANCE.md) | Retired provenance | Added explicit app-retirement context; original artifact/source/license facts preserved |
| [licenses/Handy-Editor-PROVENANCE.md](../licenses/Handy-Editor-PROVENANCE.md) | Retired provenance | Added explicit app-retirement context; original artifact/source/license facts preserved |
| [licenses/IndicConformer-Hindi-PROVENANCE.md](../licenses/IndicConformer-Hindi-PROVENANCE.md) | Retired provenance | Added explicit app-retirement context; original artifact/source/license facts preserved |
| [licenses/IndicXlit-PROVENANCE.md](../licenses/IndicXlit-PROVENANCE.md) | Retired provenance | Added explicit app-retirement context; original artifact/source/license facts preserved |
| [licenses/Nemotron-3.5-PROVENANCE.md](../licenses/Nemotron-3.5-PROVENANCE.md) | Active provenance, preserved | Reviewed model/source/hash facts; retained model bytes and licensing unchanged |
| [licenses/Oriserve-Hinglish-Swift-PROVENANCE.md](../licenses/Oriserve-Hinglish-Swift-PROVENANCE.md) | Retired provenance | Added explicit app-retirement context; original artifact/source/license facts preserved |
| [licenses/Parakeet-110M-PROVENANCE.md](../licenses/Parakeet-110M-PROVENANCE.md) | Active provenance, preserved | Reviewed model/source/hash facts; retained model bytes and licensing unchanged |
| [licenses/Parakeet-TDT-PROVENANCE.md](../licenses/Parakeet-TDT-PROVENANCE.md) | Active provenance, preserved | Reviewed model/source/hash facts; retained model bytes and licensing unchanged |

## Preserved local-link exceptions

The complete scan found four pre-existing unresolved relative links in preserved upstream files, not
in current first-party instructions. They are recorded explicitly rather than rewriting legal/upstream text
or fabricating files from the omitted source subset:

| Preserved file | Original relative target | Disposition |
| --- | --- | --- |
| `docs/FUTO_README.md` | `LICENSE.md` | Upstream copy resolves differently here; the actual repository license is [LICENSE.md](../LICENSE.md) |
| `alter-voice/native-nemotron/transcribe/THIRD-PARTY-LICENSES.md` | `ggml/UPSTREAM` | Omitted from the vendored subset; source pin is documented in the active native SOURCE.md |
| `alter-voice/native-nemotron/transcribe/THIRD-PARTY-LICENSES.md` | `scripts/lib/phonon-LICENSE-Apache-2.0.txt` | Reference from the full upstream tree; preserved notice unchanged |
| `alter-voice/native-nemotron/transcribe/THIRD-PARTY-LICENSES.md` | `docs/models/phonon-2.md` | Reference from the full upstream tree; this model is not offered by ALTER |

All current first-party relative file links must resolve. If the vendored source is refreshed, review the
upstream exceptions against its new pin and retain the original license/notices.

## Other file types

`NOTICE`, `.txt` licenses, source comments, workflows and code are not Markdown; their maintenance/privacy
constraints still apply. The append-only changelog and retired experiment/provenance documents intentionally
contain old model names and earlier test counts. They must not be searched-and-replaced into fictitious current
history. No raw dictation, downloaded model weight, signing secret or private crash dump is a handoff artifact.
