package app.openflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DictationEntity::class,
        DictionaryWordEntity::class,
        SnippetEntity::class,
        AppStatsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class OpenFlowDatabase : RoomDatabase() {
    abstract fun dictationDao(): DictationDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun snippetDao(): SnippetDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var instance: OpenFlowDatabase? = null

        fun get(context: Context): OpenFlowDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OpenFlowDatabase::class.java,
                    "openflow.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
