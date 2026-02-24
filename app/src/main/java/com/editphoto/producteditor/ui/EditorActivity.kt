package com.editphoto.producteditor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.editphoto.producteditor.R
import com.editphoto.producteditor.databinding.ActivityEditorBinding
import com.editphoto.producteditor.processing.*
import com.editphoto.producteditor.utils.ImageUtils
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_AUTO_ENHANCE = "extra_auto_enhance"
    }

    private lateinit var binding: ActivityEditorBinding
    private val processor = ProductImageProcessor()
    private val colorFilters = ColorFilters()
    private val backgroundTemplates = BackgroundTemplates()
    private val reflectionEffect = ReflectionEffect()
    private val platformResizer = PlatformResizer()
    private val watermarkProcessor = WatermarkProcessor()
    private val textOverlay = TextOverlay()
    private val editHistory = EditHistory()

    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var backgroundRemovedBitmap: Bitmap? = null

    private var isShowingOriginal = false
    private var currentTool: Tool? = null

    private enum class Tool {
        REMOVE_BG, STRAIGHTEN, SHADOW, LIGHTING, AUTO_ENHANCE,
        REFLECTION, FILTERS, BACKGROUNDS, RESIZE, WATERMARK,
        TEXT, PRICE_TAG
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadImage()
        setupToolbar()
        setupTools()
        setupBeforeAfterToggle()
        setupBottomActions()

        if (intent.getBooleanExtra(EXTRA_AUTO_ENHANCE, false)) {
            applyAutoEnhance()
        }
    }

    private fun loadImage() {
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: run {
            finish()
            return
        }
        val uri = Uri.parse(uriString)

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageUtils.loadBitmapFromUri(this@EditorActivity, uri)
            }
            if (bitmap != null) {
                originalBitmap = bitmap
                currentBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                binding.imagePreview.setImageBitmap(currentBitmap)
                editHistory.push(currentBitmap!!, "الأصلية")
                updateUndoRedoButtons()
            } else {
                Toast.makeText(this@EditorActivity, R.string.error_processing, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnReset.setOnClickListener { resetToOriginal() }
        binding.btnUndo.setOnClickListener { performUndo() }
        binding.btnRedo.setOnClickListener { performRedo() }
    }

    private fun setupTools() {
        binding.toolRemoveBg.setOnClickListener { applyTool(Tool.REMOVE_BG) }
        binding.toolStraighten.setOnClickListener { applyTool(Tool.STRAIGHTEN) }
        binding.toolShadow.setOnClickListener { applyTool(Tool.SHADOW) }
        binding.toolLighting.setOnClickListener { applyTool(Tool.LIGHTING) }
        binding.toolAutoEnhance.setOnClickListener { applyTool(Tool.AUTO_ENHANCE) }
        binding.toolReflection.setOnClickListener { applyTool(Tool.REFLECTION) }
        binding.toolFilters.setOnClickListener { applyTool(Tool.FILTERS) }
        binding.toolBackgrounds.setOnClickListener { applyTool(Tool.BACKGROUNDS) }
        binding.toolResize.setOnClickListener { applyTool(Tool.RESIZE) }
        binding.toolWatermark.setOnClickListener { applyTool(Tool.WATERMARK) }
        binding.toolText.setOnClickListener { applyTool(Tool.TEXT) }
        binding.toolPriceTag.setOnClickListener { applyTool(Tool.PRICE_TAG) }

        binding.adjustmentSlider.addOnChangeListener(
            Slider.OnChangeListener { _, value, fromUser ->
                if (fromUser) onSliderChanged(value)
            }
        )
    }

    private fun setupBeforeAfterToggle() {
        binding.chipBefore.setOnClickListener {
            isShowingOriginal = true
            binding.imagePreview.setImageBitmap(originalBitmap)
            binding.checkerboardBg.visibility = View.GONE
        }
        binding.chipAfter.setOnClickListener {
            isShowingOriginal = false
            binding.imagePreview.setImageBitmap(currentBitmap)
            binding.checkerboardBg.visibility = View.VISIBLE
        }
    }

    private fun setupBottomActions() {
        binding.btnSave.setOnClickListener { saveImage() }
        binding.btnShare.setOnClickListener { shareImage() }
    }

    // ─── Undo / Redo ─────────────────────────────────────────

    private fun performUndo() {
        val bitmap = editHistory.undo()
        if (bitmap != null) {
            currentBitmap = bitmap
            binding.imagePreview.setImageBitmap(currentBitmap)
        }
        updateUndoRedoButtons()
    }

    private fun performRedo() {
        val bitmap = editHistory.redo()
        if (bitmap != null) {
            currentBitmap = bitmap
            binding.imagePreview.setImageBitmap(currentBitmap)
        }
        updateUndoRedoButtons()
    }

    private fun updateUndoRedoButtons() {
        binding.btnUndo.alpha = if (editHistory.canUndo()) 1.0f else 0.35f
        binding.btnRedo.alpha = if (editHistory.canRedo()) 1.0f else 0.35f
    }

    private fun pushHistory(actionName: String) {
        currentBitmap?.let { editHistory.push(it, actionName) }
        updateUndoRedoButtons()
    }

    // ─── Tool Dispatch ───────────────────────────────────────

    private fun applyTool(tool: Tool) {
        currentTool = tool
        binding.sliderContainer.visibility = View.GONE

        when (tool) {
            Tool.REMOVE_BG -> applyBackgroundRemoval()
            Tool.STRAIGHTEN -> showStraightenSlider()
            Tool.SHADOW -> showShadowSlider()
            Tool.LIGHTING -> showLightingSlider()
            Tool.AUTO_ENHANCE -> applyAutoEnhance()
            Tool.REFLECTION -> showReflectionSlider()
            Tool.FILTERS -> showFiltersDialog()
            Tool.BACKGROUNDS -> showBackgroundsDialog()
            Tool.RESIZE -> showResizeDialog()
            Tool.WATERMARK -> showWatermarkDialog()
            Tool.TEXT -> showTextDialog()
            Tool.PRICE_TAG -> showPriceTagDialog()
        }
    }

    // ─── Background Removal ──────────────────────────────────

    private fun applyBackgroundRemoval() {
        val bitmap = currentBitmap ?: return
        showProcessing(getString(R.string.remove_bg))

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processor.removeBackground(bitmap)
                }
                backgroundRemovedBitmap = result
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                binding.checkerboardBg.visibility = View.VISIBLE
                pushHistory(getString(R.string.remove_bg))
            } catch (e: Exception) {
                showError()
            } finally {
                hideProcessing()
            }
        }
    }

    // ─── Slider-based Tools ──────────────────────────────────

    private fun showStraightenSlider() {
        binding.sliderContainer.visibility = View.VISIBLE
        binding.sliderLabel.text = getString(R.string.rotation_angle)
        binding.adjustmentSlider.valueFrom = -45f
        binding.adjustmentSlider.valueTo = 45f
        binding.adjustmentSlider.value = 0f
    }

    private fun showShadowSlider() {
        binding.sliderContainer.visibility = View.VISIBLE
        binding.sliderLabel.text = getString(R.string.shadow_intensity)
        binding.adjustmentSlider.valueFrom = 0f
        binding.adjustmentSlider.valueTo = 100f
        binding.adjustmentSlider.value = 40f
        applyShadow(0.4f)
    }

    private fun showLightingSlider() {
        binding.sliderContainer.visibility = View.VISIBLE
        binding.sliderLabel.text = getString(R.string.light_intensity)
        binding.adjustmentSlider.valueFrom = 0f
        binding.adjustmentSlider.valueTo = 100f
        binding.adjustmentSlider.value = 30f
        applyLighting(0.3f)
    }

    private fun showReflectionSlider() {
        binding.sliderContainer.visibility = View.VISIBLE
        binding.sliderLabel.text = getString(R.string.reflection_intensity)
        binding.adjustmentSlider.valueFrom = 0f
        binding.adjustmentSlider.valueTo = 100f
        binding.adjustmentSlider.value = 35f
        applyReflection(0.35f)
    }

    private fun onSliderChanged(value: Float) {
        when (currentTool) {
            Tool.STRAIGHTEN -> {
                val source = backgroundRemovedBitmap ?: currentBitmap ?: return
                val result = processor.straighten(source, value)
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.straighten))
            }
            Tool.SHADOW -> applyShadow(value / 100f)
            Tool.LIGHTING -> applyLighting(value / 100f)
            Tool.REFLECTION -> applyReflection(value / 100f)
            else -> {}
        }
    }

    private fun applyShadow(intensity: Float) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.add_shadow))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { processor.addShadow(bitmap, intensity) }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.add_shadow))
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    private fun applyLighting(intensity: Float) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.add_lighting))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { processor.addLighting(bitmap, intensity) }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.add_lighting))
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    private fun applyReflection(opacity: Float) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.reflection))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    reflectionEffect.addReflection(bitmap, opacity = opacity)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.reflection))
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    // ─── Auto-Enhance ────────────────────────────────────────

    private fun applyAutoEnhance() {
        val bitmap = originalBitmap ?: return
        showProcessing(getString(R.string.auto_enhance))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processor.autoEnhance(bitmap) { text ->
                        withContext(Dispatchers.Main) { binding.processingText.text = text }
                    }
                }
                currentBitmap = result
                backgroundRemovedBitmap = result
                binding.imagePreview.setImageBitmap(result)
                binding.checkerboardBg.visibility = View.VISIBLE
                pushHistory(getString(R.string.auto_enhance))
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    // ─── Filters Dialog ──────────────────────────────────────

    private fun showFiltersDialog() {
        val filters = colorFilters.getAvailableFilters()
        val names = filters.map { it.nameAr }.toTypedArray()

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.choose_filter)
            .setItems(names) { _, which ->
                applyColorFilter(filters[which])
            }
            .show()
    }

    private fun applyColorFilter(filter: ColorFilters.Filter) {
        val bitmap = currentBitmap ?: return
        showProcessing(getString(R.string.filters))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { colorFilters.applyFilter(bitmap, filter) }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory("${getString(R.string.filters)}: ${filter.nameAr}")
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    // ─── Background Templates Dialog ─────────────────────────

    private fun showBackgroundsDialog() {
        val templates = backgroundTemplates.getAvailableTemplates()
            .filter { it != BackgroundTemplates.Template.CUSTOM_COLOR }
        val names = templates.map { it.nameAr }.toTypedArray()

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.choose_background)
            .setItems(names) { _, which ->
                applyBackgroundTemplate(templates[which])
            }
            .show()
    }

    private fun applyBackgroundTemplate(template: BackgroundTemplates.Template) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.backgrounds))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    backgroundTemplates.applyTemplate(bitmap, template)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                binding.checkerboardBg.visibility = View.GONE
                pushHistory("${getString(R.string.backgrounds)}: ${template.nameAr}")
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    // ─── Platform Resize Dialog ──────────────────────────────

    private fun showResizeDialog() {
        val platforms = platformResizer.getPlatformList()
        val names = platforms.map { "${it.second.nameAr} (${it.second.width}x${it.second.height})" }.toTypedArray()

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.choose_platform)
            .setItems(names) { _, which ->
                applyPlatformResize(platforms[which].first)
            }
            .show()
    }

    private fun applyPlatformResize(platformKey: String) {
        val bitmap = currentBitmap ?: return
        showProcessing(getString(R.string.resize))
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    platformResizer.resizeForPlatform(bitmap, platformKey)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.resize))
                Toast.makeText(this@EditorActivity, R.string.resize_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { showError() }
            finally { hideProcessing() }
        }
    }

    // ─── Watermark Dialog ────────────────────────────────────

    private fun showWatermarkDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.watermark_hint)
            setPadding(60, 40, 60, 40)
        }

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.enter_watermark)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) applyWatermark(text)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyWatermark(text: String) {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    watermarkProcessor.addTextWatermark(bitmap, text)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.watermark))
            } catch (e: Exception) { showError() }
        }
    }

    // ─── Text Overlay Dialog ─────────────────────────────────

    private fun showTextDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val input = EditText(this).apply { hint = getString(R.string.text_hint) }
        layout.addView(input)

        val styles = TextOverlay.Style.entries.toTypedArray()
        val styleNames = styles.map { it.nameAr }.toTypedArray()
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@EditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                styleNames
            )
        }
        layout.addView(spinner)

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.enter_text)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    applyTextOverlay(text, styles[spinner.selectedItemPosition])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyTextOverlay(text: String, style: TextOverlay.Style) {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    textOverlay.addTextOverlay(bitmap, text, style)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.text_overlay))
            } catch (e: Exception) { showError() }
        }
    }

    // ─── Price Tag Dialog ────────────────────────────────────

    private fun showPriceTagDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val priceInput = EditText(this).apply {
            hint = getString(R.string.price_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(priceInput)

        val oldPriceInput = EditText(this).apply {
            hint = getString(R.string.enter_old_price)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(oldPriceInput)

        val currencies = arrayOf("ر.س", "$", "د.إ", "د.ك", "ر.ع", "ر.ق")
        val currencySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@EditorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                currencies
            )
        }
        layout.addView(currencySpinner)

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.price_tag)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val price = priceInput.text.toString().trim()
                if (price.isNotEmpty()) {
                    val oldPrice = oldPriceInput.text.toString().trim().ifEmpty { null }
                    val currency = currencies[currencySpinner.selectedItemPosition]
                    applyPriceTag(price, currency, oldPrice)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyPriceTag(price: String, currency: String, oldPrice: String?) {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    textOverlay.addPriceTag(bitmap, price, currency, oldPrice)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
                pushHistory(getString(R.string.price_tag))
            } catch (e: Exception) { showError() }
        }
    }

    // ─── Reset ───────────────────────────────────────────────

    private fun resetToOriginal() {
        currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, false)
        backgroundRemovedBitmap = null
        binding.imagePreview.setImageBitmap(currentBitmap)
        binding.sliderContainer.visibility = View.GONE
        binding.checkerboardBg.visibility = View.GONE
        currentTool = null
        currentBitmap?.let { editHistory.push(it, getString(R.string.reset)) }
        updateUndoRedoButtons()
    }

    // ─── Save & Share ────────────────────────────────────────

    private fun saveImage() {
        val bitmap = currentBitmap ?: return
        showProcessing(getString(R.string.save_image))
        lifecycleScope.launch {
            try {
                val fileName = "product_${System.currentTimeMillis()}"
                val uri = withContext(Dispatchers.IO) {
                    ImageUtils.saveToGallery(this@EditorActivity, bitmap, fileName)
                }
                if (uri != null) {
                    Toast.makeText(this@EditorActivity, R.string.image_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EditorActivity, R.string.error_saving, Toast.LENGTH_SHORT).show()
                }
            } finally { hideProcessing() }
        }
    }

    private fun shareImage() {
        val bitmap = currentBitmap ?: return
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    ImageUtils.saveToCacheDir(
                        this@EditorActivity, bitmap,
                        "share_${System.currentTimeMillis()}"
                    )
                }
                val uri = FileProvider.getUriForFile(
                    this@EditorActivity, "${packageName}.fileprovider", file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)))
            } catch (e: Exception) {
                Toast.makeText(this@EditorActivity, R.string.error_saving, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────

    private fun showProcessing(text: String) {
        binding.processingOverlay.visibility = View.VISIBLE
        binding.processingText.text = text
    }

    private fun hideProcessing() {
        binding.processingOverlay.visibility = View.GONE
    }

    private fun showError() {
        Toast.makeText(this, R.string.error_processing, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        processor.close()
        editHistory.clear()
    }
}
