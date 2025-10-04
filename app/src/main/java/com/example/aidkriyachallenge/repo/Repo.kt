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


    suspend fun signIn(email: String, password: String,isWanderer: Boolean): ResultState<UserProfile> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return ResultState.Error("No UID")
            val snap = if (isWanderer) {
                wanderer.document(uid).get().await()
            }else{
                walker.document(uid).get().await()
            }
            val profile = snap.toObject(UserProfile::class.java) ?: UserProfile(uid = uid, email = email, isWanderer = isWanderer)
            ResultState.Success(profile)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Sign in failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String,isWanderer: Boolean): ResultState<UserProfile> {
        return try {
            Log.d("Repo", "signInWithGoogle: got idToken=${idToken.take(15)}...")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return ResultState.Error("No UID")
            val email = result.user?.email ?: ""
            Log.d("Repo", "Firebase signInWithCredential success: uid=$uid, email=$email")


    // Ensure a profile exists
            val doc = if(isWanderer){
                wanderer.document(uid).get().await()
            }else{
                walker.document(uid).get().await()
            }
            if (!doc.exists()) {
                Log.d("Repo", "No profile found, creating new one for uid=$uid")
                val profile = UserProfile(uid = uid, email = email, createdAt = System.currentTimeMillis(), isWanderer = isWanderer)
                if(isWanderer){
                    wanderer.document(uid).set(profile).await()
                }else{
                    walker.document(uid).set(profile).await()
                }
            }

            val profile =if(isWanderer){
                wanderer.document(uid).get().await().toObject(UserProfile::class.java)
                    ?: UserProfile(uid = uid, email = email, isWanderer = isWanderer)
            }else{
                walker.document(uid).get().await().toObject(UserProfile::class.java)
                    ?: UserProfile(uid = uid, email = email, isWanderer = isWanderer)
            }
            Log.d("Repo", "Final profile loaded: $profile")
            ResultState.Success(profile)
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