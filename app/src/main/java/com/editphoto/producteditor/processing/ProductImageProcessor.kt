package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Main image processing pipeline that orchestrates all product image
 * enhancement operations: background removal, straightening, shadow, and lighting.
 */
class ProductImageProcessor {

    val backgroundRemover = BackgroundRemover()
    val straightener = ImageStraightener()
    val shadowEffect = ShadowEffect()
    val lightingEffect = LightingEffect()

    /**
     * Applies the full auto-enhance pipeline:
     * 1. Remove background
     * 2. Auto-straighten
     * 3. Apply studio lighting
     * 4. Add contact shadow + drop shadow
     */
    suspend fun autoEnhance(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Bitmap {
        onProgress("إزالة الخلفية...")
        var result = backgroundRemover.removeBackground(bitmap)

        onProgress("تعديل الميلان...")
        result = straightener.autoStraighten(result)

        onProgress("إضافة إضاءة احترافية...")
        result = lightingEffect.applyStudioLight(result, 0.25f)
        result = lightingEffect.adjustBrightnessContrast(result, 0.05f, 1.1f)

        onProgress("إضافة ظل...")
        result = shadowEffect.addContactShadow(result, 0.4f)

        return result
    }

    /**
     * Removes background only.
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap {
        return backgroundRemover.removeBackground(bitmap)
    }

    /**
     * Straightens the product (should be called after background removal).
     */
    fun straighten(bitmap: Bitmap, manualAngle: Float? = null): Bitmap {
        return if (manualAngle != null) {
            straightener.rotateBitmap(bitmap, manualAngle)
        } else {
            straightener.autoStraighten(bitmap)
        }
    }

    /**
     * Adds shadow effect.
     */
    fun addShadow(bitmap: Bitmap, intensity: Float = 0.4f): Bitmap {
        return shadowEffect.addDropShadow(bitmap, intensity)
    }

    /**
     * Adds lighting effect.
     */
    fun addLighting(bitmap: Bitmap, intensity: Float = 0.3f): Bitmap {
        var result = lightingEffect.applyStudioLight(bitmap, intensity)
        result = lightingEffect.applyRimLight(result, intensity * 0.5f)
        return result
    }

    fun close() {
        backgroundRemover.close()
    }
}
