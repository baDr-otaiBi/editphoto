package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.nio.FloatBuffer

/**
 * Uses ML Kit Subject Segmentation to remove backgrounds from product images.
 * The segmentation model identifies the main subject (product) and creates
 * a precise mask to separate it from the background.
 */
class BackgroundRemover {

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
    )

    /**
     * Removes the background from the given bitmap, returning the product
     * on a transparent background.
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val result = segmenter.process(inputImage).await()

        val confidenceMask: FloatBuffer = result.foregroundConfidenceMask
            ?: return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val maskWidth = result.foregroundConfidenceMask?.let { width } ?: width
        val maskHeight = result.foregroundConfidenceMask?.let { height } ?: height

        // Create mask bitmap
        val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
        confidenceMask.rewind()

        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                val confidence = confidenceMask.get()
                // Use a threshold with smooth edges for better quality
                val alpha = when {
                    confidence > 0.9f -> 255
                    confidence > 0.1f -> (confidence * 255).toInt()
                    else -> 0
                }
                maskBitmap.setPixel(x, y, Color.argb(alpha, 255, 255, 255))
            }
        }

        // Apply mask to original image
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw the mask
        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)

        // Apply the original image using SRC_IN to keep only masked area
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        maskBitmap.recycle()
        return output
    }

    /**
     * Removes background and replaces it with a solid color.
     */
    suspend fun replaceBackground(bitmap: Bitmap, backgroundColor: Int): Bitmap {
        val foreground = removeBackground(bitmap)

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(foreground, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        foreground.recycle()
        return output
    }

    fun close() {
        segmenter.close()
    }
}
