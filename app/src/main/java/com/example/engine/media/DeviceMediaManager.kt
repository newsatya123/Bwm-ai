package com.example.engine.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

data class DiscoveredMedia(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateAdded: Long,
    val size: Long
)

class DeviceMediaManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceMediaManager"
    }

    /**
     * Finds the latest camera photo captured on the phone.
     */
    fun getLatestPhoto(): DiscoveredMedia? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Photo.jpg"
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val date = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    Log.d(TAG, "Found latest photo: $name ($uri)")
                    return DiscoveredMedia(uri, name, mime, date, size)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying latest photo: ${e.message}", e)
            null
        }
    }

    /**
     * Finds the latest screenshot captured on the phone.
     */
    fun getLatestScreenshot(): DiscoveredMedia? {
        return try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )

            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%Screenshot%", "%Screenshot%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Screenshot.png"
                    val mime = cursor.getString(mimeCol) ?: "image/png"
                    val date = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    Log.d(TAG, "Found latest screenshot: $name ($uri)")
                    return DiscoveredMedia(uri, name, mime, date, size)
                }
            }
            // Fallback: if no screenshot bucket match, return latest photo if display name contains screenshot
            getLatestPhoto()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying latest screenshot: ${e.message}", e)
            getLatestPhoto()
        }
    }

    /**
     * Finds the latest PDF document in the downloads or files collection.
     */
    fun getLatestDownloadPdf(): DiscoveredMedia? {
        return try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.SIZE
            )

            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("application/pdf", "%.pdf")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            val filesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            context.contentResolver.query(
                filesUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Document.pdf"
                    val mime = cursor.getString(mimeCol) ?: "application/pdf"
                    val date = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)

                    val uri = ContentUris.withAppendedId(filesUri, id)
                    Log.d(TAG, "Found latest download PDF: $name ($uri)")
                    return DiscoveredMedia(uri, name, mime, date, size)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying latest PDF: ${e.message}", e)
            null
        }
    }

    /**
     * Shares a media URI to WhatsApp.
     */
    fun shareMediaToWhatsApp(media: DiscoveredMedia, caption: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = media.mimeType
                putExtra(Intent.EXTRA_STREAM, media.uri)
                if (caption.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Target WhatsApp or WhatsApp Business
            val pm = context.packageManager
            if (pm.getLaunchIntentForPackage("com.whatsapp") != null) {
                intent.setPackage("com.whatsapp")
            } else if (pm.getLaunchIntentForPackage("com.whatsapp.w4b") != null) {
                intent.setPackage("com.whatsapp.w4b")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share media to WhatsApp: ${e.message}", e)
            false
        }
    }
}
