package app.jarrah.pdf.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/** عرض الصفحات عبر PdfRenderer (الأسرع والأخف). صفحة واحدة مفتوحة في كل مرة، لذلك نستخدم Mutex. */
class PdfPageRenderer(file: File) : Closeable {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    @Volatile private var closed = false

    val pageCount: Int get() = renderer.pageCount

    /** نسبة الارتفاع/العرض لكل صفحة، لحجز المساحة قبل التحميل. */
    suspend fun aspects(): List<Float> = mutex.withLock {
        withContext(Dispatchers.IO) {
            List(renderer.pageCount) { i ->
                renderer.openPage(i).use { it.height.toFloat() / it.width }
            }
        }
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            check(!closed) { "renderer closed" }
            renderer.openPage(index).use { page ->
                val w = widthPx.coerceAtLeast(1)
                val h = (page.height.toFloat() / page.width * w).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }

    suspend fun safeRender(index: Int, widthPx: Int): Bitmap? = safe { render(index, widthPx) }

    override fun close() {
        closed = true
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}
