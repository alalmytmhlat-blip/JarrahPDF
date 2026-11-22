package app.jarrah.pdf.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.jarrah.pdf.AppViewModel
import app.jarrah.pdf.ui.screens.EditScreen
import app.jarrah.pdf.ui.screens.HomeScreen
import app.jarrah.pdf.ui.screens.MergeScreen
import app.jarrah.pdf.ui.screens.OrganizeScreen
import app.jarrah.pdf.ui.screens.TableScreen
import app.jarrah.pdf.ui.screens.ViewerScreen

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val doc by vm.doc.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(msg) {
        msg?.let {
            snack.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri) { nav.navigate("viewer") { launchSingleTop = true } }
    }

    val toViewer: () -> Unit = {
        nav.navigate("viewer") {
            popUpTo("home")
            launchSingleTop = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onPick = { picker.launch(arrayOf("application/pdf")) },
                    onNavigate = { route ->
                        if (doc == null && route != "merge") vm.say("افتح ملف PDF أولاً")
                        else nav.navigate(route) { launchSingleTop = true }
                    },
                )
            }
            composable("viewer") { ViewerScreen(vm, onBack = { nav.popBackStack() }) }
            composable("merge") { MergeScreen(vm, onBack = { nav.popBackStack() }, onDone = toViewer) }
            composable("organize") { OrganizeScreen(vm, onBack = { nav.popBackStack() }, onDone = toViewer) }
            composable("edit") { EditScreen(vm, onBack = { nav.popBackStack() }, onDone = toViewer) }
            composable("tables") { TableScreen(vm, onBack = { nav.popBackStack() }) }
        }

        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())

        if (busy) {
            Box(
                Modifier.fillMaxSize().background(Color(0x66000000)).clickable(enabled = true) {},
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}
