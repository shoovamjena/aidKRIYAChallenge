package com.example.aidkriyachallenge.view.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.aidkriyachallenge.MainActivity // Import your MainActivity
import com.example.aidkriyachallenge.viewModel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    distanceInMeters: Int,
    amountInPaise: Int
) {
    val context = LocalContext.current
    // Cast context to your MainActivity to call the payment function
    val activity = context as? MainActivity

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Format values for display
    val distanceInKm = "%.2f".format(distanceInMeters / 1000.0)
    val amountInRupees = "%.2f".format(amountInPaise / 100.0)
    var companionEarningsToShow by remember { mutableStateOf<Int?>(null) }

    // Listen for payment results from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.paymentResult.collect { result ->
            when (result) {
                is MainViewModel.PaymentResult.Success -> {
                    // On success, ViewModel calls completeSessionCleanup.
                    // The LaunchedEffect(sessionId) in MapScreen is no longer
                    // on the back stack. This screen must navigate Home.
                    snackbarHostState.showSnackbar("Payment Successful!")
                    // Trigger the dialog by setting the earnings amount
                    companionEarningsToShow = result.companionEarningsPaise
                }
                is MainViewModel.PaymentResult.Error -> {
                    snackbarHostState.showSnackbar(
                        message = "Payment Failed: ${result.description}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
    if (companionEarningsToShow != null) {
        val earningsInRupees = "%.2f".format(companionEarningsToShow!! / 100.0)

        AlertDialog(
            onDismissRequest = {
                // Don't allow dismissing by clicking outside
            },
            title = { Text("Payment Processed") },
            text = {
                Text("The payment was successful. The companion has been credited with their 80% share of ₹$earningsInRupees.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 1. Hide the dialog
                        companionEarningsToShow = null

                        // 2. NOW navigate home
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Session Complete", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            // Summary Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Distance:", style = MaterialTheme.typography.bodyLarge)
                        Text("$distanceInKm km", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rate:", style = MaterialTheme.typography.bodyLarge)
                        Text("₹10 / km", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Divider(Modifier.padding(vertical = 16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount:", style = MaterialTheme.typography.titleLarge)
                        Text("₹$amountInRupees", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    if (activity == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error: Cannot start payment. Invalid Activity context.")
                        }
                        return@Button
                    }

                    // Prepare Razorpay options
                    val options = JSONObject()
                    try {
                        options.put("name", "AidKriya Challenge") // Your App Name
                        options.put("description", "Payment for walking session")
                        options.put("theme.color", "#3399cc") // Your theme color hex

                        // You should get these from the walker's profile
                        val prefill = JSONObject()
                        prefill.put("email", "walker@example.com")
                        prefill.put("contact", "9876543210")
                        options.put("prefill", prefill)

                        // Call the function in MainActivity
                        activity.startRazorpayPayment(amountInPaise, options)

                    } catch (e: Exception) {
                        Log.e("PaymentScreen", "Failed to create payment JSON", e)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Pay Now (₹$amountInRupees)")
            }
        }
    }
}
