package app.jarrah.pdf.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.core.Overlay
import app.jarrah.pdf.core.OverlayKind
import app.jarrah.pdf.core.PageText
import app.jarrah.pdf.core.PdfPageRenderer
import app.jarrah.pdf.core.TextBitmap
import app.jarrah.pdf.core.TextExtractor
import app.jarrah.pdf.core.Word
import app.jarrah.pdf.core.decodeBitmap
import app.jarrah.pdf.core.safe
import app.jarrah.pdf.ui.components.SignatureDialog
import app.jarrah.pdf.ui.components.TextEntryDialog
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Mode { MOVE, EDIT_WORD, HIGHLIGHT, COVER }

private val Yellow = 0xFFFFEB3B.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
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
    val overlays = remember(d.file) { mutableStateListOf<Overlay>() }
    var nextId by remember { mutableIntStateOf(1) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(Mode.MOVE) }
    var showText by remember { mutableStateOf(false) }
    var showSign by remember { mutableStateOf(false) }
    var editWord by remember { mutableStateOf<Word?>(null) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    val renderer = remember(d.file) { PdfPageRenderer(d.file) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }

    val aspects by produceState(emptyList<Float>(), renderer) { value = safe { renderer.aspects() } ?: emptyList() }
    val aspect = aspects.getOrNull(pageSafe) ?: 1.414f
    val bmp by produceState<Bitmap?>(null, renderer, pageSafe) { value = renderer.safeRender(pageSafe, 1100) }
    val text by produceState<PageText?>(null, d.file, pageSafe) { value = safe { TextExtractor.extract(d.file, pageSafe) } }

    fun selectPage(p: Int) {
        page = p
        selectedId = null
        dragStart = null
        dragEnd = null
    }

    fun addBitmap(kind: OverlayKind, b: Bitmap, wFrac: Float) {
        val w = wFrac.coerceIn(0.05f, 0.95f)
        val h = (w * b.height / b.width / aspect).coerceIn(0.01f, 0.95f)
        val o = Overlay(nextId++, pageSafe, kind, b, (1f - w) / 2f, (1f - h) / 2f, w, h)
        overlays.add(o)
        selectedId = o.id
        mode = Mode.MOVE
    }

    fun addRect(kind: OverlayKind, x: Float, y: Float, w: Float, h: Float, color: Int) {
        overlays.add(Overlay(nextId++, pageSafe, kind, null, x, y, w, h, color))
    }

    fun overlayAt(p: Offset, size: IntSize): Overlay? = overlays.lastOrNull {
        it.page == pageSafe &&
            p.x / size.width in it.x..(it.x + it.w) &&
            p.y / size.height in it.y..(it.y + it.h)
    }

    fun moveSelected(dx: Float, dy: Float) {
        val id = selectedId ?: return
        val i = overlays.indexOfFirst { it.id == id }
        if (i < 0) return
        val o = overlays[i]
        overlays[i] = o.copy(
            x = (o.x + dx).coerceIn(0f, (1f - o.w).coerceAtLeast(0f)),
            y = (o.y + dy).coerceIn(0f, (1f - o.h).coerceAtLeast(0f)),
        )
    }

    fun finishRect(size: IntSize) {
        val s = dragStart
        val e = dragEnd
        dragStart = null
        dragEnd = null
        if (s == null || e == null) return
        val x = minOf(s.x, e.x) / size.width
        val y = minOf(s.y, e.y) / size.height
        val w = abs(s.x - e.x) / size.width
        val h = abs(s.y - e.y) / size.height
        if (w > 0.01f && h > 0.003f) {
            if (mode == Mode.HIGHLIGHT) addRect(OverlayKind.HIGHLIGHT, x, y, w, h, Yellow)
            else addRect(OverlayKind.COVER, x, y, w, h, 0)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            ctx.decodeBitmap(uri)?.let { addBitmap(OverlayKind.IMAGE, it, 0.4f) } ?: vm.say("تعذّر فتح الصورة")
        }
    }

    val selected = overlays.firstOrNull { it.id == selectedId && it.page == pageSafe }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحرير وتوقيع") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
                actions = {
                    TextButton(
                        enabled = overlays.isNotEmpty(),
                        onClick = { vm.applyOverlays(overlays.toList(), onDone) },
                    ) { Text("تطبيق (${overlays.size})") }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = pageSafe < d.pages - 1, onClick = { selectPage(pageSafe + 1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "التالية")
                }
                Text(
                    "صفحة ${pageSafe + 1} من ${d.pages}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                IconButton(enabled = pageSafe > 0, onClick = { selectPage(pageSafe - 1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "السابقة")
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / aspect.coerceIn(0.2f, 5f))
                    .pointerInput(mode, pageSafe) {
                        detectTapGestures { o ->
                            when (mode) {
                                Mode.MOVE -> selectedId = overlayAt(o, size)?.id
                                Mode.EDIT_WORD -> {
                                    val t = text ?: return@detectTapGestures
                                    val xPt = o.x / size.width * t.width
                                    val yPt = o.y / size.height * t.height
                                    editWord = t.words.firstOrNull {
                                        xPt in (it.x - 2f)..(it.x + it.w + 2f) && yPt in (it.y - 2f)..(it.y + it.h + 2f)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    .pointerInput(mode, pageSafe) {
                        detectDragGestures(
                            onDragStart = { o ->
                                when (mode) {
                                    Mode.MOVE -> selectedId = overlayAt(o, size)?.id ?: selectedId
                                    Mode.HIGHLIGHT, Mode.COVER -> { dragStart = o; dragEnd = o }
                                    else -> {}
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                when (mode) {
                                    Mode.MOVE -> moveSelected(amount.x / size.width, amount.y / size.height)
                                    Mode.HIGHLIGHT, Mode.COVER -> dragEnd = change.position
                                    else -> {}
                                }
                            },
                            onDragEnd = { finishRect(size) },
                            onDragCancel = { dragStart = null; dragEnd = null },
                        )
                    },
            ) {
                bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
                Canvas(Modifier.fillMaxSize()) {
                    overlays.filter { it.page == pageSafe }.forEach { o ->
                        val ox = o.x * size.width
                        val oy = o.y * size.height
                        val ow = o.w * size.width
                        val oh = o.h * size.height
                        when (o.kind) {
                            OverlayKind.HIGHLIGHT -> drawRect(Color(o.color).copy(alpha = 0.4f), Offset(ox, oy), Size(ow, oh))
                            OverlayKind.COVER -> drawRect(Color.White, Offset(ox, oy), Size(ow, oh))
                            else -> o.bitmap?.let {
                                drawImage(
                                    it.asImageBitmap(),
                                    dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                                    dstSize = IntSize(ow.roundToInt().coerceAtLeast(1), oh.roundToInt().coerceAtLeast(1)),
                                )
                            }
                        }
                        if (o.id == selectedId) {
                            drawRect(Color(0xFF0B7A75), Offset(ox, oy), Size(ow, oh), style = Stroke(3f))
                        }
                    }
                    val s = dragStart
                    val e = dragEnd
                    if (s != null && e != null) {
                        val c = if (mode == Mode.HIGHLIGHT) Color(Yellow).copy(alpha = 0.4f) else Color(0x55000000)
                        drawRect(c, Offset(minOf(s.x, e.x), minOf(s.y, e.y)), Size(abs(s.x - e.x), abs(s.y - e.y)))
                    }
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { showText = true }, label = { Text("إضافة نص") })
                AssistChip(onClick = { imagePicker.launch("image/*") }, label = { Text("إضافة صورة") })
                AssistChip(onClick = { showSign = true }, label = { Text("توقيع") })
                FilterChip(selected = mode == Mode.EDIT_WORD, onClick = { mode = Mode.EDIT_WORD }, label = { Text("تعديل كلمة") })
                FilterChip(selected = mode == Mode.HIGHLIGHT, onClick = { mode = Mode.HIGHLIGHT }, label = { Text("تظليل") })
                FilterChip(selected = mode == Mode.COVER, onClick = { mode = Mode.COVER }, label = { Text("تبييض") })
                FilterChip(selected = mode == Mode.MOVE, onClick = { mode = Mode.MOVE }, label = { Text("تحريك") })
            }

            Text(
                when (mode) {
                    Mode.MOVE -> "اضغط عنصراً لتحديده ثم اسحبه لتحريكه."
                    Mode.EDIT_WORD -> "اضغط على كلمة في الصفحة لتعديلها (يُستبدل الخط الأصلي بخط النظام)."
                    Mode.HIGHLIGHT -> "اسحب على الصفحة لرسم تظليل أصفر."
                    Mode.COVER -> "اسحب على الصفحة لتغطية منطقة ببياض (لا يمحو النص الأصلي من الملف)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (selected != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected.bitmap != null && selected.kind != OverlayKind.COVER) {
                        Text("الحجم", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = selected.w,
                            onValueChange = { nw ->
                                val i = overlays.indexOfFirst { it.id == selected.id }
                                val b = selected.bitmap
                                if (i >= 0 && b != null) {
                                    val w = nw.coerceIn(0.05f, 1f)
                                    val h = (w * b.height / b.width / aspect).coerceIn(0.01f, 1f)
                                    overlays[i] = selected.copy(
                                        w = w, h = h,
                                        x = selected.x.coerceIn(0f, (1f - w).coerceAtLeast(0f)),
                                        y = selected.y.coerceIn(0f, (1f - h).coerceAtLeast(0f)),
                                    )
                                }
                            },
                            valueRange = 0.05f..1f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    IconButton(onClick = {
                        overlays.removeAll { it.id == selected.id }
                        selectedId = null
                    }) { Icon(Icons.Filled.Delete, "حذف العنصر", tint = MaterialTheme.colorScheme.secondary) }
                }
            }
        }
    }

    if (showText) {
        TextEntryDialog(
            title = "أضف نصاً",
            onDismiss = { showText = false },
            onConfirm = { t, color, bold ->
                showText = false
                val b = TextBitmap.render(t, color, bold)
                addBitmap(OverlayKind.TEXT, b, b.width * 0.0003125f)
            },
        )
    }

    if (showSign) {
        SignatureDialog(
            onDismiss = { showSign = false },
            onDone = { b ->
                showSign = false
                addBitmap(OverlayKind.SIGNATURE, b, 0.3f)
            },
        )
    }

    editWord?.let { w ->
        TextEntryDialog(
            title = "تعديل الكلمة",
            initial = w.text,
            showStyle = false,
            onDismiss = { editWord = null },
            onConfirm = { newText, _, _ ->
                editWord = null
                val t = text
                if (t != null && t.width > 0f && t.height > 0f) {
                    addRect(
                        OverlayKind.COVER,
                        (w.x - 1f) / t.width, (w.y - 1f) / t.height,
                        (w.w + 2f) / t.width, (w.h + 2f) / t.height, 0,
                    )
                    val b = TextBitmap.render(newText, 0xFF111111.toInt(), false)
                    val hFrac = (w.h * 1.3f / t.height).coerceIn(0.005f, 0.5f)
                    val wFrac = (hFrac * aspect * b.width / b.height).coerceAtMost(0.95f)
                    val x = (w.x / t.width).coerceIn(0f, (1f - wFrac).coerceAtLeast(0f))
                    val y = ((w.y - 0.15f * w.h) / t.height).coerceIn(0f, (1f - hFrac).coerceAtLeast(0f))
                    val o = Overlay(nextId++, pageSafe, OverlayKind.TEXT, b, x, y, wFrac, hFrac)
                    overlays.add(o)
                    selectedId = o.id
                }
            },
        )
    }
}
