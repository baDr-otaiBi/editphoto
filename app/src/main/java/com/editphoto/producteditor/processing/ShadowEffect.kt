package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * Adds realistic shadow effects to product images.
 * Supports drop shadow, contact shadow, and perspective shadow.
 */
class ShadowEffect {

    /**
     * Adds a natural drop shadow below the product.
     * @param bitmap The product image (with transparent background)
     * @param intensity Shadow opacity (0.0 - 1.0)
     * @param offsetY Vertical offset for the shadow
     * @param blurRadius How much to blur the shadow
     */
    fun addDropShadow(
        bitmap: Bitmap,
        intensity: Float = 0.4f,
        offsetY: Float = 0.05f,
        blurRadius: Float = 0.03f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val shadowOffsetY = (height * offsetY).toInt()
        val shadowBlur = max(1f, width * blurRadius)

        // Create output with extra space for shadow
        val extraHeight = shadowOffsetY + (shadowBlur * 2).toInt()
        val outputWidth = width + (shadowBlur * 2).toInt()
        val outputHeight = height + extraHeight
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val offsetX = shadowBlur.toInt()

        // Create shadow from the product silhouette
        val shadowBitmap = createSilhouette(bitmap, (intensity * 255).toInt())

        // Draw blurred shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(shadowBlur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(
            shadowBitmap,
            offsetX.toFloat(),
            shadowOffsetY.toFloat(),
            shadowPaint
        )

        // Draw original product on top
        canvas.drawBitmap(bitmap, offsetX.toFloat(), 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        shadowBitmap.recycle()
        return output
    }

    /**
     * Adds a contact shadow - a subtle shadow right at the base of the product,
     * simulating it sitting on a surface.
     */
    fun addContactShadow(
        bitmap: Bitmap,
        intensity: Float = 0.5f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Find the bottom edge of the product
        val bottomY = findBottomEdge(bitmap)
        val shadowHeight = (height * 0.08f).toInt()
        val shadowBlur = width * 0.02f

        val extraSpace = shadowHeight + (shadowBlur * 2).toInt()
        val output = Bitmap.createBitmap(width, height + extraSpace, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Create an elliptical contact shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((intensity * 180).toInt(), 0, 0, 0)
            maskFilter = BlurMaskFilter(
                max(1f, shadowBlur),
                BlurMaskFilter.Blur.NORMAL
            )
        }

        // Find horizontal extent of the product at the bottom
        val (leftX, rightX) = findBottomExtent(bitmap, bottomY)
        val centerX = (leftX + rightX) / 2f
        val shadowWidth = (rightX - leftX) * 0.9f

        canvas.drawOval(
            centerX - shadowWidth / 2,
            bottomY.toFloat(),
            centerX + shadowWidth / 2,
            bottomY.toFloat() + shadowHeight,
            shadowPaint
        )

        // Draw original on top
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        return output
    }

    /**
     * Creates a silhouette (solid color version) of the product from its alpha channel.
     */
    private fun createSilhouette(bitmap: Bitmap, alpha: Int): Bitmap {
        val silhouette = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val pixelAlpha = Color.alpha(pixel)
                if (pixelAlpha > 50) {
                    val shadowAlpha = (pixelAlpha.toFloat() / 255f * alpha).toInt()
                    silhouette.setPixel(x, y, Color.argb(shadowAlpha, 0, 0, 0))
                }
            }
        }
        return silhouette
    }

    private fun findBottomEdge(bitmap: Bitmap): Int {
        for (y in bitmap.height - 1 downTo 0) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 50) {
                    return y
                }
            }
        }
        return bitmap.height - 1
    }

    private fun findBottomExtent(bitmap: Bitmap, bottomY: Int): Pair<Int, Int> {
        var leftX = bitmap.width
        var rightX = 0
        val searchRange = max(1, (bitmap.height * 0.1f).toInt())

        for (y in max(0, bottomY - searchRange)..bottomY) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 50) {
                    leftX = min(leftX, x)
                    rightX = max(rightX, x)
                }
            }
        }

        if (leftX >= rightX) {
            leftX = bitmap.width / 4
            rightX = bitmap.width * 3 / 4
        }

        return Pair(leftX, rightX)
    }
}
