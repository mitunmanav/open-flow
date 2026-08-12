package app.openflow

import android.app.Application
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.notify.DictationNotifier
import app.openflow.prefs.FlowPrefs

class OpenFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DictationNotifier.createChannel(this)
    }

    val database by lazy { OpenFlowDatabase.get(this) }
    val dictations by lazy {
        DictationRepository(
            database,
            database.dictationDao(),
            database.dictionaryDao(),
            database.snippetDao(),
            database.statsDao()
        )
    }
    val prefs by lazy { FlowPrefs(this) }
}
