package vault.local.db

import androidx.room.RoomDatabase

// Absolute database file path. Android callers pass an absolute path
// (no Context needed); iOS uses NSDocumentDirectory; JVM uses temp/app dir.
expect fun localDatabaseBuilder(path: String): RoomDatabase.Builder<VaultDatabase>
