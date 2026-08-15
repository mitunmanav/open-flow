package app.openflow.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object OpenFlowMigrations {
    /** v4 → v5: add standalone FTS4 index for dictation search. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS `dictations_fts`
                USING FTS4(`sessionId`, `text`, `rawText`)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `dictations_fts`(`sessionId`, `text`, `rawText`)
                SELECT `id`, `text`, `rawText` FROM `dictations`
                """.trimIndent()
            )
        }
    }

    /** v5 → v6: add index on createdAtEpochMs for fast history queries. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_dictations_createdAtEpochMs` ON `dictations` (`createdAtEpochMs`)"
            )
        }
    }
}
