package app.jarrah.pdf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.jarrah.pdf.core.Overlay
import app.jarrah.pdf.core.OverlayWriter
import app.jarrah.pdf.core.PdfEngine
import app.jarrah.pdf.core.copyUriToWork
import app.jarrah.pdf.core.displayName
import app.jarrah.pdf.core.newWorkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class AppViewModel(app: Application) : AndroidViewModel(app) {

    data class DocState(val file: File, val name: String, val pages: Int, val version: Int = 0)

    private val ctx: Application get() = getApplication()

    val doc = MutableStateFlow<DocState?>(null)
    private val history = ArrayDeque<DocState>()
    val canUndo = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    fun say(text: String) { message.value = text }
    fun consumeMessage() { message.value = null }

    private fun work(block: suspend () -> Unit): Job = viewModelScope.launch {
        busy.value = true
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            message.value = "تعذّرت العملية: ${t.localizedMessage ?: t.javaClass.simpleName}"
        } finally {
            busy.value = false
        }
    }

    private suspend fun setResult(file: File, name: String? = null) {
        val cur = doc.value
        if (cur != null) {
            history.addLast(cur)
            if (history.size > 8) history.removeFirst()
        }
        canUndo.value = history.isNotEmpty()
        doc.value = DocState(
            file = file,
            name = name ?: cur?.name ?: "document.pdf",
            pages = PdfEngine.pageCount(file),
            version = (cur?.version ?: 0) + 1,
        )
    }

    fun open(uri: Uri, onDone: () -> Unit) = work {
        val f = ctx.copyUriToWork(uri)
        history.clear()
        canUndo.value = false
        doc.value = DocState(f, ctx.displayName(uri), PdfEngine.pageCount(f))
        onDone()
    }

    fun applyPages(ops: List<PdfEngine.PageOp>, onDone: () -> Unit) = work {
        val cur = doc.value ?: return@work
        val out = ctx.newWorkFile()
        PdfEngine.rebuild(cur.file, ops, out)
        setResult(out)
        onDone()
    }

    fun merge(uris: List<Uri>, onDone: () -> Unit) = work {
        val files = uris.map { ctx.copyUriToWork(it) }
        val out = ctx.newWorkFile()
        PdfEngine.merge(files, out)
        setResult(out, "merged.pdf")
        onDone()
    }

    fun applyOverlays(list: List<Overlay>, onDone: () -> Unit) = work {
        val cur = doc.value ?: return@work
        val out = ctx.newWorkFile()
        OverlayWriter.apply(cur.file, out, list)
        setResult(out)
        message.value = "تم تطبيق التعديلات"
        onDone()
    }

    fun undo() {
        val prev = history.removeLastOrNull() ?: return
        val cur = doc.value
        doc.value = prev.copy(version = (cur?.version ?: 0) + 1)
        canUndo.value = history.isNotEmpty()
        message.value = "تم التراجع"
    }

    /** تحويل عام: يقرأ المستند الحالي ويكتب نتيجة جديدة تصبح المستند الحالي (قابلة للتراجع). */
    fun transform(msg: String?, onDone: (() -> Unit)? = null, block: suspend (input: File, output: File) -> Unit) = work {
        val cur = doc.value ?: return@work
        val out = ctx.newWorkFile()
        block(cur.file, out)
        setResult(out)
        msg?.let { message.value = it }
        onDone?.invoke()
    }

    fun export(uri: Uri) = work {
        val cur = doc.value ?: return@work
        withContext(Dispatchers.IO) {
            val os = ctx.contentResolver.openOutputStream(uri) ?: error("تعذّر الكتابة")
            os.use { o -> cur.file.inputStream().use { it.copyTo(o) } }
        }
        message.value = "تم الحفظ"
    }
}
