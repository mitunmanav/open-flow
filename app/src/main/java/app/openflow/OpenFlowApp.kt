package app.openflow

import android.app.Application
import android.content.ComponentCallbacks2
import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.engine.BrainId
import app.openflow.engine.EarId
import app.openflow.engine.EnginePrefs
import app.openflow.engine.ProviderRegistry
import app.openflow.notify.DictationNotifier
import app.openflow.prefs.FlowPrefs
import app.openflow.runtime.TrimPolicy
import app.openflow.secrets.AndroidSecretStore
import app.openflow.stt.AndroidSpeechEngine
import kotlinx.coroutines.launch

class OpenFlowApp : Application(), ComponentCallbacks2 {
    /**
     * Last [onTrimMemory] level.
     * Bubble service owns idle-STT drop — do not import bubble here.
     * Service reads [dropIdleStt] / [lastTrimLevel] + TrimPolicy.shouldDropIdleStt.
     */
    @Volatile
    var lastTrimLevel: Int = 0
        private set

    /** True when last trim says drop idle STT. In-memory only. */
    @Volatile
    var dropIdleStt: Boolean = false
        private set

    /** Product AI hook. Always [NoAI] — no network, no model. */
    val textAI: TextAIProvider = NoAI

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
        dropIdleStt = TrimPolicy.shouldDropIdleStt(level)
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
