package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Adds professional text overlays and price tags to product images.
 * Perfect for creating social media-ready product promotions.
 */
class TextOverlay {

    enum class Style(val nameAr: String) {
        SIMPLE("بسيط"),
        BADGE("شارة"),
        BANNER("بانر"),
        PRICE_TAG("بطاقة سعر"),
        RIBBON("شريط"),
        CIRCLE_BADGE("شارة دائرية")
    }

    /**
     * Adds a styled text overlay to the image.
     */
    fun addTextOverlay(
        bitmap: Bitmap,
        text: String,
        style: Style = Style.SIMPLE,
        position: Float = 0.9f, // Y position as fraction of height
        primaryColor: Int = Color.parseColor("#E53935"),
        textColor: Int = Color.WHITE,
        textSizeFraction: Float = 0.05f
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val textSize = width * textSizeFraction

        when (style) {
            Style.SIMPLE -> drawSimpleText(canvas, text, width, height, position, textColor, textSize)
            Style.BADGE -> drawBadge(canvas, text, width, height, position, primaryColor, textColor, textSize)
            Style.BANNER -> drawBanner(canvas, text, width, height, position, primaryColor, textColor, textSize)
            Style.PRICE_TAG -> drawPriceTag(canvas, text, width, height, position, primaryColor, textColor, textSize)
            Style.RIBBON -> drawRibbon(canvas, text, width, height, primaryColor, textColor, textSize)
            Style.CIRCLE_BADGE -> drawCircleBadge(canvas, text, width, height, primaryColor, textColor, textSize)
        }

        return output
    }

    /**
     * Adds a price tag with currency.
     */
    fun addPriceTag(
        bitmap: Bitmap,
        price: String,
        currency: String = "ر.س",
        originalPrice: String? = null,
        color: Int = Color.parseColor("#E53935")
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val textSize = width * 0.06f

        val tagWidth = width * 0.4f
        val tagHeight = if (originalPrice != null) textSize * 3.5f else textSize * 2.5f
        val tagX = width - tagWidth - width * 0.03f
        val tagY = height * 0.02f

        // Draw tag background
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        val tagRect = RectF(tagX, tagY, tagX + tagWidth, tagY + tagHeight)
        canvas.drawRoundRect(tagRect, textSize * 0.3f, textSize * 0.3f, tagPaint)

        // Draw price
        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val priceText = "$price $currency"
        canvas.drawText(
            priceText,
            tagX + tagWidth / 2,
            tagY + tagHeight / 2 + textSize * 0.15f,
            pricePaint
        )

        // Draw original price with strikethrough if provided
        if (originalPrice != null) {
            val oldPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(180, 255, 255, 255)
                this.textSize = textSize * 0.6f
                flags = flags or Paint.STRIKE_THRU_TEXT_FLAG
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "$originalPrice $currency",
                tagX + tagWidth / 2,
                tagY + tagHeight / 2 - textSize * 0.6f,
                oldPricePaint
            )
        }

        return output
    }

    private fun drawSimpleText(
        canvas: Canvas, text: String,
        width: Float, height: Float, position: Float,
        textColor: Int, textSize: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 2f, 2f, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(text, width / 2, height * position, paint)
    }

    private fun drawBadge(
        canvas: Canvas, text: String,
        width: Float, height: Float, position: Float,
        bgColor: Int, textColor: Int, textSize: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val padH = textSize * 0.8f
        val padV = textSize * 0.4f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val rectF = RectF(
            width / 2 - textBounds.width() / 2 - padH,
            height * position - textBounds.height() - padV,
            width / 2 + textBounds.width() / 2 + padH,
            height * position + padV
        )
        canvas.drawRoundRect(rectF, textSize * 0.4f, textSize * 0.4f, bgPaint)

        paint.color = textColor
        canvas.drawText(text, width / 2, height * position, paint)
    }

    private fun drawBanner(
        canvas: Canvas, text: String,
        width: Float, height: Float, position: Float,
        bgColor: Int, textColor: Int, textSize: Float
    ) {
        val bannerHeight = textSize * 2f
        val y = height * position - bannerHeight / 2

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, y, width, y,
                Color.argb(200, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)),
                bgColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, y, width, y + bannerHeight, bgPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, width / 2, y + bannerHeight / 2 + textSize * 0.3f, paint)
    }

    private fun drawPriceTag(
        canvas: Canvas, text: String,
        width: Float, height: Float, position: Float,
        bgColor: Int, textColor: Int, textSize: Float
    ) {
        val tagWidth = width * 0.5f
        val tagHeight = textSize * 2.5f
        val x = (width - tagWidth) / 2
        val y = height * position - tagHeight

        // Draw tag body
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRoundRect(
            RectF(x, y, x + tagWidth, y + tagHeight),
            textSize * 0.3f, textSize * 0.3f, bgPaint
        )

        // Draw hole
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(x + textSize * 0.6f, y + tagHeight / 2, textSize * 0.2f, holePaint)

        // Draw text
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, x + tagWidth / 2, y + tagHeight / 2 + textSize * 0.3f, paint)
    }

    private fun drawRibbon(
        canvas: Canvas, text: String,
        width: Float, height: Float,
        bgColor: Int, textColor: Int, textSize: Float
    ) {
        // Corner ribbon at top-left
        canvas.save()
        canvas.rotate(-45f, 0f, 0f)

        val ribbonWidth = width * 0.5f
        val ribbonHeight = textSize * 2f
        val offsetY = width * 0.12f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawRect(
            -ribbonWidth / 2, offsetY,
            ribbonWidth / 2, offsetY + ribbonHeight,
            bgPaint
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize * 0.8f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, 0f, offsetY + ribbonHeight / 2 + textSize * 0.25f, paint)

        canvas.restore()
    }

    private fun drawCircleBadge(
        canvas: Canvas, text: String,
        width: Float, height: Float,
        bgColor: Int, textColor: Int, textSize: Float
    ) {
        val radius = textSize * 2f
        val cx = width * 0.85f
        val cy = height * 0.12f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, radius - 2f, borderPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize * 0.7f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        // Split text into lines if needed
        val words = text.split(" ")
        if (words.size > 1) {
            canvas.drawText(words[0], cx, cy - textSize * 0.1f, paint)
            canvas.drawText(words.drop(1).joinToString(" "), cx, cy + textSize * 0.7f, paint)
        } else {
            canvas.drawText(text, cx, cy + textSize * 0.25f, paint)
        }
    }
}
