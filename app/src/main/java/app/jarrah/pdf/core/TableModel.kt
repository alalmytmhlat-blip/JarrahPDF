package app.jarrah.pdf.core

/** المحور نفسه يُستعمل للأعمدة (أفقياً) وللصفوف (رأسياً). */
typealias Band = ColumnBand

enum class Axis { COLUMNS, ROWS }

/** قطعة في التخطيط الجديد: مصدرها شريط من الصفحة، أو فراغ إن كان src = null. */
data class Piece(val src: Band?, val size: Float, val gapAfter: Float)

object RowDetector {
    /** يجمّع الكلمات في أسطر بإسقاطها على المحور الرأسي (النطاق الأوسط من كل كلمة لتفادي التلامس). */
    fun detect(words: List<Word>, minHeight: Float = 3f): List<Band> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedBy { it.y }
        val rows = mutableListOf<Band>()
        var coreEnd = sorted[0].y + sorted[0].h * 0.75f
        var top = sorted[0].y
        var bottom = sorted[0].y + sorted[0].h
        for (w in sorted.drop(1)) {
            val coreStart = w.y + w.h * 0.25f
            if (coreStart <= coreEnd) {
                coreEnd = maxOf(coreEnd, w.y + w.h * 0.75f)
                top = minOf(top, w.y)
                bottom = maxOf(bottom, w.y + w.h)
            } else {
                rows.add(Band(top, bottom))
                coreEnd = w.y + w.h * 0.75f
                top = w.y
                bottom = w.y + w.h
            }
        }
        rows.add(Band(top, bottom))
        // منع التداخل بين صفين متجاورين
        for (i in 0 until rows.size - 1) {
            if (rows[i].end > rows[i + 1].start) {
                val mid = (rows[i].end + rows[i + 1].start) / 2f
                rows[i] = Band(rows[i].start, mid)
                rows[i + 1] = Band(mid, rows[i + 1].end)
            }
        }
        return rows.filter { it.width >= minHeight }
    }
}

/** منطق التخطيط الصافي: كل دالة تعيد قائمة قطع جديدة من الأشرطة الحالية. */
object TableLayout {
    private fun gapAfter(b: List<Band>, i: Int): Float =
        if (i < b.lastIndex) (b[i + 1].start - b[i].end).coerceAtLeast(0f) else 0f

    fun avgGap(b: List<Band>): Float =
        if (b.size < 2) 6f else (0 until b.lastIndex).map { gapAfter(b, it) }.average().toFloat()

    fun swap(b: List<Band>, i: Int, j: Int): List<Piece> {
        val order = b.indices.toMutableList()
        order[i] = j
        order[j] = i
        return order.mapIndexed { slot, idx -> Piece(b[idx], b[idx].width, gapAfter(b, slot)) }
    }

    fun delete(b: List<Band>, remove: Set<Int>): List<Piece> =
        b.indices.filter { it !in remove }.map { Piece(b[it], b[it].width, gapAfter(b, it)) }

    fun insertBlank(b: List<Band>, after: Int, size: Float): List<Piece> {
        val out = mutableListOf<Piece>()
        b.forEachIndexed { i, band ->
            out.add(Piece(band, band.width, gapAfter(b, i)))
            if (i == after) out.add(Piece(null, size, avgGap(b)))
        }
        return out
    }

    /** دمج شريطين متجاورين: إزالة الفراغ بينهما. */
    fun merge(b: List<Band>, first: Int): List<Piece> =
        b.indices.map { Piece(b[it], b[it].width, if (it == first) 0f else gapAfter(b, it)) }
}

object TableExport {
    private fun isArabic(c: Char) = c.code in 0x0600..0x06FF || c.code in 0x0750..0x077F

    /** CSV بترميز UTF-8 مع BOM ليفتحه Excel بالعربية. الجداول العربية تُرتَّب أعمدتها من اليمين. */
    fun csv(words: List<Word>, rows: List<Band>, cols: List<Band>): String {
        val arabic = words.count { w -> w.text.any(::isArabic) } > words.size / 2
        val orderedCols = if (arabic) cols.reversed() else cols
        val sb = StringBuilder("\uFEFF")
        for (r in rows) {
            val line = orderedCols.map { c ->
                val cell = words.filter { w ->
                    val cx = w.x + w.w / 2f
                    val cy = w.y + w.h / 2f
                    cx in c.start..c.end && cy in r.start..r.end
                }
                val sorted = if (arabic) cell.sortedByDescending { it.x } else cell.sortedBy { it.x }
                sorted.joinToString(" ") { it.text }
            }
            sb.append(line.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }).append("\r\n")
        }
        return sb.toString()
    }
}
