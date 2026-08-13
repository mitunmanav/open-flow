package app.openflow.data

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Prod Room open rules. Version stays put unless schema changes.
 * Missing Migration must fail loud — never wipe user history quietly.
 */
object RoomOpenPolicy {
    const val VERSION = 4
    const val ALLOW_DESTRUCTIVE_FALLBACK = false

    fun <T : RoomDatabase> applyTo(builder: RoomDatabase.Builder<T>): RoomDatabase.Builder<T> {
        // Do not call fallbackToDestructiveMigration(). Room then throws
        // on version mismatch instead of dropping tables.
        return builder.addCallback(DestructiveMigrationBan)
    }

    private object DestructiveMigrationBan : RoomDatabase.Callback() {
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            error("openflow.db: destructive migration forbidden")
        }
    }
}
