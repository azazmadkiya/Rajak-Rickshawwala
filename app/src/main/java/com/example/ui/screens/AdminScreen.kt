package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppNotification
import com.example.viewmodel.RegisteredUser
import com.example.viewmodel.RickshawViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: RickshawViewModel) {
  val context = LocalContext.current
  val allUsers by viewModel.allUsers.collectAsState()
  val allBookings by viewModel.allBookingsAdmin.collectAsState()
  val sentNotifications by viewModel.sentNotifications.collectAsState()
  val userSession by viewModel.userSession.collectAsState()

  var selectedTab by remember { mutableStateOf(0) }
  val tabTitles = listOf("👥 Users (${allUsers.size})", "📢 Send Notice", "📜 History (${sentNotifications.size})", "🛺 Bookings (${allBookings.size})")

  var searchQuery by remember { mutableStateOf("") }
  var showUserPicker by remember { mutableStateOf(false) }
  var selectedUserForDetails by remember { mutableStateOf<RegisteredUser?>(null) }

  // Quick notification templates
  val templates = listOf(
    "🎉 સ્પેશિયલ ઑફર" to "આજે જ રજાક રીક્ષા બુક કરો અને મેળવો ખાસ ડિસ્કાઉન્ટ! કૉલ અથવા WhatsApp કરો: +91 82000 19788.",
    "🛺 રીક્ષા હાજર છે" to "તમારા વિસ્તારમાં રજાક રીક્ષાવાળા ઉપલબ્ધ છે. તાત્કાલિક મુસાફરી માટે હમણાં જ રાઇડ બુક કરો.",
    "⏰ સવારનું બુકિંગ ચાલુ છે" to "કાલ સવારની સ્કૂલ, ઑફિસ કે સ્ટેશન મુસાફરી માટે અગાઉથી રજાક રીક્ષા બુક કરી રાખો.",
    "📢 જરૂરી સૂચના" to "નમસ્તે, રજાક રીક્ષાવાળા સેવા હંમેશા આપની સુરક્ષિત અને સુવિધાજનક મુસાફરી માટે તૈયાર છે. આભાર!"
  )

  val filteredUsers = remember(allUsers, searchQuery) {
    if (searchQuery.isBlank()) allUsers
    else allUsers.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
      it.mobile.contains(searchQuery, ignoreCase = true) ||
      it.email.contains(searchQuery, ignoreCase = true) ||
      it.address.contains(searchQuery, ignoreCase = true) ||
      it.uid.contains(searchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Admin Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
              Surface(
                color = Color(0xFF10B981),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  "ADMIN",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              "Logged in as: azazmadkiya@gmail.com",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }
        },
        actions = {
          IconButton(onClick = {
            viewModel.fetchAdminData()
            Toast.makeText(context, "Firebase data synchronized", Toast.LENGTH_SHORT).show()
          }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Summary Stats Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${allUsers.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Registered Users", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
          }
        }
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${sentNotifications.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Notices Sent", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
          }
        }
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
          Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${allBookings.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Total Rides", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
          }
        }
      }

      // Tab Navigation
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MaterialTheme.colorScheme.primary
          )
        }
      ) {
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
          )
        }
      }

      // Tab Contents
      when (selectedTab) {
        0 -> {
          // Users Tab
          Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              modifier = Modifier.fillMaxWidth(),
              placeholder = { Text("Search by name, mobile, address, email...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                  }
                }
              },
              shape = RoundedCornerShape(14.dp),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "All Registered Users (${filteredUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(color = Color(0xFF10B981), shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                Text(
                  "Firebase Realtime",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF10B981),
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredUsers.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No registered users matching search", color = Color.Gray)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                items(filteredUsers, key = { it.uid }) { user ->
                  val userBookingsCount = remember(allBookings, user.uid, user.mobile) {
                    allBookings.count {
                      (it["userId"] as? String) == user.uid ||
                      (it["userMobile"] as? String) == user.mobile
                    }
                  }

                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { selectedUserForDetails = user },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (user.role == "admin") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                  ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                      // Top Row: Avatar + Name + Role + Bookings Badge
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                          Box(
                            modifier = Modifier
                              .size(46.dp)
                              .clip(CircleShape)
                              .background(if (user.role == "admin") Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                          ) {
                            Text(
                              user.name.take(1).uppercase(),
                              color = Color.White,
                              fontWeight = FontWeight.Bold,
                              fontSize = 20.sp
                            )
                          }
                          Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                              Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                              if (user.role == "admin") {
                                Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                                  Text("ADMIN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                              }
                            }
                            Text("UID: ${user.uid.take(14)}...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                          }
                        }

                        Surface(
                          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                          shape = RoundedCornerShape(8.dp)
                        ) {
                          Text(
                            "🛺 $userBookingsCount Rides",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                          )
                        }
                      }

                      Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                      // User Details Grid
                      if (user.mobile.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Icon(Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                          Text("Mobile: ${user.mobile}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                      }

                      if (user.address.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Icon(Icons.Default.LocationOn, contentDescription = "Address", modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                          Text("Address: ${user.address}", style = MaterialTheme.typography.bodySmall)
                        }
                      }

                      if (user.email.isNotEmpty() && !user.email.endsWith("@rajakrickshaw.com")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
                          Text("Email: ${user.email}", style = MaterialTheme.typography.bodySmall)
                        }
                      }

                      if (user.deviceModel.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Icon(Icons.Default.Smartphone, contentDescription = "Device", modifier = Modifier.size(16.dp), tint = Color(0xFF8B5CF6))
                          Text("Device: ${user.deviceModel} (${user.androidVersion})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text("📅 Registered: ${user.registeredAt}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          if (user.mobile.isNotEmpty()) {
                            IconButton(
                              onClick = { viewModel.callDriver(context, user.mobile) },
                              modifier = Modifier.size(36.dp).background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                            ) {
                              Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                              onClick = { viewModel.openWhatsAppChat(context, user.mobile, "Hello ${user.name}, from Rajak Rickshawwala Admin.") },
                              modifier = Modifier.size(36.dp).background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                            ) {
                              Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                            }
                          }

                          FilledTonalButton(
                            onClick = {
                              viewModel.adminNotifTargetType.value = "SINGLE"
                              viewModel.adminNotifSelectedUser.value = user
                              selectedTab = 1
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                          ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Notice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        1 -> {
          // Send Notifications Tab
          val notifTitle by viewModel.adminNotifTitle.collectAsState()
          val notifMessage by viewModel.adminNotifMessage.collectAsState()
          val targetType by viewModel.adminNotifTargetType.collectAsState()
          val selectedUser by viewModel.adminNotifSelectedUser.collectAsState()
          val isSending by viewModel.adminNotifSending.collectAsState()
          val statusMsg by viewModel.adminNotifStatusMessage.collectAsState()

          val scroll = rememberScrollState()

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scroll)
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Target Selection Card
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Notification Target:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  FilterChip(
                    selected = targetType == "ALL",
                    onClick = { viewModel.adminNotifTargetType.value = "ALL" },
                    label = { Text("📢 All Users (${allUsers.size})") },
                    leadingIcon = if (targetType == "ALL") {
                      { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                  )

                  FilterChip(
                    selected = targetType == "SINGLE",
                    onClick = {
                      viewModel.adminNotifTargetType.value = "SINGLE"
                      if (selectedUser == null && allUsers.isNotEmpty()) {
                        viewModel.adminNotifSelectedUser.value = allUsers.first()
                      }
                    },
                    label = { Text("👤 Single User") },
                    leadingIcon = if (targetType == "SINGLE") {
                      { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                  )
                }

                if (targetType == "SINGLE") {
                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable { showUserPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column {
                        Text("Target User:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                          selectedUser?.let { "${it.name} (${it.mobile})" } ?: "Click to choose a user",
                          fontWeight = FontWeight.Bold
                        )
                      }
                      Icon(Icons.Default.ArrowDropDown, contentDescription = "Select User")
                    }
                  }
                }
              }
            }

            // Quick Templates Chips
            Text("Quick Notice Templates (ઝડપી ટેમ્પલેટ્સ):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              templates.take(2).forEach { (tTitle, tMsg) ->
                SuggestionChip(
                  onClick = {
                    viewModel.adminNotifTitle.value = tTitle
                    viewModel.adminNotifMessage.value = tMsg
                  },
                  label = { Text(tTitle, fontSize = 11.sp, maxLines = 1) },
                  modifier = Modifier.weight(1f)
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              templates.drop(2).forEach { (tTitle, tMsg) ->
                SuggestionChip(
                  onClick = {
                    viewModel.adminNotifTitle.value = tTitle
                    viewModel.adminNotifMessage.value = tMsg
                  },
                  label = { Text(tTitle, fontSize = 11.sp, maxLines = 1) },
                  modifier = Modifier.weight(1f)
                )
              }
            }

            // Notification Inputs
            OutlinedTextField(
              value = notifTitle,
              onValueChange = { viewModel.adminNotifTitle.value = it },
              label = { Text("Notification Title (શીર્ષક)") },
              placeholder = { Text("e.g., 🎉 Special Offer / Ride Update") },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              singleLine = true,
              leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
            )

            OutlinedTextField(
              value = notifMessage,
              onValueChange = { viewModel.adminNotifMessage.value = it },
              label = { Text("Notification Message / Body (મેસેજ)") },
              placeholder = { Text("Enter detailed notification message for passengers...") },
              modifier = Modifier.fillMaxWidth().height(120.dp),
              shape = RoundedCornerShape(14.dp),
              leadingIcon = { Icon(Icons.Default.Message, contentDescription = null) }
            )

            // Live Preview Card
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text("Live Phone Status Bar Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text(
                  text = notifTitle.ifEmpty { "Notification Title" },
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleSmall
                )
                Text(
                  text = notifMessage.ifEmpty { "Your notification message will appear here for passengers." },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (statusMsg != null) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = statusMsg ?: "",
                  color = Color(0xFF10B981),
                  modifier = Modifier.padding(12.dp),
                  fontWeight = FontWeight.SemiBold,
                  textAlign = TextAlign.Center
                )
              }
            }

            // Send Button
            Button(
              onClick = {
                val targetUid = if (targetType == "ALL") "ALL" else (selectedUser?.uid ?: "ALL")
                val targetName = if (targetType == "ALL") "All Users (બધા યુઝર્સ)" else (selectedUser?.name ?: "Selected User")

                viewModel.sendNotificationFromAdmin(
                  context = context,
                  title = notifTitle,
                  message = notifMessage,
                  targetUserId = targetUid,
                  targetUserName = targetName,
                  onSuccess = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                  },
                  onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                  }
                )
              },
              modifier = Modifier.fillMaxWidth().height(52.dp),
              shape = RoundedCornerShape(16.dp),
              enabled = !isSending && notifTitle.isNotBlank() && notifMessage.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending...")
              } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Notification Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }
          }
        }

        2 -> {
          // History Tab
          Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
              "Sent Notifications Log (${sentNotifications.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (sentNotifications.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No notifications sent yet", color = Color.Gray)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(sentNotifications, key = { it.id }) { notif ->
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Surface(
                          color = if (notif.targetUserId == "ALL") Color(0xFF3B82F6) else Color(0xFF10B981),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Text(
                            if (notif.targetUserId == "ALL") "📢 ALL USERS" else "👤 ${notif.targetUserName}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }

                        IconButton(
                          onClick = { viewModel.deleteNotification(notif.id) },
                          modifier = Modifier.size(28.dp)
                        ) {
                          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                      }

                      Text(notif.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                      Text(notif.message, style = MaterialTheme.typography.bodyMedium)

                      Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text("Sent by: ${notif.sentBy}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(notif.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        3 -> {
          // Bookings Tab
          Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
              "All Customer Ride Bookings (${allBookings.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (allBookings.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("No bookings recorded yet", color = Color.Gray)
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(allBookings) { booking ->
                  val passenger = (booking["userName"] as? String)?.ifEmpty { "Passenger" } ?: "Passenger"
                  val mobile = (booking["userMobile"] as? String) ?: ""
                  val pickup = (booking["pickup"] as? String) ?: "Pickup"
                  val drop = (booking["drop"] as? String) ?: "Destination"
                  val date = (booking["date"] as? String) ?: ""
                  val notes = (booking["notes"] as? String) ?: ""

                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text("👤 $passenger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (mobile.isNotEmpty()) {
                          Text(mobile, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Text("From: $pickup", style = MaterialTheme.typography.bodySmall)
                      }
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Text("To: $drop", style = MaterialTheme.typography.bodySmall)
                      }

                      if (notes.isNotEmpty()) {
                        Text("Notes: $notes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                      }

                      Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                      Text("📅 $date", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Full User Details Modal Dialog
  selectedUserForDetails?.let { user ->
    val userRides = remember(allBookings, user.uid, user.mobile) {
      allBookings.filter {
        (it["userId"] as? String) == user.uid ||
        (it["userMobile"] as? String) == user.mobile
      }
    }

    AlertDialog(
      onDismissRequest = { selectedUserForDetails = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(if (user.role == "admin") Color(0xFF10B981) else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
          }
          Column {
            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(if (user.role == "admin") "Administrator" else "Verified Passenger", fontSize = 11.sp, color = if (user.role == "admin") Color(0xFF10B981) else Color.Gray)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Contact & Actions Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (user.mobile.isNotEmpty()) {
              Button(
                onClick = { viewModel.callDriver(context, user.mobile) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call", fontSize = 12.sp)
              }

              Button(
                onClick = { viewModel.openWhatsAppChat(context, user.mobile, "Hello ${user.name}, from Rajak Rickshawwala Admin.") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 12.sp)
              }
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("📱 Mobile: ${user.mobile.ifEmpty { "Not specified" }}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text("🏠 Full Address: ${user.address.ifEmpty { "Not specified" }}", fontSize = 13.sp)
              Text("✉️ Email: ${user.email.ifEmpty { "Not specified" }}", fontSize = 13.sp)
              if (user.deviceModel.isNotEmpty()) {
                Text("📱 Phone: ${user.deviceModel} (${user.androidVersion})", fontSize = 13.sp, color = Color(0xFF8B5CF6))
              }
              Text("📅 Registration Date: ${user.registeredAt}", fontSize = 12.sp, color = Color.Gray)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("🆔 UID: ${user.uid.take(16)}...", fontSize = 11.sp, color = Color.Gray)
                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("User UID", user.uid))
                    Toast.makeText(context, "User UID copied to clipboard", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(14.dp))
                }
              }
            }
          }

          // Rides History for this user
          Text("Ride Booking History (${userRides.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          if (userRides.isEmpty()) {
            Text("No ride bookings recorded for this user yet.", fontSize = 12.sp, color = Color.Gray)
          } else {
            userRides.take(5).forEach { ride ->
              Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text("📍 From: ${(ride["pickup"] as? String) ?: "Pickup"}", fontSize = 12.sp)
                  Text("🏁 To: ${(ride["drop"] as? String) ?: "Destination"}", fontSize = 12.sp)
                  Text("📅 Date: ${(ride["date"] as? String) ?: ""}", fontSize = 11.sp, color = Color.Gray)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.adminNotifTargetType.value = "SINGLE"
            viewModel.adminNotifSelectedUser.value = user
            selectedUserForDetails = null
            selectedTab = 1
          }
        ) {
          Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Send Notification")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedUserForDetails = null }) {
          Text("Close")
        }
      }
    )
  }

  // User Picker Dialog
  if (showUserPicker) {
    AlertDialog(
      onDismissRequest = { showUserPicker = false },
      title = { Text("Select User to Notify") },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(allUsers) { user ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.adminNotifSelectedUser.value = user
                  showUserPicker = false
                },
              shape = RoundedCornerShape(10.dp),
              color = if (viewModel.adminNotifSelectedUser.value?.uid == user.uid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Box(
                  modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center
                ) {
                  Text(user.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column {
                  Text(user.name, fontWeight = FontWeight.Bold)
                  Text(user.mobile.ifEmpty { user.email }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showUserPicker = false }) {
          Text("Close")
        }
      }
    )
  }
}
