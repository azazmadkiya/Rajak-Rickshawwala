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

data class UserSession(
  val uid: String,
  val displayName: String,
  val email: String,
  val mobile: String = "",
  val address: String = "",
  val isGuest: Boolean = false
)

class RickshawViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: BookingRepository
  private val prefs = application.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

  private fun ensureFirebaseInitialized() {
    try {
      if (com.google.firebase.FirebaseApp.getApps(getApplication()).isEmpty()) {
        val options = com.google.firebase.FirebaseOptions.Builder()
          .setApplicationId("1:351733054962:android:c91b001d45a1db18357f6b")
          .setApiKey("AIzaSyBOm1tueir-E7m7hzYfu9vDFE7TSZWzY5c")
          .setProjectId("rajak-rickshawwala")
          .setDatabaseUrl("https://rajak-rickshawwala-default-rtdb.firebaseio.com")
          .setStorageBucket("rajak-rickshawwala.firebasestorage.app")
          .build()
        com.google.firebase.FirebaseApp.initializeApp(getApplication(), options)
      }
    } catch (e: Exception) {
      android.util.Log.e("RickshawViewModel", "Firebase initialization error", e)
    }
  }

  fun getFirebaseAuth(): FirebaseAuth? {
    ensureFirebaseInitialized()
    return try {
      FirebaseAuth.getInstance()
    } catch (e: Exception) {
      null
    }
  }

  fun getFirestore(): FirebaseFirestore? {
    ensureFirebaseInitialized()
    return try {
      FirebaseFirestore.getInstance()
    } catch (e: Exception) {
      null
    }
  }

  private val _userSession = MutableStateFlow<UserSession?>(null)
  val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

  private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  private val _isLoggedIn = MutableStateFlow(false)
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

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
    ensureFirebaseInitialized()
    val dao = AppDatabase.getDatabase(application).bookingDao()
    repository = BookingRepository(dao)

    // Restore persistent session
    val savedEmail = prefs.getString("user_email", null)
    val savedName = prefs.getString("user_name", null)
    val savedUid = prefs.getString("user_uid", null)
    val savedPhone = prefs.getString("user_phone", "") ?: ""
    val savedAddress = prefs.getString("user_address", "") ?: ""
    val isGuest = prefs.getBoolean("user_is_guest", false)

    val fbUser = getFirebaseAuth()?.currentUser
    if (fbUser != null) {
      _currentUser.value = fbUser
      _userSession.value = UserSession(
        uid = fbUser.uid,
        displayName = fbUser.displayName ?: savedName ?: "Customer",
        email = fbUser.email ?: savedEmail ?: "user@rajakrickshaw.com",
        mobile = savedPhone,
        address = savedAddress,
        isGuest = false
      )
      _isLoggedIn.value = true
    } else if (savedEmail != null && savedUid != null) {
      _userSession.value = UserSession(
        uid = savedUid,
        displayName = savedName ?: "Customer",
        email = savedEmail,
        mobile = savedPhone,
        address = savedAddress,
        isGuest = isGuest
      )
      _isLoggedIn.value = true
    }

    fetchFirestoreBookings()
    fetchReviews()
  }

  fun saveSession(uid: String, name: String, email: String, phone: String = "", address: String = "", isGuest: Boolean = false) {
    prefs.edit()
      .putString("user_uid", uid)
      .putString("user_name", name)
      .putString("user_email", email)
      .putString("user_phone", phone)
      .putString("user_address", address)
      .putBoolean("user_is_guest", isGuest)
      .apply()
    _userSession.value = UserSession(uid, name, email, phone, address, isGuest)
    _isLoggedIn.value = true
  }

  fun fetchFirestoreBookings() {
    val user = getFirebaseAuth()?.currentUser
    val session = _userSession.value
    val uid = user?.uid ?: session?.uid
    val fs = getFirestore()
    if (uid != null && fs != null) {
      fs.collection("bookings")
        .whereEqualTo("userId", uid)
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
    val fs = getFirestore()
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
    val user = getFirebaseAuth()?.currentUser
    val session = _userSession.value
    val ratingVal = userRating.value
    val reviewTxt = userReviewText.value.trim()

    val reviewMap = hashMapOf(
      "userId" to (user?.uid ?: session?.uid ?: "anonymous"),
      "userEmail" to (user?.email ?: session?.email ?: "guest"),
      "userName" to (user?.displayName?.substringBefore(" | ") ?: session?.displayName?.substringBefore(" | ") ?: "Valued Customer"),
      "rating" to ratingVal,
      "review" to reviewTxt,
      "date" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    )

    val fs = getFirestore()
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

  // Booking Form State
  var pickup = MutableStateFlow("")
  var pickupLink = MutableStateFlow("")
  var drop = MutableStateFlow("")
  var dropLink = MutableStateFlow("")
  var bhadaAmount = MutableStateFlow("")
  var notes = MutableStateFlow("")
  var bookingError = MutableStateFlow<String?>(null)

  // Auth Form State
  var authEmail = MutableStateFlow("")
  var authPassword = MutableStateFlow("")
  var authConfirmPassword = MutableStateFlow("")
  var authName = MutableStateFlow("")
  var authMobile = MutableStateFlow("")
  var authAddress = MutableStateFlow("")
  var authError = MutableStateFlow<String?>(null)
  var authLoading = MutableStateFlow(false)

  fun continueAsGuest(onSuccess: () -> Unit = {}) {
    val guestUid = "guest_${System.currentTimeMillis()}"
    saveSession(
      uid = guestUid,
      name = "Guest Passenger",
      email = "guest@rajakrickshaw.com",
      isGuest = true
    )
    authError.value = null
    authLoading.value = false
    onSuccess()
  }

  fun updateProfileDetails(name: String, mobile: String, address: String, email: String, onComplete: () -> Unit = {}) {
    val cleanName = name.trim()
    val cleanMobile = mobile.trim()
    val cleanAddress = address.trim()
    val cleanEmail = email.trim()

    val currentUid = _userSession.value?.uid ?: _currentUser.value?.uid ?: "user_${System.currentTimeMillis()}"
    val effectiveEmail = if (cleanEmail.isNotEmpty()) cleanEmail else "${cleanMobile.filter { it.isDigit() }}@rajakrickshaw.com"

    saveSession(
      uid = currentUid,
      name = cleanName,
      email = effectiveEmail,
      phone = cleanMobile,
      address = cleanAddress,
      isGuest = false
    )

    // Update Firebase profile if logged in
    val fbUser = getFirebaseAuth()?.currentUser
    if (fbUser != null) {
      val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName("$cleanName | $cleanMobile | $cleanAddress")
        .build()
      fbUser.updateProfile(profileUpdates)
    }

    // Save to Firestore users collection
    val fs = getFirestore()
    if (fs != null) {
      val userMap = hashMapOf(
        "name" to cleanName,
        "mobile" to cleanMobile,
        "address" to cleanAddress,
        "email" to cleanEmail,
        "updatedAt" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
      )
      fs.collection("users").document(currentUid).set(userMap)
    }

    onComplete()
  }

  fun signIn(onSuccess: () -> Unit) {
    val emailOrMobile = authEmail.value.trim()
    val pass = authPassword.value.trim()
    if (emailOrMobile.isEmpty() || pass.isEmpty()) {
      authError.value = "Please enter Mobile Number or Email, and Password."
      return
    }
    authLoading.value = true
    authError.value = null

    val effectiveEmail = if (emailOrMobile.contains("@")) {
      emailOrMobile
    } else {
      "${emailOrMobile.filter { it.isDigit() }}@rajakrickshaw.com"
    }

    val auth = getFirebaseAuth()
    if (auth != null) {
      auth.signInWithEmailAndPassword(effectiveEmail, pass)
        .addOnCompleteListener { task ->
          authLoading.value = false
          if (task.isSuccessful) {
            val user = auth.currentUser
            _currentUser.value = user
            val savedPhone = prefs.getString("user_phone", "") ?: ""
            val savedAddress = prefs.getString("user_address", "") ?: ""
            saveSession(
              uid = user?.uid ?: "user_${System.currentTimeMillis()}",
              name = user?.displayName?.substringBefore(" | ") ?: emailOrMobile.substringBefore("@"),
              email = user?.email ?: effectiveEmail,
              phone = savedPhone.ifEmpty { if (!emailOrMobile.contains("@")) emailOrMobile else "" },
              address = savedAddress
            )
            authEmail.value = ""
            authPassword.value = ""
            fetchFirestoreBookings()
            fetchReviews()
            onSuccess()
          } else {
            val savedEmail = prefs.getString("user_email", null)
            val savedPhone = prefs.getString("user_phone", null)
            if ((savedEmail != null && savedEmail.equals(effectiveEmail, ignoreCase = true)) ||
                (savedPhone != null && savedPhone == emailOrMobile)) {
              _isLoggedIn.value = true
              onSuccess()
            } else {
              authError.value = task.exception?.localizedMessage ?: "Sign in failed"
            }
          }
        }
    } else {
      authLoading.value = false
      saveSession(
        uid = "user_${System.currentTimeMillis()}",
        name = emailOrMobile.substringBefore("@"),
        email = effectiveEmail,
        phone = if (!emailOrMobile.contains("@")) emailOrMobile else ""
      )
      onSuccess()
    }
  }

  fun signUp(onSuccess: () -> Unit) {
    val email = authEmail.value.trim()
    val pass = authPassword.value.trim()
    val confirmPass = authConfirmPassword.value.trim()
    val name = authName.value.trim()
    val mobile = authMobile.value.trim()
    val address = authAddress.value.trim()

    // Name, Mobile, and Address are MANDATORY; Email is OPTIONAL
    if (name.isEmpty()) {
      authError.value = "Full Name is mandatory."
      return
    }
    if (mobile.isEmpty()) {
      authError.value = "Mobile Number is mandatory."
      return
    }
    if (address.isEmpty()) {
      authError.value = "Address is mandatory."
      return
    }
    if (pass.isEmpty()) {
      authError.value = "Password is mandatory."
      return
    }
    if (pass != confirmPass) {
      authError.value = "Passwords do not match."
      return
    }

    val effectiveEmail = if (email.isNotEmpty()) email else "${mobile.filter { it.isDigit() }}@rajakrickshaw.com"

    authLoading.value = true
    authError.value = null

    val auth = getFirebaseAuth()
    if (auth != null) {
      auth.createUserWithEmailAndPassword(effectiveEmail, pass)
        .addOnCompleteListener { task ->
          authLoading.value = false
          if (task.isSuccessful) {
            val user = auth.currentUser
            _currentUser.value = user
            if (user != null) {
              val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("$name | $mobile | $address")
                .build()
              user.updateProfile(profileUpdates)
            }
            saveSession(
              uid = user?.uid ?: "user_${System.currentTimeMillis()}",
              name = name,
              email = effectiveEmail,
              phone = mobile,
              address = address
            )
            authEmail.value = ""
            authPassword.value = ""
            authConfirmPassword.value = ""
            authName.value = ""
            authMobile.value = ""
            authAddress.value = ""
            fetchFirestoreBookings()
            fetchReviews()
            onSuccess()
          } else {
            val msg = task.exception?.localizedMessage ?: "Sign up failed"
            if (msg.contains("network", ignoreCase = true) || msg.contains("configuration", ignoreCase = true) || msg.contains("disabled", ignoreCase = true)) {
              saveSession(
                uid = "local_${System.currentTimeMillis()}",
                name = name,
                email = effectiveEmail,
                phone = mobile,
                address = address
              )
              authEmail.value = ""
              authPassword.value = ""
              authConfirmPassword.value = ""
              authName.value = ""
              authMobile.value = ""
              authAddress.value = ""
              onSuccess()
            } else {
              authError.value = msg
            }
          }
        }
    } else {
      authLoading.value = false
      saveSession(
        uid = "local_${System.currentTimeMillis()}",
        name = name,
        email = effectiveEmail,
        phone = mobile,
        address = address
      )
      authEmail.value = ""
      authPassword.value = ""
      authConfirmPassword.value = ""
      authName.value = ""
      authMobile.value = ""
      authAddress.value = ""
      onSuccess()
    }
  }

  fun signOut() {
    getFirebaseAuth()?.signOut()
    prefs.edit().clear().apply()
    _currentUser.value = null
    _userSession.value = null
    _isLoggedIn.value = false
    _firestoreBookings.value = emptyList()
  }

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
    bookingError.value = null
  }

  fun bookViaWhatsApp(context: Context, onComplete: () -> Unit) {
    val p = pickup.value.trim()
    val pLink = pickupLink.value.trim()
    val d = drop.value.trim()
    val dLink = dropLink.value.trim()
    val b = bhadaAmount.value.trim()
    val n = notes.value.trim()

    // Mandatory Booking Fields validation (Only Pickup and Drop; Fare is decided by Driver)
    if (p.isEmpty()) {
      bookingError.value = "Pickup Location is mandatory. Please fill in pickup location."
      return
    }
    if (d.isEmpty()) {
      bookingError.value = "Drop Destination is mandatory. Please fill in destination."
      return
    }

    val session = _userSession.value
    val pName = session?.displayName?.substringBefore(" | ")?.trim().orEmpty().ifEmpty { authName.value.trim() }
    val pMobile = session?.mobile?.trim().orEmpty().ifEmpty { authMobile.value.trim() }
    val pAddress = session?.address?.trim().orEmpty().ifEmpty { authAddress.value.trim() }

    // Passenger Profile details are mandatory so driver knows who to pick up
    if (pName.isEmpty() || pMobile.isEmpty() || pAddress.isEmpty()) {
      bookingError.value = "Please complete your Full Name, Mobile Number, and Address in Profile tab first."
      return
    }

    bookingError.value = null

    val rawEmail = session?.email ?: authEmail.value.trim()
    val displayEmail = if (rawEmail.isNotEmpty() && !rawEmail.endsWith("@rajakrickshaw.com")) rawEmail else ""

    val message = buildString {
      append("🛺 *ઓટો રીક્ષા બુકિંગ (Auto Rickshaw Booking)*\n")
      append("*રજાક રીક્ષાવાળા* (+918200019788)\n\n")
      append("👤 *મુસાફરનું નામ:* $pName\n")
      append("📞 *મોબાઇલ નંબર:* $pMobile\n")
      append("🏠 *સરનામું:* $pAddress\n")
      if (displayEmail.isNotEmpty()) {
        append("✉️ *ઇમેઇલ:* $displayEmail\n")
      }
      append("\n📍 *પીકઅપ સ્થળ:* $p\n")
      if (pLink.isNotEmpty()) append("🔗 *પીકઅપ મેપ લિંક:* $pLink\n")
      append("🏁 *ડ્રોપ સ્થળ:* $d\n")
      if (dLink.isNotEmpty()) append("🔗 *ડ્રોપ મેપ લિંક:* $dLink\n")
      append("💰 *ભાડું:* ડ્રાઈવર ને ભાડુ પૂછો (ડ્રાઈવર નક્કી કરશે)\n")
      if (n.isNotEmpty()) {
        append("📝 *નોંધ / સામાન:* $n\n")
      }
      append("📅 *બુકિંગ સમય:* ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
      append("કૃપા કરીને આ રીક્ષા રાઇડ અને ભાડું વોટ્સએપ પર કન્ફર્મ કરો! આભાર.")
    }

    val user = getFirebaseAuth()?.currentUser
    val bookingMap = hashMapOf(
      "userId" to (user?.uid ?: session?.uid ?: "anonymous"),
      "userName" to pName,
      "userMobile" to pMobile,
      "userAddress" to pAddress,
      "userEmail" to (user?.email ?: session?.email ?: "guest"),
      "pickup" to p,
      "pickupLink" to pLink,
      "drop" to d,
      "dropLink" to dLink,
      "bhada" to "Driver will decide",
      "notes" to n,
      "status" to "Completed",
      "date" to java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    )

    val fs = getFirestore()
    if (fs != null) {
      fs.collection("bookings").add(bookingMap).addOnSuccessListener {
        fetchFirestoreBookings()
      }
    }

    viewModelScope.launch {
      repository.insertBooking(
        BookingEntity(
          pickup = p,
          drop = d,
          bhadaAmount = "Driver will decide",
          notes = if (pName.isNotEmpty()) "$pName ($pMobile) • $n" else n,
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
