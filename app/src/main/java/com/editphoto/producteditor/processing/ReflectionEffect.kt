package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlin.math.max

/**
 * Creates professional mirror reflection effects beneath products,
 * simulating a glossy surface like those used in high-end product photography.
 */
class ReflectionEffect {

    /**
     * Adds a mirror reflection below the product.
     * @param bitmap Product image (ideally with transparent background)
     * @param reflectionHeight Percentage of image height for reflection (0.0 - 1.0)
     * @param opacity Starting opacity of the reflection (0.0 - 1.0)
     * @param gap Gap between product and reflection in pixels fraction
     */
    fun addReflection(
        bitmap: Bitmap,
        reflectionHeight: Float = 0.4f,
        opacity: Float = 0.35f,
        gap: Float = 0.01f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val reflHeight = (height * reflectionHeight).toInt()
        val gapPixels = (height * gap).toInt()

        // Create flipped version of bottom portion
        val matrix = Matrix().apply { preScale(1f, -1f) }
        val bottomPortion = Bitmap.createBitmap(
            bitmap, 0,
            max(0, height - reflHeight),
            width, reflHeight,
            matrix, true
        )

        // Create fade mask for reflection
        val fadeMask = Bitmap.createBitmap(width, reflHeight, Bitmap.Config.ARGB_8888)
        val fadeCanvas = Canvas(fadeMask)
        val fadePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, reflHeight.toFloat(),
                Color.argb((opacity * 255).toInt(), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        fadeCanvas.drawRect(0f, 0f, width.toFloat(), reflHeight.toFloat(), fadePaint)

        // Apply fade to reflection
        val fadedReflection = Bitmap.createBitmap(width, reflHeight, Bitmap.Config.ARGB_8888)
        val refCanvas = Canvas(fadedReflection)
        refCanvas.drawBitmap(bottomPortion, 0f, 0f, Paint())
        val maskPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        refCanvas.drawBitmap(fadeMask, 0f, 0f, maskPaint)

        // Combine: original + gap + reflection
        val totalHeight = height + gapPixels + reflHeight
        val output = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawBitmap(fadedReflection, 0f, (height + gapPixels).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))

        bottomPortion.recycle()
        fadeMask.recycle()
        fadedReflection.recycle()

        return output
    }

    /**
     * Adds a glossy floor reflection with a surface tint.
     * @param bitmap Product image
     * @param surfaceColor Color tint for the reflective surface
     * @param reflectionStrength How visible the reflection is (0.0 - 1.0)
     */
    fun addGlossyFloorReflection(
        bitmap: Bitmap,
        surfaceColor: Int = Color.parseColor("#F5F5F5"),
        reflectionStrength: Float = 0.25f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val reflHeight = (height * 0.35f).toInt()
        val totalHeight = height + reflHeight

        val output = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw surface color for the bottom area
        val surfacePaint = Paint().apply {
            shader = LinearGradient(
                0f, height.toFloat(), 0f, totalHeight.toFloat(),
                surfaceColor, Color.argb(
                    Color.alpha(surfaceColor),
                    (Color.red(surfaceColor) * 0.9f).toInt(),
                    (Color.green(surfaceColor) * 0.9f).toInt(),
                    (Color.blue(surfaceColor) * 0.9f).toInt()
                ),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, height.toFloat(), width.toFloat(), totalHeight.toFloat(), surfacePaint)

        // Add the reflection
        val reflection = addReflection(bitmap, 0.35f, reflectionStrength, 0.005f)
        canvas.drawBitmap(reflection, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        reflection.recycle()

        return output
    }
}
