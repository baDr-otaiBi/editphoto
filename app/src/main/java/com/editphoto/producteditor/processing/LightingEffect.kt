package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

/**
 * Applies professional lighting effects to product images.
 * Supports studio-style top lighting, rim lighting, and gradient enhancement.
 */
class LightingEffect {

    /**
     * Applies a soft studio light effect from the top,
     * simulating professional product photography lighting.
     * @param bitmap Product image (with or without transparent background)
     * @param intensity Light strength (0.0 - 1.0)
     */
    fun applyStudioLight(bitmap: Bitmap, intensity: Float = 0.3f): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Create a radial gradient light from the top center
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f,
                height * 0.2f,
                max(width, height).toFloat(),
                intArrayOf(
                    Color.argb((intensity * 100).toInt(), 255, 255, 255),
                    Color.argb((intensity * 50).toInt(), 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightPaint)

        return output
    }

    /**
     * Adds a rim/edge light effect that highlights the product edges,
     * creating a professional separation from the background.
     * @param bitmap Product image with transparent background
     * @param intensity Light strength (0.0 - 1.0)
     * @param lightColor The color of the rim light
     */
    fun applyRimLight(
        bitmap: Bitmap,
        intensity: Float = 0.6f,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val r = Color.red(lightColor)
        val g = Color.green(lightColor)
        val b = Color.blue(lightColor)

        // Detect edges and apply light
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)

                if (alpha > 128) {
                    // Check if near edge (near transparent pixel)
                    val isNearEdge = isNearTransparentEdge(bitmap, x, y, width, height, 3)
                    if (isNearEdge) {
                        val origR = Color.red(pixel)
                        val origG = Color.green(pixel)
                        val origB = Color.blue(pixel)

                        val newR = min(255, origR + (r * intensity).toInt())
                        val newG = min(255, origG + (g * intensity).toInt())
                        val newB = min(255, origB + (b * intensity).toInt())

                        output.setPixel(x, y, Color.argb(alpha, newR, newG, newB))
                    }
                }
            }
        }

        return output
    }

    /**
     * Enhances the overall brightness and contrast of the product.
     * @param bitmap Product image
     * @param brightness Brightness adjustment (-1.0 to 1.0)
     * @param contrast Contrast adjustment (0.5 to 2.0)
     */
    fun adjustBrightnessContrast(
        bitmap: Bitmap,
        brightness: Float = 0.1f,
        contrast: Float = 1.1f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val brightnessOffset = (brightness * 255).toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)

                if (alpha > 0) {
                    var r = Color.red(pixel).toFloat()
                    var g = Color.green(pixel).toFloat()
                    var b = Color.blue(pixel).toFloat()

                    // Apply contrast
                    r = ((r - 128) * contrast + 128)
                    g = ((g - 128) * contrast + 128)
                    b = ((b - 128) * contrast + 128)

                    // Apply brightness
                    r += brightnessOffset
                    g += brightnessOffset
                    b += brightnessOffset

                    output.setPixel(
                        x, y,
                        Color.argb(
                            alpha,
                            r.toInt().coerceIn(0, 255),
                            g.toInt().coerceIn(0, 255),
                            b.toInt().coerceIn(0, 255)
                        )
                    )
                }
            }
        }

        return output
    }

    private fun isNearTransparentEdge(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int
    ): Boolean {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) return true
                if (Color.alpha(bitmap.getPixel(nx, ny)) < 50) return true
            }
        }
        return false
    }
}
