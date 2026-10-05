package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit
) {
  var startAnimation by remember { mutableStateOf(false) }

  // Scale and entrance animations
  val scaleAnim = animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0.7f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "scale"
  )

  val alphaAnim = animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(durationMillis = 800),
    label = "alpha"
  )

  // Gentle driving bounce animation
  val infiniteTransition = rememberInfiniteTransition(label = "driving")
  val bounceOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bounce"
  )

  LaunchedEffect(Unit) {
    startAnimation = true
    delay(2200)
    onSplashFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF064E3B), // Deep CNG Emerald
            Color(0xFF042F2E),
            Color(0xFF021B17)
          )
        )
      )
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
        .scale(scaleAnim.value)
        .alpha(alphaAnim.value)
    ) {

      // Green CNG Badge Pill
      Surface(
        color = Color(0xFF10B981),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "🌿 100% GREEN CNG AUTO RICKSHAW",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // App Title
      Text(
        text = "Rajak Rickshawwala",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFBBF24), // Vibrant Taxi Yellow
        textAlign = TextAlign.Center
      )

      Text(
        text = "Fast • Safe • Eco-Friendly City Rides",
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFFD1FAE5),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(24.dp))

      // FULL IMAGE AUTO RICKSHAW CARD
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .offset(y = bounceOffset.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF03221C).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          // Full Image Auto Rickshaw Vector Asset
          Image(
            painter = painterResource(id = R.drawable.img_auto_rickshaw_full),
            contentDescription = "Full Image Auto Rickshaw CNG",
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp),
            contentScale = ContentScale.Fit
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Driver & Contact Badge Card
      Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "🛺 Driver: Rajak Bhai",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "•",
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "+91 82000 19788",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFDE68A),
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      // Circular Loading Indicator
      CircularProgressIndicator(
        modifier = Modifier.size(36.dp),
        color = Color(0xFFFBBF24),
        strokeWidth = 3.5.dp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Starting your ride experience...",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.75f)
      )
    }

    // Bottom Version text
    Text(
      text = "v1.2.0 • Rajak Rickshawwala CNG Service",
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.4f),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 20.dp)
    )
  }
}
