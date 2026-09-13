package cz.twocom.core.crypto.store

import android.content.Context
import cz.twocom.core.crypto.KeystoreEnvelope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SignalDatabaseModule {

    @Provides
    @Singleton
    fun provideSignalDatabase(
        @ApplicationContext context: Context,
        envelope: KeystoreEnvelope,
    ): SignalDatabase {
        val key = runBlocking { getOrCreateSignalDbKey(context, envelope) }
        return SignalDatabase.create(context, key)
    }

    @Provides
    fun provideSignalStoreDao(db: SignalDatabase): SignalStoreDao = db.dao()
}
