package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.WalletSummaryCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDark
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WalletScreen(
    mainViewModel: MainViewModel,
    onOpenAddCash: () -> Unit,
    onOpenWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallet by mainViewModel.wallet.collectAsState()
    val transactions by mainViewModel.recentTransactions.collectAsState()

    var showAddCashModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "My Digital Wallet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Multi-ledger transparency powered by secure backend computation",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Wallet Summary Card
        item {
            WalletSummaryCard(
                wallet = wallet,
                onAddCashClick = { showAddCashModal = true },
                onWithdrawClick = onOpenWithdraw
            )
        }

        // Ledger Breakdown Explanation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backend Ledger Security Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Earned Balance: Accumulated from offerwalls, games, check-ins, and referrals. Eligible for direct UPI cash withdrawal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Added Cash: Deposited via UPI/Gateway. Used for mobile & DTH recharges.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Total Balance: Computed automatically from backend database. Client-side modifications are prohibited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Transaction History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallet Activity Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transactions.size} Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        items(transactions) { txn ->
            TransactionRowItem(transaction = txn)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Add Cash Modal Dialog
    if (showAddCashModal) {
        AddCashModalDialog(
            onDismiss = { showAddCashModal = false },
            onProcessAddCash = { amount, method ->
                mainViewModel.addCash(amount, method)
                showAddCashModal = false
            }
        )
    }
}

@Composable
fun AddCashModalDialog(
    onDismiss: () -> Unit,
    onProcessAddCash: (Double, String) -> Unit
) {
    var customAmountText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(100.0) }
    var selectedMethod by remember { mutableStateOf("PhonePe UPI") }

    val presets = listOf(50.0, 100.0, 200.0, 500.0, 1000.0)

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
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Add Cash to Wallet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instant credit upon verified payment webhook confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Predefined Preset Pills
                Text(text = "Select Amount (₹)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(3).forEach { amount ->
                        FilterChip(
                            selected = selectedPreset == amount && customAmountText.isEmpty(),
                            onClick = {
                                selectedPreset = amount
                                customAmountText = ""
                            },
                            label = { Text("₹${amount.toInt()}", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(3).forEach { amount ->
                        FilterChip(
                            selected = selectedPreset == amount && customAmountText.isEmpty(),
                            onClick = {
                                selectedPreset = amount
                                customAmountText = ""
                            },
                            label = { Text("₹${amount.toInt()}", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Amount Input
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("Or Enter Custom Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Gateway Selector
                Text(text = "Select Payment Gateway", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val methods = listOf("PhonePe UPI", "Google Pay UPI", "Paytm Gateway")
                methods.forEach { method ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMethod == method) EmeraldPrimary.copy(alpha = 0.1f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedMethod == method) EmeraldPrimary else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = method, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val finalAmount = customAmountText.toDoubleOrNull() ?: selectedPreset

                Button(
                    onClick = { onProcessAddCash(finalAmount, selectedMethod) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_add_cash_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = "PAY ₹${finalAmount.toInt()} & CREDIT CASH", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
