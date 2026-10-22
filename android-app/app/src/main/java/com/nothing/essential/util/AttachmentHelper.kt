package com.nothing.essential.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.nothing.essential.data.model.AttachmentItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object AttachmentHelper {

    fun processUri(context: Context, uri: Uri): AttachmentItem? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "attachment"
            var sizeBytes = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            var mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType.isBlank()) {
                mimeType = when {
                    displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    displayName.endsWith(".html", ignoreCase = true) || displayName.endsWith(".htm", ignoreCase = true) -> "text/html"
                    displayName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    displayName.endsWith(".md", ignoreCase = true) -> "text/markdown"
                    displayName.endsWith(".png", ignoreCase = true) -> "image/png"
                    displayName.endsWith(".jpg", ignoreCase = true) || displayName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    displayName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                    else -> "application/octet-stream"
                }
            }

            var imageBytes: ByteArray = byteArrayOf()
            var extractedText = ""

            when {
                mimeType.startsWith("image/") -> {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val rawBytes = stream.readBytes()
                        imageBytes = compressImageIfNeeded(rawBytes)
                        if (sizeBytes == 0L) sizeBytes = rawBytes.size.toLong()
                    }
                }
                mimeType == "application/pdf" || displayName.endsWith(".pdf", ignoreCase = true) -> {
                    val renderedImages = renderPdfPages(context, uri, maxPages = 2)
                    if (renderedImages.isNotEmpty()) {
                        imageBytes = renderedImages.first()
                    }
                    extractedText = "[ATTACHED PDF: $displayName (${renderedImages.size} pages rendered for vision)]"
                }
                mimeType.startsWith("text/") || displayName.endsWith(".html", ignoreCase = true) ||
                        displayName.endsWith(".txt", ignoreCase = true) || displayName.endsWith(".md", ignoreCase = true) -> {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val rawText = stream.bufferedReader().use { it.readText() }
                        extractedText = if (rawText.length > 25000) {
                            rawText.take(25000) + "\n...[TRUNCATED]"
                        } else {
                            rawText
                        }
                    }
                }
                else -> {
                    // Generic file, read first 10KB as text if printable
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val rawBytes = stream.readBytes()
                        extractedText = "[FILE ATTACHED: $displayName (${rawBytes.size} bytes)]"
                    }
                }
            }

            AttachmentItem(
                uriString = uri.toString(),
                name = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                bytes = imageBytes,
                extractedText = extractedText
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImageIfNeeded(bytes: ByteArray, maxDim: Int = 1600): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val width = bitmap.width
            val height = bitmap.height
            val maxSide = maxOf(width, height)
            val scaledBitmap = if (maxSide > maxDim) {
                val scale = maxDim.toFloat() / maxSide
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        } catch (e: Exception) {
            bytes
        }
    }

    private fun renderPdfPages(context: Context, uri: Uri, maxPages: Int = 2): List<ByteArray> {
        val pageBitmaps = mutableListOf<ByteArray>()
        try {
            // Copy to temp file because PdfRenderer needs seekable file descriptor
            val tempFile = File(context.cacheDir, "pdf_preview_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = minOf(renderer.pageCount, maxPages)

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val targetWidth = 1080
                val scale = targetWidth.toFloat() / page.width
                val targetHeight = (page.height * scale).toInt().coerceAtMost(2400)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                pageBitmaps.add(out.toByteArray())
            }

            renderer.close()
            pfd.close()
            tempFile.delete()
        } catch (e: Exception) {
            // pdf render error fallback
        }
        return pageBitmaps
    }
}
