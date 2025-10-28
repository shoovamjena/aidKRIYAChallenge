package com.example.aidkriyachallenge.googleauthentication

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.aidkriyachallenge.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthClient(
    private val context: Context,
) {

    private val tag = "GoogleAuthClient: "
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(): String? {
        return try {
            val result = buildCredentialRequest()
            handleSignIn(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            println(tag + "signIn error: ${e.message}")
            null
        }
    }

    private fun handleSignIn(result: GetCredentialResponse): String? {
        Log.d(tag, "handleSignIn: credential=${result.credential}")
        val credential = result.credential
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return try {
                val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                println(tag + "name: ${tokenCredential.displayName}")
                println(tag + "email: ${tokenCredential.id}")

                tokenCredential.idToken
            } catch (e: GoogleIdTokenParsingException) {
                println(tag + "GoogleIdTokenParsingException: ${e.message}")
                null
            }
        }
        return null
    }

    private suspend fun buildCredentialRequest(): GetCredentialResponse {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("861229805389-rj1v61hcs7cacfveah6j1ckiba0bfna3.apps.googleusercontent.com") // your web client ID
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        return try {
            Log.d(tag, "Requesting credential with clientId=${context.getString(R.string.default_web_client_id)}")

            val re = credentialManager.getCredential(context, request)
            Log.d("GoogleAuth", "Got credential: $re")
            re
        }catch (e: Exception){
            Log.e("GoogleAuth", "getCredential failed", e)
            throw e


        }
    }

     suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}