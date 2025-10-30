package com.example.aidkriyachallenge.viewModel


import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.common.UserPreferences
import com.example.aidkriyachallenge.dataModel.Request
import com.example.aidkriyachallenge.location.LocationClient
import com.example.aidkriyachallenge.repo.RealtimeRepo
import com.example.aidkriyachallenge.utils.PolylineDecoder
import com.example.aidkriyachallenge.utils.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.maps.android.SphericalUtil
import com.razorpay.PaymentData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.checkerframework.checker.units.qual.Speed

data class ReviewTriggerInfo(
    val isWalker: Boolean,
    val walkerId: String,
    val companionId: String,
    val walkId: String // The reviewRepo needs a 'walkid'. The requestId is perfect for this.
)



class MainViewModel(
    @SuppressLint("StaticFieldLeak") private val context: Context
) : ViewModel() {

    sealed class PaymentResult {
        data class Success(
            val companionEarningsPaise: Int
        ) : PaymentResult()

        data class Error(val description: String) : PaymentResult()
    }

    private val userPreferences = UserPreferences(context)
    private val _totalEarnings = MutableStateFlow(0L) // Stored in paise
    val totalEarnings = _totalEarnings.asStateFlow()

    private val _showEarningsReceivedDialog = MutableStateFlow<Long?>(null) // Holds the amount earned
    val showEarningsReceivedDialog = _showEarningsReceivedDialog.asStateFlow()

    // --- State Properties Exposed to the UI ---
    private val _repo = MutableStateFlow<RealtimeRepo?>(null)
    val repo = _repo.asStateFlow()
    private val _reviewTrigger = MutableStateFlow<ReviewTriggerInfo?>(null)
    val reviewTrigger = _reviewTrigger.asStateFlow()


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

    private val _pendingCompanionInterests = MutableStateFlow<Set<String>>(emptySet())
    val pendingCompanionInterests: StateFlow<Set<String>> = _pendingCompanionInterests.asStateFlow()

    // --- PAYMENT FLOW STATE ---
    private val _navigateToPayment = MutableStateFlow<Pair<Int, Int>?>(null)
    val navigateToPayment: StateFlow<Pair<Int, Int>?> = _navigateToPayment.asStateFlow()

    private val _paymentResult = MutableSharedFlow<PaymentResult>()
    val paymentResult: SharedFlow<PaymentResult> = _paymentResult.asSharedFlow()

    fun sendInterestToWalker(requestId: String) {
        viewModelScope.launch {
            // TODO: Call your repository to tell Firestore the companion is interested
            // e.g., repo.sendInterest(requestId, companionId)

            // Add to our local state to update the UI
            _pendingCompanionInterests.update { it + requestId }

            // TODO: Start a 10-minute timer to auto-cancel this interest
            // startTimeoutForRequest(requestId)
        }
    }

    // 3. THIS FUNCTION IS CALLED WHEN COMPANION CLICKS "CANCEL REQUEST"
    fun cancelInterest(requestId: String) {
        // TODO: Call your repo.value?.cancelInterest(requestId, ...) function here

        // Remove from our local "waiting" state
        _pendingCompanionInterests.update { it.filterNot { it == requestId }.toSet() }

        // Also deselect the request to close the card
        if (_selectedRequest.value?.id == requestId) {
            _selectedRequest.value = null
        }
    }

    fun observeRequestChanges() { // Make this public if you call it from HomeScreen
        viewModelScope.launch {
            combine(pendingRequests, pendingCompanionInterests) { available, waiting ->
                Pair(available, waiting)
            }.collect { (availableRequests, waitingForRequests) ->

                val currentSelectedId = _selectedRequest.value?.id ?: return@collect
                // Nothing selected, do nothing

                val isStillInPendingList = availableRequests.any { it.id == currentSelectedId }
                val isBeingWaitedFor = waitingForRequests.any { it == currentSelectedId }

                // If it's not in the public list AND not in our waiting list...
                // THEN the walker *must have* cancelled it.
                if (!isStillInPendingList && !isBeingWaitedFor) {
                    // Now it's safe to clear the selection
                    _selectedRequest.value = null
                }
            }
        }
    }

    private fun observeActiveRequestForCancellations() {
        viewModelScope.launch {
            activeRequest.collect { activeReq ->
                // If the active request becomes null (e.g., walker cancels while we wait)
                if (activeReq == null) {
                    // Find any requests we were waiting for
                    val waitingIds = _pendingCompanionInterests.value.map { it }

                    // If there were any, clear them. This will trigger
                    // observeRequestChanges() to hide the card.
                    if (waitingIds.isNotEmpty()) {
                        _pendingCompanionInterests.update { emptySet() }
                    }
                }
            }
        }
    }
    private fun listenToUserWallet(userId: String) {
        dbReference.child("users").child(userId).child("walletBalance")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newBalance = snapshot.getValue(Long::class.java) ?: 0L

                    // Get the *current* balance from our state (which came from DataStore)
                    val currentBalance = _totalEarnings.value

                    if (newBalance > currentBalance) {
                        // We just got paid!
                        val amountEarned = newBalance - currentBalance

                        // Trigger the dialog
                        _showEarningsReceivedDialog.value = amountEarned
                    }

                    // Save the new *total* balance to DataStore
                    viewModelScope.launch {
                        userPreferences.saveTotalEarnings(newBalance)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("ViewModel", "Wallet listener cancelled: ${error.message}")
                }
            })
    }


    var currentUserId: String? = null
        private set

    // --- Internal Properties ---
    private val dbReference: DatabaseReference = Firebase.database.reference
    private var locationListener: ValueEventListener? = null
    private var activeRequestListener: ValueEventListener? = null
    private var sessionStatusListener: ValueEventListener? = null
    private var locationUpdateJob: Job? = null

    init {
        observeRequestChanges() // Start the smart observer
        observeActiveRequestForCancellations() // Start the cancellation observer
        trackActiveRequestForCleanup()
        userPreferences.totalEarnings
            .onEach { earnings -> _totalEarnings.value = earnings }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            // 1. Get user ID from Firebase Auth
            val userPrefs = UserPreferences(context)
            val userIdFlow: Flow<String?> = userPrefs.getUserid() // Use your function name
            val userId: String? = userIdFlow.first() // Get the value once
            if (userId == null) {
                Log.e("MainViewModel", "User not signed in.")
                // Optionally emit an error state for the UI
                return@launch
            }
            currentUserId = userId // Set the user ID for the class

            // 2. Fetch the role from user preference


            // 2. Call the function on the instance
            val isWandererFlow: Flow<Boolean?> = userPrefs.getUserRole()

            // Get the value from the Flow
            val isWanderer: Boolean? = isWandererFlow.first()

            // Map Boolean? to String? (using your mapping)
            val role: String? = when (isWanderer) {
                true -> "Walker"
                false -> "Companion"
                null -> null
            }
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
            if (role == "Companion") {
                listenToUserWallet(userId)
            }
        }
    }
    fun dismissEarningsDialog() {
        _showEarningsReceivedDialog.value = null
    }

    // --- Public Functions for the UI ---

    private fun registerUser(role: String) {
        repo.value?.registerUser(role)
    }

    // In MainViewModel.kt

    fun raiseCall(destination: LatLng, walkerName: String, imageUrl: android.net.Uri?,genders: String,walkingSpeed: String) {
        viewModelScope.launch {
            val repo = _repo.value ?: return@launch
            try {
                val locationClient = LocationClient(context)
                val location = locationClient.getLocationUpdates(0L).first()

                repo.raiseCall(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    destLat = destination.latitude,
                    destLng = destination.longitude,
                    walkerName = walkerName,
                    gender = genders,
                    walkingSpeed = walkingSpeed,
                    // This is 100% correct.
                    // It converts the https://... Uri into a String?
                    profileImageUrl = imageUrl?.toString(),
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
        val location = _currentUserLocation.value
        if (location == null) {
            Log.e("MainViewModel", "Cannot send interest, user location is null.")
            return
        }

        // Find the full Request object before we "lose" it
        val requestToPend = _selectedRequest.value
        if (requestToPend == null || requestToPend.id != requestId) {
            Log.e("MainViewModel", "Selected request does not match ID, cannot pend")
            return
        }

        // Your repo.acceptCall is fine. It will remove the request from the public list.
        repo.value?.acceptCall(requestId, location.latitude, location.longitude) {
            // This is the FIX: We add the *full Request object* to our
            // new "waiting" state, so we don't lose it.
            _pendingCompanionInterests.update { setOf((it + requestToPend).toString()) }

            listenToActiveRequest(requestId)

            // TODO: Start your 10-minute timeout logic here.
            // E.g., startTimeoutForRequest(requestToPend)
        }
    }

    fun rejectCompanion(requestId: String, companionId: String) {
        repo.value?.rejectCompanion(requestId, companionId) {}
    }

    fun confirmMatch(requestId: String, companionId: String) {
        Log.d(
            "MainViewModel",
            "confirmMatch called. Request ID: $requestId, Companion ID: $companionId"
        )

        // We change the callback to just log success, not set the state.
        repo.value?.createSession(requestId, companionId) {
            // The session is created. The 'listenToActiveRequest' listener
            // will now see the "linked" status and session ID in Firebase
            // and will handle updating the _sessionId.value for us.
            Log.d("MainViewModel", "createSession callback executed. Firebase update is complete.")
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
                val companionLocation =
                    _locations.value[activeReq.companionId]?.let { "${it.first},${it.second}" }
                if (companionLocation == null) {
                    _routePoints.value = emptyList(); return@launch
                }
                origin = companionLocation
                destination = "${activeReq.destLat},${activeReq.destLng}"
            } else {
                origin = "${locs[0].first},${locs[0].second}"
                destination = "${locs[1].first},${locs[1].second}"
            }

            try {
                val response = RetrofitClient.instance.getDirections(origin, destination, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    val encodedPolyline =
                        response.body()!!.routes.firstOrNull()?.overview_polyline?.points
                    if (encodedPolyline != null) {
                        _routePoints.value = PolylineDecoder.decode(encodedPolyline)
                    } else {
                        _routePoints.value = emptyList()
                    }
                } else {
                    Log.e(
                        "MainViewModel",
                        "API call failed: ${response.code()} ${response.errorBody()?.string()}"
                    )
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
        sessionStatusListener =
            dbReference.child("sessions").child(currentSessionId).child("status")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _sessionStatus.value = snapshot.getValue(String::class.java) ?: "active"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(
                            "MainViewModel",
                            "Session status listener cancelled: ${error.message}"
                        )
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

    private fun completeSessionCleanup() {
        val sessionToEnd = _sessionId.value
        val requestInfo = _activeRequest.value

        // Ensure we have all the info we need
        if (sessionToEnd != null && requestInfo != null && requestInfo.companionId != null) {

            // --- 4. CAPTURE THE INFO ---
            // We must do this *before* clearing the state!
            val isCurrentUserWalker = currentUserId == requestInfo.walkerId
            val reviewInfo = ReviewTriggerInfo(
                isWalker = isCurrentUserWalker,
                walkerId = requestInfo.walkerId,
                companionId = requestInfo.companionId,
                walkId = requestInfo.id // Use the request ID as the "walkId"
            )

            // This part is the same as before
            repo.value?.endSession(
                sessionToEnd,
                requestInfo.id,
                requestInfo.walkerId,
                requestInfo.companionId
            ) {
                viewModelScope.launch {
                    UserPreferences(context).clearSessionId()
                }

                // This local state reset logic remains the same
                removeLocationListener()
                removeSessionStatusListener()
                _userStatus.value = "idle"
                _sessionId.value = null
                _locations.value = emptyMap()
                _activeRequest.value = null
                _routePoints.value = emptyList()
                _sessionStatus.value = "active"
                _isCloseEnoughToStartWalk.value = false

                // --- 5. SET THE TRIGGER ---
                // This will be emitted *after* sessionId is null.
                // The user will navigate home AND this will trigger the dialog.
                _reviewTrigger.value = reviewInfo
            }
        } else {
            Log.e("MainViewModel", "completeSessionCleanup called but session or request info is missing.")
        }
    }

    // 6. ADD THIS FUNCTION TO DISMISS THE DIALOG
    fun dismissReviewDialog() {
        _reviewTrigger.value = null
    }
    fun endSession(isWalker: Boolean) {
        val currentSessionId = _sessionId.value ?: return

        if (isWalker) {
            // Walker is ending to get paid
            // 1. SET STATUS TO PENDING IN FIREBASE
            // This will trigger the dialog for the Companion
            repo.value?.updateSessionStatus(currentSessionId, "payment_pending")

            // 2. Calculate distance...
            val distanceInMeters = calculateTotalDistance(_routePoints.value)
            val amountInPaise = ((distanceInMeters / 1000.0) * 10.0 * 100).toInt()

            // 3. Trigger navigation (for Walker)
            if (amountInPaise > 100) {
                _navigateToPayment.value = Pair(distanceInMeters.toInt(), amountInPaise)
            } else {
                Log.d("MainViewModel", "Distance too short, skipping payment.")
                // Not enough to charge, just end the session normally
                completeSessionCleanup()
            }
        } else {
            // Companion is ending early (this is a "cancel" action)
            Log.d("MainViewModel", "Companion ended session early.")
            // This will end the session for everyone, skipping payment
            completeSessionCleanup()
        }
    }


    private fun calculateTotalDistance(points: List<LatLng>): Double {
        var totalDistance = 0.0
        if (points.size > 1) {
            for (i in 0 until points.size - 1) {
                totalDistance += SphericalUtil.computeDistanceBetween(points[i], points[i + 1])
            }
        }
        return totalDistance // Returns distance in meters
    }

    fun onPaymentNavigationComplete() {
        _navigateToPayment.value = null
    }


    fun onPaymentSuccess(
        paymentId: String?,
        paymentData: PaymentData?,
        totalAmountInPaise: Int // <-- This is the new parameter from MainActivity
    ) {
        viewModelScope.launch {
            Log.d("ViewModel", "Payment Successful: $paymentId")

            var companionSharePaise = 0L // Changed to Long

            try {
                val companionId = _activeRequest.value?.companionId
                // Use the reliable parameter, not paymentData
                val totalAmountPaise = totalAmountInPaise.toLong()

                if (companionId != null && totalAmountPaise > 0) {
                    companionSharePaise = (totalAmountPaise * 0.8).toLong() // Calculate as Long
                    updateCompanionWallet(companionId, companionSharePaise)
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error processing wallet update: ${e.message}")
            }

            // Emit the new, simpler Success object
            _paymentResult.emit(
                PaymentResult.Success(companionSharePaise.toInt())
            )
        }
        completeSessionCleanup()
    }

    // <<< 3. MODIFIED THIS FUNCTION >>>
    fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        viewModelScope.launch {
            // Emit the new, simpler Error object
            _paymentResult.emit(
                PaymentResult.Error(description ?: "Unknown payment error (code: $code)")
            )
        }
    }

    private fun updateCompanionWallet(companionId: String, amountPaiseToAdd: Long) { // Changed to Long
        val amountRupees = "%.2f".format(amountPaiseToAdd / 100.0)
        Log.d("ViewModel_Wallet", "**********************************************")
        Log.d("ViewModel_Wallet", "ATTEMPTING: Adding ₹$amountRupees to wallet of user $companionId")
        Log.d("ViewModel_Wallet", "**********************************************")

        // --- This is the new, real code ---
        // It finds the companion's wallet and securely adds the new amount.
        val companionWalletRef = dbReference.child("users").child(companionId).child("walletBalance")

        companionWalletRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentBalance = currentData.getValue(Long::class.java) ?: 0L
                val newBalance = currentBalance + amountPaiseToAdd
                currentData.value = newBalance
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                data: DataSnapshot?
            ) {
                if (committed) {
                    Log.d("ViewModel_Wallet", "SUCCESS: Wallet transaction complete. New balance: ${data?.value}")
                } else {
                    Log.e("ViewModel_Wallet", "FAILURE: Wallet transaction failed: ${error?.message}")
                }
            }
        })
    }

    // --- END OF NEW PAYMENT FLOW FUNCTIONS ---


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

                viewModelScope.launch { UserPreferences(context).saveSessionId(request.sessionId) }
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

    fun loadSession(sessionId: String, requestId: String) {
        _sessionId.value = sessionId
        // This will fetch the request data and start all the necessary listeners
        listenToActiveRequest(requestId)
    }
    private fun trackActiveRequestForCleanup() {
        viewModelScope.launch {
            var previousRequest: Request? = null // Holds the last known request

            _activeRequest.collect { currentRequest ->

                // Check if the state just changed from "had a request" to "no request"
                if (previousRequest != null && currentRequest == null) {

                    // --- THIS IS THE COMPANION'S CLEANUP ---
                    Log.d("MainViewModel", "Active request became null. Running Companion cleanup.")

                    // 1. Create the review trigger for the Companion
                    val reviewInfo = ReviewTriggerInfo(
                        isWalker = false, // We know this is the Companion
                        walkerId = previousRequest!!.walkerId,
                        companionId = previousRequest!!.companionId ?: "",
                        walkId = previousRequest!!.id
                    )

                    // 2. Run all the same cleanup logic
                    viewModelScope.launch {
                        UserPreferences(context).clearSessionId()
                    }
                    removeLocationListener()
                    removeSessionStatusListener()
                    _userStatus.value = "idle"
                    _locations.value = emptyMap()
                    _routePoints.value = emptyList()
                    _sessionStatus.value = "active"
                    _isCloseEnoughToStartWalk.value = false

                    // 3. Set the review trigger
                    _reviewTrigger.value = reviewInfo

                    // 4. Set sessionId to null TO TRIGGER NAVIGATION
                    _sessionId.value = null
                }

                // Update the 'previous' value for the next collection
                previousRequest = currentRequest
            }
        }
    }
}