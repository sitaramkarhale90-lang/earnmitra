package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WithdrawalStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDark
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val withdrawals by mainViewModel.withdrawals.collectAsState()
    val recharges by mainViewModel.recharges.collectAsState()

    var showAdjustmentDialog by remember { mutableStateOf(false) }

    val pendingWithdrawals = withdrawals.filter { it.status == WithdrawalStatus.PENDING }
    val pendingRecharges = recharges.filter { it.status == com.example.data.model.TransactionStatus.PENDING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EarnMitra Protected Admin Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Admin Role Verified. You have permission to review withdrawals, adjust user ledgers with mandatory reasons, and inspect transactions.",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Stats Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pending Recharges", fontSize = 11.sp, color = Color.Gray)
                            Text("${pendingRecharges.size} Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pending Withdrawals", fontSize = 11.sp, color = Color.Gray)
                            Text("${pendingWithdrawals.size} Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }
                    }
                }
            }

            // Quick Admin Actions
            item {
                var showSettingsDialog by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_configure_rules_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONFIGURE REWARD RULES & LIMITS (FIRESTORE)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAdjustmentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_adjust_balance_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MANUAL WALLET ADJUSTMENT (WITH REASON)", fontWeight = FontWeight.Bold)
                    }
                }

                if (showSettingsDialog) {
                    val currentAppSettings by mainViewModel.appSettings.collectAsState()
                    var coinsPerRupeeText by remember { mutableStateOf(currentAppSettings.coinsPerRupee.toString()) }
                    var gameDurationText by remember { mutableStateOf(currentAppSettings.gameDurationSec.toString()) }
                    var gameRewardCoinsText by remember { mutableStateOf(currentAppSettings.gameRewardCoins.toString()) }
                    var dailyGameLimitText by remember { mutableStateOf(currentAppSettings.dailyGameLimit.toString()) }
                    var referralRewardCoinsText by remember { mutableStateOf(currentAppSettings.referralRewardCoins.toString()) }
                    var minRechargeText by remember { mutableStateOf(currentAppSettings.minRechargeAmount.toString()) }
                    var maxRechargeText by remember { mutableStateOf(currentAppSettings.maxRechargeAmount.toString()) }
                    var minWithdrawalText by remember { mutableStateOf(currentAppSettings.minWithdrawal.toString()) }

                    Dialog(onDismissRequest = { showSettingsDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Text("Global Reward Rules & System Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Stored in Firestore appSettings/global.", fontSize = 11.sp, color = Color.Gray)
                                }

                                item {
                                    OutlinedTextField(
                                        value = coinsPerRupeeText,
                                        onValueChange = { coinsPerRupeeText = it },
                                        label = { Text("Coins per ₹1 (e.g. 100)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = gameDurationText,
                                        onValueChange = { gameDurationText = it },
                                        label = { Text("Play & Earn Required Sec (e.g. 60)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = gameRewardCoinsText,
                                        onValueChange = { gameRewardCoinsText = it },
                                        label = { Text("Play & Earn Reward Coins (e.g. 2)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = dailyGameLimitText,
                                        onValueChange = { dailyGameLimitText = it },
                                        label = { Text("Daily Game Session Limit (e.g. 10)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = referralRewardCoinsText,
                                        onValueChange = { referralRewardCoinsText = it },
                                        label = { Text("Referral Bonus Coins (e.g. 50)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = minRechargeText,
                                        onValueChange = { minRechargeText = it },
                                        label = { Text("Min Recharge Amount ₹ (e.g. 10)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = maxRechargeText,
                                        onValueChange = { maxRechargeText = it },
                                        label = { Text("Max Recharge Amount ₹ (e.g. 999)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = minWithdrawalText,
                                        onValueChange = { minWithdrawalText = it },
                                        label = { Text("Min Withdrawal Amount ₹ (e.g. 50)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showSettingsDialog = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }

                                        Button(
                                            onClick = {
                                                val updated = currentAppSettings.copy(
                                                    coinsPerRupee = coinsPerRupeeText.toDoubleOrNull() ?: 100.0,
                                                    gameDurationSec = gameDurationText.toIntOrNull() ?: 60,
                                                    gameRewardCoins = gameRewardCoinsText.toDoubleOrNull() ?: 2.0,
                                                    dailyGameLimit = dailyGameLimitText.toIntOrNull() ?: 10,
                                                    referralRewardCoins = referralRewardCoinsText.toDoubleOrNull() ?: 50.0,
                                                    minRechargeAmount = minRechargeText.toDoubleOrNull() ?: 10.0,
                                                    maxRechargeAmount = maxRechargeText.toDoubleOrNull() ?: 999.0,
                                                    minWithdrawal = minWithdrawalText.toDoubleOrNull() ?: 50.0
                                                )
                                                mainViewModel.adminUpdateSettings(updated)
                                                showSettingsDialog = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                        ) {
                                            Text("SAVE TO FIRESTORE")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Recharge Requests Review List
            item {
                Text("Pending Recharge Requests (${pendingRecharges.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (pendingRecharges.isEmpty()) {
                item {
                    Text("No pending recharge requests at the moment.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                items(pendingRecharges) { req ->
                    var opTxnIdText by remember { mutableStateOf("") }
                    var noteText by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.userName.ifBlank { "UID: " + req.uid.take(8) }, fontWeight = FontWeight.Bold)
                                    Text("${req.category} (${req.operator}) • No: ${req.accountNumber}", fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Text("₹${req.amount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = opTxnIdText,
                                    onValueChange = { opTxnIdText = it },
                                    label = { Text("Operator Ref Txn ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    label = { Text("Admin Note") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { mainViewModel.adminProcessRecharge(req.id, false, opTxnIdText, noteText) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("REJECT & REFUND")
                                }

                                Button(
                                    onClick = { mainViewModel.adminProcessRecharge(req.id, true, opTxnIdText, noteText) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("MARK SUCCESS")
                                }
                            }
                        }
                    }
                }
            }

            // Pending Withdrawals Review List
            item {
                Text("Pending Withdrawal Requests (${pendingWithdrawals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (pendingWithdrawals.isEmpty()) {
                item {
                    Text("No pending withdrawal requests at the moment.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                items(pendingWithdrawals) { req ->
                    var noteText by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(req.userName, fontWeight = FontWeight.Bold)
                                    Text("UPI: ${req.upiId}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("₹${req.amount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                label = { Text("Admin Note / Transaction Ref") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { mainViewModel.adminApproveWithdrawal(req.id, false, noteText) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("REJECT & REFUND")
                                }

                                Button(
                                    onClick = { mainViewModel.adminApproveWithdrawal(req.id, true, noteText) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("APPROVE PAYOUT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Adjustment Dialog
    if (showAdjustmentDialog) {
        Dialog(onDismissRequest = { showAdjustmentDialog = false }) {
            var targetUid by remember { mutableStateOf("demo_user_101") }
            var amountText by remember { mutableStateOf("10.0") }
            var isCash by remember { mutableStateOf(false) }
            var reason by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manual Wallet Adjustment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("A mandatory reason must be provided for audit trails.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetUid,
                        onValueChange = { targetUid = it },
                        label = { Text("Target User UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount Adjustment (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Mandatory Adjustment Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg!!, color = Color.Red, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showAdjustmentDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (reason.isBlank()) {
                                    errorMsg = "Reason cannot be blank"
                                } else {
                                    mainViewModel.adminAdjustBalance(targetUid, amountText.toDoubleOrNull() ?: 0.0, isCash, reason)
                                    showAdjustmentDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("APPLY")
                        }
                    }
                }
            }
        }
    }
}
