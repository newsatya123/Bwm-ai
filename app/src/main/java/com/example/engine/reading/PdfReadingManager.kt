package com.example.engine.reading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.remote.GeminiAssistantAgent
import com.example.engine.ocr.OcrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class PdfDocumentInfo(
    val title: String,
    val totalPages: Int,
    val uri: Uri
)

class PdfReadingManager(
    private val context: Context,
    private val ocrManager: OcrManager
) {
    companion object {
        private const val TAG = "PdfReadingManager"
    }

    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    private val _currentDocInfo = MutableStateFlow<PdfDocumentInfo?>(null)
    val currentDocInfo: StateFlow<PdfDocumentInfo?> = _currentDocInfo.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val pageTextCache = mutableMapOf<Int, String>()

    suspend fun openPdf(uri: Uri, fileName: String = "Document.pdf"): Result<PdfDocumentInfo> = withContext(Dispatchers.IO) {
        closeCurrentPdf()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext Result.failure(IllegalArgumentException("Could not open file descriptor for $uri"))

            currentFileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer

            val info = PdfDocumentInfo(
                title = fileName,
                totalPages = renderer.pageCount,
                uri = uri
            )
            _currentDocInfo.value = info
            _currentPage.value = 1
            pageTextCache.clear()
            Log.i(TAG, "Opened PDF '${info.title}' with ${info.totalPages} pages")
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open PDF: ${e.message}", e)
            closeCurrentPdf()
            Result.failure(e)
        }
    }

    suspend fun extractPageText(pageIndexZeroBased: Int): Result<String> = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext Result.failure(IllegalStateException("No PDF is currently open"))
        if (pageIndexZeroBased !in 0 until renderer.pageCount) {
            return@withContext Result.failure(IndexOutOfBoundsException("Page ${pageIndexZeroBased + 1} does not exist (Total: ${renderer.pageCount})"))
        }

        pageTextCache[pageIndexZeroBased]?.let {
            _currentPage.value = pageIndexZeroBased + 1
            return@withContext Result.success(it)
        }

        try {
            val page = renderer.openPage(pageIndexZeroBased)
            // Render at high resolution for accurate OCR
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            val ocrResult = ocrManager.recognizeText(bitmap)
            bitmap.recycle()

            ocrResult.fold(
                onSuccess = { text ->
                    val cleanText = if (text.isBlank()) "Is page par koi readable text nahi mila (page blank ya image-only ho sakta hai)." else text
                    pageTextCache[pageIndexZeroBased] = cleanText
                    _currentPage.value = pageIndexZeroBased + 1
                    Result.success(cleanText)
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page $pageIndexZeroBased: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun readNextPage(): Result<String> {
        val nextZeroBased = _currentPage.value // Since _currentPage is 1-based, its current value is the 0-based index of next page
        val renderer = pdfRenderer ?: return Result.failure(IllegalStateException("No PDF is open"))
        if (nextZeroBased >= renderer.pageCount) {
            return Result.failure(IndexOutOfBoundsException("PDF end ho chuki hai. Total pages: ${renderer.pageCount}"))
        }
        return extractPageText(nextZeroBased)
    }

    suspend fun summarizeDocument(agent: GeminiAssistantAgent): String = withContext(Dispatchers.IO) {
        val doc = _currentDocInfo.value ?: return@withContext "Sir, summary ke liye pehle koi PDF open karein."
        val pagesToRead = doc.totalPages.coerceAtMost(5)
        val combinedText = StringBuilder()

        for (i in 0 until pagesToRead) {
            val res = extractPageText(i)
            res.getOrNull()?.let {
                combinedText.append("--- Page ${i + 1} ---\n").append(it).append("\n\n")
            }
        }

        if (combinedText.isBlank()) {
            return@withContext "Sir, PDF se readable text extract nahi ho saka."
        }

        val prompt = "Neeche '${doc.title}' document ke pehle $pagesToRead pages ka text hai:\n\n$combinedText\n\nIs document ka concise Hindi/Hinglish bullet points me summary do, key points highlight karein."
        val response = agent.processCommand(prompt)
        response.responseText
    }

    fun closeCurrentPdf() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            currentFileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        currentFileDescriptor = null
        _currentDocInfo.value = null
        pageTextCache.clear()
    }
}
