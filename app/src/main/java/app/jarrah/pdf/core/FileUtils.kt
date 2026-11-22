package app.jarrah.pdf.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

fun Context.workDir(): File = File(cacheDir, "work").apply { mkdirs() }

fun Context.newWorkFile(prefix: String = "doc"): File =
    File(workDir(), "${prefix}_${System.nanoTime()}.pdf")

suspend fun Context.copyUriToWork(uri: Uri): File = withContext(Dispatchers.IO) {
    val out = newWorkFile("in")
    val input = contentResolver.openInputStream(uri) ?: error("تعذّر فتح الملف")
    input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
    out
}

fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) return c.getString(0) ?: "document.pdf"
    }
    return "document.pdf"
}

/** تنفيذ آمن: يعيد null عند الخطأ ويمرّر الإلغاء. */
suspend fun <T> safe(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
