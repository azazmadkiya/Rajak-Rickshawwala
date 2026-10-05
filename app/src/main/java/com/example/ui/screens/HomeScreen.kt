package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.viewmodel.RickshawViewModel
import java.util.Locale

@Composable
fun HomeScreen(viewModel: RickshawViewModel, onNavigateToPayment: () -> Unit) {
  val context = LocalContext.current
  val pickup by viewModel.pickup.collectAsState()
  val pickupLink by viewModel.pickupLink.collectAsState()
  val drop by viewModel.drop.collectAsState()
  val dropLink by viewModel.dropLink.collectAsState()
  val notes by viewModel.notes.collectAsState()
  val bookingError by viewModel.bookingError.collectAsState()
  val userSession by viewModel.userSession.collectAsState()

  var isFetchingLocation by remember { mutableStateOf(false) }

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

  // Automatically fetch current GPS location when screen opens if permission is already granted
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
        }

        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            Text("4.9 (500+ Rides)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = { viewModel.callDriver(context) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Call Now", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Quick Action Banner (UPI Pay)
    OutlinedCard(
      onClick = onNavigateToPayment,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = "UPI", tint = Color(0xFF10B981))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("UPI Payment Portal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text("Pay via Paytm / GPay (8200019788@paytm)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    // WhatsApp Booking Card
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF25D366)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(20.dp))
          }
          Column {
            Text(
              text = "Book via WhatsApp",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Direct booking with Rajak Bhai (+918200019788)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Passenger Profile info banner
        val pName = userSession?.displayName?.substringBefore(" | ")?.trim().orEmpty()
        val pMobile = userSession?.mobile?.trim().orEmpty()
        val pAddress = userSession?.address?.trim().orEmpty()
        val hasSavedProfile = pName.isNotEmpty() && pMobile.isNotEmpty() && pAddress.isNotEmpty()

        if (hasSavedProfile) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Saved", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Text("Saved Passenger Profile (Auto-Attached)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
              Text("👤 $pName  •  📞 $pMobile", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("🏠 $pAddress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else {
          Surface(
            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Info, contentDescription = "Notice", tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
              Text("Save your Name, Mobile & Address once in the Profile tab so it attaches automatically without retyping!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
            }
          }
        }

        Button(
          onClick = { requestLocationOrFetch() },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          enabled = !isFetchingLocation
        ) {
          if (isFetchingLocation) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detecting current GPS location...", fontWeight = FontWeight.SemiBold)
          } else {
            Icon(Icons.Default.MyLocation, contentDescription = "GPS", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (pickup.isEmpty()) "📍 Auto-Detect Current GPS Pickup" else "🔄 Refresh Current GPS Location",
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Mandatory Pickup Field with direct GPS trailing action
        OutlinedTextField(
          value = pickup,
          onValueChange = {
            viewModel.pickup.value = it
            if (it.isNotEmpty()) viewModel.bookingError.value = null
          },
          label = { Text("Pickup Location / Landmark * (Mandatory)") },
          leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = "Pickup", tint = MaterialTheme.colorScheme.primary) },
          trailingIcon = {
            if (isFetchingLocation) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              IconButton(onClick = { requestLocationOrFetch() }) {
                Icon(
                  Icons.Default.GpsFixed,
                  contentDescription = "Autofill GPS",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        // Optional Pickup Map Link
        OutlinedTextField(
          value = pickupLink,
          onValueChange = { viewModel.pickupLink.value = it },
          label = { Text("Pickup Location Map Link (Optional)") },
          leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        // Mandatory Drop Field
        OutlinedTextField(
          value = drop,
          onValueChange = {
            viewModel.drop.value = it
            if (it.isNotEmpty()) viewModel.bookingError.value = null
          },
          label = { Text("Drop Destination * (Mandatory)") },
          leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Drop", tint = Color(0xFFEF4444)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        // Optional Drop Map Link
        OutlinedTextField(
          value = dropLink,
          onValueChange = { viewModel.dropLink.value = it },
          label = { Text("Drop Destination Map Link (Optional)") },
          leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        // Optional Notes Field
        OutlinedTextField(
          value = notes,
          onValueChange = { viewModel.notes.value = it },
          label = { Text("Notes / Passenger count / Luggage (Optional)") },
          leadingIcon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          maxLines = 2
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
          Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Booking to WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }
}
