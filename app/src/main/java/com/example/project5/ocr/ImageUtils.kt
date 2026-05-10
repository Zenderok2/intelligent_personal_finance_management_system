package com.example.project5.ocr

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {

    private fun resizeIfTooLarge(src: Bitmap): Bitmap {
        val maxSize = 2000

        val w = src.width
        val h = src.height

        if (w <= maxSize && h <= maxSize) return src

        val scale = max(w, h).toFloat() / maxSize
        val nw = (w / scale).toInt()
        val nh = (h / scale).toInt()

        val matrix = Matrix()
        matrix.postScale(nw.toFloat() / w, nh.toFloat() / h)

        return Bitmap.createBitmap(src, 0, 0, w, h, matrix, true)
    }

    fun toBase64(bitmap: Bitmap): String {
        val bmp = resizeIfTooLarge(bitmap)

        val baos = ByteArrayOutputStream()
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } finally {
            baos.close()
        }
    }
}
