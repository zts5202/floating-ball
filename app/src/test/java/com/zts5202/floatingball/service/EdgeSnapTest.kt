package com.zts5202.floatingball.service

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeSnapTest {
    private val bounds = ScreenBounds(left = 0, top = 0, right = 1000, bottom = 2000)

    @Test
    fun snapsLeftWhenCenterIsOnTheLeftHalf() {
        val result = EdgeSnap.snap(
            x = 100,
            y = 400,
            width = 100,
            height = 100,
            bounds = bounds,
            margin = 8,
        )
        assertEquals(SnapSide.LEFT, result.side)
        assertEquals(8, result.x)
        assertEquals(400, result.y)
    }

    @Test
    fun snapsRightWhenCenterIsOnTheRightHalf() {
        val result = EdgeSnap.snap(
            x = 700,
            y = 400,
            width = 100,
            height = 100,
            bounds = bounds,
            margin = 8,
        )
        assertEquals(SnapSide.RIGHT, result.side)
        assertEquals(1000 - 100 - 8, result.x)
        assertEquals(400, result.y)
    }

    @Test
    fun midlineSnapsRight() {
        val result = EdgeSnap.snap(
            x = 450,
            y = 100,
            width = 100,
            height = 80,
            bounds = bounds,
            margin = 0,
        )
        assertEquals(SnapSide.RIGHT, result.side)
        assertEquals(900, result.x)
    }

    @Test
    fun clampsVerticalPositionInsideBounds() {
        val above = EdgeSnap.snap(
            x = 0,
            y = -40,
            width = 100,
            height = 100,
            bounds = bounds,
            margin = 8,
        )
        assertEquals(8, above.y)

        val below = EdgeSnap.snap(
            x = 0,
            y = 5000,
            width = 100,
            height = 100,
            bounds = bounds,
            margin = 8,
        )
        assertEquals(2000 - 100 - 8, below.y)
    }

    @Test
    fun yRatioRoundTripsInsideTheUsableArea() {
        val ratio = 0.5f
        val y = EdgeSnap.yForRatio(bounds, height = 100, margin = 10, ratio = ratio)
        val restored = EdgeSnap.yRatio(y, height = 100, bounds = bounds, margin = 10)
        assertEquals(0.5f, restored, 0.02f)
    }
}
