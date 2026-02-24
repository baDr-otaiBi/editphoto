package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * Provides professional background templates for product images.
 * Replaces transparent background with studio-quality backgrounds.
 */
class BackgroundTemplates {

    enum class Template(val nameAr: String) {
        PURE_WHITE("أبيض نقي"),
        SOFT_GRAY("رمادي ناعم"),
        GRADIENT_LIGHT("تدرج فاتح"),
        GRADIENT_BLUE("تدرج أزرق"),
        GRADIENT_WARM("تدرج دافئ"),
        GRADIENT_SUNSET("تدرج غروب"),
        STUDIO_DARK("استوديو داكن"),
        STUDIO_SPOTLIGHT("استوديو سبوت لايت"),
        PASTEL_PINK("وردي باستيل"),
        PASTEL_MINT("نعناعي باستيل"),
        MARKETPLACE_WHITE("خلفية متجر إلكتروني"),
        CUSTOM_COLOR("لون مخصص")
    }

    /**
     * Applies a background template to a product image with transparent background.
     */
    fun applyTemplate(
        bitmap: Bitmap,
        template: Template,
        customColor: Int = Color.WHITE
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (template) {
            Template.PURE_WHITE -> {
                canvas.drawColor(Color.WHITE)
            }

            Template.SOFT_GRAY -> {
                canvas.drawColor(Color.parseColor("#F0F0F0"))
            }

            Template.GRADIENT_LIGHT -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.WHITE, Color.parseColor("#E8E8E8"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.GRADIENT_BLUE -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#E3F2FD"), Color.parseColor("#90CAF9"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.GRADIENT_WARM -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#FFF3E0"), Color.parseColor("#FFE0B2"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.GRADIENT_SUNSET -> {
                paint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#FF6B6B"),
                        Color.parseColor("#FFE66D"),
                        Color.parseColor("#4ECDC4")
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.STUDIO_DARK -> {
                paint.shader = RadialGradient(
                    width / 2f, height / 2f,
                    (maxOf(width, height) * 0.7f),
                    Color.parseColor("#2C2C2C"),
                    Color.parseColor("#0A0A0A"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.STUDIO_SPOTLIGHT -> {
                // Dark background with a spotlight from above
                canvas.drawColor(Color.parseColor("#1A1A1A"))
                paint.shader = RadialGradient(
                    width / 2f, height * 0.3f,
                    (maxOf(width, height) * 0.5f),
                    Color.parseColor("#60FFFFFF"),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.PASTEL_PINK -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#FCE4EC"), Color.parseColor("#F8BBD0"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.PASTEL_MINT -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#E0F2F1"), Color.parseColor("#B2DFDB"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.MARKETPLACE_WHITE -> {
                // Pure white with very subtle vignette for marketplace listings
                canvas.drawColor(Color.WHITE)
                paint.shader = RadialGradient(
                    width / 2f, height / 2f,
                    (maxOf(width, height) * 0.8f),
                    Color.TRANSPARENT,
                    Color.parseColor("#08000000"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            Template.CUSTOM_COLOR -> {
                canvas.drawColor(customColor)
            }
        }

        // Draw the product on top
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        return output
    }

    /**
     * Gets a list of all available templates.
     */
    fun getAvailableTemplates(): List<Template> = Template.entries.toList()
}
