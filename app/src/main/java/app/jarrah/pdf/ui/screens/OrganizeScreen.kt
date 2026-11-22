package app.jarrah.pdf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.core.PdfEngine
import app.jarrah.pdf.core.PdfPageRenderer
import app.jarrah.pdf.ui.components.PageThumb

private data class PageItem(val id: Int, val source: Int, val rotation: Int = 0, val selected: Boolean = false)

private fun SnapshotStateList<PageItem>.swap(a: Int, b: Int) {
    val t = this[a]; this[a] = this[b]; this[b] = t
}

private fun SnapshotStateList<PageItem>.moveSelected(dir: Int) {
    val idxs = indices.filter { this[it].selected }
    if (dir < 0) {
        for (i in idxs) if (i > 0 && !this[i - 1].selected) swap(i, i - 1)
    } else {
        for (i in idxs.reversed()) if (i < lastIndex && !this[i + 1].selected) swap(i, i + 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val doc by vm.doc.collectAsStateWithLifecycle()
    val d = doc
    if (d == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val renderer = remember(d.file) { PdfPageRenderer(d.file) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }
    val items = remember(d.file) {
        mutableStateListOf<PageItem>().apply { addAll(List(d.pages) { PageItem(it, it) }) }
    }
    val selectedCount = items.count { it.selected }

    fun mapSelected(f: (PageItem) -> PageItem) {
        for (i in items.indices) if (items[i].selected) items[i] = f(items[i])
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedCount > 0) "$selectedCount محددة" else "تنظيم الصفحات") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
                actions = {
                    IconButton(onClick = {
                        val all = items.all { it.selected }
                        for (i in items.indices) items[i] = items[i].copy(selected = !all)
                    }) { Icon(Icons.Filled.SelectAll, "تحديد الكل") }
                    TextButton(
                        enabled = items.isNotEmpty(),
                        onClick = { vm.applyPages(items.map { PdfEngine.PageOp(it.source, it.rotation) }, onDone) },
                    ) { Text("تطبيق") }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                val on = selectedCount > 0
                IconButton(enabled = on, onClick = { items.moveSelected(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "نقل للأمام")
                }
                IconButton(enabled = on, onClick = { items.moveSelected(+1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "نقل للخلف")
                }
                IconButton(enabled = on, onClick = { mapSelected { it.copy(rotation = (it.rotation + 270) % 360) } }) {
                    Icon(Icons.Filled.RotateLeft, "تدوير لليسار")
                }
                IconButton(enabled = on, onClick = { mapSelected { it.copy(rotation = (it.rotation + 90) % 360) } }) {
                    Icon(Icons.Filled.RotateRight, "تدوير لليمين")
                }
                IconButton(enabled = on, onClick = { items.removeAll { it.selected } }) {
                    Icon(Icons.Filled.Delete, "حذف", tint = if (on) MaterialTheme.colorScheme.secondary else Color.Unspecified)
                }
                IconButton(enabled = on, onClick = {
                    vm.applyPages(items.filter { it.selected }.map { PdfEngine.PageOp(it.source, it.rotation) }, onDone)
                }) { Icon(Icons.Filled.ContentCut, "استخراج المحدد") }
            }
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                val border = if (item.selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                             else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = border,
                    modifier = Modifier
                        .aspectRatio(0.72f)
                        .clickable { items[idx] = item.copy(selected = !item.selected) },
                ) {
                    Box {
                        PageThumb(renderer, item.source, item.rotation, Modifier.fillMaxSize().padding(4.dp))
                        Text(
                            "${idx + 1}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(Color(0xCC0F1B2D), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        if (item.selected) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
