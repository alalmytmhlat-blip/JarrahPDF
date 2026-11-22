package app.jarrah.pdf.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** يكتب العناصر داخل ملف PDF (فوق محتوى الصفحة) مع مراعاة تدوير الصفحة. */
object OverlayWriter {

    private data class R(val x: Float, val y: Float, val w: Float, val h: Float)

    // من إحداثيات الصفحة كما تظهر (بعد التدوير) إلى إحداثيات الصفحة الأصلية
    private fun mapRect(rot: Int, o: Overlay): R = when (rot) {
        90 -> R(o.y, 1f - o.x - o.w, o.h, o.w)
        180 -> R(1f - o.x - o.w, 1f - o.y - o.h, o.w, o.h)
        270 -> R(1f - o.y - o.h, o.x, o.h, o.w)
        else -> R(o.x, o.y, o.w, o.h)
    }

    private fun rotate(b: Bitmap, deg: Int): Bitmap {
        if (deg == 0) return b
        val m = Matrix().apply { postRotate(deg.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    suspend fun apply(input: File, output: File, overlays: List<Overlay>) = withContext(Dispatchers.IO) {
        require(overlays.isNotEmpty()) { "لا توجد تعديلات لتطبيقها" }
        PDDocument.load(input).use { doc ->
            for ((pageIdx, list) in overlays.groupBy { it.page }) {
                val page = doc.getPage(pageIdx)
                val box = page.mediaBox
                val rot = ((page.rotation % 360) + 360) % 360
                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    for (o in list) {
                        val r = mapRect(rot, o)
                        val x = box.lowerLeftX + r.x * box.width
                        val w = r.w * box.width
                        val h = r.h * box.height
                        val y = box.lowerLeftY + (1f - r.y - r.h) * box.height
                        when (o.kind) {
                            OverlayKind.HIGHLIGHT -> {
                                cs.saveGraphicsState()
                                val gs = PDExtendedGraphicsState()
                                gs.setNonStrokingAlphaConstant(0.4f)
                                cs.setGraphicsStateParameters(gs)
                                cs.setNonStrokingColor(Color.red(o.color), Color.green(o.color), Color.blue(o.color))
                                cs.addRect(x, y, w, h)
                                cs.fill()
                                cs.restoreGraphicsState()
                            }
                            OverlayKind.COVER -> {
                                cs.setNonStrokingColor(255, 255, 255)
                                cs.addRect(x, y, w, h)
                                cs.fill()
                            }
                            else -> {
                                val bmp = o.bitmap ?: continue
                                val img = LosslessFactory.createFromImage(doc, rotate(bmp, (360 - rot) % 360))
                                cs.drawImage(img, x, y, w, h)
                            }
                        }
                    }
                }
            }
            doc.save(output)
        }
    }
}
