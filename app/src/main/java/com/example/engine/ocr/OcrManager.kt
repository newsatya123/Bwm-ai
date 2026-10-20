package com.example.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume

class OcrManager(private val context: Context) {

    companion object {
        private const val TAG = "OcrManager"
    }

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(bitmap: Bitmap): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extracted = visionText.text.trim()
                    Log.i(TAG, "OCR extracted ${extracted.length} chars from bitmap")
                    if (continuation.isActive) {
                        continuation.resume(Result.success(extracted))
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "OCR recognition failed: ${error.message}", error)
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating InputImage: ${e.message}")
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun recognizeTextFromUri(uri: Uri): Result<String> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val res = recognizeText(bitmap)
                bitmap.recycle()
                res
            } else {
                Result.failure(IllegalArgumentException("Could not decode image from URI: $uri"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from URI: ${e.message}")
            Result.failure(e)
        }
    }
}
