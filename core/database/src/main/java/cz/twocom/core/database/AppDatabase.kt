package cz.twocom.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.database.dao.MessageDao
import cz.twocom.core.database.entity.ContactEntity
import cz.twocom.core.database.entity.MessageEntity
import net.sqlcipher.database.SupportFactory

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // agreementPublicKeyHex was always "" in v1 (never populated — 2COM_AUDIT.md #3);
        // v2's handshake actually populates a single libsignal identity public key hex.
        db.execSQL("ALTER TABLE contacts RENAME COLUMN agreementPublicKeyHex TO identityPublicKeyHex")
    }
}

@Database(
    entities = [ContactEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, AppDatabase::class.java, "2com.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
