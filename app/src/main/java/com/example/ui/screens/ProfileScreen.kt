package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.RickshawViewModel

@Composable
fun ProfileScreen(viewModel: RickshawViewModel) {
  val context = LocalContext.current
  val currentUser by viewModel.currentUser.collectAsState()
  val userSession by viewModel.userSession.collectAsState()
  val isLoggedIn by viewModel.isLoggedIn.collectAsState()
  val email by viewModel.authEmail.collectAsState()
  val password by viewModel.authPassword.collectAsState()
  val confirmPassword by viewModel.authConfirmPassword.collectAsState()
  val name by viewModel.authName.collectAsState()
  val mobile by viewModel.authMobile.collectAsState()
  val address by viewModel.authAddress.collectAsState()
  val error by viewModel.authError.collectAsState()
  val loading by viewModel.authLoading.collectAsState()
  val isDark by viewModel.isDarkMode.collectAsState()
  val firestoreBookings by viewModel.firestoreBookings.collectAsState()

  val userRating by viewModel.userRating.collectAsState()
  val userReviewText by viewModel.userReviewText.collectAsState()
  val reviews by viewModel.reviews.collectAsState()
  val averageRating by viewModel.averageRating.collectAsState()

  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }

  val notifPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      com.example.service.NotificationHelper.showNotification(context, "Rajak Rickshawwala", "Notifications enabled for ride updates!")
      Toast.makeText(context, "Push notifications enabled!", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
    }
  }

  var isSignUp by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var editName by remember { mutableStateOf("") }
  var editMobile by remember { mutableStateOf("") }
  var editAddress by remember { mutableStateOf("") }
  var editEmail by remember { mutableStateOf("") }
  var editError by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "User Profile & Settings",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.align(Alignment.Start)
    )



    if (!isLoggedIn) {
      // Login / Sign Up Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isSignUp) Icons.Default.PersonAdd else Icons.Default.Lock,
              contentDescription = "Auth",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }

          Text(
            text = if (isSignUp) "Sign Up & Save Profile" else "Sign In to Your Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )

          if (isSignUp) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "📌 Fill your Name, Mobile, and Address once. They will be saved to your Profile tab permanently so you never have to re-enter them again!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(10.dp)
              )
            }

            OutlinedTextField(
              value = name,
              onValueChange = { viewModel.authName.value = it },
              label = { Text("Full Name * (Mandatory)") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )

            OutlinedTextField(
              value = mobile,
              onValueChange = { viewModel.authMobile.value = it },
              label = { Text("Mobile Number * (Mandatory)") },
              leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Mobile") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
          }

          OutlinedTextField(
            value = email,
            onValueChange = { viewModel.authEmail.value = it },
            label = { Text(if (isSignUp) "Email Address (Optional)" else "Mobile Number or Email *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          if (isSignUp) {
            OutlinedTextField(
              value = address,
              onValueChange = { viewModel.authAddress.value = it },
              label = { Text("Address * (Mandatory)") },
              leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Address") },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              maxLines = 2
            )
          }

          OutlinedTextField(
            value = password,
            onValueChange = { viewModel.authPassword.value = it },
            label = { Text(if (isSignUp) "Password * (Mandatory)" else "Password *") },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = "Password") },
            trailingIcon = {
              val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = "Toggle Password Visibility")
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          if (isSignUp) {
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = { viewModel.authConfirmPassword.value = it },
              label = { Text("Confirm Password * (Mandatory)") },
              leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = "Confirm Password") },
              trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                  Icon(imageVector = image, contentDescription = "Toggle Confirm Password Visibility")
                }
              },
              visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )
          }

          if (error != null) {
            Surface(
              color = Color(0xFFEF4444).copy(alpha = 0.1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = error ?: "",
                color = Color(0xFFEF4444),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
          } else {
            Button(
              onClick = {
                if (isSignUp) {
                  viewModel.signUp { }
                } else {
                  viewModel.signIn { }
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
              shape = RoundedCornerShape(16.dp)
            ) {
              Text(if (isSignUp) "Sign Up & Save Profile" else "Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          }

          TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
          }

          HorizontalDivider()

          OutlinedButton(
            onClick = { viewModel.continueAsGuest { } },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.DirectionsCar, contentDescription = "Guest", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue as Guest / Explore App", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    } else {
      // Logged In User Profile Card
      val pName = userSession?.displayName?.substringBefore(" | ")?.trim().orEmpty()
      val pMobile = userSession?.mobile?.trim().orEmpty()
      val pAddress = userSession?.address?.trim().orEmpty()
      val pEmail = userSession?.email?.trim().orEmpty()
      val displayEmail = if (pEmail.isNotEmpty() && !pEmail.endsWith("@rajakrickshaw.com")) pEmail else "Not provided (Optional)"

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "User Avatar",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(56.dp)
            )
          }

          Text(
            text = if (pName.isNotEmpty()) pName else "Passenger Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )

          // Saved Profile Details card (Name, Mobile, Address, Email)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Person, contentDescription = "Name", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Name: ${if (pName.isNotEmpty()) pName else "Not set"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Phone, contentDescription = "Mobile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Mobile: ${if (pMobile.isNotEmpty()) pMobile else "Not set"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Home, contentDescription = "Address", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Address: ${if (pAddress.isNotEmpty()) pAddress else "Not set"}", style = MaterialTheme.typography.bodyMedium)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Email: $displayEmail", style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          Surface(
            color = Color(0xFF10B981).copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Verified, contentDescription = "Saved", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
              Text("Saved for WhatsApp Bookings — No retyping needed!", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(
              onClick = {
                editName = pName
                editMobile = pMobile
                editAddress = pAddress
                editEmail = if (pEmail.endsWith("@rajakrickshaw.com")) "" else pEmail
                editError = null
                showEditProfileDialog = true
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Edit Profile")
            }

            Button(
              onClick = { viewModel.signOut() },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      if (showEditProfileDialog) {
        AlertDialog(
          onDismissRequest = { showEditProfileDialog = false },
          title = {
            Text("Edit Profile Details", fontWeight = FontWeight.Bold)
          },
          text = {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text(
                "Save your details once. They will be stored permanently so you never have to re-enter them again for WhatsApp bookings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Full Name * (Mandatory)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
              )

              OutlinedTextField(
                value = editMobile,
                onValueChange = { editMobile = it },
                label = { Text("Mobile Number * (Mandatory)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Mobile") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
              )

              OutlinedTextField(
                value = editAddress,
                onValueChange = { editAddress = it },
                label = { Text("Address * (Mandatory)") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
              )

              OutlinedTextField(
                value = editEmail,
                onValueChange = { editEmail = it },
                label = { Text("Email Address (Optional)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
              )

              if (editError != null) {
                Text(
                  text = editError ?: "",
                  color = Color(0xFFEF4444),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          },
          confirmButton = {
            Button(
              onClick = {
                if (editName.isBlank() || editMobile.isBlank() || editAddress.isBlank()) {
                  editError = "Full Name, Mobile Number, and Address are mandatory."
                } else {
                  editError = null
                  viewModel.updateProfileDetails(editName, editMobile, editAddress, editEmail) {
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile details saved successfully!", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            ) {
              Text("Save Profile")
            }
          },
          dismissButton = {
            TextButton(onClick = { showEditProfileDialog = false }) {
              Text("Cancel")
            }
          }
        )
      }

      // Booking History Card (Only for Logged In Passengers)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
              Text("Booking History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { viewModel.fetchFirestoreBookings() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
            }
          }

          if (firestoreBookings.isEmpty()) {
            Text(
              "No past bookings yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            firestoreBookings.forEach { booking ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  val status = (booking["status"] as? String) ?: "Completed"
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text("📅 ${booking["date"] ?: "N/A"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Surface(
                      color = when (status) {
                        "Completed" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        "Confirmed" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                      },
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = "● $status",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                          "Completed" -> Color(0xFF10B981)
                          "Confirmed" -> Color(0xFF3B82F6)
                          else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }
                  Text("📍 Pickup: ${booking["pickup"] ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                  if ((booking["pickupLink"] as? String)?.isNotEmpty() == true) {
                    Text("🔗 Pickup Link: ${booking["pickupLink"]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                  }
                  Text("🏁 Drop: ${booking["drop"] ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                  if ((booking["dropLink"] as? String)?.isNotEmpty() == true) {
                    Text("🔗 Drop Link: ${booking["dropLink"]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                  }
                  Text("💰 Bhada: ${booking["bhada"] ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    }

    // Theme Toggle Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
            contentDescription = "Theme",
            tint = MaterialTheme.colorScheme.primary
          )
          Column {
            Text("Dark Mode Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (isDark) "Currently in Dark Mode" else "Currently in Light Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Switch(
          checked = isDark,
          onCheckedChange = { viewModel.toggleDarkMode() }
        )
      }
    }

    // Notifications Permission Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.primary
          )
          Column {
            Text("Push Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Receive booking & promo updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Button(
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              com.example.service.NotificationHelper.showNotification(context, "Rajak Rickshawwala", "Notifications enabled for ride updates!")
              Toast.makeText(context, "Push notifications are enabled!", Toast.LENGTH_SHORT).show()
            }
          },
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Enable")
        }
      }
    }

    // Driver Rating & Review Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Driver Rating & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Rajak Parmar (Auto Rickshaw)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
              Text("$averageRating / 5.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
          }
        }

        Divider()

        Text("Rate Your Recent Trip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        // Star Row (1 to 5)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 1..5) {
            IconButton(onClick = { viewModel.userRating.value = i }) {
              Icon(
                imageVector = if (i <= userRating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i star",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(36.dp)
              )
            }
          }
        }

        OutlinedTextField(
          value = userReviewText,
          onValueChange = { viewModel.userReviewText.value = it },
          label = { Text("Write your review (e.g. Great ride, polite driver!)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          maxLines = 3
        )

        Button(
          onClick = {
            viewModel.submitReview {
              Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = "Submit")
          Spacer(modifier = Modifier.width(8.dp))
          Text("Submit Review & Rating", fontWeight = FontWeight.Bold)
        }

        if (reviews.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text("Recent Passenger Reviews", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          reviews.take(5).forEach { rev ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(rev["userName"] as? String ?: "Passenger", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Text(" ${rev["rating"] ?: 5}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                  }
                }
                if ((rev["review"] as? String)?.isNotEmpty() == true) {
                  Text(rev["review"] as? String ?: "", style = MaterialTheme.typography.bodyMedium)
                }
                Text(rev["date"] as? String ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }

    // Rate on Google Play Store Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = "Play Store",
          tint = Color.White,
          modifier = Modifier.size(36.dp)
        )
        Text(
          text = "Love Rajak Rickshawwala?",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Rate us 5 stars on Google Play Store to support our service!",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
          textAlign = TextAlign.Center
        )
        Button(
          onClick = { viewModel.rateOnPlayStore(context) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
          Icon(Icons.Default.Store, contentDescription = "Play Store", tint = Color(0xFF3B82F6))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Rate on Google Play Store ⭐", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
        }
      }
    }

    // Driver Info Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Driver & Owner Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Person, contentDescription = "Owner", tint = MaterialTheme.colorScheme.primary)
          Text("Rajak Parmar", fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Phone, contentDescription = "Phone", tint = MaterialTheme.colorScheme.primary)
          Text("+91 82000 19788", fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // Share App Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = "Share",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(32.dp)
        )
        Text(
          text = "રજાક રીક્ષાવાળા ઍપ શેર કરો",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
          text = "તમારા મિત્રો અને પરિવારજનો સાથે ઍપ શેર કરો જેથી તેઓ પણ સરળતાથી રીક્ષા બુક કરી શકે.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
          onClick = { viewModel.shareApp(context) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("ઍપ શેર કરો (Share App)", fontWeight = FontWeight.Bold)
        }
      }
    }

    // Developer Credit Footer
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, bottom = 24.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "Developer",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Developed By Azazmadkiya",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
