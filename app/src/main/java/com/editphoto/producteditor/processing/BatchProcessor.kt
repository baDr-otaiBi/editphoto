package com.editphoto.producteditor.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.editphoto.producteditor.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Processes multiple product images in batch with the same settings.
 * Great for e-commerce sellers who need to process entire product catalogs.
 */
class BatchProcessor(private val context: Context) {

    data class BatchSettings(
        val removeBackground: Boolean = true,
        val autoStraighten: Boolean = true,
        val addShadow: Boolean = true,
        val shadowIntensity: Float = 0.4f,
        val addLighting: Boolean = true,
        val lightingIntensity: Float = 0.3f,
        val addReflection: Boolean = false,
        val reflectionStrength: Float = 0.3f,
        val filter: ColorFilters.Filter = ColorFilters.Filter.NONE,
        val backgroundTemplate: BackgroundTemplates.Template? = null,
        val platformResize: String? = null, // Platform key
        val watermarkText: String? = null
    )

    data class BatchResult(
        val sourceUri: Uri,
        val processedBitmap: Bitmap?,
        val error: String?
    )

    private val processor = ProductImageProcessor()
    private val colorFilters = ColorFilters()
    private val backgroundTemplates = BackgroundTemplates()
    private val reflectionEffect = ReflectionEffect()
    private val platformResizer = PlatformResizer()
    private val watermarkProcessor = WatermarkProcessor()

    /**
     * Processes a batch of images with the given settings.
     * @param uris List of image URIs to process
     * @param settings Processing settings to apply to all images
     * @param onProgress Callback with (current, total) progress
     */
    suspend fun processBatch(
        uris: List<Uri>,
        settings: BatchSettings,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<BatchResult> = withContext(Dispatchers.Default) {
        val results = mutableListOf<BatchResult>()
        val total = uris.size

        for ((index, uri) in uris.withIndex()) {
            onProgress(index + 1, total)

            val result = try {
                val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
                    ?: throw Exception("فشل تحميل الصورة")

                val processed = processWithSettings(bitmap, settings)
                BatchResult(uri, processed, null)
            } catch (e: Exception) {
                BatchResult(uri, null, e.message)
            }

            results.add(result)
        }

        results
    }

    /**
     * Saves all processed images to gallery.
     * @return Number of successfully saved images
     */
    suspend fun saveAllToGallery(results: List<BatchResult>): Int =
        withContext(Dispatchers.IO) {
            var saved = 0
            for ((index, result) in results.withIndex()) {
                if (result.processedBitmap != null) {
                    val fileName = "batch_product_${System.currentTimeMillis()}_$index"
                    val uri = ImageUtils.saveToGallery(context, result.processedBitmap, fileName)
                    if (uri != null) saved++
                }
            }
            saved
        }

    private suspend fun processWithSettings(
        bitmap: Bitmap,
        settings: BatchSettings
    ): Bitmap {
        var result = bitmap

        // 1. Remove background
        if (settings.removeBackground) {
            result = processor.removeBackground(result)
        }

        // 2. Auto straighten
        if (settings.autoStraighten) {
            result = processor.straighten(result)
        }

        // 3. Apply color filter
        if (settings.filter != ColorFilters.Filter.NONE) {
            result = colorFilters.applyFilter(result, settings.filter)
        }

        // 4. Add lighting
        if (settings.addLighting) {
            result = processor.addLighting(result, settings.lightingIntensity)
        }

        // 5. Add shadow
        if (settings.addShadow) {
            result = processor.addShadow(result, settings.shadowIntensity)
        }

        // 6. Add reflection
        if (settings.addReflection) {
            result = reflectionEffect.addReflection(result, opacity = settings.reflectionStrength)
        }

        // 7. Apply background template
        if (settings.backgroundTemplate != null) {
            result = backgroundTemplates.applyTemplate(result, settings.backgroundTemplate)
        }

        // 8. Resize for platform
        if (settings.platformResize != null) {
            result = platformResizer.resizeForPlatform(result, settings.platformResize)
        }

        // 9. Add watermark
        if (settings.watermarkText != null) {
            result = watermarkProcessor.addTextWatermark(
                result, settings.watermarkText,
                position = WatermarkProcessor.Position.BOTTOM_RIGHT
            )
        }

        return result
    }

    fun close() {
        processor.close()
    }
}
