package vault.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import vault.api.AppContext

// MainActivity sets AppContext before any database use.
actual fun localDatabaseBuilder(path: String): RoomDatabase.Builder<VaultDatabase> =
    Room.databaseBuilder<VaultDatabase>(context = AppContext.app, name = path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        .setQueryCoroutineContext(Dispatchers.IO)
