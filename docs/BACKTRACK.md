# Backtrack: spoken corrections

ALTER voice keeps what the speaker meant when they correct themselves mid-sentence, like Wispr Flow's Backtrack.
The rules are deterministic and run in `alter-agent` (`resolveSpokenCorrections`, `collapseRestatements`,
`applyScratchThat`, together `deterministicEdits`). They work with dictation cleanup off, before the Qwen3 editor
when it is on, and the validator checks every editor result against the corrected text.

## What is corrected

| Said | Written | Rule |
| --- | --- | --- |
| the meeting is Friday actually Monday | The meeting is Monday. | cue between two values of the same kind |
| let's do coffee at 2 actually 3 | Let's do coffee at 3. | numbers and times |
| call me at 5 pm sorry 6 | Call me at 6 pm. | the corrected value keeps the old unit |
| send ₹500 actually 600 | Send ₹600. | currency kept |
| kal nahi nahi parso Rahul ko call karna hai | Parso Rahul ko call karna hai. | Hinglish cue |
| 5 baje matlab 6 baje milte hain | 6 baje milte hain. | Hinglish cue |
| Meeting Monday ko. Nahi nahi, Tuesday ko. | Meeting Tuesday ko. | Hindi postposition before the cue |
| schedule a meeting for Monday, Tuesday | Schedule a meeting for Tuesday. | no-cue restatement |
| send it to Rahul scratch that send it to Priya | Send it to Priya. | "scratch that" removes the clause before it |

Values: English days and Roman-Hindi days (somvar … ravivar), months, relative days (today, tomorrow, aaj, kal,
parso), and numbers or times with an optional currency or unit (am, pm, baje, minutes, rupees, percent).

Cues: actually, sorry, I mean, no wait, wait no, no no, or rather, make that; nahi nahi, nahi, matlab,
mera matlab, galat bola, galti se, uske/uski jagah. An optional "make it", "change it to", "to", "on", "for" or
"at" may follow the cue. Chains resolve to the last value ("Monday actually Tuesday sorry Wednesday" → Wednesday).

"Scratch that" and "strike that" remove the clause before them, back to the previous sentence end or comma.
"Delete that" works only when a pause follows it, so "please delete that file" stays.

## What is never changed

- A value right after a negation: "We do not meet Friday actually Monday", "main nahi aaunga kal actually parso".
- A cue that is not between two values of the same kind: "I actually enjoyed the movie on Monday",
  "Monday nahi aa sakta, Tuesday aaunga", "I mean it", "room 4 sorry for the noise".
- Lists: "Monday, Tuesday and Wednesday", "Monday or Tuesday", "Monday, Tuesday, Wednesday",
  "between/from Monday Tuesday", "Monday to Friday", "Monday-Friday".
- Hindi idioms with two relative days: "kal parso" (in a day or two), "aaj kal" (these days). Relative days are
  corrected only with a cue.
- Plain numbers without a cue ("call 5 6 times"), "may" (usually the verb) and anything in quotes or backticks.

## Why deterministic, and the validator

Qwen3 1.7B turned "Monday, Tuesday" into "Monday and Tuesday" on the host test, so no-cue restatements are not
left to the model. The validator now rejects any edit that adds "and", "or", "aur" or "ya", so the editor cannot
turn a correction into a list. Every other content, number, negation and address check is unchanged.
When the corrected text is a simple phrase, the editor is not called at all.

## Not handled yet

- A cue after a verb: "meeting Monday ko rakho nahi nahi Tuesday ko" stays as said.
- Whole-clause restatements without a cue, "never mind", and Hinglish "rehne do" / "chhodo" (often literal).
- Names and places ("Rahul actually Priya") are not values; the editor may keep both.
- Undoing a correction after insertion.

Tests: `alter-voice/src/test/java/com/aistudio/altermind/voice/BacktrackTest.kt` (about 70 English and Hinglish
cases, including the never-changed list above).
