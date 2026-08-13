package com.example.data.model

import androidx.annotation.DrawableRes

/**
 * Core User Model
 */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val referralCode: String = "",
    val referredBy: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Separate Ledgers Wallet Model
 * totalBalance is computed from earnedBalance + cashBalance
 */
data class WalletLedger(
    val uid: String = "",
    val earnedCoins: Double = 0.0,
    val earnedBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val todayEarningsCoins: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalBalance: Double
        get() = earnedBalance + cashBalance

    fun totalBalanceInRupees(coinsPerRupee: Double = 100.0): Double {
        val earnedRupees = if (coinsPerRupee > 0) earnedCoins / coinsPerRupee else earnedBalance
        return earnedRupees + cashBalance
    }

    fun earnedInRupees(coinsPerRupee: Double = 100.0): Double {
        return if (coinsPerRupee > 0 && earnedCoins > 0) earnedCoins / coinsPerRupee else earnedBalance
    }
}

/**
 * Transaction Types
 */
enum class TransactionType {
    OFFER_REWARD,
    GAME_REWARD,
    DAILY_BONUS,
    REFERRAL_REWARD,
    CASH_DEPOSIT,
    RECHARGE,
    WITHDRAWAL,
    REFUND,
    ADJUSTMENT
}

/**
 * Transaction Status
 */
enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

/**
 * Transaction Log Item
 */
data class TransactionLog(
    val id: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.DAILY_BONUS,
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String = ""
)

/**
 * Offerwall Item
 */
enum class OfferStatus {
    AVAILABLE,
    PENDING,
    COMPLETED
}

data class OfferItem(
    val id: String,
    val title: String,
    val description: String,
    val reward: Double,
    val provider: String,
    val status: OfferStatus = OfferStatus.AVAILABLE,
    val requirement: String,
    val estMinutes: Int = 5,
    val providerTxnId: String = ""
)

/**
 * Game Session
 */
enum class GameSessionStatus {
    STARTED,
    COMPLETED,
    VERIFIED,
    REJECTED
}

data class GameSession(
    val sessionId: String,
    val uid: String,
    val startTime: Long,
    val endTime: Long = 0,
    val durationSeconds: Int = 0,
    val coinsAwarded: Double = 2.0,
    val status: GameSessionStatus = GameSessionStatus.STARTED
)

/**
 * Withdrawal Request
 */
enum class WithdrawalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class WithdrawalRequest(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val upiId: String = "",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

/**
 * Recharge Order
 */
data class RechargeOrder(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val category: String = "Mobile", // Mobile, DTH
    val operator: String = "",
    val accountNumber: String = "",
    val amount: Double = 0.0,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val operatorTxnId: String = "",
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

/**
 * Daily Check-in Record
 */
data class DailyCheckinRecord(
    val id: String = "",
    val uid: String = "",
    val streak: Int = 1,
    val claimedAt: Long = System.currentTimeMillis(),
    val rewardAmount: Double = 1.0
)

/**
 * Referral Record
 */
data class ReferralRecord(
    val id: String = "",
    val referrerUid: String = "",
    val referrerCode: String = "",
    val newUid: String = "",
    val newUserName: String = "",
    val rewardCoins: Double = 50.0,
    val rewardAmount: Double = 0.50,
    val status: String = "PENDING_QUALIFYING_ACTION", // PENDING_QUALIFYING_ACTION, COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Offerwall Conversion Record
 */
data class OfferwallConversion(
    val id: String = "",
    val offerId: String = "",
    val uid: String = "",
    val provider: String = "CPX Research",
    val providerTxnId: String = "",
    val providerPayoutUsd: Double = 0.0,
    val userRewardCoins: Double = 0.0,
    val userRewardRupees: Double = 0.0,
    val status: String = "COMPLETED",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cash Deposit Record
 */
data class CashDepositRecord(
    val id: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "SUCCESS",
    val referenceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * App Security & Config Settings
 * Fully configurable via Firestore appSettings/global by Admin
 */
data class AppSettings(
    val coinsPerRupee: Double = 100.0,             // 100 Coins = ₹1
    val gameDurationSec: Int = 60,                // 60 seconds active play
    val gameRewardCoins: Double = 2.0,             // 2 Coins per session
    val dailyGameLimit: Int = 10,                 // Daily limit
    val checkinRewardsCoins: List<Double> = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0), // 7-day cycle: Day 1 = 1 Coin...
    val referralRewardCoins: Double = 50.0,        // Configurable referral reward (50 coins = ₹0.50)
    val minWithdrawal: Double = 50.0,             // Min withdrawal in ₹
    val offerwallUserRewardPercentage: Double = 80.0, // Offerwall user payout share %
    val minRechargeAmount: Double = 10.0,          // Min recharge amount ₹10
    val maxRechargeAmount: Double = 1000.0         // Max recharge amount ₹1000
)

/**
 * Provider Integration Architecture (CPX Research / Approved Provider)
 */
interface OfferwallProvider {
    val providerName: String
    fun buildSurveyIframeUrl(uid: String): String
    fun verifyCallback(params: Map<String, String>): Boolean
}

class CPXResearchProviderIntegration(
    private val appId: String = "cpx_earnmitra_app_id",
    val userRewardPercentage: Double = 80.0
) : OfferwallProvider {
    override val providerName: String = "CPX Research"

    override fun buildSurveyIframeUrl(uid: String): String {
        return "https://offers.cpx-research.com/index.php?app_id=$appId&ext_user_id=$uid"
    }

    override fun verifyCallback(params: Map<String, String>): Boolean {
        // Postback verification relies on server-side IP & signature checks
        val transId = params["trans_id"] ?: ""
        val status = params["status"] ?: ""
        return transId.isNotBlank() && (status == "1" || status == "completed")
    }
}
