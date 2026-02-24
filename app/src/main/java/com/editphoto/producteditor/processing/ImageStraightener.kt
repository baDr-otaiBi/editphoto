package com.editphoto.producteditor.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Analyzes product images and automatically straightens tilted products.
 * Uses edge detection on the product mask to determine the principal axis
 * and corrects the rotation angle.
 */
class ImageStraightener {

    /**
     * Auto-detects tilt angle from a product image (with transparent background)
     * and straightens it.
     */
    fun autoStraighten(bitmap: Bitmap): Bitmap {
        val angle = detectTiltAngle(bitmap)
        return if (abs(angle) > 0.5f) {
            rotateBitmap(bitmap, -angle)
        } else {
            bitmap
        }
    }

    /**
     * Rotates the bitmap by the specified angle (in degrees).
     * Maintains transparent background and adjusts canvas to fit rotated image.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (abs(degrees) < 0.1f) return bitmap

        val matrix = Matrix()
        matrix.postRotate(degrees)

        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )

        // Crop to non-transparent content
        return cropToContent(rotated)
    }

    /**
     * Detects the tilt angle of the product in the image by analyzing
     * the bounding box orientation of non-transparent pixels.
     * Uses a minimum area bounding rectangle approach.
     */
    private fun detectTiltAngle(bitmap: Bitmap): Float {
        val edgePoints = findEdgePoints(bitmap)
        if (edgePoints.size < 10) return 0f

        // Use PCA-like approach: find the principal axis of the edge points
        val centerX = edgePoints.map { it.first }.average().toFloat()
        val centerY = edgePoints.map { it.second }.average().toFloat()

        var sumXX = 0.0
        var sumXY = 0.0
        var sumYY = 0.0

        for ((x, y) in edgePoints) {
            val dx = x - centerX
            val dy = y - centerY
            sumXX += dx * dx
            sumXY += dx * dy
            sumYY += dy * dy
        }

        // Calculate the angle of the principal axis
        val angle = 0.5 * atan2(2 * sumXY, sumXX - sumYY)
        val degrees = Math.toDegrees(angle).toFloat()

        // Only correct small tilts (< 45 degrees)
        return if (abs(degrees) < 45f) degrees else 0f
    }

    /**
     * Finds edge points of non-transparent content in the bitmap.
     * Samples points along the boundary of the subject.
     */
    private fun findEdgePoints(bitmap: Bitmap): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val width = bitmap.width
        val height = bitmap.height
        val step = max(1, min(width, height) / 100)

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)

                if (alpha > 128) {
                    // Check if this is an edge pixel (has transparent neighbor)
                    val isEdge = isEdgePixel(bitmap, x, y, width, height)
                    if (isEdge) {
                        points.add(Pair(x.toFloat(), y.toFloat()))
                    }
                }
            }
        }

        return points
    }

    private fun isEdgePixel(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Boolean {
        val neighbors = listOf(
            Pair(x - 1, y), Pair(x + 1, y),
            Pair(x, y - 1), Pair(x, y + 1)
        )

        for ((nx, ny) in neighbors) {
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) return true
            if (Color.alpha(bitmap.getPixel(nx, ny)) < 128) return true
        }
        return false
    }

    /**
     * Crops the bitmap to the bounding box of non-transparent content
     * with a small padding.
     */
    private fun cropToContent(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
            }
        }

        if (minX >= maxX || minY >= maxY) return bitmap

        // Add small padding
        val padding = 4
        minX = max(0, minX - padding)
        minY = max(0, minY - padding)
        maxX = min(width - 1, maxX + padding)
        maxY = min(height - 1, maxY + padding)

        return Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }
}
