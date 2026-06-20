package net.tenbo.hubble.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import net.tenbo.hubble.app.proximity.BleEnvironment

/**
 * Requests the BLE runtime permissions once on first composition, then renders
 * [granted] or [denied]. Re-checks against the system each time so returning from
 * Settings reflects the latest grant.
 */
@Composable
fun WithBlePermissions(
    granted: @Composable () -> Unit,
    denied: @Composable (retry: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(BleEnvironment(context).hasAllPermissions())
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        isGranted = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!isGranted) launcher.launch(BleEnvironment.requiredPermissions())
    }

    if (isGranted) granted() else denied { launcher.launch(BleEnvironment.requiredPermissions()) }
}
