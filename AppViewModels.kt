package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameSession
import com.example.data.model.OfferItem
import com.example.data.model.RechargeOrder
import com.example.data.model.TransactionLog
import com.example.data.model.UserProfile
import com.example.data.model.WalletLedger
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.EarnMitraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Authentication ViewModel
 */
class AuthViewModel(private val repository: EarnMitraRepository) : ViewModel() {
    val currentUser: StateFlow<UserProfile?> = repository.currentUser

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(name: String, email: String, pass: String, confirmPass: String, referralCode: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedConfirmPass = confirmPass.trim()
        val trimmedReferral = referralCode.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedPass.isBlank() || trimmedConfirmPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all required fields.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (trimmedPass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters long.")
            return
        }
        if (trimmedPass != trimmedConfirmPass) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                repository.registerUser(trimmedName, trimmedEmail, trimmedPass, trimmedReferral)
                    .onSuccess {
                        _uiState.value = AuthUiState.Success("Account registered successfully!")
                    }
                    .onFailure { err ->
                        val errMsg = err.message ?: "Registration failed. Please try again."
                        _uiState.value = AuthUiState.Error(errMsg)
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unexpected error occurred during registration.")
            }
        }
    }

    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email and password.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                repository.loginUser(trimmedEmail, trimmedPass)
                    .onSuccess {
                        _uiState.value = AuthUiState.Success("Welcome back, ${it.name}!")
                    }
                    .onFailure { err ->
                        val errMsg = err.message ?: "Login failed. Please try again."
                        _uiState.value = AuthUiState.Error(errMsg)
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unexpected error occurred during login.")
            }
        }
    }

    fun forgotPassword(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email address.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                repository.sendPasswordReset(trimmedEmail)
                    .onSuccess { _uiState.value = AuthUiState.Success("Password reset instructions sent to $trimmedEmail") }
                    .onFailure { err -> _uiState.value = AuthUiState.Error(err.message ?: "Failed to send reset email") }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                repository.signInWithCredential(credential)
                    .onSuccess {
                        _uiState.value = AuthUiState.Success("Welcome, ${it.name}!")
                    }
                    .onFailure { err ->
                        _uiState.value = AuthUiState.Error(err.message ?: "Google Sign-In failed.")
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "An unexpected error occurred during Google Sign-In.")
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState.Idle
    }

    fun clearState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val error: String) : AuthUiState()
}

/**
 * Main Home & Wallet ViewModel
 */
class MainViewModel(private val repository: EarnMitraRepository) : ViewModel() {
    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val wallet: StateFlow<WalletLedger> = repository.wallet
    val dailyStreak: StateFlow<Int> = repository.dailyCheckinStreak
    val offers: StateFlow<List<OfferItem>> = repository.offers
    val withdrawals: StateFlow<List<WithdrawalRequest>> = repository.withdrawals
    val recharges: StateFlow<List<RechargeOrder>> = repository.recharges

    val appSettings = repository.appSettings
    val recentTransactions: StateFlow<List<TransactionLog>> = repository.observeTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Daily Check-in Claim
    fun claimDailyCheckin() {
        viewModelScope.launch {
            repository.claimDailyCheckin()
                .onSuccess { reward ->
                    _actionMessage.value = "Daily Check-in Success! Earned ₹${reward.toInt()} coins added to wallet."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    // Play & Earn Session Start / Claim
    private val _activeGameSession = MutableStateFlow<GameSession?>(null)
    val activeGameSession: StateFlow<GameSession?> = _activeGameSession.asStateFlow()

    fun startGameSession() {
        viewModelScope.launch {
            _activeGameSession.value = repository.startPlaySession()
        }
    }

    fun claimGameReward(elapsedSec: Int) {
        val session = _activeGameSession.value ?: return
        viewModelScope.launch {
            repository.verifyAndClaimGameReward(session, elapsedSec)
                .onSuccess { reward ->
                    _actionMessage.value = "Game Verified! Earned ₹${reward.toInt()} coins added to wallet."
                    _activeGameSession.value = null
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    fun cancelGameSession() {
        _activeGameSession.value = null
    }

    // Offer Postback Simulation
    fun simulateOfferPostback(offerId: String) {
        val providerTxnId = "CPX_" + (100000..999999).random()
        viewModelScope.launch {
            repository.processProviderOfferPostback(offerId, providerTxnId)
                .onSuccess { reward ->
                    _actionMessage.value = "Offer Completed! Server postback verified (Txn: $providerTxnId). Earned ₹${reward.toInt()}!"
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    // Add Cash
    fun addCash(amount: Double, paymentMethod: String) {
        viewModelScope.launch {
            repository.processAddCash(amount, paymentMethod)
                .onSuccess {
                    _actionMessage.value = "Payment Gateway Success! ₹${amount.toInt()} Added Cash credited to wallet."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    // Recharge
    fun submitRecharge(category: String, operator: String, accountNumber: String, amount: Double) {
        viewModelScope.launch {
            repository.submitRecharge(category, operator, accountNumber, amount)
                .onSuccess { order ->
                    _actionMessage.value = "Recharge Request Submitted! Order ID: ${order.id}. Status is PENDING Admin Processing."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    fun adminProcessRecharge(orderId: String, approve: Boolean, opTxnId: String, adminNote: String) {
        viewModelScope.launch {
            repository.adminProcessRecharge(orderId, approve, opTxnId, adminNote)
                .onSuccess {
                    val statusStr = if (approve) "Approved (SUCCESS)" else "Rejected & Refunded (FAILED)"
                    _actionMessage.value = "Recharge Request $orderId marked as $statusStr."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    // Withdraw Request
    fun submitWithdrawal(amount: Double, upiId: String) {
        viewModelScope.launch {
            repository.submitWithdrawalRequest(amount, upiId)
                .onSuccess { req ->
                    _actionMessage.value = "Withdrawal Request ₹${amount.toInt()} submitted! Reserved pending admin approval."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    // Admin Actions
    fun adminApproveWithdrawal(requestId: String, approve: Boolean, note: String) {
        viewModelScope.launch {
            repository.adminProcessWithdrawal(requestId, approve, note)
                .onSuccess {
                    val status = if (approve) "Approved" else "Rejected & Refunded"
                    _actionMessage.value = "Withdrawal $requestId $status successfully."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    fun adminAdjustBalance(targetUid: String, amount: Double, isCash: Boolean, reason: String) {
        viewModelScope.launch {
            repository.adminAdjustWalletBalance(targetUid, amount, isCash, reason)
                .onSuccess {
                    _actionMessage.value = "Admin Wallet Adjustment completed ($reason)."
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    fun adminUpdateSettings(newSettings: com.example.data.model.AppSettings) {
        viewModelScope.launch {
            repository.adminUpdateAppSettings(newSettings)
                .onSuccess {
                    _actionMessage.value = "App Security & Reward Rules updated successfully!"
                }
                .onFailure { err ->
                    _actionMessage.value = err.message
                }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}

/**
 * ViewModel Factory
 */
class EarnMitraViewModelFactory(private val repository: EarnMitraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}
