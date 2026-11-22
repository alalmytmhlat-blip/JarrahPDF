package app.jarrah.pdf.core

import android.graphics.Bitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * عمليات الجداول على صفحة PDF. الإحداثيات بالنقاط والأصل أعلى-يسار.
 * إعادة التركيب: نقتطع صورة لكل شريط (عمود أو صف) ونرسمها بالترتيب الجديد فوق منطقة مبيَّضة.
 * المنطقة المعاد تركيبها تصبح صورة، أما باقي الصفحة فيبقى نصاً قابلاً للتحديد.
 */
object TableOps {
    private const val SCALE = 3f
    private const val ROTATED = "الصفحات المدوّرة غير مدعومة في محرر الجداول. أعد تدويرها أولاً من تنظيم الصفحات."

    private fun crop(full: Bitmap, axis: Axis, s: Band, cross: Band): Bitmap {
        val (x0, x1, y0, y1) = if (axis == Axis.COLUMNS) listOf(s.start, s.end, cross.start, cross.end)
                               else listOf(cross.start, cross.end, s.start, s.end)
        val px0 = (x0 * SCALE).toInt().coerceIn(0, full.width - 1)
        val px1 = (x1 * SCALE).toInt().coerceIn(px0 + 1, full.width)
        val py0 = (y0 * SCALE).toInt().coerceIn(0, full.height - 1)
        val py1 = (y1 * SCALE).toInt().coerceIn(py0 + 1, full.height)
        return Bitmap.createBitmap(full, px0, py0, px1 - px0, py1 - py0)
    }

    /** مستطيل بنظام PDF (الأصل أسفل-يسار) من مجال على المحور ومجال عرضي. */
    private fun rect(box: PDRectangle, axis: Axis, a0: Float, a1: Float, cross: Band): FloatArray =
        if (axis == Axis.COLUMNS)
            floatArrayOf(box.lowerLeftX + a0, box.lowerLeftY + box.height - cross.end, a1 - a0, cross.width)
        else
            floatArrayOf(box.lowerLeftX + cross.start, box.lowerLeftY + box.height - a1, cross.width, a1 - a0)

    suspend fun recompose(
        input: File, output: File, pageIndex: Int,
        axis: Axis, cross: Band, span: Band, pieces: List<Piece>,
    ) = withContext(Dispatchers.IO) {
        PDDocument.load(input).use { doc ->
            val page = doc.getPage(pageIndex)
            require(page.rotation % 360 == 0) { ROTATED }
            val box = page.mediaBox
            val limit = if (axis == Axis.COLUMNS) box.width else box.height
            val full = PdfPageRenderer(input).use { it.render(pageIndex, (box.width * SCALE).toInt()) }

            val total = pieces.mapIndexed { i, p -> p.size + if (i < pieces.lastIndex) p.gapAfter else 0f }.sum()
            val k = if (total > 0f) minOf(1f, (limit - span.start) / total) else 1f
            val clearEnd = maxOf(span.end, span.start + total * k)

            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.setNonStrokingColor(255, 255, 255)
                val c = rect(box, axis, span.start, clearEnd, cross)
                cs.addRect(c[0], c[1], c[2], c[3])
                cs.fill()
                var pos = span.start
                pieces.forEachIndexed { i, p ->
                    val len = p.size * k
                    p.src?.let { s ->
                        val img = LosslessFactory.createFromImage(doc, crop(full, axis, s, cross))
                        val r = rect(box, axis, pos, pos + len, cross)
                        cs.drawImage(img, r[0], r[1], r[2], r[3])
                    }
                    pos += len + if (i < pieces.lastIndex) p.gapAfter * k else 0f
                }
            }
            doc.save(output)
        }
    }

    /** إخفاء أشرطة بتغطيتها ببياض (النص الأصلي يبقى داخل الملف). */
    suspend fun cover(
        input: File, output: File, pageIndex: Int, axis: Axis, cross: Band, bands: List<Band>,
    ) = withContext(Dispatchers.IO) {
        PDDocument.load(input).use { doc ->
            val page = doc.getPage(pageIndex)
            require(page.rotation % 360 == 0) { ROTATED }
            val box = page.mediaBox
            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.setNonStrokingColor(255, 255, 255)
                for (b in bands) {
                    val r = rect(box, axis, b.start - 1f, b.end + 1f, cross)
                    cs.addRect(r[0], r[1], r[2], r[3])
                    cs.fill()
                }
            }
            doc.save(output)
        }
    }

    /** قص منطقة كصفحة مستقلة مع الحفاظ على النص قابلاً للتحديد (بتغيير حدود الصفحة). */
    suspend fun extract(
        input: File, output: File, pageIndex: Int, x0: Float, y0: Float, x1: Float, y1: Float,
    ) = withContext(Dispatchers.IO) {
        PDDocument.load(input).use { src ->
            val page = src.getPage(pageIndex)
            require(page.rotation % 360 == 0) { ROTATED }
            val box = page.mediaBox
            PDDocument().use { dst ->
                val imported = dst.importPage(page)
                val r = PDRectangle(
                    box.lowerLeftX + x0, box.lowerLeftY + box.height - y1,
                    (x1 - x0).coerceAtLeast(1f), (y1 - y0).coerceAtLeast(1f),
                )
                imported.mediaBox = r
                imported.cropBox = r
                dst.save(output)
            }
        }
    }
}
