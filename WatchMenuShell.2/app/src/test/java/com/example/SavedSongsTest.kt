package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rules of the watch's heart: no Android needed. */
class SavedSongsTest {
    @Test
    fun theSameSongIsFoundWhateverTheCaseOrSpacing() {
        assertEquals(SavedSongKey.of("Blinding Lights", "The Weeknd"), SavedSongKey.of("  blinding   LIGHTS ", "the weeknd"))
        assertTrue(SavedSongKey.of("Blinding Lights", "The Weeknd") != SavedSongKey.of("Blinding Lights", "Someone Else"))
        // the pair is not just glued together: "ab" + "c" is not "a" + "bc"
        assertTrue(SavedSongKey.of("ab", "c") != SavedSongKey.of("a", "bc"))
    }

    @Test
    fun theHeartSavesThenUnsaves() {
        var list = emptyList<SavedSong>()
        val (afterSave, saved) = SavedSongLogic.toggle(list, "Blinding Lights", "The Weeknd", "Control media", 10L)
        assertTrue(saved)
        assertEquals(1, afterSave.size)
        assertEquals(SavedSong("Blinding Lights", "The Weeknd", 10L, "Control media"), afterSave[0])
        assertTrue(SavedSongLogic.contains(afterSave, "blinding lights", "THE WEEKND"))
        list = afterSave
        val (afterRemove, savedAgain) = SavedSongLogic.toggle(list, "Blinding Lights ", "The Weeknd", "Music", 20L)
        assertFalse(savedAgain)
        assertTrue(afterRemove.isEmpty())
    }

    @Test
    fun newestIsFirstAndOtherSongsStay() {
        var list = emptyList<SavedSong>()
        list = SavedSongLogic.toggle(list, "One", "A", "Music", 1L).first
        list = SavedSongLogic.toggle(list, "Two", "B", "Music", 2L).first
        assertEquals(listOf("Two", "One"), list.map { it.title })
        list = SavedSongLogic.toggle(list, "Two", "B", "Music", 3L).first
        assertEquals(listOf("One"), list.map { it.title })
    }

    @Test
    fun aSongWithNoTitleIsNeverSaved() {
        val (list, saved) = SavedSongLogic.toggle(emptyList(), "   ", "Someone", "Control media", 1L)
        assertFalse(saved)
        assertTrue(list.isEmpty())
    }

    @Test
    fun theTextListHasOneLinePerSong() {
        val list = listOf(
            SavedSong("Two", "B", 2L, "Music"),
            SavedSong("One", "", 1L, "Music")
        )
        assertEquals("Two - B\nOne", SavedSongLogic.asText(list))
    }
}
