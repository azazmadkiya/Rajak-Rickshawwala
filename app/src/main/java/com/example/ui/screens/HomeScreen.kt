package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.viewmodel.RickshawViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

@Composable
fun HomeScreen(
  viewModel: RickshawViewModel,
  onNavigateToPayment: () -> Unit,
  onNavigateToAdmin: () -> Unit = {}
) {
  val context = LocalContext.current
  val pickup by viewModel.pickup.collectAsState()
  val pickupLink by viewModel.pickupLink.collectAsState()
  val drop by viewModel.drop.collectAsState()
  val dropLink by viewModel.dropLink.collectAsState()
  val notes by viewModel.notes.collectAsState()
  val bookingError by viewModel.bookingError.collectAsState()
  val userSession by viewModel.userSession.collectAsState()
  val isAdmin by viewModel.isAdmin.collectAsState()
  val userNotifs by viewModel.userNotifications.collectAsState()
  val unreadCount by viewModel.unreadNotificationCount.collectAsState()

  var isFetchingLocation by remember { mutableStateOf(false) }
  var showNotifDialog by remember { mutableStateOf(false) }

  fun applyLocation(location: Location) {
    isFetchingLocation = false
    val lat = location.latitude
    val lng = location.longitude
    viewModel.pickupLink.value = "https://maps.google.com/?q=$lat,$lng"
    viewModel.bookingError.value = null

    // Reverse Geocoding for readable pickup landmark/area
    var addressText = "GPS: Lat ${String.format(Locale.US, "%.5f", lat)}, Lng ${String.format(Locale.US, "%.5f", lng)}"
    try {
      val geocoder = Geocoder(context, Locale.getDefault())
      @Suppress("DEPRECATION")
      val list = geocoder.getFromLocation(lat, lng, 1)
      if (!list.isNullOrEmpty()) {
        val addr = list[0]
        val feature = addr.featureName
        val thoroughfare = addr.thoroughfare
        val subLocality = addr.subLocality
        val locality = addr.locality
        val parts = listOfNotNull(feature, thoroughfare, subLocality, locality).distinct().filter { it.isNotBlank() }
        if (parts.isNotEmpty()) {
          addressText = "${parts.joinToString(", ")} (GPS: ${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
        }
      }
    } catch (e: Exception) {
      // Coordinates fallback
    }

    viewModel.pickup.value = addressText
    Toast.makeText(context, "📍 Pickup location populated with current GPS!", Toast.LENGTH_SHORT).show()
  }

  fun fetchCurrentLocation() {
    isFetchingLocation = true
    try {
      val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
      fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
          if (location != null) {
            applyLocation(location)
          } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
              if (lastLoc != null) {
                applyLocation(lastLoc)
              } else {
                isFetchingLocation = false
                Toast.makeText(context, "GPS location unavailable. Please ensure GPS is ON and try again.", Toast.LENGTH_SHORT).show()
              }
            }.addOnFailureListener {
              isFetchingLocation = false
              Toast.makeText(context, "Could not fetch GPS location.", Toast.LENGTH_SHORT).show()
            }
          }
        }
        .addOnFailureListener { e ->
          isFetchingLocation = false
          Toast.makeText(context, "Failed to get location: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: SecurityException) {
      isFetchingLocation = false
      Toast.makeText(context, "Location permission missing.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      isFetchingLocation = false
      Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      fetchCurrentLocation()
    } else {
      isFetchingLocation = false
      Toast.makeText(context, "Location permission denied. Please allow permission to fetch GPS location.", Toast.LENGTH_SHORT).show()
    }
  }

  fun requestLocationOrFetch() {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (fine || coarse) {
      fetchCurrentLocation()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  LaunchedEffect(Unit) {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if ((fine || coarse) && viewModel.pickup.value.isEmpty()) {
      fetchCurrentLocation()
    }
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Admin Top Banner Alert if Admin
    if (isAdmin) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToAdmin() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF10B981))
            Column {
              Text("Admin Mode Active", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
              Text("Tap to view all registered users & send notices", style = MaterialTheme.typography.bodySmall)
            }
          }
          Icon(Icons.Default.ArrowForward, contentDescription = "Open Admin", tint = Color(0xFF10B981))
        }
      }
    }

    // Top Driver Profile Banner Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
          Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = "Rickshaw",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
              )
            }
            Column {
              Text(
                text = "Rajak Rickshawwala",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = "Verified",
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Owner: Rajak Parmar • Verified",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notification Bell with Badge
            Box {
              IconButton(
                onClick = { showNotifDialog = true },
                modifier = Modifier
                  .size(42.dp)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
              ) {
                Icon(
                  imageVector = Icons.Default.Notifications,
                  contentDescription = "Notifications",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              if (userNotifs.isNotEmpty()) {
                Surface(
                  color = Color(0xFFEF4444),
                  shape = CircleShape,
                  modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      "${userNotifs.size}",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }

            // Share App Button
            IconButton(
              onClick = { viewModel.shareApp(context) },
              modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share App",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Direct Call / WhatsApp",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
              text = "+91 82000 19788",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
              onClick = { viewModel.callDriver(context) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.onPrimary)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Call", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Booking Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
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
          Text(
            text = "Book Auto Rickshaw Ride",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }

        // 1. Pickup Location Input + Auto GPS detection button
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = pickup,
            onValueChange = {
              viewModel.pickup.value = it
              viewModel.bookingError.value = null
            },
            label = { Text("Pickup Location * (Mandatory)") },
            placeholder = { Text("Enter Landmark, Area or GPS Location") },
            leadingIcon = {
              Icon(Icons.Default.LocationOn, contentDescription = "Pickup", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
              IconButton(
                onClick = { requestLocationOrFetch() }
              ) {
                if (isFetchingLocation) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Detect GPS Location",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            isError = bookingError != null && pickup.isBlank()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (pickupLink.isNotEmpty()) "✅ GPS auto-detected" else "💡 Tap GPS icon to auto-detect",
              style = MaterialTheme.typography.bodySmall,
              color = if (pickupLink.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
              onClick = { requestLocationOrFetch() },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Use Current GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // 2. Drop Destination Input
        OutlinedTextField(
          value = drop,
          onValueChange = {
            viewModel.drop.value = it
            viewModel.bookingError.value = null
          },
          label = { Text("Drop Destination * (Mandatory)") },
          placeholder = { Text("e.g. Bus Stand, Railway Station, Mall, Hospital") },
          leadingIcon = {
            Icon(Icons.Default.Place, contentDescription = "Drop Destination", tint = Color(0xFFEF4444))
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          isError = bookingError != null && drop.isBlank()
        )

        // 3. Notes / Passenger luggage
        OutlinedTextField(
          value = notes,
          onValueChange = { viewModel.notes.value = it },
          label = { Text("Special Notes / Luggage (Optional)") },
          placeholder = { Text("e.g. 2 Passengers, 1 Big Suitcase, Urgent Ride") },
          leadingIcon = {
            Icon(Icons.Default.Notes, contentDescription = "Notes", tint = MaterialTheme.colorScheme.outline)
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        // Error Feedback if any mandatory field is missing
        if (bookingError != null) {
          Surface(
            color = Color(0xFFEF4444).copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFEF4444))
              Text(
                text = bookingError ?: "",
                color = Color(0xFFDC2626),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Button(
          onClick = {
            viewModel.bookViaWhatsApp(context) {
              Toast.makeText(context, "Opening WhatsApp with your booking details...", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Booking to WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }

  // Notification Inbox Dialog
  if (showNotifDialog) {
    AlertDialog(
      onDismissRequest = { showNotifDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Notification Inbox", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        if (userNotifs.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("No notifications received yet", color = Color.Gray, fontSize = 13.sp)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(userNotifs) { notif ->
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(notif.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                  }
                  Text(notif.message, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showNotifDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}
