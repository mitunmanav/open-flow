package app.openflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

/** Transaction hook so tests can fake Room without an emulator. */
interface OpenFlowDb {
    suspend fun <R> transact(block: suspend () -> R): R
}

@Database(
    entities = [
        DictationEntity::class,
        DictationFtsEntity::class,
        DictionaryWordEntity::class,
        SnippetEntity::class,
        AppStatsEntity::class,
        VoiceProfileEntity::class,
    ],
    version = RoomOpenPolicy.VERSION,
    exportSchema = false
)
abstract class OpenFlowDatabase : RoomDatabase(), OpenFlowDb {
    abstract fun dictationDao(): DictationDao
    abstract fun dictationFtsDao(): DictationFtsDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun snippetDao(): SnippetDao
    abstract fun statsDao(): StatsDao
    abstract fun voiceProfileDao(): VoiceProfileDao

    override suspend fun <R> transact(block: suspend () -> R): R = withTransaction(block)

    companion object {
        @Volatile private var instance: OpenFlowDatabase? = null

        fun get(context: Context): OpenFlowDatabase {
            return instance ?: synchronized(this) {
                instance ?: RoomOpenPolicy.applyTo(
                    Room.databaseBuilder(
                        context.applicationContext,
                        OpenFlowDatabase::class.java,
                        "openflow.db"
                    )
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
