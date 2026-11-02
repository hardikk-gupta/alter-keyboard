package com.aistudio.altermind.voice

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Backtrack: spoken self-corrections in English and Hinglish, and the words that must never be touched. */
class BacktrackTest {
    @Test fun cueCorrectionsKeepOnlyTheLatestValue() {
        val cases = listOf(
            "the meeting is Friday actually Monday" to "the meeting is Monday",
            "let's do coffee at 2 actually 3" to "let's do coffee at 3",
            "call me at 5 pm sorry 6" to "call me at 6 pm",
            "call me at 5 pm sorry 6 pm" to "call me at 6 pm",
            "send ₹500 actually 600" to "send ₹600",
            "book it for Monday I mean Tuesday" to "book it for Tuesday",
            "Meet Friday, no wait, Monday" to "Meet Monday",
            "we fly in March actually April" to "we fly in April",
            "submit it tomorrow actually today" to "submit it today",
            "Monday actually Tuesday sorry Wednesday" to "Wednesday",
            "it costs 40 percent actually 45" to "it costs 45 percent",
            "meeting at 10:30 actually 11" to "meeting at 11",
            "change it to friday make that saturday" to "change it to saturday",
            // Hinglish cues
            "kal nahi nahi parso Rahul ko call karna hai" to "parso Rahul ko call karna hai",
            "meeting Monday nahi nahi Tuesday ko rakho" to "meeting Tuesday ko rakho",
            "5 baje matlab 6 baje milte hain" to "6 baje milte hain",
            "somvar galat bola mangalvar ko aana" to "mangalvar ko aana",
            "aaj nahi kal bhejna" to "kal bhejna",
            "500 rupees nahi 700 rupees dena" to "700 rupees dena",
            "Meeting Monday ko. Nahi nahi, Tuesday ko." to "Meeting Tuesday ko.",
        )
        for ((input, expected) in cases) assertEquals(input, expected, resolveSpokenCorrections(input))
    }

    @Test fun wordsThatOnlyLookLikeCorrectionsStay() {
        val unchanged = listOf(
            "I actually enjoyed the movie on Monday",
            "Monday nahi aa sakta, Tuesday aaunga",
            "We do not meet Friday actually Monday",
            "main nahi aaunga kal actually parso",
            "sorry I am late for the 3 pm call",
            "I mean it, see you on Friday",
            "the code is `Friday actually Monday`",
            "he said \"5 actually 6\" twice",
            "Monday actually Monday works",
            "actually Monday works better",
            "kal milte hain, matlab office mein",
            "I paid 500 and actually it was worth it",
            "room 4 sorry for the noise",
            "we may actually join in June",
        )
        for (text in unchanged) assertEquals(text, text, resolveSpokenCorrections(text))
    }

    @Test fun scratchThatRemovesTheAbandonedClause() {
        val cases = listOf(
            "send it to Rahul scratch that send it to Priya" to "send it to Priya",
            "Hi Rahul, can you call me, scratch that, can you text me" to "Hi Rahul, can you text me",
            "The demo is ready. Ship it today. Strike that. Ship it Monday." to "The demo is ready. Ship it Monday.",
            "buy milk and eggs delete that." to "",
            "scratch that, start over" to "start over",
        )
        for ((input, expected) in cases) assertEquals(input, expected, applyScratchThat(input))
        for (literal in listOf("please delete that file", "I can't scratch the paint", "cancel that order")) {
            assertEquals(literal, applyScratchThat(literal))
        }
    }

    @Test fun restatementsCollapseButListsNeverDo() {
        val collapse = listOf(
            "schedule a meeting for Monday, Tuesday" to "schedule a meeting for Tuesday",
            "schedule a meeting for monday tuesday" to "schedule a meeting for tuesday",
            "call me at 5 pm 6 pm" to "call me at 6 pm",
            "flight in March April" to "flight in April",
        )
        for ((input, expected) in collapse) assertEquals(input, expected, collapseRestatements(input))
        val lists = listOf(
            "I can do Monday, Tuesday and Wednesday",
            "Monday, Tuesday, Wednesday",
            "Monday or Tuesday works",
            "between Monday Tuesday",
            "from Monday Tuesday onwards",
            "Monday aur Tuesday dono",
            "Monday ya Tuesday",
            "Monday Monday again",
            "open Monday to Friday",
            "Monday-Friday",
            "call 5 6 times",
            "kal parso milte hain",
            "aaj kal sab busy hain",
        )
        for (text in lists) assertEquals(text, text, collapseRestatements(text))
    }

    @Test fun validatorAcceptsCorrectionsAndRejectsEverythingElse() = runTest {
        // The restatement is resolved before the editor; an edit restoring both values, or adding "and", is rejected.
        assertEquals("Schedule a meeting for Tuesday.", acceptedDictationEdit("schedule a meeting for Monday, Tuesday",
            "Schedule a meeting for Tuesday."))
        assertNull(acceptedDictationEdit("schedule a meeting for Monday, Tuesday", "Schedule a meeting for Monday and Tuesday."))
        assertNull(acceptedDictationEdit("call Rahul Priya tomorrow", "Call Rahul and Priya tomorrow."))
        assertEquals("Schedule a meeting for Tuesday.",
            AlterDictationAgent.process("schedule a meeting for monday tuesday") { error("simple after correction") })
        assertEquals("Let's do coffee at 3.", acceptedDictationEdit("let's do coffee at 2 actually 3", "Let's do coffee at 3."))
        assertNull(acceptedDictationEdit("let's do coffee at 2 actually 3", "Let's do coffee at 2."))
        assertNull(acceptedDictationEdit("I can do Monday, Tuesday and Wednesday", "I can do Wednesday."))
        assertNull(acceptedDictationEdit("Monday or Tuesday works", "Tuesday works."))
        assertNull(acceptedDictationEdit("schedule a meeting for Monday, Tuesday", "Schedule a meeting for Wednesday."))
        assertNull(acceptedDictationEdit("send 500 to Rahul", "Send 600 to Rahul."))
        assertNull(acceptedDictationEdit("we will not go on Monday", "We will go on Monday."))
        assertEquals("Send it to Priya.", acceptedDictationEdit("send it to Rahul scratch that send it to Priya", "Send it to Priya."))
        assertNull(acceptedDictationEdit("send it to Rahul and send it to Priya", "Send it to Priya."))
    }

    @Test fun backtrackWorksWithoutTheEditorModel() = runTest {
        // Short, cleanup-off and failed-editor paths still resolve corrections instantly.
        assertEquals("Meet Monday.", quickDictation("meet Friday actually Monday"))
        assertEquals("Parso bhejna.", quickDictation("aaj nahi nahi parso bhejna"))
        assertEquals("Send it to Priya.", quickDictation("send it to Rahul scratch that send it to Priya"))
        assertEquals("Coffee at 3.", AlterDictationAgent.process("coffee at 2 actually 3") { error("editor unavailable") })
    }

    @Test fun correctionsLeaveTheSimplePathAndShortResultsSkipTheEditor() = runTest {
        for (text in listOf("schedule a meeting for monday tuesday", "kal nahi nahi parso", "5 baje matlab 6", "rahul scratch that priya")) {
            assertNotEquals(text, DictationCleanupLevel.BASIC, dictationCleanupLevel(text))
        }
        // "Send it to Priya." is simple once corrected, so the editor is never called for it.
        assertEquals("Send it to Priya.",
            AlterDictationAgent.process("send it to Rahul scratch that send it to Priya") { error("must not call the editor") })
        // A longer corrected transcript still goes to the editor, after Backtrack.
        var sent = ""
        AlterDictationAgent.process("um so please book the conference room for the whole team on Friday actually Monday") { sent = it; it }
        assertFalse(sent.contains("Friday"))
    }
}
