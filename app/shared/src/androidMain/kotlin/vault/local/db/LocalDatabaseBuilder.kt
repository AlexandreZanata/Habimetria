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
        .addMigrations(MIGRATION_1_2)
        .setQueryCoroutineContext(Dispatchers.IO)
