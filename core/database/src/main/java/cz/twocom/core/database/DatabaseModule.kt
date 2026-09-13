package cz.twocom.core.database

import android.content.Context
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import cz.twocom.core.crypto.KeystoreEnvelope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.security.SecureRandom
import javax.inject.Singleton

private val Context.dbKeyStore by preferencesDataStore("db_key")
private const val KEYSTORE_ALIAS_DB = "db_master_v1"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, envelope: KeystoreEnvelope): AppDatabase {
        val key = runBlocking { getOrCreateDbKey(context, envelope) }
        return AppDatabase.create(context, key)
    }

    @Provides
    fun provideContactDao(db: AppDatabase) = db.contactDao()

    @Provides
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    private suspend fun getOrCreateDbKey(context: Context, envelope: KeystoreEnvelope): ByteArray {
        val prefKey = byteArrayPreferencesKey("db_passphrase_sealed")
        val prefs = context.dbKeyStore.data.firstOrNull()
        prefs?.get(prefKey)?.let { return envelope.open(KEYSTORE_ALIAS_DB, it) }
        val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        context.dbKeyStore.edit { it[prefKey] = envelope.seal(KEYSTORE_ALIAS_DB, newKey) }
        return newKey
    }
}
