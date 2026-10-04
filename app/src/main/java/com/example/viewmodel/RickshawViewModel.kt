package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookingEntity
import com.example.data.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RickshawViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: BookingRepository
  private val firebaseAuth: FirebaseAuth? = try {
    FirebaseAuth.getInstance()
  } catch (e: Exception) {
    null
  }

  private val firestore: FirebaseFirestore? = try {
    FirebaseFirestore.getInstance()
  } catch (e: Exception) {
    null
  }

  private val _firestoreBookings = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val firestoreBookings: StateFlow<List<Map<String, Any>>> = _firestoreBookings.asStateFlow()

  // Ratings & Reviews State
  var userRating = MutableStateFlow(5)
  var userReviewText = MutableStateFlow("")

  private val _reviews = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val reviews: StateFlow<List<Map<String, Any>>> = _reviews.asStateFlow()

  private val _averageRating = MutableStateFlow(4.9)
  val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

  init {
    val dao = AppDatabase.getDatabase(application).bookingDao()
    repository = BookingRepository(dao)
    fetchFirestoreBookings()
    fetchReviews()
  }

  fun fetchFirestoreBookings() {
    val user = firebaseAuth?.currentUser
    val fs = firestore
    if (user != null && fs != null) {
      fs.collection("bookings")
        .whereEqualTo("userId", user.uid)
        .get()
        .addOnSuccessListener { result ->
          val list = result.documents.map { doc -> doc.data ?: emptyMap() }
          _firestoreBookings.value = list
        }
    } else {
      _firestoreBookings.value = emptyList()
    }
  }

  fun fetchReviews() {
    val fs = firestore
    if (fs != null) {
      fs.collection("reviews")
        .get()
        .addOnSuccessListener { result ->
          val list = result.documents.map { doc -> doc.data ?: emptyMap() }
          _reviews.value = list
          if (list.isNotEmpty()) {
            val avg = list.mapNotNull { (it["rating"] as? Number)?.toDouble() }.average()
            if (!avg.isNaN()) {
              _averageRating.value = String.format(java.util.Locale.getDefault(), "%.1f", avg).toDouble()
            }
          }
        }
    }
  }

  fun submitReview(onSuccess: () -> Unit) {
    val user = firebaseAuth?.currentUser
    val ratingVal = userRating.value
    val reviewTxt = userReviewText.value.trim()

    val reviewMap = hashMapOf(
      "userId" to (user?.uid ?: "anonymous"),
      "userEmail" to (user?.email ?: "guest"),
      "userName" to (user?.displayName?.substringBefore(" | ") ?: "Valued Customer"),
      "rating" to ratingVal,
      "review" to reviewTxt,
      "date" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    )

    val fs = firestore
    if (fs != null) {
      fs.collection("reviews").add(reviewMap).addOnSuccessListener {
        userReviewText.value = ""
        userRating.value = 5
        fetchReviews()
        onSuccess()
      }
    }
  }

  fun rateOnPlayStore(context: Context) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
      context.startActivity(intent)
    } catch (e: Exception) {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
        context.startActivity(intent)
      } catch (ex: Exception) {
        // Ignore
      }
    }
  }

  val bookings: StateFlow<List<BookingEntity>> =
    repository.allBookings.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Theme State
  var isDarkMode = MutableStateFlow(false)
  fun toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
  }

  // Auth State
  private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth?.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  var authEmail = MutableStateFlow("")
  var authPassword = MutableStateFlow("")
  var authConfirmPassword = MutableStateFlow("")
  var authName = MutableStateFlow("")
  var authMobile = MutableStateFlow("")
  var authAddress = MutableStateFlow("")
  var authError = MutableStateFlow<String?>(null)
  var authLoading = MutableStateFlow(false)

  fun signIn(onSuccess: () -> Unit) {
    val email = authEmail.value.trim()
    val pass = authPassword.value.trim()
    if (email.isEmpty() || pass.isEmpty()) {
      authError.value = "Please enter email and password."
      return
    }
    authLoading.value = true
    authError.value = null

    if (firebaseAuth != null) {
      firebaseAuth.signInWithEmailAndPassword(email, pass)
        .addOnCompleteListener { task ->
          authLoading.value = false
          if (task.isSuccessful) {
            _currentUser.value = firebaseAuth.currentUser
            authEmail.value = ""
            authPassword.value = ""
            fetchFirestoreBookings()
            fetchReviews()
            onSuccess()
          } else {
            authError.value = task.exception?.localizedMessage ?: "Sign in failed"
          }
        }
    } else {
      authLoading.value = false
      authError.value = "Firebase Auth not initialized."
    }
  }

  fun signUp(onSuccess: () -> Unit) {
    val email = authEmail.value.trim()
    val pass = authPassword.value.trim()
    val confirmPass = authConfirmPassword.value.trim()
    val name = authName.value.trim()
    val mobile = authMobile.value.trim()
    val address = authAddress.value.trim()

    if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty() || name.isEmpty() || mobile.isEmpty() || address.isEmpty()) {
      authError.value = "All fields are mandatory."
      return
    }
    if (pass != confirmPass) {
      authError.value = "Passwords do not match."
      return
    }
    authLoading.value = true
    authError.value = null

    if (firebaseAuth != null) {
      firebaseAuth.createUserWithEmailAndPassword(email, pass)
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            val user = firebaseAuth.currentUser
            if (user != null) {
              val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("$name | $mobile | $address")
                .build()
              user.updateProfile(profileUpdates).addOnCompleteListener {
                authLoading.value = false
                _currentUser.value = firebaseAuth.currentUser
                authEmail.value = ""
                authPassword.value = ""
                authConfirmPassword.value = ""
                authName.value = ""
                authMobile.value = ""
                authAddress.value = ""
                fetchFirestoreBookings()
                fetchReviews()
                onSuccess()
              }
            } else {
              authLoading.value = false
              _currentUser.value = user
              authEmail.value = ""
              authPassword.value = ""
              authConfirmPassword.value = ""
              authName.value = ""
              authMobile.value = ""
              authAddress.value = ""
              fetchFirestoreBookings()
              fetchReviews()
              onSuccess()
            }
          } else {
            authLoading.value = false
            authError.value = task.exception?.localizedMessage ?: "Sign up failed"
          }
        }
    } else {
      authLoading.value = false
      authError.value = "Firebase Auth not initialized."
    }
  }

  fun signOut() {
    firebaseAuth?.signOut()
    _currentUser.value = null
    _firestoreBookings.value = emptyList()
  }

  // Booking Form State
  var pickup = MutableStateFlow("")
  var pickupLink = MutableStateFlow("")
  var drop = MutableStateFlow("")
  var dropLink = MutableStateFlow("")
  var bhadaAmount = MutableStateFlow("")
  var notes = MutableStateFlow("")

  // Fare Calculator State
  var calcDistanceKm = MutableStateFlow("5")
  var hasLuggage = MutableStateFlow(false)
  var isNightRide = MutableStateFlow(false)

  private val _calculatedFareResult = MutableStateFlow(120)
  val calculatedFareResult: StateFlow<Int> = _calculatedFareResult.asStateFlow()

  fun computeFare() {
    val km = calcDistanceKm.value.toDoubleOrNull() ?: 5.0
    var fare = if (km <= 2.0) 30 else 30 + ((km - 2.0) * 15).toInt()
    if (hasLuggage.value) fare += 20
    if (isNightRide.value) fare += 30
    _calculatedFareResult.value = fare.coerceAtLeast(30)
  }

  fun applyCalculatedFare() {
    bhadaAmount.value = _calculatedFareResult.value.toString()
  }

  fun bookViaWhatsApp(context: Context, onComplete: () -> Unit) {
    val p = pickup.value.trim()
    val pLink = pickupLink.value.trim()
    val d = drop.value.trim()
    val dLink = dropLink.value.trim()
    val b = bhadaAmount.value.trim()
    val n = notes.value.trim()

    val message = buildString {
      append("🛺 *New Auto Rickshaw Booking*\n\n")
      append("📍 *Pickup:* ${if (p.isNotEmpty()) p else "Not specified"}\n")
      if (pLink.isNotEmpty()) append("🔗 *Pickup Map Link:* $pLink\n")
      append("🏁 *Drop:* ${if (d.isNotEmpty()) d else "Not specified"}\n")
      if (dLink.isNotEmpty()) append("🔗 *Drop Map Link:* $dLink\n")
      if (n.isNotEmpty()) {
        append("📝 *Notes:* $n\n")
      }
      append("\nBooked via Rajak Rickshawwala App. Please confirm ride!")
    }

    val user = firebaseAuth?.currentUser
    val bookingMap = hashMapOf(
      "userId" to (user?.uid ?: "anonymous"),
      "userEmail" to (user?.email ?: "guest"),
      "pickup" to if (p.isNotEmpty()) p else "Current Location",
      "pickupLink" to pLink,
      "drop" to if (d.isNotEmpty()) d else "Destination",
      "dropLink" to dLink,
      "bhada" to if (b.isNotEmpty()) "₹$b" else "Discuss",
      "notes" to n,
      "status" to "Completed",
      "date" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    )

    val fs = firestore
    if (fs != null) {
      fs.collection("bookings").add(bookingMap).addOnSuccessListener {
        fetchFirestoreBookings()
      }
    }

    viewModelScope.launch {
      repository.insertBooking(
        BookingEntity(
          pickup = if (p.isNotEmpty()) p else "Current Location",
          drop = if (d.isNotEmpty()) d else "Destination",
          bhadaAmount = if (b.isNotEmpty()) "₹$b" else "Discuss",
          notes = n,
          date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
      )
    }

    try {
      val encodedMessage = URLEncoder.encode(message, "UTF-8")
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/918200019788?text=$encodedMessage"))
      context.startActivity(intent)
    } catch (e: Exception) {
      // Fallback
    }
    onComplete()
  }

  fun callDriver(context: Context) {
    try {
      val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918200019788"))
      context.startActivity(intent)
    } catch (e: Exception) {
      // Ignore
    }
  }

  fun payViaUpi(context: Context) {
    try {
      val uri = Uri.parse("upi://pay?pa=8200019788@paytm&pn=Rajak%20Parmar&cu=INR")
      val intent = Intent(Intent.ACTION_VIEW, uri)
      context.startActivity(intent)
    } catch (e: Exception) {
      // Ignore
    }
  }

  fun shareApp(context: Context) {
    try {
      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(
          Intent.EXTRA_TEXT,
          "Book Rajak Rickshawwala (Rajak Parmar - +918200019788) for fast, safe & reliable auto rickshaw transport! Download the app now."
        )
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share App via"))
    } catch (e: Exception) {
      // Ignore
    }
  }

  fun deleteBooking(id: Long) {
    viewModelScope.launch {
      repository.deleteBooking(id)
    }
  }
}
