package com.zts5202.floatingball.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zts5202.floatingball.ui.theme.FloatingBallTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HomeContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingPermissionShowsGrantAction() {
        var requested = false
        composeRule.setContent {
            FloatingBallTheme {
                HomeContent(
                    enabled = true,
                    overlayGranted = false,
                    onEnable = {},
                    onDisable = {},
                    onRequestPermission = { requested = true },
                )
            }
        }

        composeRule.onNodeWithText("尚未允许「显示在其他应用上层」。授权后返回本页会自动检测。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("去授权").performClick()
        assertTrue(requested)
        composeRule.onAllNodesWithText("悬浮球运行中").assertCountEquals(0)
    }

    @Test
    fun switchOffCallsDisableAndShowsClosedStatus() {
        var disabled = false
        composeRule.setContent {
            FloatingBallTheme {
                HomeContent(
                    enabled = true,
                    overlayGranted = true,
                    onEnable = {},
                    onDisable = { disabled = true },
                    onRequestPermission = {},
                )
            }
        }

        composeRule.onNodeWithText("悬浮球运行中").assertIsDisplayed()
        composeRule.onNodeWithText("启用悬浮球").assertIsDisplayed()
        composeRule.onAllNodesWithText("去授权").assertCountEquals(0)
        composeRule.onAllNodes(isToggleable()).get(0).performClick()
        assertTrue(disabled)
    }
}
