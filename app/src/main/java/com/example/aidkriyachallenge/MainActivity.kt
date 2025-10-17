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
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.navControl.AppNavHost
import com.example.aidkriyachallenge.repo.Repo
import com.example.aidkriyachallenge.repo.ReviewRepo
import com.example.aidkriyachallenge.ui.theme.AidKRIYAChallengeTheme
import com.example.aidkriyachallenge.viewmodel.ReviewViewModel
import com.example.aidkriyachallenge.viewmodel.MyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        val storage = FirebaseStorage.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repo = Repo(firestore, auth)
        val reviewRepo = ReviewRepo(firestore = firestore)
        val userPref = UserPreferences(this)
        val reviewViewModel = ReviewViewModel(reviewRepo = reviewRepo)
        val viewModel = MyViewModel(
            repo = repo,
            userPref = userPref,
            context = this
        )
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setContent {
            AidKRIYAChallengeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavHost(viewModel, reviewViewModel = reviewViewModel)
                }
            }
        }
    }
}
