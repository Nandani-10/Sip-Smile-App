package com.sipsmile.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipsmile.app.data.PhotoStore
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs

/** All the smiles you captured, newest first. Tap one to open it. */
@Composable
fun AlbumScreen(prefs: UserPrefs, onBack: () -> Unit, onCamera: () -> Unit) {
    val context = LocalContext.current
    var shots by remember { mutableStateOf(PhotoStore.list(context)) }
    var open by remember { mutableStateOf<PhotoStore.Shot?>(null) }

    BackHandler {
        if (open != null) open = null else onBack()
    }

    SkyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Column {
                    Text("Smile Album 📸", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (shots.isEmpty()) "Nothing here yet" else "${shots.size} captured ${if (shots.size == 1) "moment" else "moments"}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (shots.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CharacterView(Roster.BUTTERFLY, Modifier.size(200.dp), happy = true)
                    Spacer(Modifier.height(10.dp))
                    Text("No smiles captured yet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Tap \"I smiled\" and say yes to the selfie, or start one right now.",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    CuteButton("📸 Take a smile selfie", onClick = onCamera, container = Pastel.PinkDeep)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shots, key = { it.file.name }) { shot ->
                        Thumb(shot) { open = shot }
                    }
                }
            }
        }

        val current = open
        if (current != null) {
            PhotoDetail(
                shot = current,
                name = prefs.name.ifBlank { "You" },
                onClose = { open = null },
                onDeleted = {
                    shots = PhotoStore.list(context)
                    open = null
                }
            )
        }
    }
}

@Composable
private fun Thumb(shot: PhotoStore.Shot, onClick: () -> Unit) {
    val photo = rememberPhoto(shot, 400)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Smile from ${shot.dateText}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            shot.dateText.substringBefore(","),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PhotoDetail(shot: PhotoStore.Shot, name: String, onClose: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val saveToGallery = rememberGallerySaver()
    val photo = rememberPhoto(shot, 1200)
    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() }
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (photo != null) {
                Polaroid(
                    photo = photo,
                    caption = "$name smiling with ${Roster.displayName(shot.characterId).substringBefore(" the")}",
                    subCaption = shot.dateText,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("💾 Save", Modifier.weight(1f)) { saveToGallery(shot) }
                Pill("↗ Share", Modifier.weight(1f)) { PhotoStore.share(context, shot, "A smile from Sip & Smile 😊") }
                Pill("🗑 Delete", Modifier.weight(1f)) { confirmDelete = true }
            }
            Spacer(Modifier.height(10.dp))
            Pill("Close", onClick = onClose)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this smile?") },
            text = { Text("It will be removed from your Smile Album. A copy you saved to your gallery stays there.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    PhotoStore.delete(shot)
                    onDeleted()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
        )
    }
}
