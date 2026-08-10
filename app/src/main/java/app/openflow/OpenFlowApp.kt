package app.openflow

import android.app.Application
import app.openflow.data.OpenFlowDatabase
import app.openflow.data.SessionRepository

class OpenFlowApp : Application() {
    val database by lazy { OpenFlowDatabase.get(this) }
    val sessions by lazy { SessionRepository(database.sessionDao()) }
}
