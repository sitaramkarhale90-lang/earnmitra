package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDark

const val SUPPORT_PHONE_NUMBER = "7972947395"
const val WHATSAPP_CHANNEL_URL = "https://whatsapp.com/channel/0029Vb8PFds002T6GrvwUE28"
const val YOUTUBE_CHANNEL_URL = "https://youtube.com/@timeversehindi24?si=MC3A6_GOMS9Ai9aQ"

@Composable
fun CustomerSupportCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_support_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Customer Support",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Customer Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WhatsApp & Telegram Support",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Direct Contact Options (WhatsApp & Telegram)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // WhatsApp Support Tile
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25D366).copy(alpha = 0.12f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openWhatsAppChat(context, SUPPORT_PHONE_NUMBER) }
                        .testTag("whatsapp_support_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF128C7E),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF075E54)
                            )
                            Text(
                                text = SUPPORT_PHONE_NUMBER,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Telegram Support Tile
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0088CC).copy(alpha = 0.12f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openTelegramChat(context, SUPPORT_PHONE_NUMBER) }
                        .testTag("telegram_support_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = Color(0xFF0088CC),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Telegram",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005580)
                            )
                            Text(
                                text = SUPPORT_PHONE_NUMBER,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Community Channels (WhatsApp Channel & YouTube Channel)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Join WhatsApp Channel
                Button(
                    onClick = { openExternalUrl(context, WHATSAPP_CHANNEL_URL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("whatsapp_channel_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54))
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "WhatsApp Channel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join WhatsApp Channel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // YouTube Channel
                OutlinedButton(
                    onClick = { openExternalUrl(context, YOUTUBE_CHANNEL_URL) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("youtube_channel_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF0000))
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "YouTube Channel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "YouTube Channel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun openWhatsAppChat(context: Context, phone: String) {
    try {
        val url = "https://api.whatsapp.com/send?phone=91$phone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openTelegramChat(context: Context, phone: String) {
    try {
        val url = "https://t.me/+91$phone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch Telegram: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
