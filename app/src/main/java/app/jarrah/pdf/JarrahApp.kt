package app.jarrah.pdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class JarrahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
