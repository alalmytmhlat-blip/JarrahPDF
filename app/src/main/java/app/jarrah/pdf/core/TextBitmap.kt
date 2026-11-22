package app.jarrah.pdf.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

object TextBitmap {
    const val BASE_SIZE = 64f

    /**
     * يرسم النص كصورة شفافة عبر محرك نص أندرويد، فيُشكَّل العربي ويُكتب من اليمين لليسار بشكل صحيح.
     * (خطوط PDF القياسية لا تدعم العربية، لذلك نضمّن الصورة بدل الخط.)
     */
    fun render(text: String, color: Int, bold: Boolean): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = BASE_SIZE
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val width = ceil(Layout.getDesiredWidth(text, paint)).toInt().coerceIn(8, 3000) + 8
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        val bmp = Bitmap.createBitmap(layout.width, layout.height.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        layout.draw(Canvas(bmp))
        return bmp
    }

    /** يحوّل خطوط التوقيع (نقاط) إلى صورة شفافة مقصوصة على حدود الرسم. */
    fun signature(strokes: List<List<Pair<Float, Float>>>, color: Int, strokeWidth: Float = 6f): Bitmap? {
        val pts = strokes.flatten()
        if (pts.size < 2) return null
        val pad = strokeWidth * 2
        val minX = pts.minOf { it.first } - pad
        val minY = pts.minOf { it.second } - pad
        val w = (pts.maxOf { it.first } + pad - minX).toInt().coerceAtLeast(8)
        val h = (pts.maxOf { it.second } + pad - minY).toInt().coerceAtLeast(8)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (s in strokes) {
            if (s.size < 2) continue
            val path = Path()
            path.moveTo(s[0].first - minX, s[0].second - minY)
            for (i in 1 until s.size) path.lineTo(s[i].first - minX, s[i].second - minY)
            canvas.drawPath(path, p)
        }
        return bmp
    }

    fun solid(color: Int): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}

/** فك صورة من المعرض بحجم مناسب لتفادي نفاد الذاكرة. */
suspend fun Context.decodeBitmap(uri: Uri, maxDim: Int = 1600): Bitmap? = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}
