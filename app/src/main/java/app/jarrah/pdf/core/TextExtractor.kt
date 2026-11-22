package app.jarrah.pdf.core

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** كلمة بإحداثياتها بالنقاط (1/72 إنش)، الأصل أعلى-يسار الصفحة. */
data class Word(val text: String, val x: Float, val y: Float, val w: Float, val h: Float)

data class PageText(val width: Float, val height: Float, val words: List<Word>)

object TextExtractor {
    suspend fun extract(file: File, pageIndex: Int): PageText = withContext(Dispatchers.IO) {
        PDDocument.load(file).use { doc ->
            val page = doc.getPage(pageIndex)
            val stripper = WordStripper()
            stripper.sortByPosition = true
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(doc)
            PageText(page.mediaBox.width, page.mediaBox.height, stripper.words)
        }
    }
}

private class WordStripper : PDFTextStripper() {
    val words = mutableListOf<Word>()

    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (textPositions == null) return
        var sb = StringBuilder()
        var x0 = 0f
        var x1 = 0f
        var y = 0f
        var h = 0f

        fun flush() {
            if (sb.isNotEmpty()) {
                words.add(Word(sb.toString(), x0, y, x1 - x0, h))
                sb = StringBuilder()
            }
        }

        for (tp in textPositions) {
            val ch = tp.unicode ?: continue
            if (ch.isBlank()) {
                flush()
                continue
            }
            if (sb.isEmpty()) {
                x0 = tp.xDirAdj
                y = tp.yDirAdj - tp.heightDir
                h = tp.heightDir
            }
            sb.append(ch)
            x1 = tp.xDirAdj + tp.widthDirAdj
        }
        flush()
    }
}
