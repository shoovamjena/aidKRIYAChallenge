package com.example.aidkriyachallenge.repo

import android.util.Log
import com.example.aidkriyachallenge.common.ResultState
import com.example.aidkriyachallenge.common.USER_PATH
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class Repo @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {

    private val users = firestore.collection(USER_PATH)


    suspend fun SignUp(email: String,password: String): ResultState<UserProfile> = try {
        val result = auth.createUserWithEmailAndPassword(email,password).await()
        val uid = result.user?.uid ?: return ResultState.Error("No UID returned")

        val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis())
        users.document(uid).set(profile).await()


        ResultState.Success(profile)


    }
    catch (e: Exception) {
        ResultState.Error(e.message ?: "Sign up failed")
    }


    suspend fun SignIn(email: String, password: String): ResultState<UserProfile> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: return ResultState.Error("No UID")
        val snap = users.document(uid).get().await()
        val profile = snap.toObject(UserProfile::class.java) ?: UserProfile(uid = uid, email = email)
        ResultState.Success(profile)
    } catch (e: Exception) {
        ResultState.Error(e.message ?: "Sign in failed")
    }

    suspend fun signInWithGoogle(idToken: String): ResultState<UserProfile> = try {
        Log.d("Repo", "signInWithGoogle: got idToken=${idToken.take(15)}...")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: return ResultState.Error("No UID")
        val email = result.user?.email ?: ""
        Log.d("Repo", "Firebase signInWithCredential success: uid=$uid, email=$email")


// Ensure a profile exists
        val doc = users.document(uid).get().await()
        if (!doc.exists()) {
            Log.d("Repo", "No profile found, creating new one for uid=$uid")
            val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis())
            users.document(uid).set(profile).await()
        }

        val profile = users.document(uid).get().await().toObject(UserProfile::class.java)
            ?: UserProfile(uid = uid, email = email)
        Log.d("Repo", "Final profile loaded: $profile")
        ResultState.Success(profile)
    } catch (e: Exception) {
        ResultState.Error(e.message ?: "Google sign-in failed")
    }

    suspend fun saveUserProfile(profile: UserProfile): ResultState<String> = try {
        users.document(profile.uid).set(profile).await()
        ResultState.Success("Profile saved successfully")
    } catch (e: Exception) {
        ResultState.Error(e.message ?: "Failed to save profile")
    }


    suspend fun forgotPassword(email: String): ResultState<String> = try {
        auth.sendPasswordResetEmail(email).await()
        ResultState.Success("Password reset email sent")
    } catch (e: Exception) {
        ResultState.Error(e.message ?: "Failed to send reset email")
    }

    suspend fun getUserProfile(uid: String): ResultState<UserProfile?> = try {
        val snapshot = users.document(uid).get().await()
        val profile = snapshot.toObject(UserProfile::class.java)
        ResultState.Success(profile)
    } catch (e: Exception) {
        ResultState.Error(e.localizedMessage ?: "Unknown error")
    }

    fun signOut() { auth.signOut() }


}