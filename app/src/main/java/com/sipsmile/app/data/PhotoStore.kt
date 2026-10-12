package com.sipsmile.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The smile album. Photos live in the app's private storage (nobody else can see them) as
 * smile_<epochMillis>_<characterId>.jpg. "Save to gallery" makes an extra copy in Pictures/SipAndSmile.
 */
object PhotoStore {

    data class Shot(val file: File, val epochMillis: Long, val characterId: String) {
        val dateText: String
            get() = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
                .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()))
    }

    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "smiles").apply { mkdirs() }

    fun newFile(context: Context, characterId: String): File =
        File(dir(context), "smile_${System.currentTimeMillis()}_$characterId.jpg")

    fun parse(file: File): Shot {
        val core = file.name.removePrefix("smile_").removeSuffix(".jpg")
        val epoch = core.substringBefore('_').toLongOrNull() ?: file.lastModified()
        val character = core.substringAfter('_', Roster.DROP)
        return Shot(file, epoch, character)
    }

    /** Newest first. */
    fun list(context: Context): List<Shot> =
        (dir(context).listFiles { f -> f.isFile && f.name.startsWith("smile_") && f.name.endsWith(".jpg") } ?: emptyArray())
            .map { parse(it) }
            .sortedByDescending { it.epochMillis }

    fun count(context: Context): Int = list(context).size

    fun delete(shot: Shot): Boolean = shot.file.delete()

    /** Decodes a downscaled copy so big photos don't eat memory. */
    fun decode(file: File, maxPx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Camera files come out rotated by an EXIF flag. Bake the rotation into the pixels (and mirror,
     * because the selfie preview is mirrored) so every viewer shows the photo the way it looked on screen.
     */
    fun normalize(file: File, mirror: Boolean) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val degrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val src = decode(file, 1600) ?: return
            val m = Matrix()
            if (degrees != 0f) m.postRotate(degrees)
            if (mirror) m.postScale(-1f, 1f)
            val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } catch (e: Throwable) {
            // keep the original file if anything goes wrong
        }
    }

    /** Needs WRITE_EXTERNAL_STORAGE on Android 9 and older (the UI asks for it). */
    fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Copies the photo to Pictures/SipAndSmile so it shows up in the Gallery / Google Photos. */
    fun saveToGallery(context: Context, shot: Shot): Boolean {
        val app = context.applicationContext
        val displayName = "SipAndSmile_${shot.epochMillis}.jpg"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SipAndSmile")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = app.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out -> shot.file.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SipAndSmile")
                folder.mkdirs()
                val target = File(folder, displayName)
                shot.file.copyTo(target, overwrite = true)
                MediaScannerConnection.scanFile(app, arrayOf(target.absolutePath), arrayOf("image/jpeg"), null)
                true
            }
        } catch (e: Throwable) {
            false
        }
    }

    /** Opens the Android share sheet for the photo. */
    fun share(context: Context, shot: Shot, text: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", shot.file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share your smile").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
