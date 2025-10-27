package com.example.aidkriyachallenge.dummyUi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aidkriyachallenge.viewModel.ReviewViewModel

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    userId: String,
    isWanderer: Boolean
) {

    val reviews by viewModel.reviews.collectAsState()


    // ✅ Collect UI events for toast

    LaunchedEffect(Unit) {
        if (isWanderer) viewModel.loadWandererReviews(userId)
        else viewModel.loadWalkerReviews(userId)
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Reviews", style = MaterialTheme.typography.titleLarge)
        Text(userId)
        if(reviews.isEmpty()) {
            Text("NO review to show")
        } else{
            reviews.forEach { review ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Rating: ⭐ ${review.rating}")
                        Text("Review: ${review.reviewText}")
                        Text("By: ${review.reviewerId}")
                    }
                }
            }
        }

    }

}

