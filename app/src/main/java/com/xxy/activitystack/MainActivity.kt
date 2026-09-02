package com.xxy.activitystack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xxy.activitystack.ui.screen.HomeScreen
import com.xxy.activitystack.ui.theme.ActivityStackTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityStackTheme {
                HomeScreen()
            }
        }
    }
}
