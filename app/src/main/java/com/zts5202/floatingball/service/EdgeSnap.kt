package com.zts5202.floatingball.service

enum class SnapSide {
    LEFT,
    RIGHT,
}

data class ScreenBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

data class SnapResult(
    val x: Int,
    val y: Int,
    val side: SnapSide,
)

/**
 * 松手后把窗口贴到离中心更近的一侧，并保证窗口留在可用区域内。
 * 不依赖 Android 框架，方便用普通单元测试覆盖。
 */
object EdgeSnap {
    fun snap(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bounds: ScreenBounds,
        margin: Int,
    ): SnapResult {
        val safeWidth = width.coerceAtLeast(0)
        val safeHeight = height.coerceAtLeast(0)
        val mid = (bounds.left + bounds.right) / 2
        val center = x + safeWidth / 2
        val side = if (center < mid) SnapSide.LEFT else SnapSide.RIGHT

        val minX = bounds.left
        val maxX = (bounds.right - safeWidth).coerceAtLeast(minX)
        val snappedX = when (side) {
            SnapSide.LEFT -> (bounds.left + margin).coerceIn(minX, maxX)
            SnapSide.RIGHT -> (bounds.right - safeWidth - margin).coerceIn(minX, maxX)
        }
        val minY = bounds.top + margin
        val maxY = (bounds.bottom - safeHeight - margin).coerceAtLeast(minY)

        return SnapResult(
            x = snappedX,
            y = y.coerceIn(minY, maxY),
            side = side,
        )
    }

    fun yForRatio(bounds: ScreenBounds, height: Int, margin: Int, ratio: Float): Int {
        val minY = bounds.top + margin
        val maxY = (bounds.bottom - height - margin).coerceAtLeast(minY)
        val clamped = ratio.coerceIn(0f, 1f)
        return (minY + (maxY - minY) * clamped).toInt()
    }

    fun yRatio(y: Int, height: Int, bounds: ScreenBounds, margin: Int): Float {
        val minY = bounds.top + margin
        val maxY = (bounds.bottom - height - margin).coerceAtLeast(minY)
        if (maxY == minY) return 0f
        return ((y - minY).toFloat() / (maxY - minY)).coerceIn(0f, 1f)
    }
}
