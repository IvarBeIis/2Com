package cz.twocom.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_renamesAgreementColumnAndPreservesRows() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO contacts (id, peerHash, displayName, signingPublicKeyHex, " +
                    "agreementPublicKeyHex, isVerified, isBlocked, lastSeenAt, createdAt) " +
                    "VALUES (1, '${"a".repeat(64)}', 'Alice', '', '', 0, 0, NULL, 1000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        val cursor = db.query("SELECT peerHash, identityPublicKeyHex FROM contacts WHERE id = 1")
        cursor.use {
            assert(it.moveToFirst())
            assert(it.getString(0) == "a".repeat(64))
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
