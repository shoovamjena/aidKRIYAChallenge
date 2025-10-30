package com.example.aidkriyachallenge

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.aidkriyachallenge.viewModel.MainViewModel
import com.example.aidkriyachallenge.viewModel.MainViewModelFactory
import com.example.aidkriyachallenge.viewModel.MyViewModel
import com.example.aidkriyachallenge.viewModel.ReviewViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val mapRoutingViewModel: MainViewModel by viewModels {
        MainViewModelFactory(this.applicationContext)
    }

    // <<< 1. ADD THIS VARIABLE >>>
    // We need this to store the amount before starting payment
    private var currentPaymentAmount: Int = 0

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        // ... all your existing onCreate code ...
        // (No changes needed here)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        Checkout.preload(applicationContext)

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
                    AppNavHost(viewModel, reviewViewModel = reviewViewModel,mapRoutingViewModel=mapRoutingViewModel)
                }
            }
        }
    }

    fun startRazorpayPayment(amountInPaise: Int, options: JSONObject) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_hPbwhz8w8CO6Vm")

        // <<< 2. ADD THIS LINE >>>
        // Store the amount *before* opening the payment sheet
        this.currentPaymentAmount = amountInPaise

        try {
            options.put("currency", "INR")
            options.put("amount", amountInPaise)
            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in starting Razorpay Checkout", e)
            // Report the error immediately
            mapRoutingViewModel.onPaymentError(-1, "Failed to start Razorpay: ${e.message}", null)
            this.currentPaymentAmount = 0 // Reset on error
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        Log.d("MainActivity", "Payment Successful: $razorpayPaymentID")

        // <<< 3. MODIFY THIS LINE >>>
        // Pass the stored amount (currentPaymentAmount) to your ViewModel
        mapRoutingViewModel.onPaymentSuccess(
            razorpayPaymentID,
            paymentData,
            this.currentPaymentAmount // <-- This is the fix
        )

        // Reset the amount after a successful payment
        this.currentPaymentAmount = 0
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        Log.e("MainActivity", "Payment Error: $code - $description")
        mapRoutingViewModel.onPaymentError(code, description, paymentData)

        // Reset the amount on failure as well
        this.currentPaymentAmount = 0
    }
}