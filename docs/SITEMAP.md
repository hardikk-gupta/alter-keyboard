# ALTER Keyboard: sitemap and user flows

How the app is organised and how people move through it. The structure follows the UI/UX Pro Max review
(github.com/nextlevelbuilder/ui-ux-pro-max-skill, `ux` guidelines): primary task first, progressive disclosure,
grouped navigation with a predictable back, empty states with one clear next step, and 48 dp Android touch targets.

## Sitemap

```
Android launcher: ALTER Keyboard icon
└── Settings home
    ├── [Set up ALTER voice] card ............ only until voice works; one "Set up voice" button
    ├── [Payment reminder] ................... when shown by the upstream payment logic (license requirement)
    ├── Voice
    │   └── ALTER voice ...................... subtitle: "Ready · <model>" or the missing step
    │       ├── Set up ALTER voice ........... only while something is missing
    │       │   ├── 1 Allow the microphone ... [Allow]
    │       │   └── 2 Download <recommended> . [Download], progress, used as soon as it finishes
    │       ├── Speech ....................... model in use · Dictation language
    │       │   └── All speech models (n) .... collapsed: Moonshine, Whisper, device recognition; download/use/delete
    │       ├── Keyboard ..................... Start mode (Full / Pill) · End recording after silence
    │       ├── Dictation cleanup ............ switch · LFM2.5 model download/delete
    │       └── Storage used
    ├── Typing
    │   ├── Languages
    │   ├── Keyboard & typing
    │   ├── Swipe typing
    │   └── Predictive text
    ├── Look & feel
    │   ├── Themes ........................... ALTER theme is the default; other themes stay
    │   └── Actions .......................... toolbar actions (ALTER flat-eye voice icon, Minimize, undo, clipboard, …)
    └── More
        ├── Help ............................. version, attribution line (last item)
        ├── Payment .......................... upstream payment page, until paid (license requirement)
        ├── Miscellaneous
        ├── Credits
        └── Developer ........................ only in developer mode
Search (top right of the home) reaches every setting by name.
```

The keyboard itself has two surfaces:

```
Text field focused
├── Full keyboard (default start mode)
│   ├── Action bar: expand · suggestions · pinned actions (Minimize, ALTER flat-eye voice icon)
│   │   └── ALTER flat-eye icon → suggestion row becomes ALTER's voice strip (external discard ✕ + pill + external pause/resume); keys stay
│   └── Keys
└── Pill mode (start mode "Pill", or Minimize)
    └── 56 dp bar: globe (switch input method) · pill · keyboard button (back to full keyboard)
Password / no-personalized-learning field → full keyboard, voice and recovered draft hidden
```

Voice controls use the toolbar's 20dp icon size and simple round-ended strokes like the cross, with 48dp-wide touch
areas. A paused pill shows a still white line. Hiding the keyboard pauses recording; already-finished audio keeps
processing in the keyboard service and copies final text to the clipboard while retaining the private draft.

## User flows

### First run

```mermaid
flowchart TD
    A[Install APK from Releases] --> B[Open ALTER Keyboard]
    B --> C[Setup: enable ALTER Keyboard in Android]
    C --> D[Setup: select it as the input method]
    D --> E[Settings home]
    E --> F{Voice ready?}
    F -- no --> G["Set up ALTER voice" card]
    G --> H[ALTER voice: step 1 Allow microphone]
    H --> I[Step 2 Download recommended model]
    I --> J[Model used automatically when the download ends]
    J --> K[Card disappears; home shows "Ready · model"]
    F -- yes --> K
```

### Dictating in the full keyboard

```mermaid
flowchart TD
    A[Tap ALTER flat-eye icon in the action bar] --> B{Voice ready?}
    B -- no --> S[Opens Settings → ALTER voice at the setup steps]
    B -- yes --> C[Suggestions become the voice strip; wave follows the real mic level]
    C -- tap strip or flat-eye icon --> D[Full-width original orb animation: transcribe and clean up]
    C -- Pause / interrupted microphone / keyboard hides --> P[Private draft saved; paused pill on return]
    P -- Play --> C
    P -- tap centre --> D
    C -- explicit discard cross --> X[Draft deleted; suggestions return]
    P -- explicit discard cross --> X
    D -- tap / Pause --> P
    D -- keyboard hides --> BG[Continue processing in keyboard service]
    BG --> CL[Copy final text to clipboard; keep private draft]
    BG -- service destroyed --> P
    D --> E[Editor accepts text; draft deleted; suggestions return]
    D -- failure / insertion refused --> P
```

Audio for Whisper/Moonshine is journaled before decoding and is recoverable after process restart (one draft,
maximum fifteen minutes, private storage excluded from Android backup). Android on-device recognition can only save
recognized text, not source audio. Reopening never starts the microphone or inserts text automatically. Pause/resume
sits outside the right-hand curve, opposite the external discard cross; the paused centre processes saved dictation. Opening notifications alone does
not request a stop. Switching between full keyboard and pill mode pauses capture and preserves the draft; finished
audio continues processing to the clipboard.

### Pill mode

```mermaid
flowchart LR
    A[Field focused, start mode Pill] --> B[Pill bar]
    K[Full keyboard] -- Minimize --> B
    B -- keyboard button --> K
    B -- tap pill --> C[Listening] -- tap --> D[Thinking] --> E[Text inserted; pill idle]
    B -- globe --> F[Android input-method picker]
```

## UX rules applied (and where)

| Rule (UI/UX Pro Max `ux`) | Where |
| --- | --- |
| Primary task first, visual hierarchy | Voice group first on the home; setup card above everything else until voice works |
| Progressive disclosure | All speech models collapsed behind "All speech models"; setup steps only while needed |
| Empty state with guidance | "Set up ALTER voice" card and the voice row's status subtitle name the missing step |
| Remove needless steps | A finished download is used automatically when no model is chosen yet |
| Grouped navigation, predictable back | Four labelled home groups; one back arrow left of every page title |
| Multi-step progress | Numbered step badges that turn yellow with a check when done |
| Touch targets 48 dp, 8 dp spacing | Rows, buttons and pill bar buttons; the voice strip's ✕ is 48 dp wide and the full 40 dp action-bar height |
| Don't send users to wrong destinations | FUTO's "check for updates" item removed from the home (ALTER builds come from Releases) |

### Shared ALTER dictation core

The voice pipeline is audio → transcription → `alter-agent` dictation policy → optional local cleanup model →
validated text output. The reusable core owns instructions/examples, explicit weekday correction handling,
formatting, edit validation, bounded processing and full-transcript fallback. The keyboard supplies model inference
and handles insertion/clipboard delivery. No main-app installation, chat history, memories, tools or cloud fallback
is required. Cleanup remains opt-in and Hindi mode retains its existing bypass. This is a dictation-specific core,
not the main app's full chat/action agent; the main app has not yet migrated to this module.
