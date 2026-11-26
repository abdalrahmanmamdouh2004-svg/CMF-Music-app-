package com.example

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One song the person saved with the heart on the watch. */
data class SavedSong(val title: String, val artist: String, val savedAt: Long, val source: String)

/** Which songs count as the same: case, spaces and edge whitespace do not matter. */
object SavedSongKey {
    private val spaces = Regex("\\s+")

    fun norm(s: String): String = s.trim().replace(spaces, " ").lowercase()

    fun of(title: String, artist: String): String = norm(title) + "\u0000" + norm(artist)
}

/** The list rules with no Android in them (so they run in plain unit tests). */
object SavedSongLogic {
    fun contains(list: List<SavedSong>, title: String, artist: String): Boolean {
        val k = SavedSongKey.of(title, artist)
        return list.any { SavedSongKey.of(it.title, it.artist) == k }
    }

    /** Saves the song if it is not in [list], removes it if it is. A song with no title is never saved. */
    fun toggle(list: List<SavedSong>, title: String, artist: String, source: String, now: Long): Pair<List<SavedSong>, Boolean> {
        if (title.isBlank()) return list to false
        val k = SavedSongKey.of(title, artist)
        return if (list.any { SavedSongKey.of(it.title, it.artist) == k }) {
            list.filterNot { SavedSongKey.of(it.title, it.artist) == k } to false
        } else {
            (listOf(SavedSong(title.trim(), artist.trim(), now, source)) + list) to true
        }
    }

    /** The whole list as plain text, one "Title - Artist" per line, newest first. */
    fun asText(list: List<SavedSong>): String =
        list.joinToString("\n") { if (it.artist.isBlank()) it.title else "${it.title} - ${it.artist}" }
}

/**
 * The songs saved with the heart on the Watch Music player, from any app. The phone keeps them (this list is what
 * "saved" means); the watch only shows whether the song on its player is in it.
 */
object SavedSongs {
    private const val PREFS = "saved_songs"
    private const val KEY = "list"

    val songs = MutableStateFlow<List<SavedSong>>(emptyList())
    private var loaded = false

    @Synchronized
    fun init(c: Context) {
        if (loaded) return
        songs.value = read(c)
        loaded = true
    }

    fun isSaved(c: Context, title: String, artist: String): Boolean {
        init(c)
        return SavedSongLogic.contains(songs.value, title, artist)
    }

    /** Saves or unsaves. Returns true if the song is saved now. */
    @Synchronized
    fun toggle(c: Context, title: String, artist: String, source: String): Boolean {
        init(c)
        val (next, saved) = SavedSongLogic.toggle(songs.value, title, artist, source, System.currentTimeMillis())
        if (next != songs.value) {
            songs.value = next
            write(c, next)
        }
        return saved
    }

    @Synchronized
    fun remove(c: Context, song: SavedSong) {
        init(c)
        val k = SavedSongKey.of(song.title, song.artist)
        val next = songs.value.filterNot { SavedSongKey.of(it.title, it.artist) == k }
        songs.value = next
        write(c, next)
    }

    @Synchronized
    fun clear(c: Context) {
        init(c)
        songs.value = emptyList()
        write(c, emptyList())
    }

    private fun read(c: Context): List<SavedSong> = try {
        val raw = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw.isNullOrBlank()) emptyList() else {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val title = o.optString("t")
                if (title.isBlank()) null else SavedSong(title, o.optString("a"), o.optLong("at"), o.optString("s"))
            }
        }
    } catch (e: Exception) {
        AppLogger.logError("SavedSongs", "read", e)
        emptyList()
    }

    private fun write(c: Context, list: List<SavedSong>) {
        try {
            val a = JSONArray()
            for (s in list) a.put(JSONObject().put("t", s.title).put("a", s.artist).put("at", s.savedAt).put("s", s.source))
            c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
        } catch (e: Exception) {
            AppLogger.logError("SavedSongs", "write", e)
        }
    }
}
