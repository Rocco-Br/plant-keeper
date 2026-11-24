package com.plantkeeper.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Eén foto per item, als JPEG in de interne app-map. In de database staat alleen de bestandsnaam. */
class PhotoStore(val dir: File) {
    fun file(name: String) = File(dir, name)

    fun delete(name: String?) {
        if (name != null) file(name).delete()
    }

    /** Verkleint de gekozen foto tot maximaal [MAX_PX] en slaat hem op. Geeft de nieuwe bestandsnaam terug. */
    fun save(context: Context, uri: Uri): String {
        val bitmap = decodeScaled(context, uri, MAX_PX)
        dir.mkdirs()
        val name = "${newId()}.jpg"
        file(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }

    companion object {
        const val MAX_PX = 1024

        fun decodeScaled(context: Context, uri: Uri, maxPx: Int): Bitmap {
            if (Build.VERSION.SDK_INT >= 28) {
                // ImageDecoder past ook de EXIF-rotatie toe.
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
                    val (w, h) = info.size.width to info.size.height
                    val scale = maxPx.toFloat() / max(w, h)
                    if (scale < 1f) d.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }
            // shortcut: op Android 8 en 9 wordt de EXIF-rotatie genegeerd, upgraden met androidx.exifinterface als dat stoort.
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            val decoded = resolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("Foto kon niet gelezen worden")
            val scale = maxPx.toFloat() / max(decoded.width, decoded.height)
            if (scale >= 1f) return decoded
            return Bitmap.createScaledBitmap(decoded, (decoded.width * scale).roundToInt(), (decoded.height * scale).roundToInt(), true)
        }
    }
}
