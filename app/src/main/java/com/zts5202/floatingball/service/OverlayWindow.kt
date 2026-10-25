package com.zts5202.floatingball.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.zts5202.floatingball.data.OverlayPrefs
import com.zts5202.floatingball.ui.FloatingBallOverlay
import com.zts5202.floatingball.ui.OverlayUiState
import com.zts5202.floatingball.ui.PanelController
import com.zts5202.floatingball.ui.theme.FloatingBallTheme

/**
 * 一个只包住悬浮球（和展开后面板）的悬浮窗。
 * 不铺满屏幕，空白处的触摸仍交给下面的应用；点到窗外时只用来收起面板。
 */
class OverlayWindow(
    private val context: Context,
    private val windowManager: WindowManager,
    private val prefs: OverlayPrefs,
    private val onOpenHome: () -> Unit,
    private val onClose: () -> Unit,
) {
    private val host = OverlayHostView(context)
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private val uiState = OverlayUiState()
    private val panelController = PanelController()
    private var animator: ValueAnimator? = null
    private var dragging = false
    private var placed = false
    private var lastWidth = 0
    private var pendingX = 0f
    private var pendingY = 0f
    private var attached = false

    fun attach() {
        if (attached) return
        uiState.anchorRight = prefs.anchorRight
        host.onOutsideTouch = { panelController.collapse?.invoke() }
        host.onSized = { width, height -> onSized(width, height) }
        host.setContent {
            FloatingBallTheme {
                FloatingBallOverlay(
                    uiState = uiState,
                    panelController = panelController,
                    onDragStart = { dragging = true },
                    onDrag = ::moveBy,
                    onDragEnd = {
                        dragging = false
                        pendingX = 0f
                        pendingY = 0f
                        snapToEdge(animated = true)
                    },
                    onOpenHome = onOpenHome,
                    onClose = onClose,
                )
            }
        }
        // 先进入 RESUMED，再挂到 WindowManager，避免第一帧 Compose 没有生命周期。
        host.resume()
        try {
            windowManager.addView(host, params)
            attached = true
        } catch (error: RuntimeException) {
            host.destroy()
            throw error
        }
    }

    fun detach() {
        if (!attached && !host.isAttachedToWindow) return
        animator?.cancel()
        animator = null
        host.onOutsideTouch = null
        host.onSized = null
        host.destroy()
        if (host.isAttachedToWindow) {
            windowManager.removeView(host)
        }
        attached = false
    }

    fun resnap() {
        if (!attached || dragging) return
        val width = host.width
        val height = host.height
        if (width > 0 && height > 0) {
            snapToEdge(animated = false)
        }
    }

    private fun onSized(width: Int, height: Int) {
        if (!attached) return
        if (!placed) {
            placed = true
            lastWidth = width
            placeImmediate(width, height)
            return
        }
        if (uiState.anchorRight && width != lastWidth) {
            params.x -= width - lastWidth
            updateLayout()
        }
        lastWidth = width
    }

    private fun placeImmediate(width: Int, height: Int) {
        val bounds = screenBounds()
        val margin = marginPx()
        val y = EdgeSnap.yForRatio(bounds, height, margin, prefs.yRatio)
        val x = if (prefs.anchorRight) {
            bounds.right - width - margin
        } else {
            bounds.left + margin
        }
        val result = EdgeSnap.snap(x, y, width, height, bounds, margin)
        params.x = result.x
        params.y = result.y
        uiState.anchorRight = result.side == SnapSide.RIGHT
        updateLayout()
    }

    private fun moveBy(dx: Float, dy: Float) {
        animator?.cancel()
        pendingX += dx
        pendingY += dy
        val stepX = pendingX.toInt()
        val stepY = pendingY.toInt()
        if (stepX == 0 && stepY == 0) return
        pendingX -= stepX
        pendingY -= stepY
        params.x += stepX
        params.y += stepY
        updateLayout()
    }

    private fun snapToEdge(animated: Boolean) {
        val width = host.width
        val height = host.height
        if (width <= 0 || height <= 0) return
        val bounds = screenBounds()
        val result = EdgeSnap.snap(params.x, params.y, width, height, bounds, marginPx())
        animator?.cancel()
        if (!animated) {
            applySnap(result, height, bounds)
            return
        }
        val startX = params.x
        val startY = params.y
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                params.x = (startX + (result.x - startX) * fraction).toInt()
                params.y = (startY + (result.y - startY) * fraction).toInt()
                updateLayout()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!cancelled && !dragging) {
                        applySnap(result, height, bounds)
                    }
                }
            })
            start()
        }
    }

    private fun applySnap(result: SnapResult, height: Int, bounds: ScreenBounds) {
        params.x = result.x
        params.y = result.y
        uiState.anchorRight = result.side == SnapSide.RIGHT
        lastWidth = host.width
        prefs.anchorRight = uiState.anchorRight
        prefs.yRatio = EdgeSnap.yRatio(result.y, height, bounds, marginPx())
        updateLayout()
    }

    private fun updateLayout() {
        if (!host.isAttachedToWindow) return
        try {
            windowManager.updateViewLayout(host, params)
        } catch (_: RuntimeException) {
            // 窗口已经移除，或悬浮窗权限被收回时，忽略这次位移。
        }
    }

    @Suppress("DEPRECATION")
    private fun screenBounds(): ScreenBounds {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val frame = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars(),
            )
            return ScreenBounds(
                left = frame.left + insets.left,
                top = frame.top + insets.top,
                right = frame.right - insets.right,
                bottom = frame.bottom - insets.bottom,
            )
        }
        val display = windowManager.defaultDisplay
        val point = android.graphics.Point()
        @Suppress("DEPRECATION")
        display.getRealSize(point)
        return ScreenBounds(0, 0, point.x, point.y)
    }

    private fun marginPx(): Int = (8f * context.resources.displayMetrics.density).toInt()

}
