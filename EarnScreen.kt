package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.OfferwallCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDark
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun EarnScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by mainViewModel.currentUser.collectAsState()
    val offers by mainViewModel.offers.collectAsState()
    val streak by mainViewModel.dailyStreak.collectAsState()
    val activeGameSession by mainViewModel.activeGameSession.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Offerwall, 1: Play & Earn, 2: Daily Checkin, 3: Refer & Earn
    var showGameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Earn Rewards",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Complete verified tasks, surveys, games & referrals",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Section Navigation Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_offerwall")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Offers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_play")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.testTag("tab_checkin")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Checkin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    modifier = Modifier.testTag("tab_refer")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Offerwall List - Coming Soon Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offerwall_coming_soon_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = "Offerwall",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Demo Offers — Coming Soon",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Real offerwall surveys and tasks will appear here once an approved offerwall provider (such as CPX Research) is connected. Offer rewards are credited only after verified server-side postbacks.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NavyDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldAccent)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Anti-Fraud Active: Demo offers removed. No fake money or unverified conversions generated.",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Play & Earn Section
                item {
                    val settings by mainViewModel.appSettings.collectAsState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SportsEsports,
                                            contentDescription = "Game",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GoldAccent
                                ) {
                                    Text(
                                        text = "${settings.gameDurationSec}s = ${settings.gameRewardCoins.toInt()} Coins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NavyDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "EarnMitra Tap & Spin Quest",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Play actively for at least ${settings.gameDurationSec} seconds. Our secure server validates your gameplay session ID and enforces a daily session limit (${settings.dailyGameLimit}/day).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    mainViewModel.startGameSession()
                                    showGameDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_game_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("START ${settings.gameDurationSec}s GAME QUEST", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            2 -> {
                // Daily Check-in 7-Day Cycle
                item {
                    val settings by mainViewModel.appSettings.collectAsState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "7-Day Reward Cycle",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Check in every day to increase your streak coin rewards! (100 Coins = ₹1)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val days = settings.checkinRewardsCoins
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                days.forEachIndexed { idx, rewardCoins ->
                                    val dayNum = idx + 1
                                    val isCurrent = streak == dayNum
                                    val isPassed = dayNum < streak

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            isCurrent -> GoldAccent
                                            isPassed -> EmeraldPrimary.copy(alpha = 0.2f)
                                            else -> Color.LightGray.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(64.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "D$dayNum",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) NavyDark else Color.DarkGray
                                            )
                                            Text(
                                                text = "${rewardCoins.toInt()}C",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) NavyDark else EmeraldPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { mainViewModel.claimDailyCheckin() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("claim_daily_checkin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Redeem, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CLAIM TODAY'S CHECK-IN (DAY $streak)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            3 -> {
                // Refer & Earn Card
                item {
                    val settings by mainViewModel.appSettings.collectAsState()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Refer & Earn ${settings.referralRewardCoins.toInt()} Coins",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Invite friends with your unique code. Referral reward is credited once your referred friend completes their first qualifying check-in or game activity.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Referral Code Display
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldPrimary.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "YOUR REFERRAL CODE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = user?.referralCode ?: "...",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("EarnMitra Referral Code", user?.referralCode ?: "")
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("• Self-referrals are strictly prohibited and blocked.", style = MaterialTheme.typography.bodySmall)
                                    Text("• Reward Crediting: Granted after referred user's 1st activity.", style = MaterialTheme.typography.bodySmall)
                                    Text("• Admin Configurable: Referral reward amount dynamically set in Firestore.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Play & Earn 60-Second Interactive Game Modal
    if (showGameDialog && activeGameSession != null) {
        GameInteractiveModal(
            session = activeGameSession!!,
            onDismiss = {
                mainViewModel.cancelGameSession()
                showGameDialog = false
            },
            onClaim = { elapsedSec ->
                mainViewModel.claimGameReward(elapsedSec)
                showGameDialog = false
            }
        )
    }
}

@Composable
fun GameInteractiveModal(
    session: com.example.data.model.GameSession,
    onDismiss: () -> Unit,
    onClaim: (Int) -> Unit
) {
    var timerSec by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    val totalRequiredSec = 60

    LaunchedEffect(Unit) {
        while (timerSec < totalRequiredSec) {
            delay(1000L)
            timerSec += 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session: ${session.sessionId}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAccent
                    ) {
                        Text(
                            text = if (timerSec >= 60) "QUEST COMPLETED!" else "$timerSec / 60 SEC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Tap Target
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { score += 1 },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "TAP!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "$score", fontSize = 18.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { timerSec / 60f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPrimary,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (timerSec < 60)
                        "Keep playing! Server timer requires at least 60 seconds before reward verification."
                    else
                        "60 Seconds Reached! Server session verified. You can now claim 2 coins.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onClaim(timerSec) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("claim_game_reward_button"),
                        enabled = timerSec >= 60,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("CLAIM REWARD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
