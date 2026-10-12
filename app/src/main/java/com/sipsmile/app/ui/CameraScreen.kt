package com.sipsmile.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sipsmile.app.R
import com.sipsmile.app.data.PhotoStore
import com.sipsmile.app.data.PrefsRepository
import com.sipsmile.app.data.Roster
import com.sipsmile.app.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A white instant-photo card with a caption underneath. */
@Composable
fun Polaroid(
    photo: ImageBitmap,
    caption: String,
    subCaption: String,
    modifier: Modifier = Modifier,
    tilt: Float = -2f,
) {
    Column(
        modifier
            .rotate(tilt)
            .shadow(10.dp, RoundedCornerShape(10.dp))
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = photo,
            contentDescription = "Your smile",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(caption, color = Pastel.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Text(subCaption, color = Pastel.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

/** Returns a function that saves a photo to the phone's gallery, asking for storage permission on old Android versions. */
@Composable
fun rememberGallerySaver(): (PhotoStore.Shot) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<PhotoStore.Shot?>(null) }

    fun doSave(shot: PhotoStore.Shot) {
        val ok = PhotoStore.saveToGallery(context, shot)
        Toast.makeText(
            context,
            if (ok) "Saved to your gallery 📲 (Pictures/SipAndSmile)" else "Couldn't save the photo 😕",
            Toast.LENGTH_LONG
        ).show()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val shot = pending
        pending = null
        if (shot != null) {
            if (granted) doSave(shot)
            else Toast.makeText(context, "Allow storage access to save photos to your gallery", Toast.LENGTH_LONG).show()
        }
    }

    return { shot ->
        val needsPermission = PhotoStore.needsLegacyStoragePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pending = shot
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doSave(shot)
        }
    }
}

/** Loads a photo off the main thread. */
@Composable
fun rememberPhoto(shot: PhotoStore.Shot, maxPx: Int): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, shot.file.absolutePath) {
        value = withContext(Dispatchers.IO) { PhotoStore.decode(shot.file, maxPx)?.asImageBitmap() }
    }
    return state.value
}

/** Selfie flow: permission, live preview with a 3-2-1 countdown, then the "we captured the moment" card. */
@Composable
fun CameraScreen(prefs: UserPrefs, characterId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    var shot by remember { mutableStateOf<PhotoStore.Shot?>(null) }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    BackHandler { onDone() }

    val taken = shot
    when {
        taken != null -> ShotResult(
            prefs = prefs,
            shot = taken,
            onRetake = {
                PhotoStore.delete(taken)
                shot = null
            },
            onDone = onDone
        )

        granted -> LiveCamera(characterId = characterId, onClose = onDone, onShot = { shot = it })

        else -> PermissionAsk(
            denied = denied,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onDone
        )
    }
}

@Composable
private fun PermissionAsk(denied: Boolean, onAllow: () -> Unit, onClose: () -> Unit) {
    SkyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CharacterView(Roster.BIRD, Modifier.size(180.dp), happy = true)
            Spacer(Modifier.height(12.dp))
            Text("Let's capture your smile 📸", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                if (denied) "The camera is off for Sip & Smile. You can switch it on in your phone's app settings, or try again."
                else "Sip & Smile needs the camera for one selfie. Photos stay on your phone and are never uploaded.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            CuteButton("Allow camera", onAllow, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Pill("Not now", onClick = onClose)
        }
    }
}

@Composable
private fun LiveCamera(characterId: String, onClose: () -> Unit, onShot: (PhotoStore.Shot) -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }

    DisposableEffect(previewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(activity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                capture = imageCapture
            } catch (e: Exception) {
                error = "Couldn't open the front camera on this phone 😕"
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            try {
                future.get().unbindAll()
            } catch (e: Exception) {
                // nothing to clean up
            }
        }
    }

    fun takePhoto() {
        val imageCapture = capture ?: return
        val file = PhotoStore.newFile(context, characterId)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    scope.launch {
                        withContext(Dispatchers.IO) { PhotoStore.normalize(file, mirror = true) }
                        onShot(PhotoStore.parse(file))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                    error = "The photo didn't work. Please try again."
                }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // soft rounded frame
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(10.dp)
                .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.55f)), RoundedCornerShape(32.dp))
        )

        // top row: close + a little buddy cheering you on
        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .clickable(enabled = !busy) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Pastel.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            CharacterView(characterId, Modifier.size(84.dp), happy = true)
        }

        if (countdown > 0) {
            Text(
                countdown.toString(),
                color = Color.White,
                fontSize = 140.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.45f)) {
                Text(
                    if (busy) "Smile! 😊" else "Tap the button when you're ready. I'll count 3, 2, 1",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .size(78.dp)
                    .border(BorderStroke(4.dp, Color.White), CircleShape)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(if (busy || capture == null) Color.White.copy(alpha = 0.5f) else Color.White)
                    .clickable(enabled = !busy && capture != null) {
                        busy = true
                        scope.launch {
                            for (n in 3 downTo 1) {
                                countdown = n
                                delay(900)
                            }
                            countdown = 0
                            flash.snapTo(0.9f)
                            takePhoto()
                            flash.animateTo(0f, tween(350))
                        }
                    }
            )
        }

        // camera flash
        if (flash.value > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }

        val message = error
        if (message != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(28.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, color = Pastel.Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    CuteButton("Back", onClose, fontSize = 15)
                }
            }
        }
    }
}

@Composable
private fun ShotResult(prefs: UserPrefs, shot: PhotoStore.Shot, onRetake: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val saveToGallery = rememberGallerySaver()
    val photo = rememberPhoto(shot, 1200)
    val name = prefs.name.ifBlank { "You" }

    LaunchedEffect(shot.file.absolutePath) {
        if (prefs.soundOn) Sfx.play(context, R.raw.sparkle, 0.55f)
    }

    SkyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Yahh! We captured the moment! 🎉", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your smile is saved in your Smile Album.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            if (photo != null) {
                Polaroid(
                    photo = photo,
                    caption = "$name smiling with ${Roster.displayName(shot.characterId).substringBefore(" the")}",
                    subCaption = shot.dateText,
                    modifier = Modifier.fillMaxWidth(0.82f)
                )
            } else {
                Spacer(Modifier.height(240.dp))
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CuteButton(
                    "💾 Save to gallery", onClick = { saveToGallery(shot) },
                    modifier = Modifier.weight(1f), container = Pastel.Sky, fontSize = 14
                )
                CuteButton(
                    "↗ Share", onClick = { PhotoStore.share(context, shot, "A smile from Sip & Smile 😊") },
                    modifier = Modifier.weight(1f), container = Pastel.PinkDeep, fontSize = 14
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("🔁 Retake", onClick = onRetake)
                Pill("✓ Done", onClick = onDone)
            }
        }
        CelebrationBurst()
    }
}
