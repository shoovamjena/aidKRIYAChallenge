package com.example.aidkriyachallenge

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.aidkriyachallenge.navControl.AppNavHost
import com.example.aidkriyachallenge.ui.theme.AidKRIYAChallengeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setContent {
            AidKRIYAChallengeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavHost()
                }
            }
        }
    }
}
