package app.jarrah.pdf.core

data class ColumnBand(val start: Float, val end: Float) {
    val width: Float get() = end - start
}

/**
 * كشف الأعمدة بإسقاط الكلمات على المحور الأفقي: الفراغات الرأسية الأعرض من minGap تفصل بين عمودين.
 * يعمل جيداً على الصفحات التي أغلبها جدول. أما الفقرات النصية الحرة فتُدمج في عمود واحد.
 */
object ColumnDetector {
    fun detect(words: List<Word>, minGap: Float = 10f, minWidth: Float = 6f): List<ColumnBand> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedBy { it.x }
        val bands = mutableListOf<ColumnBand>()
        var s = sorted[0].x
        var e = s + sorted[0].w
        for (w in sorted.drop(1)) {
            if (w.x - e > minGap) {
                bands.add(ColumnBand(s, e))
                s = w.x
                e = w.x + w.w
            } else {
                e = maxOf(e, w.x + w.w)
            }
        }
        bands.add(ColumnBand(s, e))
        return bands.filter { it.width >= minWidth }
    }
}
