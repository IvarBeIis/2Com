package cz.twocom.core.crypto.store

import android.content.Context
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import cz.twocom.core.crypto.KeystoreEnvelope
import kotlinx.coroutines.flow.firstOrNull
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Entity(tableName = "signal_prekeys")
data class PreKeyRow(@PrimaryKey val id: Int, val data: ByteArray)

@Entity(tableName = "signal_signed_prekeys")
data class SignedPreKeyRow(@PrimaryKey val id: Int, val data: ByteArray)

@Entity(tableName = "signal_kyber_prekeys")
data class KyberPreKeyRow(@PrimaryKey val id: Int, val data: ByteArray, val used: Boolean = false)

@Entity(tableName = "signal_sessions")
data class SessionRow(@PrimaryKey val address: String, val data: ByteArray)

@Entity(tableName = "signal_trusted_identities")
data class TrustedIdentityRow(@PrimaryKey val address: String, val identityKeyBytes: ByteArray)

@Dao
interface SignalStoreDao {
    @Query("SELECT * FROM signal_prekeys WHERE id = :id")
    fun findPreKey(id: Int): PreKeyRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putPreKey(row: PreKeyRow)

    @Query("DELETE FROM signal_prekeys WHERE id = :id")
    fun deletePreKey(id: Int)

    @Query("SELECT id FROM signal_prekeys")
    fun allPreKeyIds(): List<Int>

    @Query("SELECT * FROM signal_signed_prekeys WHERE id = :id")
    fun findSignedPreKey(id: Int): SignedPreKeyRow?

    @Query("SELECT * FROM signal_signed_prekeys")
    fun allSignedPreKeys(): List<SignedPreKeyRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putSignedPreKey(row: SignedPreKeyRow)

    @Query("DELETE FROM signal_signed_prekeys WHERE id = :id")
    fun deleteSignedPreKey(id: Int)

    @Query("SELECT * FROM signal_kyber_prekeys WHERE id = :id")
    fun findKyberPreKey(id: Int): KyberPreKeyRow?

    @Query("SELECT * FROM signal_kyber_prekeys")
    fun allKyberPreKeys(): List<KyberPreKeyRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putKyberPreKey(row: KyberPreKeyRow)

    @Query("UPDATE signal_kyber_prekeys SET used = 1 WHERE id = :id")
    fun markKyberPreKeyUsed(id: Int)

    @Query("SELECT * FROM signal_sessions WHERE address = :address")
    fun findSession(address: String): SessionRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putSession(row: SessionRow)

    @Query("DELETE FROM signal_sessions WHERE address = :address")
    fun deleteSession(address: String)

    @Query("DELETE FROM signal_sessions WHERE address LIKE :namePrefix || ':%'")
    fun deleteAllSessionsForName(namePrefix: String)

    @Query("SELECT address FROM signal_sessions WHERE address LIKE :namePrefix || ':%'")
    fun sessionAddressesForName(namePrefix: String): List<String>

    @Query("SELECT * FROM signal_trusted_identities WHERE address = :address")
    fun findTrustedIdentity(address: String): TrustedIdentityRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putTrustedIdentity(row: TrustedIdentityRow)
}

@Database(
    entities = [PreKeyRow::class, SignedPreKeyRow::class, KyberPreKeyRow::class, SessionRow::class, TrustedIdentityRow::class],
    version = 1,
    exportSchema = false,
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun dao(): SignalStoreDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): SignalDatabase =
            Room.databaseBuilder(context, SignalDatabase::class.java, "2com-signal-store.db")
                .openHelperFactory(SupportFactory(passphrase))
                .build()
    }
}

private val Context.signalDbKeyStore by preferencesDataStore("signal_db_key")
private const val KEYSTORE_ALIAS_SIGNAL_DB = "signal_db_wrap_v1"

/** Same envelope-encryption pattern as the main app DB passphrase (DatabaseModule). */
suspend fun getOrCreateSignalDbKey(context: Context, envelope: KeystoreEnvelope): ByteArray {
    val prefKey = byteArrayPreferencesKey("signal_db_passphrase_sealed")
    val prefs = context.signalDbKeyStore.data.firstOrNull()
    prefs?.get(prefKey)?.let { return envelope.open(KEYSTORE_ALIAS_SIGNAL_DB, it) }
    val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
    context.signalDbKeyStore.edit { it[prefKey] = envelope.seal(KEYSTORE_ALIAS_SIGNAL_DB, newKey) }
    return newKey
}
