package com.alfredJenny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alfredJenny.app.ui.navigation.AppNavigation
import com.alfredJenny.app.ui.theme.AlfredJennyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlfredJennyTheme {
                AppNavigation()
            }
        }
    }
}
