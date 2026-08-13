package com.example.ui.screens

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.TransactionStatus
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallet by mainViewModel.wallet.collectAsState()
    val recharges by mainViewModel.recharges.collectAsState()
    val settings by mainViewModel.appSettings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mobile, 1: DTH

    val operatorsMobile = listOf("Jio", "Airtel", "Vi (Vodafone Idea)", "BSNL", "MTNL")
    val operatorsDth = listOf("Tata Play", "Airtel Digital TV", "Dish TV", "Sun Direct", "d2h")

    var selectedOperator by remember { mutableStateOf("Jio") }
    var accountNumber by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableStateOf(299.0) }
    var customAmountInput by remember { mutableStateOf("") }
    var isCustomAmountMode by remember { mutableStateOf(false) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val totalAvailableWallet = wallet.cashBalance + wallet.earnedBalance

    fun validateInput(): Boolean {
        validationError = null
        val number = accountNumber.trim()
        if (selectedTab == 0) {
            if (number.length != 10 || !number.all { it.isDigit() }) {
                validationError = "Please enter a valid 10-digit mobile number."
                return false
            }
        } else {
            if (number.length < 6) {
                validationError = "Please enter a valid DTH Customer/Subscriber ID (at least 6 characters)."
                return false
            }
        }

        val effectiveAmt = if (isCustomAmountMode) (customAmountInput.toDoubleOrNull() ?: 0.0) else selectedAmount
        if (effectiveAmt < settings.minRechargeAmount) {
            validationError = "Minimum recharge amount is ₹${settings.minRechargeAmount.toInt()}."
            return false
        }
        if (effectiveAmt > settings.maxRechargeAmount) {
            validationError = "Maximum recharge amount is ₹${settings.maxRechargeAmount.toInt()}."
            return false
        }
        if (effectiveAmt > totalAvailableWallet) {
            validationError = "Insufficient wallet balance (Available: ₹${String.format(Locale.US, "%.2f", totalAvailableWallet)})."
            return false
        }

        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mobile & DTH Recharge", fontWeight = FontWeight.Bold) },
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
            // Category Selector Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedOperator = operatorsMobile.first()
                            validationError = null
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mobile Recharge", fontWeight = FontWeight.Bold)
                        }
                    }

                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedOperator = operatorsDth.first()
                            validationError = null
                        }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DTH Recharge", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Wallet Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wallet Payment Balance", fontSize = 12.sp, color = Color.Gray)
                            Text("₹${String.format(Locale.US, "%.2f", totalAvailableWallet)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cash: ₹${wallet.cashBalance.toInt()} | Earned: ₹${wallet.earnedBalance.toInt()}", fontSize = 11.sp, color = Color.DarkGray)
                            Text("Deducted automatically", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Input Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedTab == 0) "Select Mobile Operator" else "Select DTH Operator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val ops = if (selectedTab == 0) operatorsMobile else operatorsDth

                        // Operator Grid Chips
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ops.chunked(3).forEach { rowOps ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowOps.forEach { op ->
                                        val isSelected = selectedOperator == op
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) EmeraldPrimary else Color.LightGray.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedOperator = op }
                                        ) {
                                            Text(
                                                text = op,
                                                color = if (isSelected) Color.White else Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    // Fill empty space if chunk is less than 3
                                    repeat(3 - rowOps.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mobile Number / Subscriber ID Field
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { input ->
                                accountNumber = input
                                validationError = null
                            },
                            label = { Text(if (selectedTab == 0) "10-Digit Mobile Number" else "DTH Customer / Subscriber ID") },
                            placeholder = { Text(if (selectedTab == 0) "e.g. 9876543210" else "e.g. 1029384756") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (selectedTab == 0) KeyboardType.Number else KeyboardType.Ascii
                            ),
                            singleLine = true,
                            isError = validationError != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recharge_account_input")
                        )

                        if (validationError != null) {
                            Text(
                                text = validationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Quick Recharge Amounts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        val quickAmounts = listOf(10.0, 20.0, 30.0, 50.0, 100.0, 200.0, 500.0, 999.0)

                        // 4x2 Quick Amount Chips
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickAmounts.take(4).forEach { amt ->
                                    val isSelected = !isCustomAmountMode && selectedAmount == amt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) EmeraldPrimary else Color.LightGray.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedAmount = amt
                                                isCustomAmountMode = false
                                                customAmountInput = ""
                                                validationError = null
                                            }
                                    ) {
                                        Text(
                                            text = "₹${amt.toInt()}",
                                            color = if (isSelected) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickAmounts.drop(4).forEach { amt ->
                                    val isSelected = !isCustomAmountMode && selectedAmount == amt
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) EmeraldPrimary else Color.LightGray.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedAmount = amt
                                                isCustomAmountMode = false
                                                customAmountInput = ""
                                                validationError = null
                                            }
                                    ) {
                                        Text(
                                            text = "₹${amt.toInt()}",
                                            color = if (isSelected) Color.White else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Amount Field
                        OutlinedTextField(
                            value = customAmountInput,
                            onValueChange = { input ->
                                customAmountInput = input
                                validationError = null
                                val parsed = input.toDoubleOrNull()
                                if (parsed != null) {
                                    selectedAmount = parsed
                                    isCustomAmountMode = true
                                } else if (input.isBlank()) {
                                    isCustomAmountMode = false
                                }
                            },
                            label = { Text("Or Enter Custom Amount (₹${settings.minRechargeAmount.toInt()} - ₹${settings.maxRechargeAmount.toInt()})") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recharge_custom_amount_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val effectiveAmount = if (isCustomAmountMode) (customAmountInput.toDoubleOrNull() ?: 0.0) else selectedAmount

                        Button(
                            onClick = {
                                if (validateInput()) {
                                    showConfirmationDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("review_recharge_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("REVIEW & PROCEED (₹${effectiveAmount.toInt()})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Recharge History Section
            item {
                Text("Recharge Transactions History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (recharges.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
                    ) {
                        Text(
                            "No previous recharge requests found.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recharges) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${order.category} Recharge - ${order.operator}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("No / ID: ${order.accountNumber}", fontSize = 12.sp, color = Color.Gray)
                                }

                                // Status Badge
                                val (statusBg, statusFg, statusText) = when (order.status) {
                                    TransactionStatus.PENDING -> Triple(Color(0xFFFFF3CD), Color(0xFF856404), "PENDING ADMIN")
                                    TransactionStatus.SUCCESS -> Triple(Color(0xFFD4EDDA), Color(0xFF155724), "SUCCESS")
                                    TransactionStatus.FAILED, TransactionStatus.REFUNDED -> Triple(Color(0xFFF8D7DA), Color(0xFF721C24), "FAILED / REFUNDED")
                                }

                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusFg,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(order.createdAt))
                                Text(formattedDate, fontSize = 11.sp, color = Color.Gray)
                                Text("₹${order.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimary)
                            }

                            if (order.operatorTxnId.isNotBlank() && order.operatorTxnId != "PENDING_ADMIN") {
                                Text("Op Txn Ref: ${order.operatorTxnId}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            if (order.adminNote.isNotBlank()) {
                                Text("Note: ${order.adminNote}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Screen Dialog
    if (showConfirmationDialog) {
        val effectiveAmt = if (isCustomAmountMode) (customAmountInput.toDoubleOrNull() ?: 0.0) else selectedAmount

        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text("Confirm Recharge Request", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Please verify your details before submitting:", fontSize = 13.sp, color = Color.Gray)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Type:", fontSize = 12.sp, color = Color.Gray)
                                Text(if (selectedTab == 0) "Mobile Recharge" else "DTH Recharge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Operator:", fontSize = 12.sp, color = Color.Gray)
                                Text(selectedOperator, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Number/ID:", fontSize = 12.sp, color = Color.Gray)
                                Text(accountNumber.trim(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount:", fontSize = 12.sp, color = Color.Gray)
                                Text("₹${effectiveAmt.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "This request will be sent to Admin for manual processing. Status will be PENDING until verified.",
                            fontSize = 11.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        mainViewModel.submitRecharge(
                            category = if (selectedTab == 0) "Mobile" else "DTH",
                            operator = selectedOperator,
                            accountNumber = accountNumber.trim(),
                            amount = effectiveAmt
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("CONFIRM & SUBMIT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
