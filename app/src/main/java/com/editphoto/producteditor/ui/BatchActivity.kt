package com.editphoto.producteditor.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.editphoto.producteditor.R
import com.editphoto.producteditor.databinding.ActivityBatchBinding
import com.editphoto.producteditor.processing.BatchProcessor
import com.editphoto.producteditor.processing.ColorFilters
import kotlinx.coroutines.launch

class BatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatchBinding
    private lateinit var batchProcessor: BatchProcessor
    private var batchResults: List<BatchProcessor.BatchResult> = emptyList()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            processBatch(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        batchProcessor = BatchProcessor(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSelectImages.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnSaveAll.setOnClickListener {
            saveAllResults()
        }
    }

    private fun processBatch(uris: List<Uri>) {
        val settings = BatchProcessor.BatchSettings(
            removeBackground = binding.cbRemoveBg.isChecked,
            autoStraighten = binding.cbStraighten.isChecked,
            addShadow = binding.cbShadow.isChecked,
            addLighting = binding.cbLighting.isChecked,
            addReflection = binding.cbReflection.isChecked
        )

        binding.progressLayout.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.GONE
        binding.btnSaveAll.visibility = View.GONE
        binding.progressBar.max = uris.size

        lifecycleScope.launch {
            try {
                batchResults = batchProcessor.processBatch(uris, settings) { current, total ->
                    runOnUiThread {
                        binding.tvProgress.text = getString(R.string.batch_processing, current, total)
                        binding.progressBar.progress = current
                    }
                }

                val successCount = batchResults.count { it.processedBitmap != null }
                binding.tvStatus.text = getString(R.string.batch_complete, successCount)
                binding.tvStatus.visibility = View.VISIBLE
                binding.btnSaveAll.visibility = View.VISIBLE

            } catch (e: Exception) {
                Toast.makeText(this@BatchActivity, R.string.error_processing, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAllResults() {
        lifecycleScope.launch {
            val saved = batchProcessor.saveAllToGallery(batchResults)
            Toast.makeText(
                this@BatchActivity,
                getString(R.string.batch_saved, saved),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batchProcessor.close()
    }
}
