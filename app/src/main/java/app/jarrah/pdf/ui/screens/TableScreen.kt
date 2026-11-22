package app.jarrah.pdf.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.core.Axis
import app.jarrah.pdf.core.Band
import app.jarrah.pdf.core.ColumnDetector
import app.jarrah.pdf.core.PageText
import app.jarrah.pdf.core.PdfPageRenderer
import app.jarrah.pdf.core.RowDetector
import app.jarrah.pdf.core.TableExport
import app.jarrah.pdf.core.TableLayout
import app.jarrah.pdf.core.TableOps
import app.jarrah.pdf.core.TextExtractor
import app.jarrah.pdf.core.safe
import app.jarrah.pdf.core.workDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(vm: AppViewModel, onBack: () -> Unit) {
    val doc by vm.doc.collectAsStateWithLifecycle()
    val d = doc
    if (d == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var page by remember { mutableIntStateOf(0) }
    val pageSafe = page.coerceIn(0, (d.pages - 1).coerceAtLeast(0))
    var minGap by remember { mutableFloatStateOf(10f) }
    var region by remember { mutableStateOf(0f..1f) }
    var columnsMode by remember { mutableStateOf(true) }

    val renderer = remember(d.file) { PdfPageRenderer(d.file) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }

    val text by produceState<PageText?>(null, d.file, pageSafe) {
        value = null
        value = safe { TextExtractor.extract(d.file, pageSafe) }
    }
    val bmp by produceState<Bitmap?>(null, renderer, pageSafe) {
        value = renderer.safeRender(pageSafe, 1100)
    }

    val t = text
    val words = remember(t, region) {
        t?.words?.filter { (it.y + it.h / 2f) / t.height in region } ?: emptyList()
    }
    val cols = remember(words, minGap) { ColumnDetector.detect(words, minGap) }
    val rows = remember(words) { RowDetector.detect(words) }
    val yTop = (words.minOfOrNull { it.y } ?: 0f) - 2f
    val yBottom = (words.maxOfOrNull { it.y + it.h } ?: 0f) + 2f
    val xLeft = (words.minOfOrNull { it.x } ?: 0f) - 2f
    val xRight = (words.maxOfOrNull { it.x + it.w } ?: 0f) + 2f

    val axis = if (columnsMode) Axis.COLUMNS else Axis.ROWS
    val bands: List<Band> = if (columnsMode) cols else rows
    val cross = if (columnsMode) Band(yTop, yBottom) else Band(xLeft, xRight)
    val selected = remember(d.file, pageSafe, minGap, region, columnsMode) { mutableStateListOf<Int>() }
    val sel = selected.toList().sorted()

    fun toggle(idx: Int) {
        if (idx < 0) return
        if (idx in selected) selected.remove(idx) else selected.add(idx)
    }

    fun spanOf(): Band = Band(bands.first().start, bands.last().end)

    fun recompose(msg: String, pieces: List<app.jarrah.pdf.core.Piece>) {
        val b = bands
        val c = cross
        val p = pageSafe
        val a = axis
        vm.transform(msg) { i, o -> TableOps.recompose(i, o, p, a, c, Band(b.first().start, b.last().end), pieces) }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأعمدة والجداول") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = pageSafe < d.pages - 1, onClick = { page = pageSafe + 1 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "التالية")
                }
                Text(
                    "صفحة ${pageSafe + 1} من ${d.pages}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                IconButton(enabled = pageSafe > 0, onClick = { page = pageSafe - 1 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "السابقة")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = columnsMode, onClick = { columnsMode = true }, label = { Text("الأعمدة") })
                FilterChip(selected = !columnsMode, onClick = { columnsMode = false }, label = { Text("الصفوف") })
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (t != null && t.height > 0f) t.width / t.height else 0.707f)
                    .pointerInput(cols, rows, t, columnsMode) {
                        detectTapGestures { o ->
                            val tt = t ?: return@detectTapGestures
                            if (columnsMode) {
                                val xPt = o.x / size.width * tt.width
                                toggle(cols.indexOfFirst { xPt >= it.start && xPt <= it.end })
                            } else {
                                val yPt = o.y / size.height * tt.height
                                toggle(rows.indexOfFirst { yPt >= it.start && yPt <= it.end })
                            }
                        }
                    },
            ) {
                bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                if (t != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sx = size.width / t.width
                        val sy = size.height / t.height
                        // حدود منطقة التحليل
                        val line = Color(0xFF888888)
                        drawLine(line, Offset(0f, region.start * size.height), Offset(size.width, region.start * size.height), 2f)
                        drawLine(line, Offset(0f, region.endInclusive * size.height), Offset(size.width, region.endInclusive * size.height), 2f)
                        bands.forEachIndexed { i, b ->
                            val on = i in selected
                            val c = if (on) secondary else primary
                            val (x, y, w, h) = if (columnsMode)
                                listOf(b.start * sx, yTop * sy, b.width * sx, (yBottom - yTop) * sy)
                            else
                                listOf(xLeft * sx, b.start * sy, (xRight - xLeft) * sx, b.width * sy)
                            drawRect(c.copy(alpha = if (on) 0.30f else 0.12f), Offset(x, y), Size(w, h))
                            drawRect(c, Offset(x, y), Size(w, h), style = Stroke(if (on) 4f else 1.5f))
                        }
                    }
                }
            }

            Text(
                when {
                    t == null -> "جارٍ تحليل الصفحة..."
                    bands.isEmpty() -> "لا يوجد نص قابل للتحليل في هذه المنطقة. قد تكون صفحة ممسوحة ضوئياً."
                    columnsMode -> "${cols.size} عمود. اضغط على الأعمدة لتحديدها."
                    else -> "${rows.size} صف. اضغط على الصفوف لتحديدها."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("منطقة الجدول في الصفحة (من الأعلى إلى الأسفل)", style = MaterialTheme.typography.labelLarge)
            RangeSlider(value = region, onValueChange = { region = it }, valueRange = 0f..1f)
            if (columnsMode) {
                Text("حساسية الفصل بين الأعمدة", style = MaterialTheme.typography.labelLarge)
                Slider(value = minGap, onValueChange = { minGap = it }, valueRange = 4f..40f)
            }

            val one = sel.size == 1
            val two = sel.size == 2
            val has = bands.isNotEmpty()
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    enabled = two,
                    onClick = { recompose("تم التبديل", TableLayout.swap(bands, sel[0], sel[1])) },
                    label = { Text("تبديل") },
                )
                AssistChip(
                    enabled = one && sel[0] > 0,
                    onClick = { recompose("تم النقل", TableLayout.swap(bands, sel[0], sel[0] - 1)) },
                    label = { Text(if (columnsMode) "نقل لليسار" else "نقل للأعلى") },
                )
                AssistChip(
                    enabled = one && sel[0] < bands.lastIndex,
                    onClick = { recompose("تم النقل", TableLayout.swap(bands, sel[0], sel[0] + 1)) },
                    label = { Text(if (columnsMode) "نقل لليمين" else "نقل للأسفل") },
                )
                AssistChip(
                    enabled = has && sel.isNotEmpty() && sel.size < bands.size,
                    onClick = { recompose("تم الحذف", TableLayout.delete(bands, sel.toSet())) },
                    label = { Text("حذف وسدّ الفراغ") },
                )
                AssistChip(
                    enabled = has && sel.isNotEmpty(),
                    onClick = {
                        val picked = sel.map { bands[it] }
                        val c = cross
                        val p = pageSafe
                        val a = axis
                        vm.transform("تم الإخفاء") { i, o -> TableOps.cover(i, o, p, a, c, picked) }
                    },
                    label = { Text("إخفاء") },
                )
                AssistChip(
                    enabled = one,
                    onClick = {
                        val b = bands[sel[0]]
                        recompose("تم الإدراج", TableLayout.insertBlank(bands, sel[0], b.width))
                    },
                    label = { Text(if (columnsMode) "إدراج عمود فارغ بعده" else "إدراج صف فارغ بعده") },
                )
                AssistChip(
                    enabled = two && abs(sel[0] - sel[1]) == 1,
                    onClick = { recompose("تم الدمج", TableLayout.merge(bands, sel.minOrNull() ?: 0)) },
                    label = { Text("دمج المتجاورين") },
                )
                AssistChip(
                    enabled = has && sel.isNotEmpty(),
                    onClick = {
                        val s = sel.first().let { bands[it].start }
                        val e = sel.last().let { bands[it].end }
                        val p = pageSafe
                        val (x0, x1, y0, y1) = if (columnsMode) listOf(s, e, cross.start, cross.end)
                                               else listOf(cross.start, cross.end, s, e)
                        vm.transform("تم القص. المنطقة الآن هي الملف الحالي، احفظه من العارض") { i, o ->
                            TableOps.extract(i, o, p, x0, y0, x1, y1)
                        }
                    },
                    label = { Text("قص كملف") },
                )
                AssistChip(
                    enabled = rows.isNotEmpty() && cols.isNotEmpty(),
                    onClick = {
                        val csv = TableExport.csv(words, rows, cols)
                        val name = "table_p${pageSafe + 1}.csv"
                        scope.launch {
                            val f: File = withContext(Dispatchers.IO) {
                                File(ctx.workDir(), name).also { it.writeText(csv, Charsets.UTF_8) }
                            }
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(send, "تصدير الجدول"))
                        }
                    },
                    label = { Text("تصدير CSV") },
                )
            }

            Text(
                "ملاحظة: الجزء المعاد ترتيبه يصبح صورة داخل الصفحة. الإخفاء يغطي بالبياض ولا يمحو النص من الملف. يمكنك التراجع من العارض.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
