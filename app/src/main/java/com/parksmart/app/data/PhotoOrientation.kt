package com.parksmart.app.data

// Utilidades de orientación EXIF agregadas en v0.7.0 para que las fotos se vean derechas en reportes y Control.

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.InputStream

// Lee de los metadatos EXIF la transformación que la cámara o galería guardó para la imagen.
internal fun readPhotoOrientation(input: InputStream): Int =
    ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

// Aplica al bitmap la rotación/reflejo EXIF; devuelve el original si no requiere transformación.
internal fun Bitmap.withExifOrientation(orientation: Int): Bitmap {
    val transform = Matrix()
    // EXIF define tanto giros como reflejos; algunos valores combinan ambas operaciones.
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> transform.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> transform.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            transform.setRotate(90f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> transform.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            transform.setRotate(270f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> transform.setRotate(270f)
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, transform, true)
}
