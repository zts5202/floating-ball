package com.zts5202.floatingball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zts5202.floatingball.ui.HomeRoute
import com.zts5202.floatingball.ui.theme.FloatingBallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FloatingBallTheme {
                HomeRoute()
            }
        }
    }
}
