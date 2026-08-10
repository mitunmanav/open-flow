package app.openflow

import android.app.Application
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.prefs.FlowPrefs

class OpenFlowApp : Application() {
    val database by lazy { OpenFlowDatabase.get(this) }
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
