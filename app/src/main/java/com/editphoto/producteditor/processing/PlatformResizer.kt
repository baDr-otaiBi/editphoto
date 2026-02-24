package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.min

/**
 * Resizes product images to match specific e-commerce platform requirements.
 * Each platform has specific size and ratio requirements for product listings.
 */
class PlatformResizer {

    data class PlatformSpec(
        val nameAr: String,
        val nameEn: String,
        val width: Int,
        val height: Int,
        val backgroundColor: Int = Color.WHITE,
        val minProductCoverage: Float = 0.85f // Product should fill 85% of the frame
    )

    val platforms = mapOf(
        "amazon" to PlatformSpec("أمازون", "Amazon", 2000, 2000, Color.WHITE, 0.85f),
        "ebay" to PlatformSpec("إيباي", "eBay", 1600, 1600, Color.WHITE, 0.80f),
        "instagram_square" to PlatformSpec("انستقرام مربع", "Instagram Square", 1080, 1080, Color.WHITE, 0.90f),
        "instagram_portrait" to PlatformSpec("انستقرام عمودي", "Instagram Portrait", 1080, 1350, Color.WHITE, 0.85f),
        "instagram_story" to PlatformSpec("انستقرام ستوري", "Instagram Story", 1080, 1920, Color.WHITE, 0.75f),
        "shopify" to PlatformSpec("شوبيفاي", "Shopify", 2048, 2048, Color.WHITE, 0.85f),
        "etsy" to PlatformSpec("إتسي", "Etsy", 2000, 2000, Color.WHITE, 0.80f),
        "noon" to PlatformSpec("نون", "Noon", 2000, 2000, Color.WHITE, 0.85f),
        "salla" to PlatformSpec("سلة", "Salla", 1024, 1024, Color.WHITE, 0.85f),
        "zid" to PlatformSpec("زد", "Zid", 1024, 1024, Color.WHITE, 0.85f),
        "whatsapp" to PlatformSpec("واتساب", "WhatsApp", 800, 800, Color.WHITE, 0.85f),
        "twitter" to PlatformSpec("تويتر/X", "Twitter/X", 1200, 675, Color.WHITE, 0.80f),
        "facebook" to PlatformSpec("فيسبوك", "Facebook", 1200, 630, Color.WHITE, 0.80f),
        "custom_square" to PlatformSpec("مربع مخصص", "Custom Square", 1500, 1500, Color.WHITE, 0.85f),
    )

    /**
     * Resizes the product image to fit a platform's specifications.
     * Centers the product and fills remaining space with background.
     */
    fun resizeForPlatform(
        bitmap: Bitmap,
        platformKey: String,
        backgroundColor: Int? = null
    ): Bitmap {
        val spec = platforms[platformKey] ?: return bitmap
        return resizeToSpec(bitmap, spec, backgroundColor ?: spec.backgroundColor)
    }

    /**
     * Resizes to custom dimensions.
     */
    fun resizeCustom(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val spec = PlatformSpec("مخصص", "Custom", targetWidth, targetHeight, backgroundColor)
        return resizeToSpec(bitmap, spec, backgroundColor)
    }

    private fun resizeToSpec(bitmap: Bitmap, spec: PlatformSpec, bgColor: Int): Bitmap {
        val output = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(bgColor)

        // Calculate scale to fit product within the coverage area
        val maxProductWidth = (spec.width * spec.minProductCoverage).toInt()
        val maxProductHeight = (spec.height * spec.minProductCoverage).toInt()

        val scale = min(
            maxProductWidth.toFloat() / bitmap.width,
            maxProductHeight.toFloat() / bitmap.height
        )

        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // Center the product
        val x = (spec.width - scaledWidth) / 2f
        val y = (spec.height - scaledHeight) / 2f

        canvas.drawBitmap(scaledBitmap, x, y, Paint(Paint.ANTI_ALIAS_FLAG))
        scaledBitmap.recycle()

        return output
    }

    fun getPlatformList(): List<Pair<String, PlatformSpec>> {
        return platforms.toList()
    }
}
