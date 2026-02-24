package com.editphoto.producteditor.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.editphoto.producteditor.R
import com.editphoto.producteditor.databinding.ActivityMainBinding
import com.editphoto.producteditor.utils.PermissionHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var tempPhotoUri: Uri? = null

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            launchGallery()
        } else {
            Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    // Camera capture result
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                openEditor(uri)
            }
        }
    }

    // Gallery picker result
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { openEditor(it) }
    }

    // Auto-enhance: pick from gallery and go directly to auto-enhance mode
    private val autoEnhanceLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { openEditor(it, autoEnhance = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener {
            if (PermissionHelper.hasCameraPermission(this)) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(PermissionHelper.getCameraPermission())
            }
        }

        binding.btnGallery.setOnClickListener {
            if (PermissionHelper.hasStoragePermission(this)) {
                launchGallery()
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermissions())
            }
        }

        binding.btnAutoEnhance.setOnClickListener {
            if (PermissionHelper.hasStoragePermission(this)) {
                autoEnhanceLauncher.launch("image/*")
            } else {
                storagePermissionLauncher.launch(PermissionHelper.getStoragePermissions())
            }
        }

        binding.btnBatch.setOnClickListener {
            startActivity(Intent(this, BatchActivity::class.java))
        }
    }

    private fun launchCamera() {
        val photoFile = File(cacheDir, "images").apply { mkdirs() }
            .let { File(it, "capture_${System.currentTimeMillis()}.jpg") }

        tempPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(tempPhotoUri!!)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openEditor(imageUri: Uri, autoEnhance: Boolean = false) {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_IMAGE_URI, imageUri.toString())
            putExtra(EditorActivity.EXTRA_AUTO_ENHANCE, autoEnhance)
        }
        startActivity(intent)
    }
}
