package app.openflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, SessionFts::class],
    version = 1,
    exportSchema = false
)
abstract class OpenFlowDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: OpenFlowDatabase? = null

        fun get(context: Context): OpenFlowDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OpenFlowDatabase::class.java,
                    "openflow.db"
                ).build().also { instance = it }
            }
        }
    }
}
