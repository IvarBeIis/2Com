package cz.twocom.core.database

import android.content.Context
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        val key = runBlocking { getOrCreateDbKey(context) }
        return AppDatabase.create(context, key)
    }

    @Provides
    fun provideContactDao(db: AppDatabase) = db.contactDao()

    @Provides
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    private suspend fun getOrCreateDbKey(context: Context): ByteArray {
        val prefKey = byteArrayPreferencesKey("db_passphrase")
        val prefs = context.dbKeyStore.data.firstOrNull()
        val existing = prefs?.get(prefKey)
        if (existing != null) return existing
        val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        context.dbKeyStore.edit { it[prefKey] = newKey }
        return newKey
    }
}
