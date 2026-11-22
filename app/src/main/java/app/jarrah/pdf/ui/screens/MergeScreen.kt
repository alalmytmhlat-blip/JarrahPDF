package app.jarrah.pdf.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.core.displayName

private data class Picked(val uri: Uri, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val files = remember { mutableStateListOf<Picked>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { files.add(Picked(it, ctx.displayName(it))) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دمج الملفات") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, null)
                    Text("إضافة ملفات")
                }
                Button(
                    enabled = files.size >= 2,
                    onClick = { vm.merge(files.map { it.uri }, onDone) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (files.size >= 2) "دمج ${files.size} ملفات" else "دمج") }
            }
        },
    ) { pad ->
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "أضف ملفين أو أكثر، ثم رتّبهم بالأسهم قبل الدمج.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(pad),
            ) {
                itemsIndexed(files, key = { i, it -> "$i-${it.uri}" }) { i, f ->
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp)) {
                                Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(enabled = i > 0, onClick = { val t = files[i]; files[i] = files[i - 1]; files[i - 1] = t }) {
                                Icon(Icons.Filled.ArrowUpward, "أعلى")
                            }
                            IconButton(enabled = i < files.lastIndex, onClick = { val t = files[i]; files[i] = files[i + 1]; files[i + 1] = t }) {
                                Icon(Icons.Filled.ArrowDownward, "أسفل")
                            }
                            IconButton(onClick = { files.removeAt(i) }) { Icon(Icons.Filled.Close, "إزالة") }
                        }
                    }
                }
            }
        }
    }
}
