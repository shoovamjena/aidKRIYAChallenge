package com.example.aidkriyachallenge.dummyui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.aidkriyachallenge.viewModel.ReviewViewModel

@Composable
fun ReviewSubmitScreen(
    walkid: String,
    viewModel: ReviewViewModel,
    isWanderer: Boolean,
    userid: String,
    reviewingId: String
) {
    var review by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = review,
            onValueChange = { review = it },
            placeholder = {
                Text("Write a review")
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = rating,
            onValueChange = { rating = it },
            placeholder = {
                Text("Rate")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect {message->
                Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
            }
        }

        Button(
            onClick = {
                if (!isWanderer) {
                    viewModel.addReviewForWanderer(
                        reviewingId,
                        review,
                        walkid,
                        userid,
                        rating.toInt()
                    )
                } else {
                    viewModel.addReviewForWalker(
                        reviewingId,
                        review,
                        walkid,
                        userid,
                        rating.toInt()
                    )
                }
            }
        ) {
            Text("Submit")
        }

    }
}
