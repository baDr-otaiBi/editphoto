package com.editphoto.producteditor.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Loads a bitmap from URI, scaling down if needed to avoid OOM.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int = 2048): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // First decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size
            val width = options.outWidth
            val height = options.outHeight
            var sampleSize = 1
            while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val newStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(newStream, null, decodeOptions)
            newStream.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves a bitmap to the device gallery.
     * Returns the saved file URI or null if failed.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, bitmap, fileName)
            } else {
                saveToExternalStorage(context, bitmap, fileName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveWithMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/ProductEditor"
            )
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveToExternalStorage(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "ProductEditor"
        )
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, "$fileName.png")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return Uri.fromFile(file)
    }

    /**
     * Saves bitmap to app cache directory and returns the file URI.
     */
    fun saveToCacheDir(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$fileName.png")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        return file
    }
}
