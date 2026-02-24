package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.min

/**
 * Adds watermarks and branding overlays to product images.
 * Supports text watermarks, logo overlays, and tiled patterns.
 */
class WatermarkProcessor {

    enum class Position(val nameAr: String) {
        TOP_LEFT("أعلى يسار"),
        TOP_RIGHT("أعلى يمين"),
        BOTTOM_LEFT("أسفل يسار"),
        BOTTOM_RIGHT("أسفل يمين"),
        CENTER("وسط"),
        TILED("متكرر على كامل الصورة")
    }

    /**
     * Adds a text watermark to the image.
     * @param bitmap Source image
     * @param text Watermark text
     * @param position Where to place the watermark
     * @param opacity Transparency (0.0 - 1.0)
     * @param textSizeFraction Size relative to image width (0.01 - 0.1)
     * @param color Text color
     */
    fun addTextWatermark(
        bitmap: Bitmap,
        text: String,
        position: Position = Position.BOTTOM_RIGHT,
        opacity: Float = 0.4f,
        textSizeFraction: Float = 0.04f,
        color: Int = Color.WHITE
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = bitmap.width
        val height = bitmap.height

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(
                (opacity * 255).toInt(),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
            textSize = width * textSizeFraction
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 2f, 2f, Color.argb(100, 0, 0, 0))
        }

        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()
        val padding = width * 0.03f

        when (position) {
            Position.TILED -> {
                drawTiledWatermark(canvas, text, paint, width, height)
                return output
            }
            else -> {
                val (x, y) = calculatePosition(
                    position, width, height,
                    textWidth, textHeight, padding
                )
                canvas.drawText(text, x, y, paint)
            }
        }

        return output
    }

    /**
     * Adds a logo/image watermark.
     * @param bitmap Source image
     * @param logo Logo bitmap
     * @param position Where to place the logo
     * @param opacity Logo opacity (0.0 - 1.0)
     * @param sizeFraction Logo size relative to image (0.05 - 0.3)
     */
    fun addLogoWatermark(
        bitmap: Bitmap,
        logo: Bitmap,
        position: Position = Position.BOTTOM_RIGHT,
        opacity: Float = 0.5f,
        sizeFraction: Float = 0.15f
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = bitmap.width
        val height = bitmap.height

        // Scale logo
        val maxLogoSize = (min(width, height) * sizeFraction).toInt()
        val logoScale = min(
            maxLogoSize.toFloat() / logo.width,
            maxLogoSize.toFloat() / logo.height
        )
        val scaledWidth = (logo.width * logoScale).toInt()
        val scaledHeight = (logo.height * logoScale).toInt()
        val scaledLogo = Bitmap.createScaledBitmap(logo, scaledWidth, scaledHeight, true)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (opacity * 255).toInt()
        }

        val padding = width * 0.03f
        val (x, y) = calculatePosition(
            position, width, height,
            scaledWidth, scaledHeight.toInt(), padding
        )

        canvas.drawBitmap(scaledLogo, x, y - scaledHeight, paint)
        scaledLogo.recycle()

        return output
    }

    private fun drawTiledWatermark(
        canvas: Canvas,
        text: String,
        paint: Paint,
        width: Int,
        height: Int
    ) {
        val savedRotation = canvas.save()
        canvas.rotate(-30f, width / 2f, height / 2f)

        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val stepX = textBounds.width() + width * 0.15f
        val stepY = textBounds.height() + height * 0.1f

        var y = -height.toFloat()
        while (y < height * 2) {
            var x = -width.toFloat()
            while (x < width * 2) {
                canvas.drawText(text, x, y, paint)
                x += stepX
            }
            y += stepY
        }

        canvas.restoreToCount(savedRotation)
    }

    private fun calculatePosition(
        position: Position,
        imgWidth: Int,
        imgHeight: Int,
        elementWidth: Int,
        elementHeight: Int,
        padding: Float
    ): Pair<Float, Float> {
        return when (position) {
            Position.TOP_LEFT -> Pair(padding, elementHeight + padding)
            Position.TOP_RIGHT -> Pair(imgWidth - elementWidth - padding, elementHeight + padding)
            Position.BOTTOM_LEFT -> Pair(padding, imgHeight - padding)
            Position.BOTTOM_RIGHT -> Pair(imgWidth - elementWidth - padding, imgHeight - padding)
            Position.CENTER -> Pair(
                (imgWidth - elementWidth) / 2f,
                (imgHeight + elementHeight) / 2f
            )
            Position.TILED -> Pair(0f, 0f) // Handled separately
        }
    }
}
