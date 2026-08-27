package app.openflow.ui

import app.openflow.stt.providers.ondevice.OnPhoneModelUi
import app.openflow.ui.dictionary.DictionaryTab
import app.openflow.ui.history.HistoryScreen
import app.openflow.ui.history.useHistoryRaw
import app.openflow.ui.privacy.A11yDisclosureDialog
import app.openflow.ui.settings.AppearanceSettings
import app.openflow.ui.settings.BubbleSettings
import app.openflow.ui.settings.CleanupSettings
import app.openflow.ui.settings.ModuleEditor
import app.openflow.ui.settings.PrivacySettings
import app.openflow.ui.settings.SettingsHub
import app.openflow.ui.settings.SettingsItem
import app.openflow.ui.settings.SoundsSettings
import app.openflow.ui.snippets.SnippetsTab
import app.openflow.ui.theme.SecUi
import android.graphics.BitmapFactory
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.openflow.R
import app.openflow.OpenFlowApp
import app.openflow.bubble.BubbleChrome
import app.openflow.bubble.BubbleIconPolicy
import app.openflow.bubble.BubbleScaleSteps
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.DictationEntity
import app.openflow.data.DictionaryWordEntity
import app.openflow.data.ProcessStatus
import app.openflow.data.SnippetEntity
import app.openflow.export.ExportChoice
import app.openflow.export.ExportFormat
import app.openflow.export.HistoryExport
import app.openflow.help.HelpLinks
import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.LayoutPrefs
import app.openflow.text.LearnEngine
import app.openflow.text.PairImport
import app.openflow.text.WritingStyle
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.display.DisplayRefreshController
import app.openflow.display.DisplayRefreshPolicy
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.OnDeviceSpeechPolicy
import app.openflow.stt.SttTuning
import app.openflow.ui.engine.EngineSettingsScreen
import app.openflow.ui.home.DictListPolicy
import app.openflow.ui.home.HubListPolicy
import app.openflow.ui.home.HistoryDays
import app.openflow.ui.home.HistorySearchPolicy
import app.openflow.ui.home.HistoryRowActions
import app.openflow.ui.home.HomeBannerPolicy
import app.openflow.ui.haptics.HapticsSettings
import app.openflow.ui.history.DictationCard
import app.openflow.ui.home.HomeFeed
import app.openflow.ui.home.ModuleEditorVisibility
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.insights.InsightsScreen
import app.openflow.ui.legal.LegalCopy
import app.openflow.ui.legal.LegalDocumentScreen
import app.openflow.ui.privacy.PrivacyHonesty
import app.openflow.ui.setup.BatteryExemption
import app.openflow.ui.setup.BatteryExemptionDialog
import app.openflow.ui.setup.FirstRunPolicy
import app.openflow.ui.setup.SetupWizard
import app.openflow.ui.style.StyleHubScreen
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.shell.NavStack
import app.openflow.ui.UiHapticMap
import app.openflow.ui.theme.BubbleTint
import app.openflow.ui.theme.HexColor
import app.openflow.ui.theme.Motion
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.theme.rememberMotionMs
import app.openflow.ui.theme.rememberShouldAnimate
import app.openflow.ui.walkthrough.WalkthroughPager
import app.openflow.ui.walkthrough.WalkthroughPolicy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Theme tokens for MainActivity screens.
 * MUST read [MaterialTheme.colorScheme] so light/dark/system work.
 */
/** Home layout row explainer. Used by ModuleEditor when a row is focused/moved. */
object HomeFeelCopy {
    fun moduleWhat(id: String): String = when (id) {
        "setup" -> "permissions"
        "test" -> "practice field"
        "keys" -> "cleanup chips"
        "stats" -> "last dictation"
        "recent" -> "history"
        else -> ""
    }
}

class MainActivity : ComponentActivity() {

    private val _micGranted = mutableStateOf(false)
    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        _micGranted.value = granted
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Custom bottom bar draws behind the nav bar — no system scrim.
            window.isNavigationBarContrastEnforced = false
        }
        val app = application as OpenFlowApp
        _micGranted.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        setContent {
            val darkMode by app.prefs.darkMode.collectAsState()
            val skin by app.prefs.visualSkin.collectAsState()
            val palette by app.prefs.appearance.collectAsState()
            var tapPick by remember { mutableStateOf(app.prefs.hapticPick(HapticFeel.Event.TAP)) }
            CompositionLocalProvider(LocalHapticTap provides tapPick) {
            OpenFlowTheme(darkMode = darkMode, skin = skin, palette = palette) {
                val scheme = MaterialTheme.colorScheme
                val isDark = scheme.background.luminance() < 0.5f
                val view = LocalView.current
                SideEffect {
                    val window = window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
                // Real back stack — Back pops one level; bottom tabs reset stack.
                var bubbleOn by remember {
                    mutableStateOf(
                        FlowAccessibilityService.isRunning() ||
                            FlowAccessibilityService.isEnabled(this@MainActivity)
                    )
                }
                var micOn by remember { _micGranted }
                var serviceAlive by remember {
                    mutableStateOf(FlowAccessibilityService.isRunning())
                }
                // Self-heal the rebind race: system may reconnect the service after
                // this screen composes. Poll the in-process liveness flag while visible.
                LaunchedEffect(Unit) {
                    while (true) {
                        serviceAlive = FlowAccessibilityService.isRunning()
                        kotlinx.coroutines.delay(2_000)
                    }
                }
                var batterySeen by remember { mutableStateOf(app.prefs.setupBatterySeen) }
                var navStack by rememberSaveable(stateSaver = NavStack.Saver) {
                    mutableStateOf(
                        NavStack.initial(
                            !FirstRunPolicy.needsWizard(
                                FirstRunPolicy.step(bubbleOn, micOn, batterySeen)
                            )
                        )
                    )
                }
                val route = NavStack.current(navStack)
                fun goTo(dest: AppRoute) {
                    navStack = NavStack.navigate(navStack, dest)
                }
                fun goBack() {
                    navStack = NavStack.goBack(navStack)
                }
                fun markBatterySeen() {
                    app.prefs.setupBatterySeen = true
                    batterySeen = true
                }
                val setupStep = FirstRunPolicy.step(bubbleOn, micOn, batterySeen)
                // Cold start deep link (e.g., notification). Warm links go via onNewIntent.
                androidx.compose.runtime.LaunchedEffect(intent) {
                    if (intent?.getBooleanExtra("open_history", false) == true) {
                        navStack = NavStack.openDeepLink(AppRoute.History)
                    }
                }
                DisposableEffect(Unit) {
                    val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                        if (newIntent.getBooleanExtra("open_history", false)) {
                            setIntent(newIntent)
                            navStack = NavStack.openDeepLink(AppRoute.History)
                        }
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val obs = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME) {
                            bubbleOn = FlowAccessibilityService.isRunning() ||
                                FlowAccessibilityService.isEnabled(this@MainActivity)
                            serviceAlive = FlowAccessibilityService.isRunning()
                            micOn = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            batterySeen = app.prefs.setupBatterySeen
                            FlowAccessibilityService.instance?.applyPrefsVisual()
                            DisplayRefreshController.apply(
                                this@MainActivity,
                                app.prefs.refreshHz
                            )
                        }
                    }
                    owner.lifecycle.addObserver(obs)
                    onDispose { owner.lifecycle.removeObserver(obs) }
                }
                androidx.compose.runtime.LaunchedEffect(route, setupStep) {
                    if (route == AppRoute.Setup && setupStep == FirstRunPolicy.Step.DONE) {
                        goTo(AppRoute.Home)
                    }
                }

                // Apply preferred Hz on first composition
                androidx.compose.runtime.LaunchedEffect(app.prefs.refreshHz) {
                    DisplayRefreshController.apply(this@MainActivity, app.prefs.refreshHz)
                }

                var layoutTick by remember { mutableIntStateOf(0) }

                var walkthroughSeen by remember { mutableStateOf(app.prefs.seenHowTo) }
                var walkPage by remember { mutableStateOf(WalkthroughPolicy.Page.WHAT) }

                var showA11yDisclosure by rememberSaveable { mutableStateOf(false) }
                var showBatteryDialog by rememberSaveable { mutableStateOf(false) }
                fun openA11ySettings() {
                    try {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (_: Exception) {
                        try {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (_: Exception) {
                        }
                    }
                }
                fun requestEnableBubble() {
                    if (app.prefs.a11yDisclosureAccepted) {
                        openA11ySettings()
                    } else {
                        showA11yDisclosure = true
                    }
                }
                if (showA11yDisclosure) {
                    A11yDisclosureDialog(
                        onAgree = {
                            app.prefs.a11yDisclosureAccepted = true
                            showA11yDisclosure = false
                            openA11ySettings()
                        },
                        onDecline = { showA11yDisclosure = false },
                    )
                }

                if (WalkthroughPolicy.needsWalkthrough(walkthroughSeen)) {
                    WalkthroughPager(
                        page = walkPage,
                        onNext = {
                            val pages = WalkthroughPolicy.pages()
                            val i = pages.indexOf(walkPage)
                            if (i < pages.lastIndex) walkPage = pages[i + 1]
                            else {
                                app.prefs.seenHowTo = true
                                walkthroughSeen = true
                            }
                        },
                        onSkip = {
                            app.prefs.seenHowTo = true
                            walkthroughSeen = true
                        },
                    )
                } else {
                BackHandler(enabled = NavStack.canGoBack(navStack)) {
                    goBack()
                }

                AppShell(
                    route = route,
                    onNavigate = { dest -> goTo(dest) },
                    onBack = { goBack() },
                    isDrawerExtraVisible = { true }
                ) { padding ->
                    val tabMs = rememberMotionMs(Motion.TAB_SWITCH_MS)
                    val animateTabs = rememberShouldAnimate()
                    AnimatedContent(
                        targetState = route to layoutTick,
                        transitionSpec = {
                            if (!animateTabs || tabMs == 0) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                fadeIn(tween(tabMs, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(tween(tabMs, easing = FastOutSlowInEasing))
                            }
                        },
                        label = "route_content",
                        modifier = Modifier.padding(padding)
                    ) { (r, _) ->
                        when (r) {
                            AppRoute.Home -> HomeHub(
                                app = app,
                                bubbleOn = bubbleOn,
                                micOn = micOn,
                                serviceAlive = serviceAlive,
                                onEnableBubble = { requestEnableBubble() },
                                onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                            )
                            AppRoute.History -> HistoryScreen(app)
                            AppRoute.Dictionary -> DictionaryTab(app)
                            AppRoute.Snippets -> SnippetsTab(app)
                            AppRoute.Style -> StyleTab(app.prefs)
                            AppRoute.Insights -> InsightsScreen(
                                app = app,
                                onOpenSpeechAi = { goTo(AppRoute.SpeechAi) },
                            )
                            AppRoute.Settings -> SettingsHub { item ->
                                when (item) {
                                    SettingsItem.SpeechAi -> goTo(AppRoute.SpeechAi)
                                    SettingsItem.Cleanup -> goTo(AppRoute.Cleanup)
                                    SettingsItem.Bubble -> goTo(AppRoute.BubbleSettings)
                                    SettingsItem.Haptics -> goTo(AppRoute.Haptics)
                                    SettingsItem.Sounds -> goTo(AppRoute.Sounds)
                                    SettingsItem.Appearance -> goTo(AppRoute.Appearance)
                                    SettingsItem.HomeLayout -> goTo(AppRoute.HomeModules)
                                    SettingsItem.Privacy -> goTo(AppRoute.Privacy)
                                    SettingsItem.PrivacyPolicy -> goTo(AppRoute.PrivacyPolicy)
                                    SettingsItem.Terms -> goTo(AppRoute.Terms)
                                    SettingsItem.Feedback -> startActivity(
                                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(HelpLinks.DISCUSSIONS))
                                    )
                                    SettingsItem.ReportIssue -> startActivity(
                                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(HelpLinks.ISSUES_NEW))
                                    )
                                    SettingsItem.ReportSecurity -> startActivity(
                                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(HelpLinks.SECURITY_ADVISORY))
                                    )
                                }
                            }
                            AppRoute.SpeechAi -> {
                                val session = app.engineSession
                                val scope = rememberCoroutineScope()
                                var tinyReady by remember {
                                    mutableStateOf(app.modelStore.isReady("tiny.en"))
                                }
                                var tinyBusy by remember { mutableStateOf(false) }
                                var tinyErr by remember { mutableStateOf("") }
                                EngineSettingsScreen(
                                    initialEar = app.enginePrefs.earId,
                                    initialBrain = app.enginePrefs.brainId,
                                    initialRouteMode = app.enginePrefs.routeMode,
                                    initialAiWhen = app.enginePrefs.aiWhen,
                                    initialUrl = app.enginePrefs.customBaseUrl,
                                    initialSarvamMode = app.enginePrefs.sarvamMode,
                                    initialKeyMask = session.keyMask(),
                                    initialEarKeyMask = session.earKeyMask(),
                                    initialBrainKeyMask = session.brainKeyMask(),
                                    onPick = { e, b -> session.pick(e, b) },
                                    onRouteMode = { app.enginePrefs.routeMode = it },
                                    onAiWhen = { app.enginePrefs.aiWhen = it },
                                    onSaveKey = session::saveKey,
                                    onSaveEarKey = session::saveEarKey,
                                    onSaveBrainKey = session::saveBrainKey,
                                    onSaveUrl = session::saveUrl,
                                    onSarvamMode = session::saveSarvam,
                                    onKeyMask = session::keyMask,
                                    onEarKeyMask = session::earKeyMask,
                                    onBrainKeyMask = session::brainKeyMask,
                                    flowPrefs = app.prefs,
                                    tinyEnReady = tinyReady,
                                    tinyEnBusy = tinyBusy,
                                    tinyEnError = tinyErr,
                                    onDownloadTinyEn = {
                                        tinyBusy = true
                                        tinyErr = ""
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                app.modelStore.ensure(
                                                    "tiny.en",
                                                    OnPhoneModelUi.TINY_EN_URL,
                                                )
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    tinyReady = true
                                                    tinyBusy = false
                                                }
                                            } catch (e: Exception) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    tinyBusy = false
                                                    tinyErr = e.message ?: "download failed"
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                            AppRoute.Appearance -> AppearanceSettings(app.prefs)
                            AppRoute.BubbleSettings -> BubbleSettings(
                                prefs = app.prefs,
                                onApplyBubble = {
                                    FlowAccessibilityService.instance?.applyPrefsVisual()
                                }
                            )
                            AppRoute.Haptics -> HapticsSettings(app.prefs) { tapPick = it }
                            AppRoute.Cleanup -> CleanupSettings(app.prefs)
                            AppRoute.Privacy -> PrivacySettings(prefs = app.prefs)
                            AppRoute.PrivacyPolicy -> LegalDocumentScreen(
                                title = LegalCopy.privacyTitle,
                                intro = LegalCopy.privacyIntro,
                                sections = LegalCopy.privacySections,
                                tag = "legal_privacy",
                            )
                            AppRoute.Terms -> LegalDocumentScreen(
                                title = LegalCopy.termsTitle,
                                intro = LegalCopy.termsIntro,
                                sections = LegalCopy.termsSections,
                                tag = "legal_terms",
                            )
                            AppRoute.Sounds -> SoundsSettings(app.prefs)
                            AppRoute.HomeModules -> ModuleEditor(
                                title = "Home layout",
                                subtitle = "Show, hide, reorder Home cards",
                                modules = app.prefs.homeModules(),
                                labels = mapOf(
                                    "setup" to "Setup",
                                    "stats" to "Stats",
                                    "keys" to "Key actions",
                                    "test" to "Test field",
                                    "recent" to "Recent history"
                                ),
                                defaultEncode = LayoutPrefs.DEFAULT_HOME,
                                onChange = {
                                    app.prefs.setHomeModules(it)
                                    layoutTick++
                                }
                            )
                            AppRoute.Setup -> {
                                SetupWizard(
                                    step = setupStep,
                                    onEnableBubble = { requestEnableBubble() },
                                    onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                                    onBattery = { showBatteryDialog = true },
                                    onSkipBattery = {
                                        markBatterySeen()
                                        goTo(AppRoute.Home)
                                    }
                                )
                                if (showBatteryDialog) {
                                    BatteryExemptionDialog(
                                        onAgree = {
                                            showBatteryDialog = false
                                            app.prefs.setupBatterySeen = true
                                            val ignoring = try {
                                                val pm = getSystemService(PowerManager::class.java)
                                                pm.isIgnoringBatteryOptimizations(packageName)
                                            } catch (_: Exception) {
                                                false
                                            }
                                            val batteryIntent = Intent(
                                                BatteryExemption.action(ignoring)
                                            ).setData(
                                                Uri.parse(BatteryExemption.dataUri(packageName))
                                            )
                                            try {
                                                startActivity(batteryIntent)
                                            } catch (_: Exception) {
                                                try {
                                                    startActivity(
                                                        Intent(BatteryExemption.fallbackAction())
                                                            .setData(
                                                                Uri.parse(
                                                                    BatteryExemption.dataUri(packageName)
                                                                )
                                                            )
                                                    )
                                                } catch (_: Exception) {
                                                }
                                            }
                                        },
                                        onDecline = { showBatteryDialog = false },
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHub(
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    serviceAlive: Boolean = true,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
) {
    HomeFeed(
        app = app,
        bubbleOn = bubbleOn,
        micOn = micOn,
        serviceAlive = serviceAlive,
        onEnableBubble = onEnableBubble,
        onMic = onMic,
        dictationCard = { d, onDelete, onShare, onSave, onUseRaw ->
            DictationCard(
                d = d,
                onDelete = onDelete,
                onShare = onShare,
                onSave = onSave,
                onUseRaw = onUseRaw,
            )
        },
        useHistoryRaw = ::useHistoryRaw,
    )
}












@Composable
private fun StyleTab(prefs: FlowPrefs) {
    StyleHubScreen(prefs)
}






















