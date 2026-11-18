package com.example

import android.content.Context
import android.media.AudioManager

/** Runs one menu item. Every result string starts with "SUCCESS" or "FAIL". */
object ActionRunner {

    suspend fun run(context: Context, group: MenuGroup, item: MenuItem): String {
        return try {
            when (group.type) {
                GroupType.MUSIC -> runMusic(context, item)
                GroupType.PODCAST -> runPodcast(context, item)
                GroupType.APPS -> TestEngine.launchApp(context, item.value, MediaMenuConfig.returnAfterLaunch(context))
                GroupType.TOGGLES -> runToggle(context, item)
                GroupType.CUSTOM ->
                    if (item.silent) TestEngine.sendSilentGet(context, item.value)
                    else TestEngine.openUrlAsLink(context, item.value)
            }
        } catch (e: Exception) {
            AppLogger.logError("ActionRunner", "run(${item.name})", e)
            "FAIL: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    /** Podcasts use the saved RSS episode queue; platform search is only a fallback. */
    private suspend fun runPodcast(context: Context, item: MenuItem): String {
        val pkg = when {
            item.value.contains("open.spotify.com") || item.podcastCurrentUrl.contains("open.spotify.com") || item.podcastPlayerHint.contains("open.spotify.com") -> "com.spotify.music"
            // A music.youtube.com share link explicitly means the user wants YouTube Music;
            // don't let the app-wide preferred player override that choice.
            item.value.contains("music.youtube.com", true) || item.podcastCurrentUrl.contains("music.youtube.com", true) || item.podcastPlayerHint.contains("music.youtube.com", true) -> "com.google.android.apps.youtube.music"
            item.value.contains("youtube.com") || item.value.contains("youtu.be") || item.podcastCurrentUrl.contains("youtube.com") || item.podcastPlayerHint.contains("youtube.com") || item.podcastPlayerHint.contains("youtu.be") -> MediaMenuConfig.targetPackage(context).ifBlank { "com.google.android.apps.youtube.music" }
            else -> MediaMenuConfig.targetPackage(context)
        }
        try {
            val sm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            sm.getActiveSessions(MediaListenerService.getComponentName(context))
                .firstOrNull { it.packageName == pkg }?.transportControls?.stop()
            kotlinx.coroutines.delay(500)
        } catch (e: Exception) {
            AppLogger.logError("Action", "stop before podcast", e)
        }

        val episodes = PodcastFeed.decodeEpisodes(item.podcastEpisodes)
        val current = episodes.getOrNull(item.podcastIndex)
        val episodeUrl = item.podcastCurrentUrl.ifBlank { current?.let { it.link.ifBlank { it.audioUrl } }.orEmpty() }

        // Compatibility for podcast entries saved by older versions that only stored a show URL
        // and a remembered episode title, with no RSS queue yet.
        if (episodes.isEmpty() && item.episode.isNotBlank()) {
            val before = currentTitle(context, pkg)
            val legacySearch = TestEngine.playFromSearch(context, pkg, "${item.name} ${item.episode}")
            val want = item.episode.lowercase().filter { it.isLetterOrDigit() }
            if (legacySearch.startsWith("SUCCESS") && verified(context, pkg, before) { title ->
                    val actual = title.lowercase().filter { it.isLetterOrDigit() }
                    actual.contains(want) || want.contains(actual)
                }) return "SUCCESS episode: ${item.episode}"
        }
        if (episodeUrl.isNotBlank()) {
            // On an already-running player, prefer the standard Android MediaSession URI command.
            // It can advance playback without needing to launch an Activity from the background.
            if (current != null) {
                val before = currentTitle(context, pkg)
                val viaSession = TestEngine.playFromUri(context, pkg, episodeUrl)
                val wanted = current.title.lowercase().filter { it.isLetterOrDigit() }
                if (viaSession.startsWith("SUCCESS") && verified(context, pkg, before) { title ->
                        val actual = title.lowercase().filter { it.isLetterOrDigit() }
                        actual.contains(wanted) || wanted.contains(actual)
                    }) {
                    PodcastPlayback.setActive(context, item.id)
                    return "SUCCESS episode: ${current.title} (player session)"
                }
                if (viaSession.startsWith("SUCCESS")) AppLogger.log("Action", "Podcast URI command was sent but the player did not confirm ${current.title}")
            }

            val direct = openPodcastUrl(context, pkg, episodeUrl)
            if (direct.startsWith("SUCCESS")) {
                PodcastPlayback.setActive(context, item.id)
                return "SUCCESS episode: ${current?.title ?: item.episode}"
            }
            AppLogger.log("Action", "Podcast deep link was not handled by $pkg: $direct")
        }

        if (current != null) {
            // Some players can find podcast episodes through MediaSession search even when an
            // RSS episode URL is not a native deep link. Verify the title before trusting it.
            val before = currentTitle(context, pkg)
            val search = TestEngine.playFromSearch(context, pkg, "${item.name} ${current.title}")
            val want = current.title.lowercase().filter { it.isLetterOrDigit() }
            if (search.startsWith("SUCCESS") && verified(context, pkg, before) { title ->
                    val actual = title.lowercase().filter { it.isLetterOrDigit() }
                    actual.contains(want) || want.contains(actual)
                }) {
                PodcastPlayback.setActive(context, item.id)
                return "SUCCESS episode: ${current.title}"
            }
        }

        // Last resort: hand the feed's episode page/audio URL to Android's normal app chooser.
        val fallbackUrl = episodeUrl.ifBlank { current?.audioUrl.orEmpty() }.ifBlank { item.value }
        val opened = if (fallbackUrl.isNotBlank()) openPodcastUrl(context, null, fallbackUrl) else "FAIL: no episode link was available"
        if (opened.startsWith("SUCCESS")) PodcastPlayback.setActive(context, item.id)
        return opened
    }

    private fun openPodcastUrl(context: Context, pkg: String?, url: String): String {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                if (!pkg.isNullOrBlank()) setPackage(pkg)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "SUCCESS: opened episode link"
        } catch (e: Exception) {
            "FAIL: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    private fun currentTitle(context: Context, pkg: String): String? = try {
        val sm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
        sm.getActiveSessions(MediaListenerService.getComponentName(context))
            .firstOrNull { it.packageName == pkg }?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
    } catch (e: Exception) { null }

    /** A command that was "sent" is not proof it worked: wait for the player to show a new title. */
    private suspend fun verified(context: Context, pkg: String, before: String?, ok: (String) -> Boolean = { true }): Boolean {
        repeat(10) {
            kotlinx.coroutines.delay(300)
            val t = currentTitle(context, pkg)
            if (t != null && t != before && ok(t)) return true
        }
        return false
    }

    private suspend fun runMusic(context: Context, item: MenuItem): String {
        val url = item.value
        // Spotify and Apple Music links open in their own apps; everything else uses the preferred player.
        val pkg = when {
            url.contains("open.spotify.com") -> "com.spotify.music"
            url.contains("music.apple.com") -> "com.apple.android.music"
            else -> MediaMenuConfig.targetPackage(context)
        }
        val name = item.name
        val goHome = MediaMenuConfig.returnAfterLaunch(context)

        suspend fun byUri() = TestEngine.playFromUri(context, pkg, url)
        suspend fun bySearch() = TestEngine.playFromSearch(context, pkg, name)
        suspend fun byBrowser() = TestEngine.connectMediaBrowserAndPlay(context, pkg, url, name)
        suspend fun byLaunch() = TestEngine.openLink(context, pkg, url, goHome)

        return when (MediaMenuConfig.musicMethod(context)) {
            "URI" -> byUri()
            "SEARCH" -> bySearch()
            "BROWSER" -> byBrowser()
            "LAUNCH" -> byLaunch()
            else -> {
                var last = "FAIL: nothing tried"
                val before = currentTitle(context, pkg)
                // Name search is not used here: it can play any song with a similar name.
                for (label in listOf("URI", "BROWSER", "LAUNCH")) {
                    val r = when (label) {
                        "URI" -> byUri()
                        "BROWSER" -> byBrowser()
                        else -> byLaunch()
                    }
                    if (!r.startsWith("SUCCESS")) { last = r; continue }
                    if (label == "LAUNCH" || verified(context, pkg, before)) return "SUCCESS via $label: $r"
                    last = "FAIL: $label was sent but the player did not change"
                }
                last
            }
        }
    }

    private fun runToggle(context: Context, item: MenuItem): String {
        val kind = try {
            ToggleKind.valueOf(item.value)
        } catch (e: Exception) {
            return "FAIL: unknown toggle '${item.value}'"
        }
        ToggleState.init(context)
        return when (kind) {
            ToggleKind.TORCH -> TestEngine.setTorch(context, !ToggleState.isOn(context, kind))
            ToggleKind.DND -> TestEngine.setDnd(context, !ToggleState.isOn(context, kind))
            ToggleKind.SILENT ->
                if (ToggleState.isOn(context, kind)) TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL, "NORMAL")
                else TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_SILENT, "SILENT")
            ToggleKind.VIBRATE ->
                if (ToggleState.isOn(context, kind)) TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL, "NORMAL")
                else TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE, "VIBRATE")
            ToggleKind.TORCH_ON -> TestEngine.setTorch(context, true)
            ToggleKind.TORCH_OFF -> TestEngine.setTorch(context, false)
            ToggleKind.VOLUME_UP -> TestEngine.adjustVolume(context, true)
            ToggleKind.VOLUME_DOWN -> TestEngine.adjustVolume(context, false)
            ToggleKind.DND_ON -> TestEngine.setDnd(context, true)
            ToggleKind.DND_OFF -> TestEngine.setDnd(context, false)
            ToggleKind.RINGER_SILENT -> TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_SILENT, "SILENT")
            ToggleKind.RINGER_VIBRATE -> TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_VIBRATE, "VIBRATE")
            ToggleKind.RINGER_NORMAL -> TestEngine.setRingerMode(context, AudioManager.RINGER_MODE_NORMAL, "NORMAL")
            ToggleKind.LOCK_SCREEN -> TestEngine.lockScreen(context)
            ToggleKind.SCREENSHOT -> TestEngine.takeScreenshot(context)
        }
    }
}
