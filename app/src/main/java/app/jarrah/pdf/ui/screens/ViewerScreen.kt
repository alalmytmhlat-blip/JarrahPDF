package app.jarrah.pdf.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.core.PdfPageRenderer
import app.jarrah.pdf.core.safe
import app.jarrah.pdf.ui.components.PageCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: AppViewModel, onBack: () -> Unit) {
    val doc by vm.doc.collectAsStateWithLifecycle()
    val d = doc
    if (d == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.export(uri)
    }

    val renderer = remember(d.file) { PdfPageRenderer(d.file) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(d.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
                actions = {
                    IconButton(enabled = canUndo, onClick = { vm.undo() }) { Icon(Icons.AutoMirrored.Filled.Undo, "تراجع") }
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", d.file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(send, "مشاركة الملف"))
                    }) { Icon(Icons.Filled.Share, "مشاركة") }
                    IconButton(onClick = { saver.launch(d.name) }) { Icon(Icons.Filled.Save, "حفظ") }
                },
            )
        },
    ) { pad ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(pad)) {
            val widthPx = with(LocalDensity.current) { (maxWidth - 24.dp).roundToPx() }
            val aspects by produceState(emptyList<Float>(), renderer) {
                value = safe { renderer.aspects() } ?: emptyList()
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(aspects) { i, aspect -> PageCard(renderer, i, aspect, widthPx) }
            }
        }
    }
}
