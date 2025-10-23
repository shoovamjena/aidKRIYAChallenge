package com.example.aidkriyachallenge.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.R

import com.example.aidkriyachallenge.utils.PolylineDecoder
import com.example.aidkriyachallenge.utils.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import android.location.Location // <-- Add this import
import com.example.aidkriyachallenge.dataModel.Request
import com.example.aidkriyachallenge.location.LocationClient
import com.example.aidkriyachallenge.repo.RealtimeRepo
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.firestore.firestore
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Job



import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(
    private val context: Context
) : ViewModel() {

    // --- State Properties Exposed to the UI ---
    private val _repo = MutableStateFlow<RealtimeRepo?>(null)
    val repo = _repo.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole = _userRole.asStateFlow()

    private val _userStatus = MutableStateFlow<String?>("idle")
    val userStatus = _userStatus.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<Request>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _activeRequest = MutableStateFlow<Request?>(null)
    val activeRequest = _activeRequest.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId = _sessionId.asStateFlow()

    private val _locations = MutableStateFlow<Map<String, Pair<Double, Double>>>(emptyMap())
    val locations = _locations.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    private val _currentUserLocation = MutableStateFlow<Location?>(null)
    val currentUserLocation = _currentUserLocation.asStateFlow()

    private val _selectedRequest = MutableStateFlow<Request?>(null)
    val selectedRequest = _selectedRequest.asStateFlow()

    private val _isCloseEnoughToStartWalk = MutableStateFlow(false)
    val isCloseEnoughToStartWalk = _isCloseEnoughToStartWalk.asStateFlow()

    private val _sessionStatus = MutableStateFlow("active") // Initial session status
    val sessionStatus = _sessionStatus.asStateFlow()

    var currentUserId: String? = null
        private set

    // --- Internal Properties ---
    private val dbReference: DatabaseReference = Firebase.database.reference
    private var locationListener: ValueEventListener? = null
    private var activeRequestListener: ValueEventListener? = null
    private var sessionStatusListener: ValueEventListener? = null
    private var locationUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            // 1. Get user ID from Firebase Auth
            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.e("MainViewModel", "User not signed in.")
                // Optionally emit an error state for the UI
                return@launch
            }
            currentUserId = userId // Set the user ID for the class

            // 2. Fetch the role from Firestore
            val role = fetchUserRoleFromFirestore(userId)
            _userRole.value = role

            // 3. Create the RealtimeRepo instance
            _repo.value = RealtimeRepo(db = dbReference, currentUserId = userId)

            // 4. Register user in Realtime DB (if role was found)
            if (role != null) {
                registerUser(role)
            } else {
                Log.e("MainViewModel", "User role not found in Firestore.")
                // Optionally emit an error state
            }
        }
    }

    /**
     * Fetches the user's role ("Walker" or "Companion") from Firestore
     * based on which collection their document exists in.
     */
    private suspend fun fetchUserRoleFromFirestore(userId: String): String? {
        val db = Firebase.firestore
        try {
            if (db.collection("Walker").document(userId).get().await().exists()) {
                return "Walker"
            }
            if (db.collection("Wanderer").document(userId).get().await().exists()) {
                return "Companion" // Map Firestore "Wanderer" to our "Companion"
            }
            Log.w("MainViewModel", "User document not found in Walker or Wanderer collections.")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error fetching user role from Firestore: ${e.message}")
        }
        return null
    }

    // --- Public Functions for the UI ---

    fun registerUser(role: String) {
        repo.value?.registerUser(role)
    }

    fun raiseCall(destination: LatLng) {
        viewModelScope.launch {
            val repo = _repo.value ?: return@launch
            try {
                val locationClient = LocationClient(context)
                val location = locationClient.getLocationUpdates(0L).first()
                repo.raiseCall(
                    location.latitude,
                    location.longitude,
                    destination.latitude,
                    destination.longitude
                ) { newRequestId ->
                    listenToActiveRequest(newRequestId)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Could not get location to raise call: ${e.message}")
            }
        }
    }

    fun listenToPendingRequests() {
        viewModelScope.launch {
            repo.value?.getPendingRequestsFlow()?.collect { requests ->
                _pendingRequests.value = requests
            }
        }
    }

    fun acceptCall(requestId: String) {
        _userStatus.value = "in_session"
        val location = _currentUserLocation.value
        if (location == null) {
            Log.e("MainViewModel", "Cannot accept call, user location is null.")
            return
        }
        repo.value?.acceptCall(requestId, location.latitude, location.longitude) {
            listenToActiveRequest(requestId)
        }
    }

    fun rejectCompanion(requestId: String, companionId: String) {
        repo.value?.rejectCompanion(requestId, companionId) {}
    }

    fun confirmMatch(requestId: String, companionId: String) {
        repo.value?.createSession(requestId, companionId) { newSessionId ->
            _sessionId.value = newSessionId
        }
    }

    fun fetchRoute() {
        viewModelScope.launch {
            if (_locations.value.size < 2) {
                _routePoints.value = emptyList(); return@launch
            }
            val locs = _locations.value.values.toList()
            val activeReq = _activeRequest.value
            val apiKey = context.getString(R.string.google_maps_key)
            val origin: String
            val destination: String

            if (sessionStatus.value == "walking" && activeReq != null) {
                val companionLocation = _locations.value[activeReq.companionId]?.let { "${it.first},${it.second}" }
                if (companionLocation == null) { _routePoints.value = emptyList(); return@launch }
                origin = companionLocation
                destination = "${activeReq.destLat},${activeReq.destLng}"
            } else {
                origin = "${locs[0].first},${locs[0].second}"
                destination = "${locs[1].first},${locs[1].second}"
            }

            try {
                val response = RetrofitClient.instance.getDirections(origin, destination, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    val encodedPolyline = response.body()!!.routes.firstOrNull()?.overview_polyline?.points
                    if (encodedPolyline != null) {
                        _routePoints.value = PolylineDecoder.decode(encodedPolyline)
                    } else { _routePoints.value = emptyList() }
                } else {
                    Log.e("MainViewModel", "API call failed: ${response.code()} ${response.errorBody()?.string()}")
                    _routePoints.value = emptyList()
                }
            } catch (e: Exception) {
                _routePoints.value = emptyList()
                Log.e("MainViewModel", "Failed to fetch directions: ${e.message}")
            }
        }
    }

    fun listenToLocationUpdates() {
        val currentSessionId = _sessionId.value ?: return
        removeLocationListener() // Clean up previous location listener

        // Listen for Session Status changes
        removeSessionStatusListener() // Clean up previous status listener first
        sessionStatusListener = dbReference.child("sessions").child(currentSessionId).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _sessionStatus.value = snapshot.getValue(String::class.java) ?: "active"
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w("MainViewModel", "Session status listener cancelled: ${error.message}")
                }
            })

        // Listen for Location changes
        locationListener = repo.value?.listenLocations(currentSessionId) { newLocations ->
            _locations.value = newLocations
            if (newLocations.size >= 2) {
                val isWalker = currentUserId == _activeRequest.value?.walkerId
                if (isWalker) {
                    val locList = newLocations.values.toList()
                    val point1 = LatLng(locList[0].first, locList[0].second)
                    val point2 = LatLng(locList[1].first, locList[1].second)
                    val distance = SphericalUtil.computeDistanceBetween(point1, point2)
                    _isCloseEnoughToStartWalk.value = distance < 50
                }
                fetchRoute()
            } else {
                _isCloseEnoughToStartWalk.value = false
                _routePoints.value = emptyList()
            }
        }
    }

    fun endSession() {
        val sessionToEnd = _sessionId.value
        val requestInfo = _activeRequest.value
        if (sessionToEnd != null && requestInfo?.companionId != null) {
            repo.value?.endSession(sessionToEnd, requestInfo.walkerId, requestInfo.companionId!!) {
                // Reset local state AFTER Firebase is updated
                removeLocationListener()
                removeSessionStatusListener() // Also remove the status listener
                _userStatus.value = "idle"
                _sessionId.value = null
                _locations.value = emptyMap()
                _activeRequest.value = null
                _routePoints.value = emptyList()
                _sessionStatus.value = "active" // Reset status
                _isCloseEnoughToStartWalk.value = false
            }
        }
    }

    fun startLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = viewModelScope.launch {
            val locationClient = LocationClient(context)
            try {
                locationClient.getLocationUpdates(10000L).collect { location ->
                    _currentUserLocation.value = location
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error getting location updates: ${e.message}")
            }
        }
    }

    fun onMarkerClick(request: Request?) {
        _selectedRequest.value = request
    }

    fun beginWalkingSession() {
        val currentSessionId = _sessionId.value ?: return
        repo.value?.initiateWalkingSession(currentSessionId)
    }

    fun confirmWalkingSession() {
        val currentSessionId = _sessionId.value ?: return
        repo.value?.confirmWalkingSession(currentSessionId)
    }

    // --- Listener Management ---

    private fun listenToActiveRequest(requestId: String) {
        removeActiveRequestListener()
        activeRequestListener = repo.value?.listenToRequestById(requestId) { request ->
            _activeRequest.value = request
            if (request?.status == "linked" && request.sessionId != null) {
                _sessionId.value = request.sessionId
            }
        }
    }

    private fun removeActiveRequestListener() {
        val currentRequestId = _activeRequest.value?.id
        if (activeRequestListener != null && currentRequestId != null) {
            repo.value?.removeRequestListener(currentRequestId, activeRequestListener!!)
            activeRequestListener = null
        }
    }

    private fun removeLocationListener() {
        val currentSessionId = _sessionId.value
        if (locationListener != null && currentSessionId != null) {
            repo.value?.removeLocationListener(currentSessionId, locationListener!!)
            locationListener = null
        }
    }

    private fun removeSessionStatusListener() {
        val currentSessionId = _sessionId.value
        if (sessionStatusListener != null && currentSessionId != null) {
            dbReference.child("sessions").child(currentSessionId).child("status")
                .removeEventListener(sessionStatusListener!!)
            sessionStatusListener = null
        }
    }

    override fun onCleared() {
        removeLocationListener()
        removeActiveRequestListener()
        removeSessionStatusListener() // Clean up the status listener too
        locationUpdateJob?.cancel() // Stop collecting user's own location
        super.onCleared()
    }
}