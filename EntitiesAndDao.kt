package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val amount: Double,
    val type: String,
    val status: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val referenceId: String
)

@Entity(tableName = "user_wallet_cache")
data class LocalWalletEntity(
    @PrimaryKey val uid: String,
    val earnedCoins: Double = 0.0,
    val earnedBalance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface EarnMitraDao {
    @Query("SELECT * FROM local_transactions WHERE uid = :uid ORDER BY timestamp DESC")
    fun getTransactionsForUser(uid: String): Flow<List<LocalTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LocalTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransactionEntity)

    @Query("SELECT * FROM user_wallet_cache WHERE uid = :uid LIMIT 1")
    fun getWalletForUser(uid: String): Flow<LocalWalletEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: LocalWalletEntity)

    @Query("DELETE FROM local_transactions WHERE uid = :uid")
    suspend fun clearUserTransactions(uid: String)
}

@Database(
    entities = [LocalTransactionEntity::class, LocalWalletEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EarnMitraDatabase : RoomDatabase() {
    abstract fun dao(): EarnMitraDao
}
