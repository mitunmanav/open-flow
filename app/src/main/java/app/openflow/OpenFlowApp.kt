package app.openflow

import android.app.Application
import android.content.ComponentCallbacks2
import app.openflow.ai.NoAI
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.engine.BrainId
import app.openflow.engine.EarId
import app.openflow.engine.EnginePrefs
import app.openflow.engine.ProviderRegistry
import app.openflow.notify.DictationNotifier
import app.openflow.prefs.FlowPrefs
import app.openflow.secrets.AndroidSecretStore
import app.openflow.stt.AndroidSpeechEngine
import kotlinx.coroutines.launch

class OpenFlowApp : Application(), ComponentCallbacks2 {
    /**
     * Last [onTrimMemory] level.
     * Agent A owns FlowAccessibilityService — do not call it from here.
     * Service can read this later and drop idle STT via TrimPolicy.shouldDropIdleStt.
     */
    @Volatile
    var lastTrimLevel: Int = 0
        private set

    override fun onCreate() {
        super.onCreate()
        DictationNotifier.createChannel(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            dictations.purgeOnLaunch(prefs.retentionPolicy)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        lastTrimLevel = level
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
    val enginePrefs by lazy { EnginePrefs(this) }
    val secrets by lazy { AndroidSecretStore(this) }
    val systemEar by lazy { AndroidSpeechEngine(this) }
    val registry by lazy {
        ProviderRegistry(
            fallbackEar = { systemEar },
            fallbackBrain = { NoAI },
        ).apply {
            registerEar(EarId.SYSTEM) { systemEar }
            registerBrain(BrainId.NONE) { NoAI }
        }
    }
}
