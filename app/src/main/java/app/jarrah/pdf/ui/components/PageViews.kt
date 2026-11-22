package app.jarrah.pdf.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.jarrah.pdf.core.PdfPageRenderer

/** صفحة واحدة بحجم محجوز مسبقاً حتى لا يقفز التمرير أثناء التحميل. */
@Composable
fun PageCard(
    renderer: PdfPageRenderer,
    index: Int,
    aspect: Float,
    widthPx: Int,
    modifier: Modifier = Modifier,
) {
    val bmp by produceState<Bitmap?>(null, renderer, index, widthPx) {
        value = renderer.safeRender(index, widthPx)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 3.dp,
        color = Color.White,
        modifier = modifier.fillMaxWidth().aspectRatio(1f / aspect.coerceIn(0.2f, 5f)),
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = "صفحة ${index + 1}", modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
        }
    }
}

/** مصغّرة صفحة مع تدوير بصري (للتنظيم). */
@Composable
fun PageThumb(
    renderer: PdfPageRenderer,
    index: Int,
    rotation: Int,
    modifier: Modifier = Modifier,
) {
    val bmp by produceState<Bitmap?>(null, renderer, index) { value = renderer.safeRender(index, 360) }
    Box(modifier, contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(
                b.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().rotate(rotation.toFloat()),
            )
        } else {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
