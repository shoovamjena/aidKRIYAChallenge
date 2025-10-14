package com.example.aidkriyachallenge.repo

import android.util.Log
import com.example.aidkriyachallenge.common.ResultState
import com.example.aidkriyachallenge.common.WALKER_PATH
import com.example.aidkriyachallenge.common.WANDERER_PATH
import com.example.aidkriyachallenge.dataModel.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class Repo(
    firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val wanderer = firestore.collection(WANDERER_PATH)
    private val walker = firestore.collection(WALKER_PATH)


    suspend fun signUp(email: String, password: String,isWanderer: Boolean): ResultState<UserProfile> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email,password).await()
            val uid = result.user?.uid ?: return ResultState.Error("No UID returned")

            if(isWanderer){
                val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis(), isWanderer = true)
                wanderer.document(uid).set(profile).await()
                ResultState.Success(profile)
            }else{
                val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis(), isWanderer = false)
                walker.document(uid).set(profile).await()
                ResultState.Success(profile)
            }
        }
        catch (e: Exception) {
            ResultState.Error(e.message ?: "Sign up failed")
        }
    }


    suspend fun signIn(email: String, password: String): ResultState<UserProfile> {
        return try {
            Log.d("Repo", "Starting sign in for email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return ResultState.Error("No UID")

            Log.d("Repo", "Auth successful. UID: $uid")
            Log.d("Repo", "Checking wanderer collection...")

            // Check wanderer collection first
            val wandererSnap = wanderer.document(uid).get().await()
            Log.d("Repo", "Wanderer exists: ${wandererSnap.exists()}")

            if (wandererSnap.exists()) {
                val profile = wandererSnap.toObject(UserProfile::class.java)
                Log.d("Repo", "Wanderer profile: $profile")
                if (profile != null) {
                    return ResultState.Success(profile)
                } else {
                    Log.e("Repo", "Failed to parse wanderer profile")
                }
            }

            Log.d("Repo", "Checking walker collection...")
            // Check walker collection
            val walkerSnap = walker.document(uid).get().await()
            Log.d("Repo", "Walker exists: ${walkerSnap.exists()}")

            if (walkerSnap.exists()) {
                val profile = walkerSnap.toObject(UserProfile::class.java)
                Log.d("Repo", "Walker profile: $profile")
                if (profile != null) {
                    return ResultState.Success(profile)
                } else {
                    Log.e("Repo", "Failed to parse walker profile")
                }
            }

            Log.e("Repo", "Profile not found in either collection for uid: $uid")
            ResultState.Error("User profile not found")
        } catch (e: Exception) {
            Log.e("Repo", "Sign in exception: ${e.message}", e)
            ResultState.Error(e.message ?: "Sign in failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String): ResultState<UserProfile> {
        return try {
            Log.d("Repo", "signInWithGoogle: got idToken=${idToken.take(15)}...")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return ResultState.Error("No UID")
            val email = result.user?.email ?: ""
            Log.d("Repo", "Firebase signInWithCredential success: uid=$uid, email=$email")

            // Check wanderer collection first
            val wandererDoc = wanderer.document(uid).get().await()
            if (wandererDoc.exists()) {
                val profile = wandererDoc.toObject(UserProfile::class.java)
                    ?: return ResultState.Error("Failed to parse profile")
                Log.d("Repo", "Found existing wanderer profile: $profile")
                return ResultState.Success(profile)
            }

            // Check walker collection
            val walkerDoc = walker.document(uid).get().await()
            if (walkerDoc.exists()) {
                val profile = walkerDoc.toObject(UserProfile::class.java)
                    ?: return ResultState.Error("Failed to parse profile")
                Log.d("Repo", "Found existing walker profile: $profile")
                return ResultState.Success(profile)
            }

            // If this is a new Google sign-in, you need to decide how to handle it
            // Option 1: Return error asking user to complete signup first
            ResultState.Error("Please complete signup to choose your role")

            // Option 2: Create default profile (you'd need to decide wanderer vs walker somehow)
            // val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis(), isWanderer = true)
            // wanderer.document(uid).set(profile).await()
            // ResultState.Success(profile)

        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Google sign-in failed")
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): ResultState<String> = try {
        if(profile.isWanderer){
            wanderer.document(profile.uid).set(profile).await()
        }else{
            walker.document(profile.uid).set(profile).await()
        }

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

    suspend fun getUserProfile(uid: String,isWanderer: Boolean): ResultState<UserProfile?> = try {
        val snapshot =if(isWanderer){
            wanderer.document(uid).get().await()
        }else{
            walker.document(uid).get().await()
        }
        val profile = snapshot.toObject(UserProfile::class.java)
        ResultState.Success(profile)
    } catch (e: Exception) {
        ResultState.Error(e.localizedMessage ?: "Unknown error")
    }


}