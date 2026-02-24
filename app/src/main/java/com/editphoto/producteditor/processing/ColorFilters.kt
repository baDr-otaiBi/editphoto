package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Professional color filters and enhancements for product photography.
 * Includes warm/cool tones, vibrance boost, B&W, and more.
 */
class ColorFilters {

    enum class Filter(val nameAr: String) {
        NONE("بدون"),
        WARM("دافئ"),
        COOL("بارد"),
        VIVID("حيوي"),
        SOFT("ناعم"),
        BLACK_WHITE("أبيض وأسود"),
        SEPIA("سيبيا"),
        HIGH_CONTRAST("تباين عالي"),
        BRIGHT("ساطع"),
        DRAMATIC("درامي"),
        PRODUCT_POP("إبراز المنتج"),
        CLEAN("نظيف")
    }

    /**
     * Applies a color filter to the bitmap.
     */
    fun applyFilter(bitmap: Bitmap, filter: Filter): Bitmap {
        if (filter == Filter.NONE) return bitmap.copy(Bitmap.Config.ARGB_8888, false)

        val colorMatrix = when (filter) {
            Filter.NONE -> return bitmap.copy(Bitmap.Config.ARGB_8888, false)

            Filter.WARM -> ColorMatrix(
                floatArrayOf(
                    1.2f, 0.1f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.9f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            Filter.COOL -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0.1f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            Filter.VIVID -> {
                val cm = ColorMatrix()
                cm.setSaturation(1.5f)
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        1.1f, 0f, 0f, 0f, -15f,
                        0f, 1.1f, 0f, 0f, -15f,
                        0f, 0f, 1.1f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                cm
            }

            Filter.SOFT -> {
                val cm = ColorMatrix()
                cm.setSaturation(0.8f)
                val brightMatrix = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 15f,
                        0f, 1f, 0f, 0f, 15f,
                        0f, 0f, 1f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(brightMatrix)
                cm
            }

            Filter.BLACK_WHITE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                cm
            }

            Filter.SEPIA -> {
                val bwMatrix = ColorMatrix()
                bwMatrix.setSaturation(0f)
                val sepiaMatrix = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 40f,
                        0f, 1f, 0f, 0f, 20f,
                        0f, 0f, 1f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                bwMatrix.postConcat(sepiaMatrix)
                bwMatrix
            }

            Filter.HIGH_CONTRAST -> ColorMatrix(
                floatArrayOf(
                    1.4f, 0f, 0f, 0f, -40f,
                    0f, 1.4f, 0f, 0f, -40f,
                    0f, 0f, 1.4f, 0f, -40f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            Filter.BRIGHT -> ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 20f,
                    0f, 1.15f, 0f, 0f, 20f,
                    0f, 0f, 1.15f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            Filter.DRAMATIC -> {
                val cm = ColorMatrix()
                cm.setSaturation(1.3f)
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -35f,
                        0f, 1.3f, 0f, 0f, -35f,
                        0f, 0f, 1.3f, 0f, -35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(contrastMatrix)
                cm
            }

            Filter.PRODUCT_POP -> {
                // Boost saturation slightly, increase brightness, sharpen contrast
                val cm = ColorMatrix()
                cm.setSaturation(1.2f)
                val enhanceMatrix = ColorMatrix(
                    floatArrayOf(
                        1.1f, 0f, 0f, 0f, 5f,
                        0f, 1.1f, 0f, 0f, 5f,
                        0f, 0f, 1.1f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(enhanceMatrix)
                cm
            }

            Filter.CLEAN -> {
                // Slightly desaturated, bright, clean look
                val cm = ColorMatrix()
                cm.setSaturation(0.9f)
                val cleanMatrix = ColorMatrix(
                    floatArrayOf(
                        1.05f, 0f, 0f, 0f, 10f,
                        0f, 1.05f, 0f, 0f, 10f,
                        0f, 0f, 1.08f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(cleanMatrix)
                cm
            }
        }

        return applyColorMatrix(bitmap, colorMatrix)
    }

    /**
     * Adjusts saturation of the image.
     * @param saturation 0.0 = grayscale, 1.0 = original, 2.0 = double saturation
     */
    fun adjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        return applyColorMatrix(bitmap, cm)
    }

    /**
     * Adjusts white balance / color temperature.
     * @param temperature Negative = cooler, Positive = warmer (-100 to 100)
     */
    fun adjustTemperature(bitmap: Bitmap, temperature: Float): Bitmap {
        val t = temperature / 100f
        val cm = ColorMatrix(
            floatArrayOf(
                1f + t * 0.2f, 0f, 0f, 0f, t * 10f,
                0f, 1f + t * 0.05f, 0f, 0f, 0f,
                0f, 0f, 1f - t * 0.2f, 0f, -t * 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyColorMatrix(bitmap, cm)
    }

    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun getAvailableFilters(): List<Filter> = Filter.entries.toList()
}
