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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.example.viewmodel.RickshawViewModel

@Composable
fun HomeScreen(viewModel: RickshawViewModel, onNavigateToPayment: () -> Unit) {
  val context = LocalContext.current
  val pickup by viewModel.pickup.collectAsState()
  val pickupLink by viewModel.pickupLink.collectAsState()
  val drop by viewModel.drop.collectAsState()
  val dropLink by viewModel.dropLink.collectAsState()
  val notes by viewModel.notes.collectAsState()

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
          if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            viewModel.pickup.value = "GPS: Lat $lat, Lng $lng"
            viewModel.pickupLink.value = "https://maps.google.com/?q=$lat,$lng"
            Toast.makeText(context, "GPS location autofilled!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Unable to get current location. Please try again.", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Error fetching location: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
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
              text = "Direct booking with Rajak Bhai",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        OutlinedButton(
          onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
              try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                  if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    viewModel.pickup.value = "GPS: Lat $lat, Lng $lng"
                    viewModel.pickupLink.value = "https://maps.google.com/?q=$lat,$lng"
                    Toast.makeText(context, "GPS location autofilled!", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Unable to get current location. Please try again.", Toast.LENGTH_SHORT).show()
                  }
                }
              } catch (e: Exception) {
                Toast.makeText(context, "Error fetching location: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
              }
            } else {
              locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.MyLocation, contentDescription = "GPS", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("📍 Use Current GPS Location (Optional)")
        }

        OutlinedTextField(
          value = pickup,
          onValueChange = { viewModel.pickup.value = it },
          label = { Text("Pickup Location / Landmark") },
          leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = "Pickup", tint = MaterialTheme.colorScheme.primary) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = pickupLink,
          onValueChange = { viewModel.pickupLink.value = it },
          label = { Text("Pickup Location Map Link (Optional)") },
          leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = drop,
          onValueChange = { viewModel.drop.value = it },
          label = { Text("Drop Destination") },
          leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Drop", tint = Color(0xFFEF4444)) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = dropLink,
          onValueChange = { viewModel.dropLink.value = it },
          label = { Text("Drop Destination Map Link (Optional)") },
          leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { viewModel.notes.value = it },
          label = { Text("Notes / Passenger count / Luggage (Optional)") },
          leadingIcon = { Icon(Icons.Default.Note, contentDescription = "Notes") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          maxLines = 2
        )

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
          Text("Send Booking to WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}
