package app.jarrah.pdf.core

import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** عمليات المستند الأساسية: عدّ، إعادة بناء الصفحات (ترتيب/حذف/تدوير/استخراج)، دمج. */
object PdfEngine {

    data class PageOp(val sourceIndex: Int, val rotation: Int = 0)

    suspend fun pageCount(file: File): Int = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { it.numberOfPages }
    }

    /** يبني مستنداً جديداً من الصفحات بالترتيب والتدوير المطلوبين. الحذف = عدم إدراج الصفحة. */
    suspend fun rebuild(input: File, ops: List<PageOp>, output: File) = withContext(Dispatchers.IO) {
        require(ops.isNotEmpty()) { "لا توجد صفحات للحفظ" }
        PDDocument.load(input).use { src ->
            PDDocument().use { dst ->
                for (op in ops) {
                    val srcPage = src.getPage(op.sourceIndex)
                    val imported = dst.importPage(srcPage)
                    imported.rotation = ((srcPage.rotation + op.rotation) % 360 + 360) % 360
                }
                dst.save(output)
            }
        }
    }

    suspend fun merge(inputs: List<File>, output: File) = withContext(Dispatchers.IO) {
        require(inputs.size >= 2) { "اختر ملفين على الأقل" }
        val merger = PDFMergerUtility()
        inputs.forEach { merger.addSource(it) }
        merger.destinationFileName = output.absolutePath
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
    }
}
