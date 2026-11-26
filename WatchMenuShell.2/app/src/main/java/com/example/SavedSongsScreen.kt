package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The songs saved with the heart on the Watch Music player. */
@Composable
fun SavedSongsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val songs by SavedSongs.songs.collectAsState()
    LaunchedEffect(Unit) { SavedSongs.init(context) }

    Column(Modifier.fillMaxSize()) {
        NTopBar(title = "Saved songs", onBack = onBack)
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing saved yet. Tap the heart on the Watch Music player while a song plays, in any app.",
                    color = NColors.Dim, fontFamily = NType, fontSize = 14.sp
                )
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NButton(
                        "Copy list",
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Saved songs", SavedSongLogic.asText(songs)))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NButton(
                        "Clear all",
                        onClick = {
                            SavedSongs.clear(context)
                            WatchApp.savedChanged()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(songs, key = { SavedSongKey.of(it.title, it.artist) }) { song ->
                        Row(
                            Modifier.fillMaxWidth().background(NColors.Card, RoundedCornerShape(18.dp)).padding(start = 18.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = NColors.Text, fontFamily = NType, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (song.artist.isNotBlank()) {
                                    Text(song.artist, color = NColors.Dim, fontFamily = NType, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = {
                                SavedSongs.remove(context, song)
                                WatchApp.savedChanged()
                            }) { Icon(Icons.Filled.Close, "Remove", tint = NColors.Dim) }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
