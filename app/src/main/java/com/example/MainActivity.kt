package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlterKeyboardTheme
import com.example.ui.theme.AlterYellow
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightText
import com.example.ui.theme.MutedText
import com.example.ui.theme.OrbInk
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RaisedSurface
import com.example.ui.theme.SlateGrayCard

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AlterKeyboardTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = PitchBlack
        ) { innerPadding ->
          AlterKeyboardDashboard(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

enum class PillState {
  IDLE, LISTENING, THINKING, ERROR
}

@Composable
fun AlterKeyboardDashboard(modifier: Modifier = Modifier) {
  var pillState by remember { mutableStateOf(PillState.IDLE) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SlateGrayCard),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = "ALTER Keyboard Icon",
            tint = AlterYellow,
            modifier = Modifier.size(24.dp)
          )
        }
        Column {
          Text(
            text = "ALTER Keyboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LightText,
            fontFamily = FontFamily.SansSerif
          )
          Text(
            text = "Ready for Local CLI & Claude Handoff",
            fontSize = 13.sp,
            color = MutedText
          )
        }
      }
    }

    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrayCard)
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Ready", tint = AlterYellow, modifier = Modifier.size(20.dp))
            Text("Warm-up & Scaffolding Complete", fontWeight = FontWeight.SemiBold, color = LightText, fontSize = 16.sp)
          }
          Text(
            text = "Repository initialized with strict guidelines, license notices, signing specs, and design tokens.",
            fontSize = 13.sp,
            color = MutedText,
            lineHeight = 18.sp
          )
        }
      }
    }

    item {
      Text(
        text = "Interactive Pill Mode Preview",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = LightText
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Tap the pill to cycle states (Idle → Listening → Thinking → Error)",
        fontSize = 12.sp,
        color = MutedText
      )
    }

    item {
      // 56dp Pill Mode Demonstration Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(PitchBlack)
          .testTag("pill_mode_bar"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left globe placeholder
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("🌐", fontSize = 16.sp)
          }

          // Center Pill
          Box(
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .padding(horizontal = 8.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(SlateGrayCard)
              .clickable {
                pillState = when (pillState) {
                  PillState.IDLE -> PillState.LISTENING
                  PillState.LISTENING -> PillState.THINKING
                  PillState.THINKING -> PillState.ERROR
                  PillState.ERROR -> PillState.IDLE
                }
              }
              .testTag("center_pill"),
            contentAlignment = Alignment.Center
          ) {
            when (pillState) {
              PillState.IDLE -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text("••", color = OrbInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Icon(Icons.Default.Mic, contentDescription = "Mic", tint = LightText, modifier = Modifier.size(18.dp))
                }
              }
              PillState.LISTENING -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  repeat(7) { index ->
                    val height = if (index % 2 == 0) 14.dp else 22.dp
                    Box(
                      modifier = Modifier
                        .width(3.dp)
                        .height(height)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AlterYellow)
                    )
                  }
                }
              }
              PillState.THINKING -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  repeat(3) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OrbInk)
                    )
                  }
                }
              }
              PillState.ERROR -> {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Voice Error",
                  tint = ErrorRed,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          // Right Keyboard toggle button
          IconButton(
            onClick = { /* Demo toggle */ },
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.Default.Keyboard, contentDescription = "Switch to Full Keyboard", tint = LightText, modifier = Modifier.size(20.dp))
          }
        }
      }
    }

    item {
      Text(
        text = "PR Execution Plan",
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = LightText
      )
    }

    val prList = listOf(
      "PR 1" to "Import FUTO source + submodules, license, signing, disable update checker",
      "PR 2" to "Apply ALTER design system, Lucide icons, branded theme as default",
      "PR 3" to "Port alter-voice module (Moonshine/Whisper + LiteRT LFM2.5 cleanup)",
      "PR 4" to "Implement Pill Mode (56dp bar), 4 states, Minimize action, password fallback",
      "PR 5" to "APK size reduction (<100MB), arm64 config, CI verification workflow"
    )

    items(prList.size) { index ->
      val (pr, desc) = prList[index]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(RaisedSurface)
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (index == 0) AlterYellow else SlateGrayCard),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${index + 1}",
            fontWeight = FontWeight.Bold,
            color = if (index == 0) PitchBlack else LightText,
            fontSize = 14.sp
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(pr, fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
          Text(desc, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrayCard)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Info, contentDescription = "Notice", tint = MutedText, modifier = Modifier.size(16.dp))
            Text("License Notice", fontWeight = FontWeight.SemiBold, color = LightText, fontSize = 13.sp)
          }
          Text(
            text = "ALTER Keyboard is a modified version of FUTO Keyboard by FUTO Holdings, Inc. (FUTO Source First License 1.1).",
            fontSize = 11.sp,
            color = MutedText
          )
        }
      }
    }
  }
}
