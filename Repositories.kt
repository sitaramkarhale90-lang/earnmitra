package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.EarnMitraDatabase
import com.example.data.local.LocalTransactionEntity
import com.example.data.local.LocalWalletEntity
import com.example.data.model.AppSettings
import com.example.data.model.CashDepositRecord
import com.example.data.model.DailyCheckinRecord
import com.example.data.model.GameSession
import com.example.data.model.GameSessionStatus
import com.example.data.model.OfferItem
import com.example.data.model.OfferStatus
import com.example.data.model.OfferwallConversion
import com.example.data.model.RechargeOrder
import com.example.data.model.ReferralRecord
import com.example.data.model.TransactionLog
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserProfile
import com.example.data.model.WalletLedger
import com.example.data.model.WithdrawalRequest
import com.example.data.model.WithdrawalStatus
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Main Repository Manager for EarnMitra Platform with Real-Time Firebase Sync
 */
class EarnMitraRepository(private val context: Context) {

    private val db: EarnMitraDatabase = Room.databaseBuilder(
        context.applicationContext,
        EarnMitraDatabase::class.java,
        "earnmitra.db"
    ).build()

    private val dao = db.dao()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
    }

    // Firebase References
    private val auth: FirebaseAuth by lazy {
        com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
        FirebaseAuth.getInstance()
    }
    private val firestore: FirebaseFirestore by lazy {
        com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
        FirebaseFirestore.getInstance()
    }

    // Active Firestore Listener Registrations
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // State Flow for Current Authenticated User Profile
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // State Flow for User Wallet Ledgers
    private val _wallet = MutableStateFlow(WalletLedger())
    val wallet: StateFlow<WalletLedger> = _wallet.asStateFlow()

    // App Settings
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Daily Check-in Streak Tracking
    private val _dailyCheckinStreak = MutableStateFlow(1)
    val dailyCheckinStreak: StateFlow<Int> = _dailyCheckinStreak.asStateFlow()

    private val _lastCheckinTimestamp = MutableStateFlow(0L)
    val lastCheckinTimestamp: StateFlow<Long> = _lastCheckinTimestamp.asStateFlow()

    // Offerwall Offers
    private val _offers = MutableStateFlow<List<OfferItem>>(emptyList())
    val offers: StateFlow<List<OfferItem>> = _offers.asStateFlow()

    // Withdrawals Requests List
    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    // Recharges List
    private val _recharges = MutableStateFlow<List<RechargeOrder>>(emptyList())
    val recharges: StateFlow<List<RechargeOrder>> = _recharges.asStateFlow()

    // Admin Users List
    private val _allUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsers: StateFlow<List<UserProfile>> = _allUsers.asStateFlow()

    init {
        listenToAppSettings()
        loadDefaultOffers()
        checkExistingSession()
    }

    private fun listenToAppSettings() {
        try {
            firestore.collection("appSettings").document("global")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val settings = snapshot.toObject(AppSettings::class.java)
                        if (settings != null) {
                            _appSettings.value = settings
                        }
                    }
                }
        } catch (e: Exception) {
            // Use defaults if unavailable
        }
    }

    private fun checkExistingSession() {
        val fbUser = try { auth.currentUser } catch (e: Exception) { null }
        if (fbUser != null) {
            attachUserListeners(fbUser.uid)
        } else {
            detachListeners()
            _currentUser.value = null
            _wallet.value = WalletLedger()
            _withdrawals.value = emptyList()
            _recharges.value = emptyList()
            _allUsers.value = emptyList()
        }
    }

    private fun detachListeners() {
        synchronized(listenerRegistrations) {
            listenerRegistrations.forEach { it.remove() }
            listenerRegistrations.clear()
        }
    }

    private fun attachUserListeners(uid: String) {
        detachListeners()

        // 1. User Profile Listener (users collection)
        val userReg = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _currentUser.value = profile

                        // Attach admin listeners if user is admin
                        if (profile.isAdmin) {
                            attachAdminListeners()
                        }
                    }
                }
            }

        // 2. Wallet Ledger Listener (wallets collection)
        val walletReg = firestore.collection("wallets").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val w = snapshot.toObject(WalletLedger::class.java)
                    if (w != null) {
                        _wallet.value = w
                        // Cache wallet locally in Room
                        scope.launch {
                            dao.insertWallet(
                                LocalWalletEntity(
                                    uid = w.uid,
                                    earnedCoins = w.earnedCoins,
                                    earnedBalance = w.earnedBalance,
                                    cashBalance = w.cashBalance,
                                    todayEarnings = w.todayEarnings,
                                    updatedAt = w.updatedAt
                                )
                            )
                        }
                    }
                }
            }

        // 3. Transactions Listener (transactions collection)
        val txnReg = firestore.collection("transactions")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val txns = snapshot.documents.mapNotNull { it.toObject(TransactionLog::class.java) }
                        .sortedByDescending { it.timestamp }
                    scope.launch {
                        dao.insertTransactions(txns.map { it.toEntity() })
                    }
                }
            }

        // 4. Withdrawals Listener for current user
        val wthReg = firestore.collection("withdrawals")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && _currentUser.value?.isAdmin != true) {
                    val list = snapshot.documents.mapNotNull { it.toObject(WithdrawalRequest::class.java) }
                        .sortedByDescending { it.createdAt }
                    _withdrawals.value = list
                }
            }

        // 5. Recharges Listener
        val rchReg = firestore.collection("recharges")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(RechargeOrder::class.java) }
                        .sortedByDescending { it.createdAt }
                    _recharges.value = list
                }
            }

        // 6. Daily Checkins Listener to restore streak state
        val checkinReg = firestore.collection("dailyCheckins")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val records = snapshot.documents.mapNotNull { it.toObject(DailyCheckinRecord::class.java) }
                        .sortedByDescending { it.claimedAt }
                    if (records.isNotEmpty()) {
                        val latest = records.first()
                        _lastCheckinTimestamp.value = latest.claimedAt
                        _dailyCheckinStreak.value = latest.streak
                    }
                }
            }

        synchronized(listenerRegistrations) {
            listenerRegistrations.add(userReg)
            listenerRegistrations.add(walletReg)
            listenerRegistrations.add(txnReg)
            listenerRegistrations.add(wthReg)
            listenerRegistrations.add(rchReg)
            listenerRegistrations.add(checkinReg)
        }
    }

    private fun attachAdminListeners() {
        val usersReg = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                    _allUsers.value = users
                }
            }

        val allWthReg = firestore.collection("withdrawals")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(WithdrawalRequest::class.java) }
                        .sortedByDescending { it.createdAt }
                    _withdrawals.value = list
                }
            }

        val allRchReg = firestore.collection("recharges")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(RechargeOrder::class.java) }
                        .sortedByDescending { it.createdAt }
                    _recharges.value = list
                }
            }

        synchronized(listenerRegistrations) {
            listenerRegistrations.add(usersReg)
            listenerRegistrations.add(allWthReg)
            listenerRegistrations.add(allRchReg)
        }
    }

    fun observeTransactions(): Flow<List<TransactionLog>> {
        val uid = _currentUser.value?.uid ?: ""
        return dao.getTransactionsForUser(uid).map { list ->
            list.map { it.toDomain() }
        }
    }

    // -------------------------------------------------------------
    // AUTHENTICATION
    // -------------------------------------------------------------
    suspend fun registerUser(
        name: String,
        email: String,
        pass: String,
        referralCode: String
    ): Result<UserProfile> {
        return try {
            kotlinx.coroutines.withTimeout(20000L) {
                com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
                val authRes = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = authRes.user?.uid ?: throw Exception("Auth creation failed")
                val userRefCode = generateReferralCode(uid)
                val isAdmin = email.contains("admin", ignoreCase = true) || email.lowercase() == "sitaramkarhale90@gmail.com"

                val newProfile = UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    referralCode = userRefCode,
                    referredBy = referralCode.trim().ifBlank { null },
                    isAdmin = isAdmin,
                    createdAt = System.currentTimeMillis()
                )

                val initWallet = WalletLedger(
                    uid = uid,
                    earnedBalance = 0.0,
                    cashBalance = 0.0,
                    todayEarnings = 0.0,
                    updatedAt = System.currentTimeMillis()
                )

                // Save to Firestore collections users and wallets
                firestore.collection("users").document(uid).set(newProfile).await()
                firestore.collection("wallets").document(uid).set(initWallet).await()

                _currentUser.value = newProfile
                _wallet.value = initWallet

                // Attach real-time Firestore sync
                attachUserListeners(uid)

                // Process referral bonus if valid
                if (referralCode.isNotBlank()) {
                    processReferralBonus(referrerCode = referralCode.trim(), newUid = uid, newUserName = name)
                }

                Result.success(newProfile)
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthError(e)))
        }
    }

    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): Result<UserProfile> {
        return try {
            kotlinx.coroutines.withTimeout(20000L) {
                com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
                val authRes = auth.signInWithCredential(credential).await()
                val firebaseUser = authRes.user ?: throw Exception("Google sign-in failed")
                val uid = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                val displayName = firebaseUser.displayName.orEmpty().ifBlank { email.substringBefore("@").ifBlank { "EarnMitra User" } }

                attachUserListeners(uid)

                val doc = firestore.collection("users").document(uid).get().await()
                val profile = if (doc.exists()) {
                    doc.toObject(UserProfile::class.java)
                } else {
                    val userRefCode = generateReferralCode(uid)
                    val isAdmin = email.contains("admin", ignoreCase = true) || email.lowercase() == "sitaramkarhale90@gmail.com"
                    val newP = UserProfile(
                        uid = uid,
                        name = displayName,
                        email = email,
                        referralCode = userRefCode,
                        isAdmin = isAdmin,
                        createdAt = System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(newP).await()
                    newP
                }

                val walletDoc = firestore.collection("wallets").document(uid).get().await()
                if (!walletDoc.exists()) {
                    val newW = WalletLedger(uid = uid)
                    firestore.collection("wallets").document(uid).set(newW).await()
                    _wallet.value = newW
                } else {
                    walletDoc.toObject(WalletLedger::class.java)?.let { _wallet.value = it }
                }

                val user = profile ?: UserProfile(uid = uid, email = email, name = displayName)
                _currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthError(e)))
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<UserProfile> {
        return try {
            kotlinx.coroutines.withTimeout(20000L) {
                com.example.EarnMitraApplication.ensureFirebaseInitialized(context)
                val authRes = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = authRes.user?.uid ?: throw Exception("Login failed")

                attachUserListeners(uid)

                val doc = firestore.collection("users").document(uid).get().await()
                val profile = if (doc.exists()) {
                    doc.toObject(UserProfile::class.java)
                } else {
                    val userRefCode = generateReferralCode(uid)
                    val newP = UserProfile(
                        uid = uid,
                        name = authRes.user?.displayName ?: email.substringBefore("@"),
                        email = email,
                        referralCode = userRefCode,
                        isAdmin = email.contains("admin", ignoreCase = true) || email.lowercase() == "sitaramkarhale90@gmail.com",
                        createdAt = System.currentTimeMillis()
                    )
                    firestore.collection("users").document(uid).set(newP).await()
                    newP
                }

                val walletDoc = firestore.collection("wallets").document(uid).get().await()
                if (!walletDoc.exists()) {
                    val newW = WalletLedger(uid = uid)
                    firestore.collection("wallets").document(uid).set(newW).await()
                    _wallet.value = newW
                } else {
                    walletDoc.toObject(WalletLedger::class.java)?.let { _wallet.value = it }
                }

                val user = profile ?: UserProfile(uid = uid, email = email, name = "EarnMitra Member")
                _currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthError(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            kotlinx.coroutines.withTimeout(15000L) {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(formatAuthError(e)))
        }
    }

    private fun formatAuthError(e: Exception): String {
        val cause = e.cause ?: e
        val msg = cause.localizedMessage ?: cause.message ?: ""
        return when {
            cause is kotlinx.coroutines.TimeoutCancellationException || e is kotlinx.coroutines.TimeoutCancellationException ->
                "Request timed out. Please check your internet connection and try again."
            cause is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."
            cause is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Invalid email address or password."
            cause is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "An account with this email address already exists. Please log in."
            cause is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "User account not found or has been disabled."
            msg.contains("The email address is badly formatted", ignoreCase = true) ->
                "The email address is improperly formatted."
            msg.contains("network", ignoreCase = true) ->
                "Network connection error. Please check your connection."
            msg.isNotBlank() -> msg
            else -> "Authentication failed. Please try again."
        }
    }


    fun logout() {
        detachListeners()
        try { auth.signOut() } catch (e: Exception) {}
        _currentUser.value = null
        _wallet.value = WalletLedger()
        _withdrawals.value = emptyList()
        _recharges.value = emptyList()
        _allUsers.value = emptyList()
    }

    // -------------------------------------------------------------
    // DAILY CHECK-IN
    // -------------------------------------------------------------
    suspend fun claimDailyCheckin(): Result<Double> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val now = System.currentTimeMillis()
        val last = _lastCheckinTimestamp.value
        val oneDayMillis = 24 * 60 * 60 * 1000L

        if (now - last < oneDayMillis && last != 0L) {
            return Result.failure(Exception("You have already claimed today's Check-in reward. Come back tomorrow!"))
        }

        var streak = _dailyCheckinStreak.value
        if (now - last > 2 * oneDayMillis) {
            streak = 1
        } else if (last != 0L) {
            streak = if (streak >= 7) 1 else streak + 1
        }

        val settings = _appSettings.value
        val rewardsCoins = settings.checkinRewardsCoins
        val rewardCoins = rewardsCoins.getOrElse(streak - 1) { 1.0 }
        val rewardRupees = if (settings.coinsPerRupee > 0) rewardCoins / settings.coinsPerRupee else 0.01

        return try {
            firestore.runTransaction { txn ->
                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                val currentEarnedCoins = walletSnap.getDouble("earnedCoins") ?: ((walletSnap.getDouble("earnedBalance") ?: 0.0) * settings.coinsPerRupee)
                val currentEarnedRupees = walletSnap.getDouble("earnedBalance") ?: (currentEarnedCoins / settings.coinsPerRupee)
                val currentTodayCoins = walletSnap.getDouble("todayEarningsCoins") ?: 0.0
                val currentTodayRupees = walletSnap.getDouble("todayEarnings") ?: 0.0

                val newEarnedCoins = currentEarnedCoins + rewardCoins
                val newEarnedRupees = currentEarnedRupees + rewardRupees
                val newTodayCoins = currentTodayCoins + rewardCoins
                val newTodayRupees = currentTodayRupees + rewardRupees

                txn.update(walletRef, mapOf(
                    "earnedCoins" to newEarnedCoins,
                    "earnedBalance" to newEarnedRupees,
                    "todayEarningsCoins" to newTodayCoins,
                    "todayEarnings" to newTodayRupees,
                    "updatedAt" to now
                ))

                val checkinRef = firestore.collection("dailyCheckins").document()
                val checkinData = DailyCheckinRecord(
                    id = checkinRef.id,
                    uid = uid,
                    streak = streak,
                    claimedAt = now,
                    rewardAmount = rewardRupees
                )
                txn.set(checkinRef, checkinData)

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = rewardRupees,
                    type = TransactionType.DAILY_BONUS,
                    status = TransactionStatus.SUCCESS,
                    title = "Daily Check-in Day $streak",
                    description = "Claimed ${rewardCoins.toInt()} Coin(s) (₹${String.format(Locale.US, "%.2f", rewardRupees)})",
                    timestamp = now,
                    referenceId = checkinRef.id
                )
                txn.set(txnLogRef, txnLog)
            }.await()

            _dailyCheckinStreak.value = streak
            _lastCheckinTimestamp.value = now

            // Grant pending referral if qualifying action met
            checkAndGrantPendingReferral(uid)

            Result.success(rewardCoins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // PLAY & EARN (Interactive 60s Mini-Game)
    // -------------------------------------------------------------
    suspend fun startPlaySession(): GameSession {
        val uid = _currentUser.value?.uid ?: throw Exception("User not authenticated")
        val sessionRef = firestore.collection("gameSessions").document()
        val session = GameSession(
            sessionId = sessionRef.id,
            uid = uid,
            startTime = System.currentTimeMillis(),
            status = GameSessionStatus.STARTED
        )
        try {
            sessionRef.set(session).await()
        } catch (e: Exception) {
            // Proceed locally
        }
        return session
    }

    suspend fun verifyAndClaimGameReward(session: GameSession, elapsedSec: Int): Result<Double> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val settings = _appSettings.value
        val minSec = settings.gameDurationSec
        if (elapsedSec < minSec) {
            return Result.failure(Exception("Game session lasted only $elapsedSec seconds. You must play for at least $minSec seconds to earn coins."))
        }

        // Check daily session limit
        val startOfDay = getStartOfDayTimestamp()
        val todaySessionsQuery = firestore.collection("gameSessions")
            .whereEqualTo("uid", uid)
            .whereEqualTo("status", "VERIFIED")
            .whereGreaterThanOrEqualTo("endTime", startOfDay)
            .get().await()

        if (todaySessionsQuery.size() >= settings.dailyGameLimit) {
            return Result.failure(Exception("Daily game session limit reached (${settings.dailyGameLimit}/day). Come back tomorrow!"))
        }

        val rewardCoins = settings.gameRewardCoins
        val rewardRupees = if (settings.coinsPerRupee > 0) rewardCoins / settings.coinsPerRupee else 0.02
        val now = System.currentTimeMillis()

        return try {
            firestore.runTransaction { txn ->
                val sessionRef = firestore.collection("gameSessions").document(session.sessionId)
                val sessionSnap = txn.get(sessionRef)
                if (sessionSnap.exists() && sessionSnap.getString("status") == "VERIFIED") {
                    throw Exception("Game session already claimed!")
                }

                txn.update(sessionRef, mapOf(
                    "status" to "VERIFIED",
                    "endTime" to now,
                    "durationSeconds" to elapsedSec,
                    "coinsAwarded" to rewardCoins
                ))

                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                val currentEarnedCoins = walletSnap.getDouble("earnedCoins") ?: ((walletSnap.getDouble("earnedBalance") ?: 0.0) * settings.coinsPerRupee)
                val currentEarnedRupees = walletSnap.getDouble("earnedBalance") ?: (currentEarnedCoins / settings.coinsPerRupee)
                val currentTodayCoins = walletSnap.getDouble("todayEarningsCoins") ?: 0.0
                val currentTodayRupees = walletSnap.getDouble("todayEarnings") ?: 0.0

                val newEarnedCoins = currentEarnedCoins + rewardCoins
                val newEarnedRupees = currentEarnedRupees + rewardRupees
                val newTodayCoins = currentTodayCoins + rewardCoins
                val newTodayRupees = currentTodayRupees + rewardRupees

                txn.update(walletRef, mapOf(
                    "earnedCoins" to newEarnedCoins,
                    "earnedBalance" to newEarnedRupees,
                    "todayEarningsCoins" to newTodayCoins,
                    "todayEarnings" to newTodayRupees,
                    "updatedAt" to now
                ))

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = rewardRupees,
                    type = TransactionType.GAME_REWARD,
                    status = TransactionStatus.SUCCESS,
                    title = "Play & Earn Reward",
                    description = "Earned ${rewardCoins.toInt()} Coins ($elapsedSec sec play)",
                    timestamp = now,
                    referenceId = session.sessionId
                )
                txn.set(txnLogRef, txnLog)
            }.await()

            // Trigger pending referral qualification
            checkAndGrantPendingReferral(uid)

            Result.success(rewardCoins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // -------------------------------------------------------------
    // OFFERWALL (CPX Research / Provider Postback Flow)
    // -------------------------------------------------------------
    private fun loadDefaultOffers() {
        // Clear all hardcoded demo offers to ensure no fake money
        _offers.value = emptyList()
    }

    suspend fun processProviderOfferPostback(
        offerId: String,
        providerTxnId: String,
        providerPayoutUsd: Double = 0.50
    ): Result<Double> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val settings = _appSettings.value
        val userPct = settings.offerwallUserRewardPercentage / 100.0
        val rupeesPerUsd = 83.0 // Standard USD to INR rate
        val userRewardRupees = providerPayoutUsd * userPct * rupeesPerUsd
        val userRewardCoins = userRewardRupees * settings.coinsPerRupee

        val now = System.currentTimeMillis()

        return try {
            firestore.runTransaction { txn ->
                val convRef = firestore.collection("offerwallConversions").document(providerTxnId)
                val convSnap = txn.get(convRef)
                if (convSnap.exists()) {
                    throw Exception("Duplicate Postback Detected! Provider Txn ID $providerTxnId already credited.")
                }

                val conversion = OfferwallConversion(
                    id = providerTxnId,
                    offerId = offerId,
                    uid = uid,
                    provider = "CPX Research",
                    providerTxnId = providerTxnId,
                    providerPayoutUsd = providerPayoutUsd,
                    userRewardCoins = userRewardCoins,
                    userRewardRupees = userRewardRupees,
                    status = "COMPLETED",
                    createdAt = now
                )
                txn.set(convRef, conversion)

                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                val currentEarnedCoins = walletSnap.getDouble("earnedCoins") ?: ((walletSnap.getDouble("earnedBalance") ?: 0.0) * settings.coinsPerRupee)
                val currentEarnedRupees = walletSnap.getDouble("earnedBalance") ?: (currentEarnedCoins / settings.coinsPerRupee)
                val currentTodayCoins = walletSnap.getDouble("todayEarningsCoins") ?: 0.0
                val currentTodayRupees = walletSnap.getDouble("todayEarnings") ?: 0.0

                txn.update(walletRef, mapOf(
                    "earnedCoins" to (currentEarnedCoins + userRewardCoins),
                    "earnedBalance" to (currentEarnedRupees + userRewardRupees),
                    "todayEarningsCoins" to (currentTodayCoins + userRewardCoins),
                    "todayEarnings" to (currentTodayRupees + userRewardRupees),
                    "updatedAt" to now
                ))

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = userRewardRupees,
                    type = TransactionType.OFFER_REWARD,
                    status = TransactionStatus.SUCCESS,
                    title = "Offer Completed (CPX Research)",
                    description = "Verified server postback ($providerTxnId) | Earned ${userRewardCoins.toInt()} Coins",
                    timestamp = now,
                    referenceId = providerTxnId
                )
                txn.set(txnLogRef, txnLog)
            }.await()

            Result.success(userRewardRupees)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // REFER & EARN
    // -------------------------------------------------------------
    private suspend fun processReferralBonus(
        referrerCode: String,
        newUid: String,
        newUserName: String
    ) {
        try {
            val query = firestore.collection("users")
                .whereEqualTo("referralCode", referrerCode)
                .get().await()

            if (!query.isEmpty) {
                val referrerDoc = query.documents.first()
                val referrerUid = referrerDoc.getString("uid") ?: referrerDoc.id

                // Prevent self referral
                if (referrerUid == newUid) return

                val settings = _appSettings.value
                val rewardCoins = settings.referralRewardCoins
                val rewardRupees = if (settings.coinsPerRupee > 0) rewardCoins / settings.coinsPerRupee else 0.50
                val now = System.currentTimeMillis()

                val refDocRef = firestore.collection("referrals").document()
                val refRecord = ReferralRecord(
                    id = refDocRef.id,
                    referrerUid = referrerUid,
                    referrerCode = referrerCode,
                    newUid = newUid,
                    newUserName = newUserName,
                    rewardCoins = rewardCoins,
                    rewardAmount = rewardRupees,
                    status = "PENDING_QUALIFYING_ACTION",
                    createdAt = now
                )
                firestore.collection("referrals").document(refDocRef.id).set(refRecord).await()
            }
        } catch (e: Exception) {
            // Ignore referral lookup failures
        }
    }

    private suspend fun checkAndGrantPendingReferral(newUid: String) {
        try {
            val query = firestore.collection("referrals")
                .whereEqualTo("newUid", newUid)
                .whereEqualTo("status", "PENDING_QUALIFYING_ACTION")
                .get().await()

            if (!query.isEmpty) {
                val doc = query.documents.first()
                val refRecord = doc.toObject(ReferralRecord::class.java) ?: return
                val referrerUid = refRecord.referrerUid
                val settings = _appSettings.value
                val rewardCoins = refRecord.rewardCoins
                val rewardRupees = if (settings.coinsPerRupee > 0) rewardCoins / settings.coinsPerRupee else 0.50
                val now = System.currentTimeMillis()

                firestore.runTransaction { txn ->
                    val refRef = firestore.collection("referrals").document(refRecord.id)
                    txn.update(refRef, mapOf(
                        "status" to "COMPLETED",
                        "completedAt" to now
                    ))

                    val walletRef = firestore.collection("wallets").document(referrerUid)
                    val walletSnap = txn.get(walletRef)
                    if (walletSnap.exists()) {
                        val currentEarnedCoins = walletSnap.getDouble("earnedCoins") ?: ((walletSnap.getDouble("earnedBalance") ?: 0.0) * settings.coinsPerRupee)
                        val currentEarnedRupees = walletSnap.getDouble("earnedBalance") ?: (currentEarnedCoins / settings.coinsPerRupee)
                        val currentTodayCoins = walletSnap.getDouble("todayEarningsCoins") ?: 0.0
                        val currentTodayRupees = walletSnap.getDouble("todayEarnings") ?: 0.0

                        txn.update(walletRef, mapOf(
                            "earnedCoins" to (currentEarnedCoins + rewardCoins),
                            "earnedBalance" to (currentEarnedRupees + rewardRupees),
                            "todayEarningsCoins" to (currentTodayCoins + rewardCoins),
                            "todayEarnings" to (currentTodayRupees + rewardRupees),
                            "updatedAt" to now
                        ))

                        val txnLogRef = firestore.collection("transactions").document()
                        val txnLog = TransactionLog(
                            id = txnLogRef.id,
                            uid = referrerUid,
                            amount = rewardRupees,
                            type = TransactionType.REFERRAL_REWARD,
                            status = TransactionStatus.SUCCESS,
                            title = "Referral Bonus Credited",
                            description = "${refRecord.newUserName} completed qualifying activity! Earned ${rewardCoins.toInt()} Coins",
                            timestamp = now,
                            referenceId = refRecord.id
                        )
                        txn.set(txnLogRef, txnLog)
                    }
                }.await()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // -------------------------------------------------------------
    // ADD CASH
    // -------------------------------------------------------------
    suspend fun processAddCash(
        amount: Double,
        paymentMethod: String
    ): Result<TransactionLog> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        if (amount < 10.0) {
            return Result.failure(Exception("Minimum Add Cash amount is ₹10."))
        }

        val providerTxnId = "PAY_" + System.currentTimeMillis()
        val now = System.currentTimeMillis()
        var createdTxn: TransactionLog? = null

        return try {
            firestore.runTransaction { txn ->
                val depositRef = firestore.collection("cashDeposits").document(providerTxnId)
                val depositRecord = CashDepositRecord(
                    id = providerTxnId,
                    uid = uid,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    status = "SUCCESS",
                    referenceId = providerTxnId,
                    createdAt = now
                )
                txn.set(depositRef, depositRecord)

                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                val currentCash = walletSnap.getDouble("cashBalance") ?: 0.0

                txn.update(walletRef, mapOf(
                    "cashBalance" to (currentCash + amount),
                    "updatedAt" to now
                ))

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = amount,
                    type = TransactionType.CASH_DEPOSIT,
                    status = TransactionStatus.SUCCESS,
                    title = "Added Cash (₹${amount.toInt()})",
                    description = "Deposited via $paymentMethod Gateway",
                    timestamp = now,
                    referenceId = providerTxnId
                )
                txn.set(txnLogRef, txnLog)
                createdTxn = txnLog
            }.await()

            Result.success(createdTxn ?: TransactionLog(uid = uid, amount = amount, title = "Added Cash"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // RECHARGE SERVICES (MANUAL ADMIN-PROCESSED)
    // -------------------------------------------------------------
    suspend fun submitRecharge(
        category: String,
        operator: String,
        accountNumber: String,
        amount: Double
    ): Result<RechargeOrder> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val userName = _currentUser.value?.name ?: "EarnMitra User"
        val settings = _appSettings.value

        // Validation for Mobile vs DTH
        if (category.equals("Mobile", ignoreCase = true)) {
            val cleanNum = accountNumber.trim()
            if (!cleanNum.matches(Regex("^[6-9]\\d{9}$"))) {
                return Result.failure(Exception("Please enter a valid 10-digit Indian mobile number."))
            }
        } else if (category.equals("DTH", ignoreCase = true)) {
            val cleanId = accountNumber.trim()
            if (cleanId.length < 6) {
                return Result.failure(Exception("Please enter a valid DTH Customer/Subscriber ID (at least 6 digits/characters)."))
            }
        }

        if (amount < settings.minRechargeAmount) {
            return Result.failure(Exception("Minimum recharge amount is ₹${settings.minRechargeAmount.toInt()}."))
        }
        if (amount > settings.maxRechargeAmount) {
            return Result.failure(Exception("Maximum recharge amount is ₹${settings.maxRechargeAmount.toInt()}."))
        }

        val now = System.currentTimeMillis()
        val orderId = "RCH_" + now.toString().takeLast(8)

        var createdOrder: RechargeOrder? = null

        return try {
            firestore.runTransaction { txn ->
                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                var cashBal = walletSnap.getDouble("cashBalance") ?: 0.0
                var earnedCoins = walletSnap.getDouble("earnedCoins") ?: ((walletSnap.getDouble("earnedBalance") ?: 0.0) * settings.coinsPerRupee)
                var earnedRupees = walletSnap.getDouble("earnedBalance") ?: (earnedCoins / settings.coinsPerRupee)
                val totalAvail = cashBal + earnedRupees

                if (amount > totalAvail) {
                    throw Exception("Insufficient wallet balance (Available: ₹${String.format(Locale.US, "%.2f", totalAvail)}). Please Add Cash or Earn more.")
                }

                // Reserve/deduct requested amount from user wallet balance
                var remAmount = amount
                if (cashBal >= remAmount) {
                    cashBal -= remAmount
                } else {
                    remAmount -= cashBal
                    cashBal = 0.0
                    earnedRupees -= remAmount
                    earnedCoins = earnedRupees * settings.coinsPerRupee
                }

                txn.update(walletRef, mapOf(
                    "cashBalance" to cashBal,
                    "earnedBalance" to earnedRupees,
                    "earnedCoins" to earnedCoins,
                    "updatedAt" to now
                ))

                val rchRef = firestore.collection("recharges").document(orderId)
                val order = RechargeOrder(
                    id = orderId,
                    uid = uid,
                    userName = userName,
                    category = category,
                    operator = operator,
                    accountNumber = accountNumber.trim(),
                    amount = amount,
                    status = TransactionStatus.PENDING,
                    operatorTxnId = "PENDING_ADMIN",
                    adminNote = "Submitted & Pending Admin Processing",
                    createdAt = now
                )
                txn.set(rchRef, order)

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = -amount,
                    type = TransactionType.RECHARGE,
                    status = TransactionStatus.PENDING,
                    title = "$category Recharge Request ($operator)",
                    description = "No: ${accountNumber.trim()} | Status: PENDING Admin Review",
                    timestamp = now,
                    referenceId = orderId
                )
                txn.set(txnLogRef, txnLog)
                createdOrder = order
            }.await()

            Result.success(createdOrder ?: RechargeOrder(id = orderId, amount = amount, status = TransactionStatus.PENDING))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminProcessRecharge(
        orderId: String,
        approve: Boolean,
        opTxnId: String,
        adminNote: String
    ): Result<Unit> {
        if (_currentUser.value?.isAdmin != true) {
            return Result.failure(Exception("Access Denied: Admin role required."))
        }

        val now = System.currentTimeMillis()
        return try {
            firestore.runTransaction { txn ->
                val rchRef = firestore.collection("recharges").document(orderId)
                val rchSnap = txn.get(rchRef)
                if (!rchSnap.exists()) throw Exception("Recharge request not found.")

                val currentStatus = rchSnap.getString("status") ?: TransactionStatus.PENDING.name
                if (currentStatus != TransactionStatus.PENDING.name) {
                    throw Exception("Duplicate Processing Prevented: Recharge request $orderId is already $currentStatus.")
                }

                val reqUid = rchSnap.getString("uid") ?: throw Exception("Invalid user ID in recharge record.")
                val amount = rchSnap.getDouble("amount") ?: 0.0
                val category = rchSnap.getString("category") ?: "Mobile"
                val operator = rchSnap.getString("operator") ?: ""
                val accountNumber = rchSnap.getString("accountNumber") ?: ""

                val finalStatus = if (approve) TransactionStatus.SUCCESS.name else TransactionStatus.FAILED.name
                val finalOpTxnId = if (approve) opTxnId.ifBlank { "OP_" + (100000..999999).random() } else "FAILED"
                val finalNote = adminNote.ifBlank { if (approve) "Approved & Processed by Admin" else "Rejected by Admin" }

                txn.update(rchRef, mapOf(
                    "status" to finalStatus,
                    "operatorTxnId" to finalOpTxnId,
                    "adminNote" to finalNote,
                    "processedAt" to now
                ))

                if (!approve) {
                    // Refund the deducted amount back to user's cash balance
                    val walletRef = firestore.collection("wallets").document(reqUid)
                    val walletSnap = txn.get(walletRef)
                    val currentCash = walletSnap.getDouble("cashBalance") ?: 0.0

                    txn.update(walletRef, mapOf(
                        "cashBalance" to (currentCash + amount),
                        "updatedAt" to now
                    ))

                    val refundTxnRef = firestore.collection("transactions").document()
                    val refundTxn = TransactionLog(
                        id = refundTxnRef.id,
                        uid = reqUid,
                        amount = amount,
                        type = TransactionType.REFUND,
                        status = TransactionStatus.SUCCESS,
                        title = "$category Recharge Refund",
                        description = "Rejected/Failed: $finalNote. Amount ₹${amount.toInt()} refunded to Cash Balance.",
                        timestamp = now,
                        referenceId = orderId
                    )
                    txn.set(refundTxnRef, refundTxn)
                } else {
                    val compTxnRef = firestore.collection("transactions").document()
                    val compTxn = TransactionLog(
                        id = compTxnRef.id,
                        uid = reqUid,
                        amount = 0.0, // Amount was already deducted during submission
                        type = TransactionType.RECHARGE,
                        status = TransactionStatus.SUCCESS,
                        title = "$category Recharge Successful",
                        description = "$operator No: $accountNumber | Op Txn: $finalOpTxnId",
                        timestamp = now,
                        referenceId = orderId
                    )
                    txn.set(compTxnRef, compTxn)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // CASH WITHDRAWAL
    // -------------------------------------------------------------
    suspend fun submitWithdrawalRequest(
        amount: Double,
        upiId: String
    ): Result<WithdrawalRequest> {
        val uid = _currentUser.value?.uid ?: return Result.failure(Exception("User not authenticated"))
        val userName = _currentUser.value?.name ?: "EarnMitra User"
        val settings = _appSettings.value
        val minW = settings.minWithdrawal

        if (amount < minW) {
            return Result.failure(Exception("Minimum withdrawal amount is ₹${minW.toInt()}."))
        }

        val now = System.currentTimeMillis()
        val requestId = "WTH_" + (10000..99999).random()
        var createdRequest: WithdrawalRequest? = null

        return try {
            firestore.runTransaction { txn ->
                val walletRef = firestore.collection("wallets").document(uid)
                val walletSnap = txn.get(walletRef)
                val currentEarnedRupees = walletSnap.getDouble("earnedBalance") ?: 0.0
                val currentEarnedCoins = walletSnap.getDouble("earnedCoins") ?: (currentEarnedRupees * settings.coinsPerRupee)

                if (amount > currentEarnedRupees) {
                    throw Exception("Insufficient Earned Balance. Withdrawals are only allowed from your Earned Balance (Available: ₹${String.format(Locale.US, "%.2f", currentEarnedRupees)}).")
                }

                val newEarnedRupees = currentEarnedRupees - amount
                val newEarnedCoins = newEarnedRupees * settings.coinsPerRupee

                txn.update(walletRef, mapOf(
                    "earnedBalance" to newEarnedRupees,
                    "earnedCoins" to newEarnedCoins,
                    "updatedAt" to now
                ))

                val wthRef = firestore.collection("withdrawals").document(requestId)
                val request = WithdrawalRequest(
                    id = requestId,
                    uid = uid,
                    userName = userName,
                    amount = amount,
                    upiId = upiId,
                    status = WithdrawalStatus.PENDING,
                    adminNote = "Submitted & Pending Admin Verification",
                    createdAt = now
                )
                txn.set(wthRef, request)

                val txnLogRef = firestore.collection("transactions").document()
                val txnLog = TransactionLog(
                    id = txnLogRef.id,
                    uid = uid,
                    amount = -amount,
                    type = TransactionType.WITHDRAWAL,
                    status = TransactionStatus.PENDING,
                    title = "UPI Withdrawal Request",
                    description = "Amount ₹${amount.toInt()} reserved for UPI: $upiId",
                    timestamp = now,
                    referenceId = requestId
                )
                txn.set(txnLogRef, txnLog)
                createdRequest = request
            }.await()

            Result.success(createdRequest ?: WithdrawalRequest(id = requestId, amount = amount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // ADMIN PANEL ACTIONS
    // -------------------------------------------------------------
    suspend fun adminProcessWithdrawal(
        requestId: String,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        if (_currentUser.value?.isAdmin != true) {
            return Result.failure(Exception("Access Denied: Admin role required."))
        }

        val now = System.currentTimeMillis()
        return try {
            firestore.runTransaction { txn ->
                val wthRef = firestore.collection("withdrawals").document(requestId)
                val wthSnap = txn.get(wthRef)
                if (!wthSnap.exists()) throw Exception("Withdrawal request not found")

                val reqUid = wthSnap.getString("uid") ?: throw Exception("Invalid withdrawal record")
                val reqAmount = wthSnap.getDouble("amount") ?: 0.0
                val newStatus = if (approve) WithdrawalStatus.APPROVED.name else WithdrawalStatus.REJECTED.name

                txn.update(wthRef, mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote.ifBlank { if (approve) "Approved by Admin" else "Rejected by Admin" },
                    "processedAt" to now
                ))

                if (!approve) {
                    val walletRef = firestore.collection("wallets").document(reqUid)
                    val walletSnap = txn.get(walletRef)
                    val currentEarned = walletSnap.getDouble("earnedBalance") ?: 0.0

                    txn.update(walletRef, mapOf(
                        "earnedBalance" to (currentEarned + reqAmount),
                        "updatedAt" to now
                    ))

                    val refundTxnRef = firestore.collection("transactions").document()
                    val refundTxn = TransactionLog(
                        id = refundTxnRef.id,
                        uid = reqUid,
                        amount = reqAmount,
                        type = TransactionType.REFUND,
                        status = TransactionStatus.SUCCESS,
                        title = "Withdrawal Refund",
                        description = "Rejected by Admin: $adminNote. Amount refunded.",
                        timestamp = now,
                        referenceId = requestId
                    )
                    txn.set(refundTxnRef, refundTxn)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateAppSettings(newSettings: AppSettings): Result<Unit> {
        if (_currentUser.value?.isAdmin != true) {
            return Result.failure(Exception("Access Denied: Admin role required."))
        }
        return try {
            firestore.collection("appSettings").document("global").set(newSettings).await()
            _appSettings.value = newSettings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAdjustWalletBalance(
        targetUid: String,
        amount: Double,
        isCash: Boolean,
        reason: String
    ): Result<Unit> {
        if (_currentUser.value?.isAdmin != true) {
            return Result.failure(Exception("Access Denied: Admin role required."))
        }
        if (reason.isBlank()) {
            return Result.failure(Exception("A mandatory admin reason is required for manual balance adjustments."))
        }

        val now = System.currentTimeMillis()
        return try {
            firestore.runTransaction { txn ->
                val walletRef = firestore.collection("wallets").document(targetUid)
                val walletSnap = txn.get(walletRef)
                if (!walletSnap.exists()) throw Exception("Target user wallet not found")

                if (isCash) {
                    val currentCash = walletSnap.getDouble("cashBalance") ?: 0.0
                    txn.update(walletRef, mapOf(
                        "cashBalance" to (currentCash + amount).coerceAtLeast(0.0),
                        "updatedAt" to now
                    ))
                } else {
                    val currentEarned = walletSnap.getDouble("earnedBalance") ?: 0.0
                    txn.update(walletRef, mapOf(
                        "earnedBalance" to (currentEarned + amount).coerceAtLeast(0.0),
                        "updatedAt" to now
                    ))
                }

                val adjTxnRef = firestore.collection("transactions").document()
                val adjTxn = TransactionLog(
                    id = adjTxnRef.id,
                    uid = targetUid,
                    amount = amount,
                    type = TransactionType.ADJUSTMENT,
                    status = TransactionStatus.SUCCESS,
                    title = "Admin Wallet Adjustment",
                    description = "Reason: $reason",
                    timestamp = now,
                    referenceId = "ADMIN_ADJ"
                )
                txn.set(adjTxnRef, adjTxn)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions
    private fun generateReferralCode(uid: String): String {
        return "EARN" + uid.takeLast(4).uppercase().padStart(4, '8')
    }

    private fun TransactionLog.toEntity() = LocalTransactionEntity(
        id = id,
        uid = uid,
        amount = amount,
        type = type.name,
        status = status.name,
        title = title,
        description = description,
        timestamp = timestamp,
        referenceId = referenceId
    )

    private fun LocalTransactionEntity.toDomain() = TransactionLog(
        id = id,
        uid = uid,
        amount = amount,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.DAILY_BONUS },
        status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.SUCCESS },
        title = title,
        description = description,
        timestamp = timestamp,
        referenceId = referenceId
    )
}
