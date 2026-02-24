package com.editphoto.producteditor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.editphoto.producteditor.R
import com.editphoto.producteditor.databinding.ActivityEditorBinding
import com.editphoto.producteditor.processing.ProductImageProcessor
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

    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var backgroundRemovedBitmap: Bitmap? = null

    private var isShowingOriginal = false
    private var currentTool: Tool? = null

    private enum class Tool {
        REMOVE_BG, STRAIGHTEN, SHADOW, LIGHTING, AUTO_ENHANCE
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

        // Check if auto-enhance was requested
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
            } else {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_processing,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnReset.setOnClickListener { resetToOriginal() }
    }

    private fun setupTools() {
        binding.toolRemoveBg.setOnClickListener { applyTool(Tool.REMOVE_BG) }
        binding.toolStraighten.setOnClickListener { applyTool(Tool.STRAIGHTEN) }
        binding.toolShadow.setOnClickListener { applyTool(Tool.SHADOW) }
        binding.toolLighting.setOnClickListener { applyTool(Tool.LIGHTING) }
        binding.toolAutoEnhance.setOnClickListener { applyTool(Tool.AUTO_ENHANCE) }

        // Slider listener
        binding.adjustmentSlider.addOnChangeListener(
            Slider.OnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    onSliderChanged(value)
                }
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

    private fun applyTool(tool: Tool) {
        currentTool = tool

        when (tool) {
            Tool.REMOVE_BG -> applyBackgroundRemoval()
            Tool.STRAIGHTEN -> showStraightenSlider()
            Tool.SHADOW -> showShadowSlider()
            Tool.LIGHTING -> showLightingSlider()
            Tool.AUTO_ENHANCE -> applyAutoEnhance()
        }
    }

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
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_processing,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                hideProcessing()
            }
        }
    }

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
        // Apply default shadow
        applyShadow(0.4f)
    }

    private fun showLightingSlider() {
        binding.sliderContainer.visibility = View.VISIBLE
        binding.sliderLabel.text = getString(R.string.light_intensity)
        binding.adjustmentSlider.valueFrom = 0f
        binding.adjustmentSlider.valueTo = 100f
        binding.adjustmentSlider.value = 30f
        // Apply default lighting
        applyLighting(0.3f)
    }

    private fun onSliderChanged(value: Float) {
        when (currentTool) {
            Tool.STRAIGHTEN -> {
                val source = backgroundRemovedBitmap ?: currentBitmap ?: return
                val result = processor.straighten(source, value)
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
            }
            Tool.SHADOW -> {
                applyShadow(value / 100f)
            }
            Tool.LIGHTING -> {
                applyLighting(value / 100f)
            }
            else -> {}
        }
    }

    private fun applyShadow(intensity: Float) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.add_shadow))

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processor.addShadow(bitmap, intensity)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_processing,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                hideProcessing()
            }
        }
    }

    private fun applyLighting(intensity: Float) {
        val bitmap = backgroundRemovedBitmap ?: currentBitmap ?: return
        showProcessing(getString(R.string.add_lighting))

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processor.addLighting(bitmap, intensity)
                }
                currentBitmap = result
                binding.imagePreview.setImageBitmap(result)
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_processing,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                hideProcessing()
            }
        }
    }

    private fun applyAutoEnhance() {
        val bitmap = originalBitmap ?: return
        showProcessing(getString(R.string.auto_enhance))

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    processor.autoEnhance(bitmap) { progressText ->
                        withContext(Dispatchers.Main) {
                            binding.processingText.text = progressText
                        }
                    }
                }
                currentBitmap = result
                backgroundRemovedBitmap = result
                binding.imagePreview.setImageBitmap(result)
                binding.checkerboardBg.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_processing,
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                hideProcessing()
            }
        }
    }

    private fun resetToOriginal() {
        currentBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, false)
        backgroundRemovedBitmap = null
        binding.imagePreview.setImageBitmap(currentBitmap)
        binding.sliderContainer.visibility = View.GONE
        binding.checkerboardBg.visibility = View.GONE
        currentTool = null
    }

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
                    Toast.makeText(
                        this@EditorActivity,
                        R.string.image_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@EditorActivity,
                        R.string.error_saving,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                hideProcessing()
            }
        }
    }

    private fun shareImage() {
        val bitmap = currentBitmap ?: return

        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    ImageUtils.saveToCacheDir(
                        this@EditorActivity,
                        bitmap,
                        "share_${System.currentTimeMillis()}"
                    )
                }

                val uri = FileProvider.getUriForFile(
                    this@EditorActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)))
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditorActivity,
                    R.string.error_saving,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showProcessing(text: String) {
        binding.processingOverlay.visibility = View.VISIBLE
        binding.processingText.text = text
    }

    private fun hideProcessing() {
        binding.processingOverlay.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        processor.close()
    }
}
