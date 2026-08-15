package app.openflow

import android.app.Application
import android.content.ComponentCallbacks2
import app.openflow.ai.NoAI
import app.openflow.ai.TextAIProvider
import app.openflow.ai.providers.cloud.AndroidCloudHttp
import app.openflow.ai.providers.cloud.CloudHttp
import app.openflow.ai.providers.cloud.CloudProviders
import app.openflow.ai.providers.host.HostPost
import app.openflow.ai.providers.host.LaptopBrain
import app.openflow.ai.providers.ondevice.OnDeviceBrain
import app.openflow.data.DictationRepository
import app.openflow.data.OpenFlowDatabase
import app.openflow.engine.BrainId
import app.openflow.engine.EarId
import app.openflow.engine.EnginePrefs
import app.openflow.engine.EngineSession
import app.openflow.engine.ProviderId
import app.openflow.engine.ProviderRegistry
import app.openflow.notify.DictationNotifier
import app.openflow.prefs.FlowPrefs
import app.openflow.runtime.TrimPolicy
import app.openflow.secrets.AndroidSecretStore
import app.openflow.secrets.SecretStore
import app.openflow.stt.AndroidSpeechEngine
import app.openflow.stt.SpeechEngine
import app.openflow.stt.providers.cloud.CloudSocket
import app.openflow.stt.providers.cloud.FailSoftSocket
import app.openflow.stt.providers.host.LaptopEar
import app.openflow.stt.providers.ondevice.OnDeviceEar
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

    /** Live brain from prefs. Default [NoAI] until user pick. */
    val textAI: TextAIProvider
        get() = currentBrain()

    private val appScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() +
            kotlinx.coroutines.Dispatchers.IO +
            kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                android.util.Log.e("OpenFlowApp", "background task failed", e)
            }
    )

    override fun onCreate() {
        super.onCreate()
        DictationNotifier.createChannel(this)
        appScope.launch {
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
            database.dictationFtsDao(),
            database.dictionaryDao(),
            database.snippetDao(),
            database.statsDao()
        )
    }
    val prefs by lazy { FlowPrefs(this) }
    val enginePrefs by lazy { EnginePrefs(this) }
    val secrets by lazy { AndroidSecretStore(this) }
    val engineSession by lazy { EngineSession(enginePrefs, secrets) }
    val systemEar by lazy { AndroidSpeechEngine(this) }
    val cloudHttp by lazy { AndroidCloudHttp() }
    val registry by lazy {
        ProviderRegistry(
            fallbackEar = { systemEar },
            fallbackBrain = { NoAI },
        ).also { AppEngineWire.install(it, secrets, enginePrefs, systemEar, cloudHttp, pendingSocket) }
    }

    fun currentEar(): SpeechEngine = AppEngineWire.currentEar(registry, enginePrefs)

    fun currentBrain(): TextAIProvider = AppEngineWire.currentBrain(registry, enginePrefs)
}

/** Cloud WS not wired. Connect returns a dead session. Never throws. */
private val pendingSocket = FailSoftSocket()

/** Pure wire helper. Factories exist without keys. */
object AppEngineWire {
    fun currentEar(registry: ProviderRegistry, enginePrefs: EnginePrefs): SpeechEngine =
        registry.ear(enginePrefs.earId)

    fun currentBrain(registry: ProviderRegistry, enginePrefs: EnginePrefs): TextAIProvider =
        registry.brain(enginePrefs.brainId)

    const val DEFAULT_LAPTOP_MODEL = "llama3"

    fun install(
        registry: ProviderRegistry,
        secrets: SecretStore,
        enginePrefs: EnginePrefs,
        systemEar: SpeechEngine,
        http: CloudHttp,
        socket: CloudSocket,
    ) {
        registry.registerEar(EarId.SYSTEM) { systemEar }
        registry.registerEar(EarId.ON_PHONE) { OnDeviceEar() }
        registry.registerEar(EarId.LAPTOP) {
            LaptopEar(enginePrefs.customBaseUrl.ifBlank { null })
        }
        registry.registerBrain(BrainId.NONE) { NoAI }
        registry.registerBrain(BrainId.ON_PHONE) { OnDeviceBrain() }
        registry.registerBrain(BrainId.LAPTOP) {
            LaptopBrain(
                baseUrl = enginePrefs.customBaseUrl.ifBlank { null },
                model = enginePrefs.brainModel.ifBlank { DEFAULT_LAPTOP_MODEL },
                apiKey = secrets.get("laptop"),
                post = HostPost { url, headers, json -> http.post(url, headers, json) },
            )
        }
        CloudProviders.register(object : CloudProviders.Registry {
            override fun addBrain(id: String, factory: CloudProviders.BrainFactory) {
                registry.registerBrain(ProviderId.parseBrain(id)) {
                    val url = if (id == "custom") {
                        enginePrefs.customBaseUrl.ifBlank { null }
                    } else {
                        null
                    }
                    factory.create(
                        { secrets.get(id).orEmpty() },
                        enginePrefs.brainModel,
                        url,
                        http,
                    )
                }
            }

            override fun addEar(id: String, factory: CloudProviders.EarFactory) {
                registry.registerEar(ProviderId.parseEar(id)) {
                    factory.create(
                        { secrets.get(id).orEmpty() },
                        socket,
                        enginePrefs.sarvamMode,
                    )
                }
            }
        })
    }
}
