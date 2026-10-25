package com.zts5202.floatingball.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zts5202.floatingball.R
import kotlin.math.abs

class PanelController {
    var collapse: (() -> Unit)? = null
}

class OverlayUiState {
    var anchorRight by mutableStateOf(true)
}

@Composable
fun FloatingBallOverlay(
    uiState: OverlayUiState,
    panelController: PanelController,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onOpenHome: () -> Unit,
    onClose: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var animatePanel by remember { mutableStateOf(true) }
    var hintVisible by remember { mutableStateOf(false) }

    fun collapse() {
        expanded = false
        hintVisible = false
    }

    DisposableEffect(panelController) {
        panelController.collapse = { collapse() }
        onDispose { panelController.collapse = null }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (uiState.anchorRight) {
            PanelSlot(
                visible = expanded,
                animate = animatePanel,
                hintVisible = hintVisible,
                onBlankClick = { collapse() },
                onOpenHome = {
                    animatePanel = true
                    collapse()
                    onOpenHome()
                },
                onShortcut = { hintVisible = true },
                onClose = onClose,
            )
        }
        Ball(
            onDragStart = {
                animatePanel = false
                collapse()
                onDragStart()
            },
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onClick = {
                animatePanel = true
                expanded = !expanded
                if (!expanded) hintVisible = false
            },
        )
        if (!uiState.anchorRight) {
            PanelSlot(
                visible = expanded,
                animate = animatePanel,
                hintVisible = hintVisible,
                onBlankClick = { collapse() },
                onOpenHome = {
                    animatePanel = true
                    collapse()
                    onOpenHome()
                },
                onShortcut = { hintVisible = true },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun PanelSlot(
    visible: Boolean,
    animate: Boolean,
    hintVisible: Boolean,
    onBlankClick: () -> Unit,
    onOpenHome: () -> Unit,
    onShortcut: () -> Unit,
    onClose: () -> Unit,
) {
    val enter = if (animate) {
        fadeIn(tween(180)) + expandHorizontally(tween(180))
    } else {
        EnterTransition.None
    }
    val exit = if (animate) {
        fadeOut(tween(140)) + shrinkHorizontally(tween(140))
    } else {
        ExitTransition.None
    }
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
    ) {
        ShortcutPanel(
            hintVisible = hintVisible,
            onBlankClick = onBlankClick,
            onOpenHome = onOpenHome,
            onShortcut = onShortcut,
            onClose = onClose,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun Ball(
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "ballPress",
    )
    val description = stringResource(R.string.ball_description)
    Surface(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(Unit) {
                detectBallGesture(
                    onPressChange = { pressed = it },
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onClick = onClick,
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)),
            )
        }
    }
}

@Composable
private fun ShortcutPanel(
    hintVisible: Boolean,
    onBlankClick: () -> Unit,
    onOpenHome: () -> Unit,
    onShortcut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.panel_description)
    Surface(
        modifier = modifier
            .widthIn(min = 168.dp)
            .semantics { contentDescription = description },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        onClick = onBlankClick,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelButton(
                text = stringResource(R.string.action_home),
                icon = Icons.Filled.Home,
                onClick = onOpenHome,
            )
            PanelButton(
                text = stringResource(R.string.action_shortcut),
                icon = Icons.Filled.Star,
                onClick = onShortcut,
            )
            PanelButton(
                text = stringResource(R.string.action_close),
                icon = Icons.Filled.Close,
                onClick = onClose,
            )
            if (hintVisible) {
                Text(
                    text = stringResource(R.string.shortcut_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PanelButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "buttonPress",
    )
    Button(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectBallGesture(
    onPressChange: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onPressChange(true)
        var dragging = false
        var total = 0f
        val pointerId = down.id
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                if (!change.pressed) break
                val delta = change.positionChange()
                total += abs(delta.x) + abs(delta.y)
                if (!dragging && total > viewConfiguration.touchSlop) {
                    dragging = true
                    onPressChange(false)
                    onDragStart()
                }
                if (dragging) {
                    change.consume()
                    onDrag(delta.x, delta.y)
                }
            }
            onPressChange(false)
            if (dragging) onDragEnd() else onClick()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            onPressChange(false)
            if (dragging) onDragEnd()
            throw cancelled
        }
    }
}
