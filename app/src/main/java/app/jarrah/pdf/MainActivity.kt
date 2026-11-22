package app.jarrah.pdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import app.jarrah.pdf.ui.AppNav
import app.jarrah.pdf.ui.theme.JarrahTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // فتح ملف PDF من تطبيق آخر ("فتح بواسطة")
        if (savedInstanceState == null) intent?.data?.let { uri -> vm.open(uri) {} }
        setContent { JarrahTheme { AppNav(vm) } }
    }
}
