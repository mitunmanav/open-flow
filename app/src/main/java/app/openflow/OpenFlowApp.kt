package app.openflow

import android.app.Application
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.data.SessionRepository
import app.openflow.prefs.FlowPrefs

class OpenFlowApp : Application() {
    val database by lazy { OpenFlowDatabase.get(this) }
    val sessions by lazy { SessionRepository(database.sessionDao()) }
    val dictations by lazy {
        DictationRepository(
            database.dictationDao(),
            database.dictionaryDao(),
            database.snippetDao(),
            database.statsDao()
        )
    }
    val prefs by lazy { FlowPrefs(this) }
}
