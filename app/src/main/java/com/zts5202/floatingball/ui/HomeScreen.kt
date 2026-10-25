package com.zts5202.floatingball.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zts5202.floatingball.R
import com.zts5202.floatingball.data.OverlayPrefs
import com.zts5202.floatingball.permission.OverlayPermission
import com.zts5202.floatingball.service.FloatingBallController
import com.zts5202.floatingball.ui.theme.FloatingBallTheme

@Composable
fun HomeRoute() {
    val context = LocalContext.current
    val prefs = remember { OverlayPrefs(context.applicationContext) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission.canDraw(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun refresh() {
        overlayGranted = OverlayPermission.canDraw(context)
        enabled = prefs.enabled
        if (enabled && overlayGranted) {
            requestNotificationOnce(context, prefs, notificationLauncher)
        }
        FloatingBallController.sync(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeContent(
        enabled = enabled,
        overlayGranted = overlayGranted,
        onEnable = {
            prefs.enabled = true
            enabled = true
            if (!OverlayPermission.canDraw(context)) {
                context.startActivity(OverlayPermission.manageIntent(context))
            } else {
                requestNotificationOnce(context, prefs, notificationLauncher)
                FloatingBallController.sync(context)
            }
        },
        onDisable = {
            prefs.enabled = false
            enabled = false
            FloatingBallController.sync(context)
        },
        onRequestPermission = {
            context.startActivity(OverlayPermission.manageIntent(context))
        },
    )
}

private fun requestNotificationOnce(
    context: Context,
    prefs: OverlayPrefs,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    if (Build.VERSION.SDK_INT < 33) return
    if (prefs.askedNotification || OverlayPermission.notificationsGranted(context)) return
    prefs.askedNotification = true
    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
}

@Composable
fun HomeContent(
    enabled: Boolean,
    overlayGranted: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val switchDescription = stringResource(R.string.enable_switch)
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.home_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = switchDescription,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked) onEnable() else onDisable()
                    },
                    modifier = Modifier.semantics { contentDescription = switchDescription },
                )
            }
            Text(
                text = when {
                    !overlayGranted -> stringResource(R.string.status_need_permission)
                    enabled -> stringResource(R.string.status_running)
                    else -> stringResource(R.string.status_off)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!overlayGranted) {
                Button(onClick = onRequestPermission) {
                    Text(text = stringResource(R.string.grant_permission))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "未授权")
@Composable
private fun HomePreviewNeedPermission() {
    FloatingBallTheme {
        HomeContent(
            enabled = false,
            overlayGranted = false,
            onEnable = {},
            onDisable = {},
            onRequestPermission = {},
        )
    }
}

@Preview(showBackground = true, name = "运行中")
@Composable
private fun HomePreviewRunning() {
    FloatingBallTheme {
        HomeContent(
            enabled = true,
            overlayGranted = true,
            onEnable = {},
            onDisable = {},
            onRequestPermission = {},
        )
    }
}
