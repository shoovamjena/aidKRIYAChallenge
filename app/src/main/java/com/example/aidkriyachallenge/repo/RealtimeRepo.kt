package com.example.aidkriyachallenge.repo


import android.util.Log
import com.example.aidkriyachallenge.dataModel.Request
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A corrected repository that requires the current user's stable ID.
 * This ID should come from Firebase Authentication (auth.currentUser.uid).
 */
class RealtimeRepo(
    private val db: DatabaseReference,
    private val currentUserId: String
) {

    // --- User and Session Management ---

    fun registerUser(role: String) {
        val user = mapOf(
            "role" to role,
            "status" to "idle",
            "lastActive" to ServerValue.TIMESTAMP
        )
        db.child("users").child(currentUserId).setValue(user)
    }

    // In RealtimeRepo.kt

    // In RealtimeRepo.kt

    fun raiseCall(
        latitude: Double,
        longitude: Double,
        destLat: Double,
        destLng: Double,
        walkerName: String,
        profileImageUrl: String?, // Receives the String? from ViewModel
        onSuccess: (requestId: String) -> Unit
    ) {
        val requestRef = db.child("requests").push()
        val requestId = requestRef.key ?: return

        // --- THIS IS THE FIX (from your saveProfile) ---
        // Convert a null URL to an empty string.
        val finalUrl = profileImageUrl ?: ""

        val request = mapOf(
            "walkerId" to currentUserId,
            "companionId" to null,
            "status" to "pending",
            "createdAt" to ServerValue.TIMESTAMP,
            "lat" to latitude,
            "lng" to longitude,
            "destLat" to destLat,
            "destLng" to destLng,
            "walkerName" to walkerName,
            "profileImageUrl" to finalUrl // This is now safe (it's "" not null)
        )

        // This will no longer crash.
        requestRef.setValue(request)
            .addOnSuccessListener {
                requestRef.onDisconnect().removeValue()
                onSuccess(requestId)
            }
    }
    fun acceptCall(requestId: String, lat: Double, lng: Double, onComplete: () -> Unit) {
        val updates = mapOf(
            "/requests/$requestId/companionId" to currentUserId,
            "/requests/$requestId/status" to "acceptedByCompanion",
            "/requests/$requestId/companionLat" to lat, // <-- ADD THIS
            "/requests/$requestId/companionLng" to lng, // <-- AND THIS
            "/users/$currentUserId/status" to "in_session"
        )
        db.updateChildren(updates).addOnSuccessListener { onComplete() }
    }

    fun rejectCompanion(requestId: String, companionId: String, onComplete: () -> Unit) {
        val updates = mapOf(
            "/requests/$requestId/companionId" to null,
            "/requests/$requestId/status" to "pending",
            "/users/$companionId/status" to "idle" // Make the companion available again
        )
        db.updateChildren(updates).addOnSuccessListener { onComplete() }
    }

    // This function acts as the "confirm" action from the Walker
    fun createSession(
        requestId: String, // We need this to cancel the onDisconnect
        companionId: String,
        onComplete: () -> Unit
    ) {
        // --- THIS IS THE MISSING LOGIC ---
        val sessionRef = db.child("sessions").push()
        val sessionId = sessionRef.key
        if (sessionId == null) {
            Log.e("RealtimeRepo", "Failed to get push key for new session.")
            return
        }

        val session = mapOf(
            "walkerId" to currentUserId,
            "companionId" to companionId,
            "status" to "active", // The session starts as "active"
            "startedAt" to ServerValue.TIMESTAMP
        )
        // --- END OF MISSING LOGIC ---

        sessionRef.setValue(session)
            .addOnSuccessListener {
                Log.d("RealtimeRepo", "Session node created ($sessionId). Updating request...")

                // We have a successful match, so cancel the "last will"
                val requestRef = db.child("requests").child(requestId)
                requestRef.onDisconnect().cancel() // Cancel the onDisconnect

                // Use the 'sessionId' variable we created
                requestRef.updateChildren(mapOf("status" to "linked", "sessionId" to sessionId))
                    .addOnSuccessListener {
                        Log.d("RealtimeRepo", "Request node updated. Calling onComplete callback.")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("RealtimeRepo", "Failed to update request node: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RealtimeRepo", "Failed to create session node: ${e.message}")
            }
    }
    // --- Location Functions ---

    fun updateLocation(sessionId: String, lat: Double, lng: Double) {
        val loc = mapOf(
            "lat" to lat,
            "lng" to lng,
            "updatedAt" to ServerValue.TIMESTAMP
        )
        db.child("sessions").child(sessionId).child("locations").child(currentUserId).setValue(loc)
    }

    // --- Listener and Flow Functions ---

    fun getPendingRequestsFlow(): Flow<List<Request>> = callbackFlow {
        val query = db.child("requests").orderByChild("status").equalTo("pending")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = snapshot.children.mapNotNull { data ->
                    data.getValue(Request::class.java)?.copy(id = data.key ?: "")
                }
                trySend(requests)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun listenToRequestById(requestId: String, onUpdate: (Request?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val request = snapshot.getValue(Request::class.java)?.copy(id = snapshot.key ?: "")
                onUpdate(request)
            }

            override fun onCancelled(error: DatabaseError) {
                onUpdate(null)
            }
        }
        db.child("requests").child(requestId).addValueEventListener(listener)
        return listener
    }

    fun listenLocations(
        sessionId: String,
        onUpdate: (Map<String, Pair<Double, Double>>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Pair<Double, Double>>()
                snapshot.children.forEach {
                    val lat = it.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = it.child("lng").getValue(Double::class.java) ?: 0.0
                    val id = it.key ?: return@forEach
                    map[id] = Pair(lat, lng)
                }
                onUpdate(map)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        db.child("sessions").child(sessionId).child("locations").addValueEventListener(listener)
        return listener
    }

    // --- Listener Cleanup Functions ---

    fun removeRequestListener(requestId: String, listener: ValueEventListener) {
        db.child("requests").child(requestId).removeEventListener(listener)
    }

    fun removeLocationListener(sessionId: String, listener: ValueEventListener) {
        db.child("sessions").child(sessionId).child("locations").removeEventListener(listener)
    }

    fun endSession(
        sessionId: String,
        requestId: String, // <-- ADD THIS
        walkerId: String,
        companionId: String,
        onComplete: () -> Unit
    ) {
        val updates = mapOf<String, Any?>(
            // Reset the walker's status
            "/users/$walkerId/status" to "idle",
            // Reset the companion's status
            "/users/$companionId/status" to "idle",
            // Delete the entire session node
            "/sessions/$sessionId" to null,
            // --- NEW ---
            // Delete the original request node
            "/requests/$requestId" to null
        )

        // This single call will delete both nodes and update both users
        db.updateChildren(updates).addOnSuccessListener { onComplete() }
    }

    fun initiateWalkingSession(sessionId: String) {
        db.child("sessions").child(sessionId).child("status").setValue("awaiting_walk_confirmation")
    }

    // Called by the Companion to confirm and start the walk
    fun confirmWalkingSession(sessionId: String) {
        db.child("sessions").child(sessionId).child("status").setValue("walking")
    }
    // In your Repo.kt file

    fun updateSessionStatus(sessionId: String, status: String) {
        // 1. Removed the onComplete parameter
        // 2. Changed "requests" to "sessions"
        db.child("sessions").child(sessionId).child("status").setValue(status)
            .addOnFailureListener {
                Log.e("RealtimeRepo", "Failed to update session status: ${it.message}")
            }
    }

    fun markArrived(sessionId: String, isWalker: Boolean) {
        val key = if (isWalker) "walkerArrived" else "companionArrived"
        // We add these flags to the session/request object
        db.child("requests").child(sessionId).child(key).setValue(true)
    }

}