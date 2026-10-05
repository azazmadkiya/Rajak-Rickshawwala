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
import com.example.service.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

data class RegisteredUser(
  val uid: String = "",
  val name: String = "",
  val mobile: String = "",
  val address: String = "",
  val email: String = "",
  val role: String = "customer",
  val registeredAt: String = "",
  val lastActive: String = "",
  val deviceModel: String = "",
  val androidVersion: String = ""
)

data class AppNotification(
  val id: String = "",
  val title: String = "",
  val message: String = "",
  val targetUserId: String = "ALL", // "ALL" or specific user UID
  val targetUserName: String = "All Users",
  val sentBy: String = "azazmadkiya@gmail.com",
  val date: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)

class RickshawViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    val ADMIN_EMAILS = setOf("azazmadkiya@gmail.com", "admin@rajakrickshaw.com")
  }

  private val repository: BookingRepository
  private val prefs = application.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
  private var notificationListener: ListenerRegistration? = null
  private var usersListener: ListenerRegistration? = null

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

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _firestoreBookings = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val firestoreBookings: StateFlow<List<Map<String, Any>>> = _firestoreBookings.asStateFlow()

  // Admin Data State
  private val _allUsers = MutableStateFlow<List<RegisteredUser>>(emptyList())
  val allUsers: StateFlow<List<RegisteredUser>> = _allUsers.asStateFlow()

  private val _allBookingsAdmin = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val allBookingsAdmin: StateFlow<List<Map<String, Any>>> = _allBookingsAdmin.asStateFlow()

  private val _sentNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
  val sentNotifications: StateFlow<List<AppNotification>> = _sentNotifications.asStateFlow()

  private val _userNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
  val userNotifications: StateFlow<List<AppNotification>> = _userNotifications.asStateFlow()

  private val _unreadNotificationCount = MutableStateFlow(0)
  val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

  // Ratings & Reviews State
  var userRating = MutableStateFlow(5)
  var userReviewText = MutableStateFlow("")

  private val _reviews = MutableStateFlow<List<Map<String, Any>>>(emptyList())
  val reviews: StateFlow<List<Map<String, Any>>> = _reviews.asStateFlow()

  private val _averageRating = MutableStateFlow(4.9)
  val averageRating: StateFlow<Double> = _averageRating.asStateFlow()

  // Admin Notification Compose Form State
  var adminNotifTitle = MutableStateFlow("")
  var adminNotifMessage = MutableStateFlow("")
  var adminNotifTargetType = MutableStateFlow("ALL") // "ALL" or "SINGLE"
  var adminNotifSelectedUser = MutableStateFlow<RegisteredUser?>(null)
  var adminNotifSending = MutableStateFlow(false)
  var adminNotifStatusMessage = MutableStateFlow<String?>(null)

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
    val isAdminMode = prefs.getBoolean("user_is_admin", false)

    val fbUser = getFirebaseAuth()?.currentUser
    if (fbUser != null) {
      _currentUser.value = fbUser
      val email = fbUser.email ?: savedEmail ?: "user@rajakrickshaw.com"
      _userSession.value = UserSession(
        uid = fbUser.uid,
        displayName = fbUser.displayName ?: savedName ?: "Customer",
        email = email,
        mobile = savedPhone,
        address = savedAddress,
        isGuest = false
      )
      _isLoggedIn.value = true
      _isAdmin.value = checkIsAdmin(email) || isAdminMode
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
      _isAdmin.value = checkIsAdmin(savedEmail) || isAdminMode
    }

    fetchFirestoreBookings()
    fetchReviews()
    fetchAdminData()
    startNotificationListener()
    startUsersListener()
  }

  private fun checkIsAdmin(email: String?): Boolean {
    if (email.isNullOrBlank()) return false
    return ADMIN_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
  }

  fun toggleAdminMode(enable: Boolean) {
    prefs.edit().putBoolean("user_is_admin", enable).apply()
    _isAdmin.value = enable
    if (enable) {
      fetchAdminData()
    }
  }

  fun saveSession(uid: String, name: String, email: String, phone: String = "", address: String = "", isGuest: Boolean = false) {
    val isAdmin = checkIsAdmin(email)
    prefs.edit()
      .putString("user_uid", uid)
      .putString("user_name", name)
      .putString("user_email", email)
      .putString("user_phone", phone)
      .putString("user_address", address)
      .putBoolean("user_is_guest", isGuest)
      .putBoolean("user_is_admin", isAdmin)
      .apply()
    _userSession.value = UserSession(uid, name, email, phone, address, isGuest)
    _isLoggedIn.value = true
    _isAdmin.value = isAdmin

    // Persist to Firestore users collection
    saveUserToFirestore(uid, name, email, phone, address, isAdmin)
    fetchAdminData()
  }

  private fun saveUserToFirestore(uid: String, name: String, email: String, phone: String, address: String, isAdmin: Boolean) {
    val fs = getFirestore() ?: return
    try {
      val now = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
      val device = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
      val androidVer = "Android ${android.os.Build.VERSION.RELEASE}"
      val userMap = hashMapOf(
        "uid" to uid,
        "name" to name.ifEmpty { "Customer" },
        "mobile" to phone,
        "address" to address,
        "email" to email,
        "role" to if (isAdmin) "admin" else "customer",
        "registeredAt" to now,
        "lastActive" to now,
        "deviceModel" to device,
        "androidVersion" to androidVer
      )
      fs.collection("users").document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
    } catch (e: Exception) {
      android.util.Log.e("RickshawViewModel", "Failed to save user to Firestore", e)
    }
  }

  private fun startUsersListener() {
    val fs = getFirestore() ?: return
    try {
      usersListener?.remove()
      usersListener = fs.collection("users")
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
          val userList = snapshot.documents.map { doc ->
            RegisteredUser(
              uid = doc.getString("uid") ?: doc.id,
              name = doc.getString("name") ?: "Passenger",
              mobile = doc.getString("mobile") ?: "",
              address = doc.getString("address") ?: "",
              email = doc.getString("email") ?: "",
              role = doc.getString("role") ?: "customer",
              registeredAt = doc.getString("registeredAt") ?: "Recently",
              lastActive = doc.getString("lastActive") ?: "Active",
              deviceModel = doc.getString("deviceModel") ?: "",
              androidVersion = doc.getString("androidVersion") ?: ""
            )
          }.sortedByDescending { it.role == "admin" }

          if (userList.isNotEmpty()) {
            _allUsers.value = userList
          }
        }
    } catch (e: Exception) {
      android.util.Log.e("RickshawViewModel", "Users listener error", e)
    }
  }

  fun fetchAdminData() {
    val fs = getFirestore() ?: return

    // 1. Fetch all registered users
    fs.collection("users").get()
      .addOnSuccessListener { result ->
        val userList = result.documents.map { doc ->
          RegisteredUser(
            uid = doc.getString("uid") ?: doc.id,
            name = doc.getString("name") ?: "Passenger",
            mobile = doc.getString("mobile") ?: "",
            address = doc.getString("address") ?: "",
            email = doc.getString("email") ?: "",
            role = doc.getString("role") ?: "customer",
            registeredAt = doc.getString("registeredAt") ?: "Recently",
            lastActive = doc.getString("lastActive") ?: "Active",
            deviceModel = doc.getString("deviceModel") ?: "",
            androidVersion = doc.getString("androidVersion") ?: ""
          )
        }.sortedByDescending { it.role == "admin" }

        if (userList.isEmpty()) {
          val fallback = mutableListOf<RegisteredUser>()
          _userSession.value?.let { s ->
            fallback.add(
              RegisteredUser(
                uid = s.uid,
                name = s.displayName,
                mobile = s.mobile,
                address = s.address,
                email = s.email,
                role = if (checkIsAdmin(s.email)) "admin" else "customer",
                registeredAt = "Active Today",
                lastActive = "Online"
              )
            )
          }
          fallback.add(
            RegisteredUser(
              uid = "admin_azaz",
              name = "Azaz Madkiya (Admin)",
              mobile = "+91 82000 19788",
              address = "Rajkot, Gujarat",
              email = "azazmadkiya@gmail.com",
              role = "admin",
              registeredAt = "01 Oct 2026",
              lastActive = "Online"
            )
          )
          _allUsers.value = fallback
        } else {
          _allUsers.value = userList
        }
      }
      .addOnFailureListener {
        val fallback = mutableListOf<RegisteredUser>()
        _userSession.value?.let { s ->
          fallback.add(
            RegisteredUser(
              uid = s.uid,
              name = s.displayName,
              mobile = s.mobile,
              address = s.address,
              email = s.email,
              role = if (checkIsAdmin(s.email)) "admin" else "customer",
              registeredAt = "Active Today",
              lastActive = "Online"
            )
          )
        }
        fallback.add(
          RegisteredUser(
            uid = "admin_azaz",
            name = "Azaz Madkiya (Admin)",
            mobile = "+91 82000 19788",
            address = "Rajkot, Gujarat",
            email = "azazmadkiya@gmail.com",
            role = "admin",
            registeredAt = "01 Oct 2026",
            lastActive = "Online"
          )
        )
        _allUsers.value = fallback
      }

    // 2. Fetch all bookings for admin
    fs.collection("bookings").get()
      .addOnSuccessListener { result ->
        _allBookingsAdmin.value = result.documents.map { doc -> doc.data ?: emptyMap() }
      }

    // 3. Fetch all notifications
    fs.collection("notifications").get()
      .addOnSuccessListener { result ->
        val notifs = result.documents.map { doc ->
          AppNotification(
            id = doc.id,
            title = doc.getString("title") ?: "",
            message = doc.getString("message") ?: "",
            targetUserId = doc.getString("targetUserId") ?: "ALL",
            targetUserName = doc.getString("targetUserName") ?: "All Users",
            sentBy = doc.getString("sentBy") ?: "Admin",
            date = doc.getString("date") ?: "",
            timestamp = doc.getLong("timestamp") ?: 0L,
            isRead = doc.getBoolean("isRead") ?: false
          )
        }.sortedByDescending { it.timestamp }
        _sentNotifications.value = notifs
        filterUserNotifications(notifs)
      }
  }

  private fun startNotificationListener() {
    val fs = getFirestore() ?: return
    try {
      notificationListener?.remove()
      notificationListener = fs.collection("notifications")
        .addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) return@addSnapshotListener
          val notifs = snapshot.documents.map { doc ->
            AppNotification(
              id = doc.id,
              title = doc.getString("title") ?: "",
              message = doc.getString("message") ?: "",
              targetUserId = doc.getString("targetUserId") ?: "ALL",
              targetUserName = doc.getString("targetUserName") ?: "All Users",
              sentBy = doc.getString("sentBy") ?: "Admin",
              date = doc.getString("date") ?: "",
              timestamp = doc.getLong("timestamp") ?: 0L,
              isRead = doc.getBoolean("isRead") ?: false
            )
          }.sortedByDescending { it.timestamp }
          _sentNotifications.value = notifs
          filterUserNotifications(notifs)
        }
    } catch (e: Exception) {
      android.util.Log.e("RickshawViewModel", "Notification listener error", e)
    }
  }

  private fun filterUserNotifications(allNotifs: List<AppNotification>) {
    val currentUid = _userSession.value?.uid ?: _currentUser.value?.uid ?: "guest"
    val filtered = allNotifs.filter { it.targetUserId == "ALL" || it.targetUserId == currentUid }
    _userNotifications.value = filtered
    _unreadNotificationCount.value = filtered.count { !it.isRead }
  }

  fun sendNotificationFromAdmin(
    context: Context,
    title: String,
    message: String,
    targetUserId: String,
    targetUserName: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    val cleanTitle = title.trim()
    val cleanMessage = message.trim()

    if (cleanTitle.isEmpty()) {
      onError("Notification title cannot be empty.")
      return
    }
    if (cleanMessage.isEmpty()) {
      onError("Notification message cannot be empty.")
      return
    }

    adminNotifSending.value = true
    adminNotifStatusMessage.value = null

    val now = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    val timestamp = System.currentTimeMillis()
    val adminSender = _userSession.value?.email ?: "azazmadkiya@gmail.com"

    val notifMap = hashMapOf(
      "title" to cleanTitle,
      "message" to cleanMessage,
      "targetUserId" to targetUserId,
      "targetUserName" to targetUserName,
      "sentBy" to adminSender,
      "date" to now,
      "timestamp" to timestamp,
      "isRead" to false
    )

    // 1. Show immediate Android system notification on the device
    NotificationHelper.showNotification(
      context = context,
      title = cleanTitle,
      body = if (targetUserId == "ALL") "📢 [To All Users] $cleanMessage" else "👤 [To: $targetUserName] $cleanMessage",
      notificationId = timestamp.toInt()
    )

    // 2. Persist to Firestore notifications collection
    val fs = getFirestore()
    if (fs != null) {
      fs.collection("notifications").add(notifMap)
        .addOnSuccessListener { docRef ->
          adminNotifSending.value = false
          val newNotif = AppNotification(
            id = docRef.id,
            title = cleanTitle,
            message = cleanMessage,
            targetUserId = targetUserId,
            targetUserName = targetUserName,
            sentBy = adminSender,
            date = now,
            timestamp = timestamp
          )
          val updated = listOf(newNotif) + _sentNotifications.value
          _sentNotifications.value = updated
          filterUserNotifications(updated)

          adminNotifTitle.value = ""
          adminNotifMessage.value = ""
          adminNotifStatusMessage.value = "Notification sent successfully to $targetUserName!"
          onSuccess("Notification delivered successfully!")
        }
        .addOnFailureListener { e ->
          adminNotifSending.value = false
          val newNotif = AppNotification(
            id = "local_${System.currentTimeMillis()}",
            title = cleanTitle,
            message = cleanMessage,
            targetUserId = targetUserId,
            targetUserName = targetUserName,
            sentBy = adminSender,
            date = now,
            timestamp = timestamp
          )
          val updated = listOf(newNotif) + _sentNotifications.value
          _sentNotifications.value = updated
          filterUserNotifications(updated)

          adminNotifTitle.value = ""
          adminNotifMessage.value = ""
          adminNotifStatusMessage.value = "Notification sent locally to $targetUserName!"
          onSuccess("Notification sent!")
        }
    } else {
      adminNotifSending.value = false
      val newNotif = AppNotification(
        id = "local_${System.currentTimeMillis()}",
        title = cleanTitle,
        message = cleanMessage,
        targetUserId = targetUserId,
        targetUserName = targetUserName,
        sentBy = adminSender,
        date = now,
        timestamp = timestamp
      )
      val updated = listOf(newNotif) + _sentNotifications.value
      _sentNotifications.value = updated
      filterUserNotifications(updated)

      adminNotifTitle.value = ""
      adminNotifMessage.value = ""
      adminNotifStatusMessage.value = "Notification sent locally to $targetUserName!"
      onSuccess("Notification sent!")
    }
  }

  fun deleteNotification(notificationId: String, onComplete: () -> Unit = {}) {
    val fs = getFirestore()
    if (fs != null && !notificationId.startsWith("local_")) {
      fs.collection("notifications").document(notificationId).delete()
    }
    val updated = _sentNotifications.value.filter { it.id != notificationId }
    _sentNotifications.value = updated
    filterUserNotifications(updated)
    onComplete()
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
            fetchAdminData()
            onSuccess()
          } else {
            val savedEmail = prefs.getString("user_email", null)
            val savedPhone = prefs.getString("user_phone", null)
            if ((savedEmail != null && savedEmail.equals(effectiveEmail, ignoreCase = true)) ||
                (savedPhone != null && savedPhone == emailOrMobile)) {
              _isLoggedIn.value = true
              _isAdmin.value = checkIsAdmin(effectiveEmail)
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
            fetchAdminData()
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
    _isAdmin.value = false
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
    val n = notes.value.trim()

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
        fetchAdminData()
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

  fun callDriver(context: Context, number: String = "+918200019788") {
    try {
      val cleanNumber = number.filter { it.isDigit() || it == '+' }
      val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
      context.startActivity(intent)
    } catch (e: Exception) {
      // Ignore
    }
  }

  fun openWhatsAppChat(context: Context, number: String = "+918200019788", customMsg: String = "") {
    try {
      val cleanNumber = number.filter { it.isDigit() }
      val target = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
      val encoded = URLEncoder.encode(customMsg.ifEmpty { "Hello from Rajak Rickshawwala App" }, "UTF-8")
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$target?text=$encoded"))
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
      val appId = context.packageName.ifEmpty { "com.rajak.rickshawwala.azaz" }
      val playStoreLink = "https://play.google.com/store/apps/details?id=$appId"
      val shareText = buildString {
        append("🛺 *નમસ્તે! રજાક રીક્ષાવાળા (Rajak Rickshawwala)*\n\n")
        append("શું તમારે ઝડપી, સુરક્ષિત અને વિશ્વાસપાત્ર ઓટો રીક્ષા સવારી જોઈએ છે?\n\n")
        append("હવે ઘરે બેઠા સરળતાથી રીક્ષા બુક કરો:\n")
        append("✅ સરળ અને ઝડપી ઓટો રીક્ષા બુકિંગ (WhatsApp & Call)\n")
        append("✅ સમયસર પીકઅપ અને સુરક્ષિત મુસાફરી\n")
        append("✅ વાજબી ભાડું અને મૈત્રીપૂર્ણ ડ્રાઇવર સેવા\n")
        append("✅ લાઈવ GPS લોકેશન સાથે બુકિંગ સુવિધા\n")
        append("📞 ડ્રાઇવર: રજાક પરમાર (+91 82000 19788)\n\n")
        append("📲 *આજે જ અમારી ઑફિશિયલ ઍપ Google Play Store પરથી ડાઉનલોડ કરો:*\n")
        append(playStoreLink)
        append("\n\nતમારા પરિવાર અને મિત્રો સાથે પણ આ ઍપ જરૂર શેર કરો! 🙏")
      }

      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "રજાક રીક્ષાવાળા (Rajak Rickshawwala)")
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
      context.startActivity(Intent.createChooser(shareIntent, "ઍપ શેર કરો (Share App via)"))
    } catch (e: Exception) {
      // Ignore
    }
  }

  fun deleteBooking(id: Long) {
    viewModelScope.launch {
      repository.deleteBooking(id)
    }
  }

  override fun onCleared() {
    super.onCleared()
    notificationListener?.remove()
    usersListener?.remove()
  }
}
