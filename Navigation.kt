package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EarnScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RechargeScreen
import com.example.ui.screens.TransactionScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.WithdrawScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDark
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.MainViewModel

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Default.Home, "nav_home"),
    EARN("Earn", Icons.Default.Redeem, "nav_earn"),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet, "nav_wallet"),
    HISTORY("History", Icons.Default.ReceiptLong, "nav_history"),
    PROFILE("Profile", Icons.Default.Person, "nav_profile")
}

@Composable
fun MainAppNavigation(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val actionMessage by mainViewModel.actionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) } // "recharge", "withdraw", "admin"

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearActionMessage()
        }
    }

    if (currentUser == null) {
        AuthScreen(
            authViewModel = authViewModel,
            onAuthSuccess = { /* User state updated reactively */ }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (currentSubScreen == null) {
                    NavigationBar(
                        containerColor = NavyDark,
                        contentColor = Color.White
                    ) {
                        NavigationTab.values().forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = index
                                    currentSubScreen = null
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.6f)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldAccent else Color.White.copy(alpha = 0.6f)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldPrimary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)

            when (currentSubScreen) {
                "recharge" -> RechargeScreen(
                    mainViewModel = mainViewModel,
                    onBack = { currentSubScreen = null },
                    modifier = screenModifier
                )
                "withdraw" -> WithdrawScreen(
                    mainViewModel = mainViewModel,
                    onBack = { currentSubScreen = null },
                    modifier = screenModifier
                )
                "admin" -> AdminScreen(
                    mainViewModel = mainViewModel,
                    onBack = { currentSubScreen = null },
                    modifier = screenModifier
                )
                else -> {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            mainViewModel = mainViewModel,
                            onNavigateTab = { tabIdx -> selectedTab = tabIdx },
                            onOpenAddCash = { selectedTab = 2 },
                            onOpenWithdraw = { currentSubScreen = "withdraw" },
                            onOpenRecharge = { currentSubScreen = "recharge" },
                            onOpenAdmin = { currentSubScreen = "admin" },
                            modifier = screenModifier
                        )
                        1 -> EarnScreen(
                            mainViewModel = mainViewModel,
                            modifier = screenModifier
                        )
                        2 -> WalletScreen(
                            mainViewModel = mainViewModel,
                            onOpenAddCash = { selectedTab = 2 },
                            onOpenWithdraw = { currentSubScreen = "withdraw" },
                            modifier = screenModifier
                        )
                        3 -> TransactionScreen(
                            mainViewModel = mainViewModel,
                            modifier = screenModifier
                        )
                        4 -> ProfileScreen(
                            authViewModel = authViewModel,
                            mainViewModel = mainViewModel,
                            onOpenAdmin = { currentSubScreen = "admin" },
                            modifier = screenModifier
                        )
                    }
                }
            }
        }
    }
}
