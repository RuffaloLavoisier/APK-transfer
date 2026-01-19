package com.android.app.apktransfer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.android.app.apktransfer.ui.APKTransferScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4FACFE),
                    secondary = Color(0xFF00F2FE),
                    tertiary = Color(0xFF667EEA),
                    background = Color(0xFF0F0F1E),
                    surface = Color(0xFF1A1A2E),
                    surfaceVariant = Color(0xFF252542),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                APKTransferScreen()
            }
        }
    }
}
