package com.example.aidkriyachallenge.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.dataModel.Review
import com.example.aidkriyachallenge.repo.ReviewRepo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepo: ReviewRepo
) : ViewModel() {
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _uiEvent = MutableSharedFlow<String>()  // for one-time events like toast
    val uiEvent: SharedFlow<String> = _uiEvent

    fun addReviewForWanderer(
        wandererId: String,
        review: String,
        walkid: String,
        reviewerId: String,
        rating: Int
    ){
        reviewRepo.addReviewForWanderer(wandererId,review,walkid,reviewerId,rating){success->
            if (success) {
                loadWandererReviews(wandererId)
                viewModelScope.launch { _uiEvent.emit("Review submitted successfully!") }
            } else {
                viewModelScope.launch { _uiEvent.emit("Failed to submit review!") }
            }

        }

    }

    fun addReviewForWalker(
        walkerId: String,
        review: String,
        walkid: String,
        reviewerId: String,
        rating: Int
    ){
        reviewRepo.addReviewForWalker(walkerId,review,walkid,reviewerId,rating){success->
            if (success) {
                loadWalkerReviews(walkerId)
                viewModelScope.launch { _uiEvent.emit("Review submitted successfully!") }
            } else {
                viewModelScope.launch { _uiEvent.emit("Failed to submit review!") }
            }

        }

    }

    fun loadWandererReviews(wandererId: String) {
        reviewRepo.getReviewsForWanderer(wandererId) { list ->
            _reviews.value = list
        }
    }

    fun loadWalkerReviews(walkerId: String) {
        reviewRepo.getReviewsForWalker(walkerId) { list ->
            _reviews.value = list
        }
    }


}
