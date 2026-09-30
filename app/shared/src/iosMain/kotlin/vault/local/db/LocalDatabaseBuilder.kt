package vault.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun localDatabaseBuilder(path: String): RoomDatabase.Builder<VaultDatabase> =
    Room.databaseBuilder<VaultDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .setQueryCoroutineContext(Dispatchers.Default)
