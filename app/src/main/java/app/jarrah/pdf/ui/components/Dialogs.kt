package app.jarrah.pdf.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.jarrah.pdf.core.TextBitmap

val TextPalette = listOf(0xFF111111.toInt(), 0xFFC62828.toInt(), 0xFF1565C0.toInt(), 0xFF2E7D32.toInt())

@Composable
fun TextEntryDialog(
    title: String,
    initial: String = "",
    showStyle: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (text: String, color: Int, bold: Boolean) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    var color by remember { mutableStateOf(TextPalette[0]) }
    var bold by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
                if (showStyle) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextPalette.forEach { c ->
                            Row(
                                Modifier
                                    .size(32.dp)
                                    .background(Color(c), CircleShape)
                                    .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable { color = c },
                            ) {}
                        }
                        Checkbox(checked = bold, onCheckedChange = { bold = it })
                        Text("عريض")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim(), color, bold) }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun SignatureDialog(onDismiss: () -> Unit, onDone: (Bitmap) -> Unit) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val ink = Color(0xFF0B1B6B)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("وقّع داخل المربع") },
        text = {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { current = listOf(it) },
                            onDrag = { change, _ ->
                                change.consume()
                                current = current + change.position
                            },
                            onDragEnd = {
                                if (current.size > 1) strokes.add(current)
                                current = emptyList()
                            },
                            onDragCancel = { current = emptyList() },
                        )
                    },
            ) {
                (strokes.toList() + listOf(current)).forEach { pts ->
                    if (pts.size > 1) {
                        val path = Path().apply {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                        }
                        drawPath(path, ink, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = strokes.isNotEmpty(), onClick = {
                val bmp = TextBitmap.signature(strokes.map { s -> s.map { it.x to it.y } }, ink.toArgb())
                if (bmp != null) onDone(bmp)
            }) { Text("إضافة") }
        },
        dismissButton = {
            Row {
                TextButton(enabled = strokes.isNotEmpty(), onClick = { strokes.clear() }) { Text("مسح") }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        },
    )
}
