package cz.twocom.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cz.twocom.core.database.dao.ContactDao
import cz.twocom.core.database.dao.MessageDao
import cz.twocom.core.database.entity.ContactEntity
import cz.twocom.core.database.entity.MessageEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ContactEntity::class, MessageEntity::class],
    version = 1,
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
                .build()
        }
    }
}
