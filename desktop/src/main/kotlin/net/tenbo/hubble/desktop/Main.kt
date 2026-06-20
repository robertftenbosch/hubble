package net.tenbo.hubble.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay

fun main() = application {
    val scope = rememberCoroutineScope()
    val client = remember { HubbleClient(scope) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hubble",
        state = rememberWindowState(width = 360.dp, height = 640.dp),
    ) {
        App(client)
    }

    // The classic MSN corner toast: a separate borderless, always-on-top window that
    // pops bottom-right on an incoming message and fades after a few seconds.
    val toast = client.toast
    if (toast != null) {
        Window(
            onCloseRequest = { client.dismissToast() },
            undecorated = true,
            resizable = false,
            alwaysOnTop = true,
            title = "Hubble",
            state = rememberWindowState(position = WindowPosition.Aligned(Alignment.BottomEnd), size = DpSize(300.dp, 120.dp)),
        ) {
            LaunchedEffect(toast) { delay(6000); client.dismissToast() }
            MsnToast(toast, onOpen = { client.openFromToast() }, onClose = { client.dismissToast() })
        }
    }
}
