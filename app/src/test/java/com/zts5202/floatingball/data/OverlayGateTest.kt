package com.zts5202.floatingball.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayGateTest {
    @Test
    fun showsOnlyWhenEnabledAndPermitted() {
        assertTrue(OverlayGate.shouldShow(enabled = true, canDrawOverlays = true))
    }

    @Test
    fun staysHiddenWithoutPermission() {
        assertFalse(OverlayGate.shouldShow(enabled = true, canDrawOverlays = false))
    }

    @Test
    fun staysHiddenWhenSwitchIsOff() {
        assertFalse(OverlayGate.shouldShow(enabled = false, canDrawOverlays = true))
    }
}
