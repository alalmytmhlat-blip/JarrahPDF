package app.jarrah.pdf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.ui.theme.Ink
import app.jarrah.pdf.ui.theme.Scalpel
import app.jarrah.pdf.ui.theme.Teal

private data class Tool(val route: String?, val title: String, val hint: String, val icon: ImageVector)

private val tools = listOf(
    Tool("organize", "تنظيم الصفحات", "ترتيب وتدوير وحذف واستخراج", Icons.Filled.GridView),
    Tool("merge", "دمج الملفات", "اجمع عدة ملفات في ملف واحد", Icons.Filled.CallMerge),
    Tool("tables", "الأعمدة والجداول", "كشف الأعمدة وتبديلها وإخفاؤها", Icons.Filled.TableChart),
    Tool("edit", "تحرير وتوقيع", "نص وصور وتوقيع وتظليل وتعديل كلمات", Icons.Filled.Edit),
    Tool(null, "الحماية", "قريباً", Icons.Filled.Lock),
)

@Composable
fun HomeScreen(vm: AppViewModel, onPick: () -> Unit, onNavigate: (String) -> Unit) {
    val doc by vm.doc.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(pad),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Hero(
                    docName = doc?.name,
                    pages = doc?.pages,
                    onPick = onPick,
                    onContinue = { onNavigate("viewer") },
                )
            }
            items(tools) { tool ->
                ToolCard(tool) { tool.route?.let(onNavigate) }
            }
        }
    }
}

@Composable
private fun Hero(docName: String?, pages: Int?, onPick: () -> Unit, onContinue: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(Teal, Ink)))
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PictureAsPdf, null, tint = Scalpel, modifier = Modifier.size(30.dp))
                Spacer(Modifier.size(10.dp))
                Text("جرّاح PDF", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "قصّ، واجمع، وبدّل أعمدة وصفحات ملفاتك بدقة.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onPick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("فتح ملف PDF", style = MaterialTheme.typography.labelLarge)
            }
            if (docName != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onContinue),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            docName, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${pages ?: 0} صفحة، اضغط للمتابعة",
                            color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(tool: Tool, onClick: () -> Unit) {
    val enabled = tool.route != null
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Icon(
                    tool.icon, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(tool.title, style = MaterialTheme.typography.titleMedium)
            Text(
                tool.hint, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
