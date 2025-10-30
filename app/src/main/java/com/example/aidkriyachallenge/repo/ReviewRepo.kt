package com.example.aidkriyachallenge.repo

import com.example.aidkriyachallenge.common.REVIEW_PATH
import com.example.aidkriyachallenge.common.WALKER_PATH
import com.example.aidkriyachallenge.common.WANDERER_PATH
import com.example.aidkriyachallenge.dataModel.Review
import com.google.firebase.firestore.FirebaseFirestore

class ReviewRepo(
    firestore: FirebaseFirestore
) {

    private val wanderer = firestore.collection(WANDERER_PATH)
    private val walker = firestore.collection(WALKER_PATH)

    fun addReviewForWanderer(
        wandererId: String,
        review: String,
        walkid: String,
        reviewerId: String,
        rating: Int,
        onComplete: (Boolean) -> Unit
    ) {

        val reviewProfile = Review(walkid, reviewerId = reviewerId, review, rating)
        wanderer.document(wandererId).collection(REVIEW_PATH).add(reviewProfile)
            .addOnSuccessListener {
                onComplete(true)
            }.addOnFailureListener {
                onComplete(false)
            }

    }

    fun addReviewForWalker(
        walkerId: String,
        review: String,
        walkid: String,
        reviewerId: String,
        rating: Int,
        onComplete: (Boolean) -> Unit
    ){
        val reviewProfile = Review(walkid,reviewerId,review,rating)
        walker.document(walkerId).collection(REVIEW_PATH).add(reviewProfile)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getReviewsForWanderer(wandererId: String,onResult:(List<Review>) -> Unit){
        walker.document(wandererId).collection(REVIEW_PATH).get()
            .addOnSuccessListener {snapshots ->
                val reviews = snapshots.toObjects(Review::class.java)
                onResult(reviews)
            }
    }

    fun getReviewsForWalker(walkerId: String,onResult:(List<Review>) -> Unit){
        wanderer.document(walkerId).collection(REVIEW_PATH).get()
            .addOnSuccessListener {snapshots ->
                val reviews = snapshots.toObjects(Review::class.java)
                onResult(reviews)
            }
    }

}
