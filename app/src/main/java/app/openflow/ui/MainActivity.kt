package app.openflow.ui

import android.graphics.BitmapFactory
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
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
import app.openflow.ui.home.HomeFeed
import app.openflow.ui.home.ModuleEditorVisibility
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.insights.InsightsScreen
import app.openflow.ui.legal.LegalCopy
import app.openflow.ui.legal.LegalDocumentScreen
import app.openflow.ui.privacy.PrivacyHonesty
import app.openflow.ui.setup.BatteryExemption
import app.openflow.ui.setup.FirstRunPolicy
import app.openflow.ui.setup.SetupWizard
import app.openflow.ui.style.StyleHubScreen
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.shell.NavStack
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

private object SecUi {
    val cream: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.background
    val charcoal: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
    val stone: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val ink: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.secondary
    val error: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.error
    val muted: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val hardBorder: BorderStroke
        @Composable get() = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    val thinBorder: BorderStroke
        @Composable get() = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
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
        val app = application as OpenFlowApp
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
                var batterySeen by remember { mutableStateOf(app.prefs.setupBatterySeen) }
                _micGranted.value = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
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
                androidx.compose.runtime.LaunchedEffect(intent) {
                    if (intent?.getBooleanExtra("open_history", false) == true) {
                        navStack = NavStack.openDeepLink(AppRoute.History)
                    }
                }
                DisposableEffect(Unit) {
                    val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                        if (newIntent.getBooleanExtra("open_history", false)) {
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
                                onEnableBubble = {
                                    try {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Turn ON Open Flow Bubble in Accessibility, then return.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } catch (_: Exception) {
                                        try {
                                            startActivity(Intent(Settings.ACTION_SETTINGS))
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                                onOpenInsights = { goTo(AppRoute.Insights) },
                                onPrivacyPolicy = { goTo(AppRoute.PrivacyPolicy) },
                                onTerms = { goTo(AppRoute.Terms) },
                                onHelp = {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(HelpLinks.DISCUSSIONS)
                                        )
                                    )
                                },
                            )
                            AppRoute.History -> HistoryScreen(app)
                            AppRoute.Dictionary -> DictionaryTab(app)
                            AppRoute.Snippets -> SnippetsTab(app)
                            AppRoute.Style -> StyleTab(app.prefs)
                            AppRoute.Insights -> InsightsScreen(
                                app = app,
                                onOpenSpeechAi = { goTo(AppRoute.SpeechAi) },
                            )
                            AppRoute.Settings -> SettingsHub(
                                onSpeechAi = { goTo(AppRoute.SpeechAi) },
                                onAppearance = { goTo(AppRoute.Appearance) },
                                onBubble = { goTo(AppRoute.BubbleSettings) },
                                onCleanup = { goTo(AppRoute.Cleanup) },
                                onPrivacy = { goTo(AppRoute.Privacy) },
                                onPrivacyPolicy = { goTo(AppRoute.PrivacyPolicy) },
                                onTerms = { goTo(AppRoute.Terms) },
                                onHaptics = { goTo(AppRoute.Haptics) },
                                onSounds = { goTo(AppRoute.Sounds) },
                                onFeedback = {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(HelpLinks.DISCUSSIONS)
                                        )
                                    )
                                },
                                onReportIssue = {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(HelpLinks.ISSUES_NEW)
                                        )
                                    )
                                },
                                onReportSecurity = {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            android.net.Uri.parse(HelpLinks.SECURITY_ADVISORY)
                                        )
                                    )
                                },
                            )
                            AppRoute.SpeechAi -> {
                                val session = app.engineSession
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
                            AppRoute.Privacy -> PrivacySettings(
                                prefs = app.prefs,
                                onPrivacyPolicy = { goTo(AppRoute.PrivacyPolicy) },
                                onTerms = { goTo(AppRoute.Terms) },
                            )
                            AppRoute.PrivacyPolicy -> LegalDocumentScreen(
                                title = LegalCopy.privacyTitle,
                                body = LegalCopy.privacyBody,
                                tag = "legal_privacy",
                            )
                            AppRoute.Terms -> LegalDocumentScreen(
                                title = LegalCopy.termsTitle,
                                body = LegalCopy.termsBody,
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
                            AppRoute.Setup -> SetupWizard(
                                step = setupStep,
                                onEnableBubble = {
                                    try {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Turn ON Open Flow Bubble in Accessibility, then return.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } catch (_: Exception) {
                                        try {
                                            startActivity(Intent(Settings.ACTION_SETTINGS))
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                onMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                                onBattery = {
                                    app.prefs.setupBatterySeen = true
                                    val ignoring = try {
                                        val pm = getSystemService(PowerManager::class.java)
                                        pm.isIgnoringBatteryOptimizations(packageName)
                                    } catch (_: Exception) {
                                        false
                                    }
                                    val batteryIntent = Intent(BatteryExemption.action(ignoring)).setData(
                                        Uri.parse(BatteryExemption.dataUri(packageName))
                                    )
                                    try {
                                        startActivity(batteryIntent)
                                    } catch (_: Exception) {
                                        try {
                                            startActivity(
                                                Intent(BatteryExemption.fallbackAction()).setData(
                                                    Uri.parse(BatteryExemption.dataUri(packageName))
                                                )
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                onSkipBattery = {
                                    markBatterySeen()
                                    goTo(AppRoute.Home)
                                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHub(
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
    onOpenInsights: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTerms: () -> Unit,
    onHelp: () -> Unit,
) {
    HomeFeed(
        app = app,
        bubbleOn = bubbleOn,
        micOn = micOn,
        onEnableBubble = onEnableBubble,
        onMic = onMic,
        onOpenInsights = onOpenInsights,
        onPrivacyPolicy = onPrivacyPolicy,
        onTerms = onTerms,
        onHelp = onHelp,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryScreen(app: OpenFlowApp) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var exportFormatName by rememberSaveable { mutableStateOf(ExportFormat.MARKDOWN.name) }
    var exportRaw by rememberSaveable { mutableStateOf(false) }
    val pendingExport = remember { arrayOf("") }
    val saveDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val body = pendingExport[0]
        if (uri == null || body.isEmpty()) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
            } ?: error("no stream")
        } catch (_: Exception) {
            Toast.makeText(ctx, "Could not save", Toast.LENGTH_SHORT).show()
        }
    }
    val match = HistorySearchPolicy.ftsMatch(searchQuery)
    val filtered by produceState(dictations, match, dictations) {
        value = if (match == null) {
            dictations
        } else {
            app.dictations.searchDictations(searchQuery)
        }
    }
    val days = remember(filtered) {
        val nowMs = System.currentTimeMillis()
        HistoryDays.group(
            filtered.map { HistoryDays.Row(it.id, it.createdAtEpochMs, it.text) },
            nowMs = nowMs,
            zoneOffsetMs = TimeZone.getDefault().getOffset(nowMs).toLong()
        )
    }
    val byId = remember(filtered) { filtered.associateBy { it.id } }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
        contentPadding = PaddingValues(bottom = Dimen.GAP_LG)
    ) {
        item(key = "history-hdr") {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            Text(
                "${dictations.size} recordings on device",
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            if (dictations.isNotEmpty()) {
                val fmt = runCatching { ExportFormat.valueOf(exportFormatName) }
                    .getOrDefault(ExportFormat.MARKDOWN)
                val rows = dictations.map { d ->
                    HistoryExport.Row(
                        d.createdAtEpochMs,
                        d.text,
                        d.languageTag,
                        d.wordCount,
                        d.rawText,
                        d.id,
                        d.durationMs,
                    )
                }
                val body = HistoryExport.render(rows, ExportChoice(fmt, exportRaw))
                Column(verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OpenChip(
                            label = "MD",
                            isOn = fmt == ExportFormat.MARKDOWN,
                            modifier = Modifier.testTag("history_export_md"),
                            onClick = { exportFormatName = ExportFormat.MARKDOWN.name }
                        )
                        OpenChip(
                            label = "Plain",
                            isOn = fmt == ExportFormat.PLAIN,
                            modifier = Modifier.testTag("history_export_plain"),
                            onClick = { exportFormatName = ExportFormat.PLAIN.name }
                        )
                        OpenChip(
                            label = "JSON",
                            isOn = fmt == ExportFormat.JSON,
                            modifier = Modifier.testTag("history_export_json"),
                            onClick = { exportFormatName = ExportFormat.JSON.name }
                        )
                        OpenChip(
                            label = "Raw",
                            isOn = exportRaw,
                            modifier = Modifier.testTag("history_export_raw"),
                            onClick = { exportRaw = !exportRaw }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                                try {
                                    ctx.startActivity(Intent.createChooser(send, "Export history"))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = Dimen.MIN_TOUCH)
                                .testTag("history_export"),
                            shape = MaterialTheme.shapes.small,
                            border = SecUi.hardBorder,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SecUi.charcoal,
                                containerColor = SecUi.cream
                            )
                        ) {
                            Text("Share", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                pendingExport[0] = body
                                val name = when (fmt) {
                                    ExportFormat.MARKDOWN -> "open-flow-history.md"
                                    ExportFormat.PLAIN -> "open-flow-history.txt"
                                    ExportFormat.JSON -> "open-flow-history.json"
                                }
                                try {
                                    saveDoc.launch(name)
                                } catch (_: Exception) {
                                    Toast.makeText(ctx, "Could not save", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = Dimen.MIN_TOUCH)
                                .testTag("history_export_save"),
                            shape = MaterialTheme.shapes.small,
                            border = SecUi.hardBorder,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SecUi.charcoal,
                                containerColor = SecUi.cream
                            )
                        ) {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        }

        item(key = "history-search") {
        OpenTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search transcripts…",
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SecUi.muted
                )
            }
        )
        }

        if (filtered.isEmpty()) {
            item(key = "history-empty") {
            EmptyState(
                icon = Icons.Default.MicNone,
                title = if (searchQuery.isBlank()) "No history yet" else "No matching results",
                subtitle = if (searchQuery.isBlank()) {
                    "Dictate using the floating bubble to record transcripts."
                } else {
                    "Try a different search keyword."
                },
                modifier = Modifier.testTag("history_empty")
            )
            }
        } else {
            days.forEach { day ->
                val firstId = day.rows.firstOrNull()?.id.orEmpty()
                item(key = UiScrollPolicy.dayHeaderKey(day.label, firstId)) {
                Text(
                    day.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                }
                items(
                    items = day.rows,
                    key = { UiScrollPolicy.historyRowKey(it.id) },
                    contentType = { "hist" },
                ) { row ->
                    val d = byId[row.id] ?: return@items
                    DictationCard(
                        d = d,
                        onDelete = {
                            scope.launch { app.dictations.deleteDictation(d.id) }
                        },
                        onShare = {
                            val rows = listOf(
                                HistoryExport.Row(
                                    d.createdAtEpochMs,
                                    d.text,
                                    d.languageTag,
                                    d.wordCount,
                                    d.rawText,
                                )
                            )
                            val shareText = HistoryExport.shareText(rows)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            try {
                                ctx.startActivity(Intent.createChooser(send, "Share dictation"))
                            } catch (_: Exception) {
                            }
                        },
                        onSave = { old, new ->
                            scope.launch {
                                if (app.prefs.autoLearn) {
                                    app.dictations.learnFromEdit(old, new)
                                }
                                app.dictations.updateDictationText(d.id, new)
                            }
                        },
                        onUseRaw = { raw -> useHistoryRaw(ctx, raw) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictationCard(
    d: DictationEntity,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSave: (oldText: String, newText: String) -> Unit,
    onUseRaw: (String) -> Unit,
) {
    var showRaw by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var draft by remember(d.id, d.text) { mutableStateOf(d.text) }
    val hasRaw = d.rawText.isNotBlank() && d.rawText != d.text
    val timeStr = remember(d.createdAtEpochMs) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(d.createdAtEpochMs))
    }
    val ctx = LocalContext.current

    fun copyText(value: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("dictation", value))
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            Text(
                "$timeStr · ${d.wordCount}w" +
                    if (ProcessStatus.isFailed(d.processStatus)) " · Fail" else "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (ProcessStatus.isFailed(d.processStatus)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                softWrap = true
            )

            if (editing) {
                OpenTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = false,
                    minLines = 2,
                    showClearButton = false,
                    modifier = Modifier.testTag("history_edit")
                )
                OpenButton(
                    text = "Save",
                    onClick = {
                        val new = draft.trim()
                        if (new.isNotEmpty() && new != d.text) {
                            onSave(d.text, new)
                        }
                        editing = false
                    },
                    fill = false,
                    variant = ButtonVariant.Filled
                )
            } else {
                Text(
                    d.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { copyText(d.text) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                OpenButton(
                    text = "Copy",
                    onClick = { copyText(d.text) },
                    fill = false,
                    variant = ButtonVariant.Text,
                    modifier = Modifier.testTag("history_copy")
                )
                OpenButton(
                    text = "Share",
                    onClick = onShare,
                    fill = false,
                    variant = ButtonVariant.Text,
                    modifier = Modifier.testTag("history_share")
                )
                Box {
                    IconButton(
                        onClick = { menu = true },
                        modifier = Modifier
                            .size(Dimen.MIN_TOUCH)
                            .testTag("history_more")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        HistoryRowActions.more(hasRaw).forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menu = false
                                    when (label) {
                                        "Edit" -> {
                                            draft = d.text
                                            editing = true
                                        }
                                        "Show raw" -> showRaw = !showRaw
                                        "Use raw" -> onUseRaw(d.rawText)
                                        "Delete" -> onDelete()
                                    }
                                },
                                modifier = if (label == "Use raw") Modifier.testTag("history_use_raw") else Modifier
                            )
                        }
                    }
                }
            }
            if (hasRaw && showRaw) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        d.rawText,
                        modifier = Modifier.padding(Dimen.GAP_SM),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PairImportBlock(
    testPrefix: String,
    onImport: suspend (String) -> PairImport.Outcome,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var paste by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(ctx, "Empty file", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val out = onImport(text)
            Toast.makeText(
                ctx,
                "Added ${out.added} · skip ${out.skipped} · conflict ${out.conflicts}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "CSV: heard,replace — one pair per line",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )
        OpenTextField(
            value = paste,
            onValueChange = { paste = it },
            label = "Paste from,to",
            placeholder = "wisper,Wispr",
            singleLine = false,
            minLines = 2,
            modifier = Modifier.testTag("${testPrefix}_import_paste")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpenButton(
                text = "File",
                fill = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testPrefix}_import_file"),
                variant = ButtonVariant.Outlined,
                onClick = { pick.launch("text/*") }
            )
            OpenButton(
                text = "Paste",
                fill = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testPrefix}_import_paste_go"),
                enabled = paste.isNotBlank(),
                onClick = {
                    scope.launch {
                        val out = onImport(paste)
                        Toast.makeText(
                            ctx,
                            "Added ${out.added} · skip ${out.skipped} · conflict ${out.conflicts}",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (out.added > 0) paste = ""
                    }
                }
            )
        }
    }
}

private fun useHistoryRaw(ctx: Context, raw: String) {
    val said = raw.trim()
    if (said.isBlank()) return
    val svc = FlowAccessibilityService.instance
    if (svc != null) {
        svc.useRawFromHistory(said)
        return
    }
    try {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Open Flow", said))
        Toast.makeText(ctx, ctx.getString(R.string.flow_bubble_copied_clipboard), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryTab(app: OpenFlowApp) {
    val words by app.dictations.observeDictionary().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var dictSort by rememberSaveable { mutableStateOf(DictListPolicy.Sort.ALPHA.name) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var word by rememberSaveable { mutableStateOf("") }
    var repl by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val sort = DictListPolicy.fromPref(dictSort)
    val shown = remember(words, query, sort) {
        val filtered = words.filter { HubListPolicy.matches(query, it.word, it.replacement) }
        DictListPolicy.apply(filtered, sort, { it.createdAtEpochMs }, { it.word })
    }

    Box(Modifier.fillMaxSize().background(SecUi.cream)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "dict-search") {
                OpenTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search dictionary…",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SecUi.muted
                        )
                    },
                    modifier = Modifier.testTag("dict_search")
                )
            }

            item(key = "dict-sort") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    modifier = Modifier.testTag("dict_sort")
                ) {
                    OpenChip(
                        label = "A–Z",
                        isOn = sort == DictListPolicy.Sort.ALPHA,
                        modifier = Modifier.testTag("dict_sort_alpha"),
                        onClick = { dictSort = DictListPolicy.Sort.ALPHA.name }
                    )
                    OpenChip(
                        label = "Newest",
                        isOn = sort == DictListPolicy.Sort.NEWEST,
                        modifier = Modifier.testTag("dict_sort_newest"),
                        onClick = { dictSort = DictListPolicy.Sort.NEWEST.name }
                    )
                    OpenChip(
                        label = "Oldest",
                        isOn = sort == DictListPolicy.Sort.OLDEST,
                        modifier = Modifier.testTag("dict_sort_oldest"),
                        onClick = { dictSort = DictListPolicy.Sort.OLDEST.name }
                    )
                }
            }

            if (shown.isEmpty()) {
                item(key = "dict-empty") {
                    EmptyState(
                        icon = Icons.Default.Tune,
                        title = if (query.isBlank()) "No Dict words" else "No matching results",
                        subtitle = if (query.isBlank()) {
                            "Tap + to add a heard word and what to insert."
                        } else {
                            "Try a different search keyword."
                        },
                        modifier = Modifier.testTag("dict_empty")
                    )
                }
            } else {
                if (query.isBlank()) {
                    item(key = "dict-clear") {
                        OpenButton(
                            text = "Clear all learned",
                            modifier = Modifier.testTag("dict_clear_learned"),
                            onClick = {
                                scope.launch { app.dictations.clearLearned() }
                            }
                        )
                    }
                }
                val autoSet = LearnEngine.autoKeys()
                val autoShown = shown.filter { it.word.lowercase() in autoSet }
                val rest = shown.filter { it.word.lowercase() !in autoSet }
                if (autoShown.isNotEmpty()) {
                    item(key = "dict-auto-header") {
                        Text(
                            "Learned (auto)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                        )
                    }
                    items(
                        items = autoShown,
                        key = { "auto-" + UiScrollPolicy.dictRowKey(it.id) },
                        contentType = { "dict-auto" },
                    ) { w: DictionaryWordEntity ->
                        OpenCard(modifier = Modifier.testTag("dict_auto_row")) {
                            Row(
                                Modifier
                                    .padding(Dimen.MIN_PADDING)
                                    .fillMaxWidth()
                                    .heightIn(min = Dimen.MIN_TOUCH),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        w.word,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecUi.charcoal,
                                        softWrap = true
                                    )
                                    Text(
                                        "→ ${w.replacement}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecUi.ink,
                                        softWrap = true
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch { app.dictations.forget(w.word) }
                                    },
                                    modifier = Modifier
                                        .size(Dimen.MIN_TOUCH)
                                        .testTag("dict_auto_forget")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SecUi.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                items(
                    items = rest,
                    key = { UiScrollPolicy.dictRowKey(it.id) },
                    contentType = { "dict" },
                ) { w: DictionaryWordEntity ->
                    OpenCard {
                        Row(
                            Modifier
                                .padding(Dimen.MIN_PADDING)
                                .fillMaxWidth()
                                .heightIn(min = Dimen.MIN_TOUCH),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    w.word,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SecUi.charcoal,
                                    softWrap = true
                                )
                                Text(
                                    "→ ${w.replacement}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecUi.ink,
                                    softWrap = true
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch { app.dictations.deleteWord(w.id) }
                                },
                                modifier = Modifier
                                    .size(Dimen.MIN_TOUCH)
                                    .testTag("dict_delete")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = SecUi.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("dict_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add dictionary word")
        }
        if (showAdd) {
            Dialog(onDismissRequest = { showAdd = false }) {
                OpenCard {
                    Column(
                        Modifier
                            .padding(Dimen.MIN_PADDING)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Add to Dict",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        OpenTextField(
                            value = word,
                            onValueChange = { word = it },
                            label = "Heard word",
                            placeholder = "Heard word / mistake (e.g. Wisper)",
                            modifier = Modifier.testTag("dict_word")
                        )
                        OpenTextField(
                            value = repl,
                            onValueChange = { repl = it },
                            label = "Replace with",
                            placeholder = "Replace with (e.g. Wispr)",
                            modifier = Modifier.testTag("dict_repl")
                        )
                        OpenButton(
                            text = "Save word",
                            modifier = Modifier.testTag("dict_save_word"),
                            enabled = word.isNotBlank(),
                            onClick = {
                                if (word.isNotBlank()) {
                                    scope.launch {
                                        val ok = app.dictations.addWord(
                                            word.trim(),
                                            repl.ifBlank { word }.trim()
                                        )
                                        if (!ok) {
                                            Toast.makeText(
                                                ctx,
                                                "That word is a snippet trigger",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            word = ""
                                            repl = ""
                                            showAdd = false
                                        }
                                    }
                                }
                            }
                        )
                        PairImportBlock(
                            testPrefix = "dict",
                            onImport = { app.dictations.importDictionary(it) }
                        )
                        OpenButton(
                            text = "Close",
                            variant = ButtonVariant.Outlined,
                            onClick = { showAdd = false },
                            modifier = Modifier.testTag("dict_add_close")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SnippetsTab(app: OpenFlowApp) {
    val snippets by app.dictations.observeSnippets().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var trigger by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val shown = remember(snippets, query) {
        snippets.filter { HubListPolicy.matches(query, it.trigger, it.body) }
    }

    Box(Modifier.fillMaxSize().background(SecUi.cream)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "snip-search") {
                OpenTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search snippets…",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SecUi.muted
                        )
                    },
                    modifier = Modifier.testTag("snippet_search")
                )
            }

            if (shown.isEmpty()) {
                item(key = "snip-empty") {
                    EmptyState(
                        icon = Icons.Default.Tune,
                        title = if (query.isBlank()) "No voice snippets" else "No matching results",
                        subtitle = if (query.isBlank()) {
                            "Tap + to add a trigger and paste block."
                        } else {
                            "Try a different search keyword."
                        }
                    )
                }
            } else {
                items(
                    items = shown,
                    key = { UiScrollPolicy.snippetRowKey(it.id) },
                    contentType = { "snip" },
                ) { s: SnippetEntity ->
                    OpenCard {
                        Column(
                            Modifier.padding(Dimen.MIN_PADDING),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = Dimen.MIN_TOUCH),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Trigger: \"${s.trigger}\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SecUi.charcoal,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { scope.launch { app.dictations.deleteSnippet(s.id) } },
                                    modifier = Modifier.size(Dimen.MIN_TOUCH)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SecUi.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                if (s.body.length > 200) "${s.body.take(200)}…" else s.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecUi.muted,
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("snippet_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add snippet")
        }
        if (showAdd) {
            Dialog(onDismissRequest = { showAdd = false }) {
                OpenCard {
                    Column(
                        Modifier
                            .padding(Dimen.MIN_PADDING)
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "New snippet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        OpenTextField(
                            value = trigger,
                            onValueChange = { trigger = it },
                            label = "Trigger",
                            placeholder = "Trigger (e.g. my address, email sig)"
                        )
                        OpenTextField(
                            value = body,
                            onValueChange = { body = it },
                            label = "Paste block",
                            placeholder = "Expansion text…",
                            singleLine = false,
                            minLines = 3
                        )
                        OpenButton(
                            text = "Add snippet",
                            modifier = Modifier.testTag("snippet_add"),
                            enabled = trigger.isNotBlank() && body.isNotBlank(),
                            onClick = {
                                if (trigger.isNotBlank() && body.isNotBlank()) {
                                    scope.launch {
                                        val ok = app.dictations.addSnippet(trigger.trim(), body.trim())
                                        if (!ok) {
                                            Toast.makeText(
                                                ctx,
                                                "That trigger is a dictionary word",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            trigger = ""
                                            body = ""
                                            showAdd = false
                                        }
                                    }
                                }
                            }
                        )
                        PairImportBlock(
                            testPrefix = "snippet",
                            onImport = { app.dictations.importSnippets(it) }
                        )
                        OpenButton(
                            text = "Close",
                            variant = ButtonVariant.Outlined,
                            onClick = { showAdd = false },
                            modifier = Modifier.testTag("snippet_add_close")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleTab(prefs: FlowPrefs) {
    StyleHubScreen(prefs)
}

@Composable
private fun SettingsHub(
    onSpeechAi: () -> Unit,
    onAppearance: () -> Unit,
    onBubble: () -> Unit,
    onCleanup: () -> Unit,
    onPrivacy: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTerms: () -> Unit,
    onHaptics: () -> Unit,
    onSounds: () -> Unit,
    onFeedback: () -> Unit,
    onReportIssue: () -> Unit,
    onReportSecurity: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
    ) {
        Text(
            "Preferences & local configuration",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )

        SettingsRow("Speech + AI", "Pick speech and rewrite. Where audio and text go.", onSpeechAi)
        SettingsRow("Flow Bubble", "Size, look, and feel of the floating bubble", onBubble)
        SettingsRow("Cleanup Pipeline", "Filler words, course corrections, lists", onCleanup)
        SettingsRow("Appearance", "Dark / light theme and display refresh", onAppearance)
        SettingsRow("Privacy & Retention", "Zero-cloud audit, auto-wipe policies", onPrivacy)
        SettingsRow("Privacy policy", "What this app uses. What can leave this phone.", onPrivacyPolicy)
        SettingsRow("Terms of use", "MIT license, permissions, no warranty", onTerms)
        SettingsRow("Haptics", "Off, Light, or Full tactile feedback", onHaptics)
        SettingsRow("Sounds", "Start / stop audio cues", onSounds)
        SettingsRow(
            "Share feedback",
            "Ideas and questions on GitHub Discussions",
            onFeedback,
        )
        SettingsRow(
            "Report an issue",
            "Bugs on GitHub Issues",
            onReportIssue,
        )
        SettingsRow(
            "Report a vulnerability",
            "Private Security Advisories only — not a public issue",
            onReportSecurity,
        )

        Text(
            "Open Flow is free and open source (MIT). No trackers. No analytics.",
            style = MaterialTheme.typography.labelSmall,
            color = SecUi.muted.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = Dimen.GAP_SM)
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}

@Composable
private fun CleanupSettings(prefs: FlowPrefs) {
    var level by remember { mutableStateOf(prefs.cleanupLevel) }
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
                "Real-time local text cleanup applied before inserting into fields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true
            )

        // Match Wispr Auto Cleanup copy; rules are local FOSS (no cloud AI).
        listOf(
            "none" to ("None" to "Exact speech — zero edits."),
            "light" to ("Light" to "Fillers + grammar: um/uh, repeats, spoken punct commands."),
            "medium" to ("Medium" to "Light + course-correct, false starts, lists, light clarity openers."),
            "high" to ("High" to "Medium + brevity hedges/wordiness (rules). Style still controls tone.")
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = level == v
            OpenCard(
                selected = on,
                onClick = {
                    level = v
                    prefs.cleanupLevel = v
                }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = true
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true
                        )
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacySettings(
    prefs: FlowPrefs,
    onPrivacyPolicy: () -> Unit,
    onTerms: () -> Unit,
) {
    var ret by remember { mutableStateOf(prefs.retentionPolicy) }
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            stringResource(R.string.privacy_no_internet),
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
            modifier = Modifier.testTag("privacy_internet_honesty")
        )
        Text(
            PrivacyHonesty.SETTINGS_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )
        OpenButton(
            text = "Privacy policy",
            onClick = onPrivacyPolicy,
            variant = ButtonVariant.Outlined
        )
        OpenButton(
            text = "Terms of use",
            onClick = onTerms,
            variant = ButtonVariant.Outlined
        )

        var autoLearn by remember { mutableStateOf(prefs.autoLearn) }
        OpenCard(
            selected = autoLearn,
            onClick = {
                autoLearn = !autoLearn
                prefs.autoLearn = autoLearn
            },
            modifier = Modifier.testTag("privacy_auto_learn")
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Auto-learn from fixes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "When you correct a word after dictation, remember it. Off = no new pairs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true
                    )
                }
                if (autoLearn) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "On",
                        tint = SecUi.charcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        listOf(
            "keep" to ("Keep forever" to PrivacyHonesty.KEEP_FOREVER),
            "wipe_24h" to ("Wipe after 24h" to "Delete dictations older than 24 hours on each new save."),
            "never_store" to ("Never store" to "Do not write history. Last-session copy still available until you clear it.")
        ).forEach { (v, pair) ->
            val (title, desc) = pair
            val on = ret == v
            OpenCard(
                selected = on,
                onClick = {
                    ret = v
                    prefs.retentionPolicy = v
                },
                modifier = Modifier.testTag("privacy_" + v)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(Dimen.MIN_PADDING),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true
                        )
                    }
                    if (on) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = SecUi.charcoal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HapticsSettings(prefs: FlowPrefs, onTapPick: (String) -> Unit) {
    var picks by remember {
        mutableStateOf(HapticFeel.Event.entries.associateWith { prefs.hapticPick(it) })
    }
    val view = LocalView.current
    val pickChips = listOf(
        HapticPick.OFF to "Off",
        HapticPick.TICK to "Tick",
        HapticPick.CLICK to "Click",
        HapticPick.CONFIRM to "Confirm",
        HapticPick.REJECT to "Reject",
    )
    val rows = listOf(
        HapticFeel.Event.TAP to "Tap",
        HapticFeel.Event.SAVE to "Save",
        HapticFeel.Event.CANCEL to "Cancel",
        HapticFeel.Event.ERROR to "Error",
        HapticFeel.Event.LISTEN to "Listen",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Pick a feel for each bubble action. Off = none.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = true
        )
        rows.forEach { (event, label) ->
            OpenCard {
                Column(
                    Modifier
                        .padding(Dimen.MIN_PADDING)
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    Text(label, style = MaterialTheme.typography.titleSmall, softWrap = true)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pickChips.forEach { (id, chip) ->
                            OpenChip(
                                label = chip,
                                isOn = picks[event] == id,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    picks = picks + (event to id)
                                    prefs.setHapticPick(event, id)
                                    if (event == HapticFeel.Event.TAP) onTapPick(id)
                                }
                            )
                        }
                    }
                    OpenButton(
                        text = "Test",
                        onClick = {
                            val c = HapticPick.constant(picks[event] ?: HapticPick.CLICK)
                                ?: return@OpenButton
                            view.performHapticFeedback(c)
                        },
                        variant = ButtonVariant.Outlined,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (event == HapticFeel.Event.TAP) Modifier.testTag("haptics_test")
                                else Modifier
                            )
                    )
                }
            }
        }
        OpenButton(
            text = "Reset",
            onClick = {
                prefs.resetHaptics()
                picks = HapticFeel.Event.entries.associateWith { prefs.hapticPick(it) }
                onTapPick(picks.getValue(HapticFeel.Event.TAP))
            },
            variant = ButtonVariant.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("haptics_reset")
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundsSettings(prefs: FlowPrefs) {
    var sounds by remember { mutableStateOf(prefs.bubbleSounds) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Audio cues when dictation starts or stops.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = true
        )

        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING)
                    .heightIn(min = Dimen.MIN_TOUCH),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Start / Stop Audio Cue",
                        style = MaterialTheme.typography.titleSmall,
                        softWrap = true
                    )
                    Text(
                        "Play a short tone when dictation starts or stops",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true
                    )
                }
                OpenChip(
                    label = if (sounds) "ON" else "OFF",
                    isOn = sounds,
                    onClick = {
                        sounds = !sounds
                        prefs.bubbleSounds = sounds
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleEditor(
    title: String,
    subtitle: String,
    modules: List<LayoutPrefs.Module>,
    labels: Map<String, String>,
    lockVisible: Set<String> = emptySet(),
    defaultEncode: String? = null,
    onChange: (List<LayoutPrefs.Module>) -> Unit
) {
    var local by remember(modules) { mutableStateOf(modules) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SecUi.charcoal,
            softWrap = true
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = true
        )
        Text(
            "Order = top to bottom on Home. Hide blocks you never use. Bottom tabs stay fixed.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = true
        )
        local.forEachIndexed { index, m ->
            val locked = m.id in lockVisible
            val what = HomeFeelCopy.moduleWhat(m.id)
            OpenCard(
                onClick = { focusedId = m.id }
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}. ${labels[m.id] ?: m.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SecUi.charcoal,
                            softWrap = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (m.visible) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (m.visible) SecUi.ink else SecUi.muted
                        )
                    }
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ModuleEditorVisibility.showHideChip(locked)) {
                            OpenChip(
                                label = if (m.visible) "Show" else "Hide",
                                isOn = m.visible,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    focusedId = m.id
                                    local = LayoutPrefs.toggleVisible(local, m.id)
                                    onChange(local)
                                }
                            )
                        }
                        OpenChip(
                            label = "↑ Up",
                            isOn = false,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                focusedId = m.id
                                local = LayoutPrefs.move(local, m.id, -1)
                                onChange(local)
                            }
                        )
                        OpenChip(
                            label = "↓ Down",
                            isOn = false,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                focusedId = m.id
                                local = LayoutPrefs.move(local, m.id, 1)
                                onChange(local)
                            }
                        )
                    }
                    if (focusedId == m.id && what.isNotEmpty()) {
                        Text(
                            what,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.charcoal
                        )
                    }
                    if (locked) {
                        Text(
                            "Always available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (defaultEncode != null) {
            OpenButton(
                text = "Reset to default order",
                variant = ButtonVariant.Outlined,
                onClick = {
                    val catalog = if (defaultEncode == LayoutPrefs.DEFAULT_HOME) {
                        LayoutPrefs.HOME_MODULES
                    } else {
                        LayoutPrefs.DRAWER_EXTRAS
                    }
                    local = LayoutPrefs.parseModules(defaultEncode, catalog)
                    onChange(local)
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    // Flat row — no offset-shadow OpenCard (many shadows = Settings jank).
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimen.MIN_TOUCH)
            .border(SecUi.hardBorder)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimen.MIN_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SecUi.charcoal,
                softWrap = true
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SecUi.muted,
                softWrap = true,
                maxLines = 2
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open $title",
            tint = SecUi.muted
        )
    }
}

@Composable
private fun AppearanceColorRow(
    label: String,
    hex: String,
    argb: Int,
    onHex: (String) -> Unit,
    tag: String? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
    ) {
        OpenTextField(
            value = hex,
            onValueChange = onHex,
            label = label,
            placeholder = "#RRGGBB",
            modifier = Modifier
                .weight(1f)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )
        Box(
            Modifier
                .size(32.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                .background(Color(argb))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettings(prefs: FlowPrefs) {
    val dark by prefs.darkMode.collectAsState()
    val pal by prefs.appearance.collectAsState()
    val context = LocalContext.current
    var refreshHz by remember { mutableIntStateOf(prefs.refreshHz) }
        var sttProfile by remember { mutableStateOf(prefs.sttProfile) }
        var preferOnDevice by remember { mutableStateOf(prefs.preferOnDevice) }
        var languageTag by remember { mutableStateOf(prefs.languageTag) }
    var bgHex by remember {
        mutableStateOf(prefs.colorBg.ifEmpty { HexColor.format(pal.backgroundArgb) })
    }
    var cardsHex by remember {
        mutableStateOf(prefs.colorCards.ifEmpty { HexColor.format(pal.cardsArgb) })
    }
    var textHex by remember {
        mutableStateOf(prefs.colorText.ifEmpty { HexColor.format(pal.textArgb) })
    }
    var accentHex by remember {
        mutableStateOf(prefs.colorAccent.ifEmpty { HexColor.format(pal.accentArgb) })
    }
    var borderHex by remember {
        mutableStateOf(prefs.colorBorder.ifEmpty { HexColor.format(pal.borderArgb) })
    }
    var idleHex by remember {
        mutableStateOf(prefs.colorBubbleIdle.ifEmpty { HexColor.format(pal.bubbleIdleArgb) })
    }
    var listenHex by remember {
        mutableStateOf(prefs.colorBubbleListen.ifEmpty { HexColor.format(pal.bubbleListenArgb) })
    }
    var bubbleTextHex by remember {
        mutableStateOf(prefs.colorBubbleText.ifEmpty { HexColor.format(pal.bubbleTextArgb) })
    }
    val deviceModes = remember(context) {
        try {
            val d = if (android.os.Build.VERSION.SDK_INT >= 30) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .defaultDisplay
            }
            d?.supportedModes?.map {
                DisplayRefreshPolicy.ModeInfo(it.modeId, it.refreshRate)
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }
    val hzChoices = remember(deviceModes) {
        DisplayRefreshPolicy.availableTargets(deviceModes).ifEmpty {
            DisplayRefreshPolicy.TARGETS_HZ
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Theme, motion smoothness, and STT speed. Changes apply now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = true
        )

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Colors",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = true
                )
                Text(
                    "Hex #RRGGBB or #AARRGGBB. Empty uses factory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
                AppearanceColorRow(
                    label = "Background",
                    hex = bgHex,
                    argb = pal.backgroundArgb,
                    tag = "appearance_color_bg",
                    onHex = {
                        bgHex = it
                        prefs.colorBg = it
                    }
                )
                AppearanceColorRow(
                    label = "Cards",
                    hex = cardsHex,
                    argb = pal.cardsArgb,
                    onHex = {
                        cardsHex = it
                        prefs.colorCards = it
                    }
                )
                AppearanceColorRow(
                    label = "Text",
                    hex = textHex,
                    argb = pal.textArgb,
                    onHex = {
                        textHex = it
                        prefs.colorText = it
                    }
                )
                AppearanceColorRow(
                    label = "Accent",
                    hex = accentHex,
                    argb = pal.accentArgb,
                    onHex = {
                        accentHex = it
                        prefs.colorAccent = it
                    }
                )
                AppearanceColorRow(
                    label = "Border",
                    hex = borderHex,
                    argb = pal.borderArgb,
                    onHex = {
                        borderHex = it
                        prefs.colorBorder = it
                    }
                )
                AppearanceColorRow(
                    label = "Bubble idle",
                    hex = idleHex,
                    argb = pal.bubbleIdleArgb,
                    onHex = {
                        idleHex = it
                        prefs.colorBubbleIdle = it
                        FlowAccessibilityService.instance?.applyPrefsVisual()
                    }
                )
                AppearanceColorRow(
                    label = "Bubble listen",
                    hex = listenHex,
                    argb = pal.bubbleListenArgb,
                    onHex = {
                        listenHex = it
                        prefs.colorBubbleListen = it
                        FlowAccessibilityService.instance?.applyPrefsVisual()
                    }
                )
                AppearanceColorRow(
                    label = "Bubble text",
                    hex = bubbleTextHex,
                    argb = pal.bubbleTextArgb,
                    onHex = {
                        bubbleTextHex = it
                        prefs.colorBubbleText = it
                        FlowAccessibilityService.instance?.applyPrefsVisual()
                    }
                )
                OpenButton(
                    text = "Reset colors",
                    onClick = {
                        prefs.resetAppearanceColors()
                        val p = prefs.palette()
                        bgHex = HexColor.format(p.backgroundArgb)
                        cardsHex = HexColor.format(p.cardsArgb)
                        textHex = HexColor.format(p.textArgb)
                        accentHex = HexColor.format(p.accentArgb)
                        borderHex = HexColor.format(p.borderArgb)
                        idleHex = HexColor.format(p.bubbleIdleArgb)
                        listenHex = HexColor.format(p.bubbleListenArgb)
                        bubbleTextHex = HexColor.format(p.bubbleTextArgb)
                        FlowAccessibilityService.instance?.applyPrefsVisual()
                    },
                    variant = ButtonVariant.Outlined,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appearance_reset")
                )
            }
        }

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Color theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text(
                    "Light / Dark / System — all screens + bubble chrome follow this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = dark == v,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = { prefs.setDarkMode(v) }
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Screen refresh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text(
                    "Prefer 60 / 90 / 120 / 144 Hz when the phone supports it. Device may clamp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hzChoices.forEach { hz ->
                        OpenChip(
                            label = "${hz}Hz",
                            isOn = refreshHz == hz,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                refreshHz = hz
                                prefs.refreshHz = hz
                                (context as? android.app.Activity)?.let {
                                    DisplayRefreshController.apply(it, hz)
                                }
                            }
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dictation speed (STT)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text(
                    "Fast = shorter silence wait. Accurate = longer listen + engine punctuation. Local cleanup still runs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SttTuning.PROFILE_FAST to "Fast",
                        SttTuning.PROFILE_BALANCED to "Balanced",
                        SttTuning.PROFILE_ACCURATE to "Accurate"
                    ).forEach { (id, label) ->
                        OpenChip(
                            label = label,
                            isOn = sttProfile == id,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                sttProfile = id
                                prefs.sttProfile = id
                                FlowAccessibilityService.instance?.applyPrefsVisual()
                            }
                        )
                    }
                }
                Text(
                    "Applies on next listen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "On-device speech",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = true
                )
                Text(
                    OnDeviceSpeechPolicy.honesty(preferOnDevice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                    modifier = Modifier.testTag("on_device_honesty")
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenChip(
                        label = "Off",
                        isOn = !preferOnDevice,
                        modifier = Modifier
                            .wrapContentHeight()
                            .testTag("on_device_off"),
                        onClick = {
                            preferOnDevice = false
                            prefs.preferOnDevice = false
                        }
                    )
                    OpenChip(
                        label = "On",
                        isOn = preferOnDevice,
                        modifier = Modifier
                            .wrapContentHeight()
                            .testTag("on_device_on"),
                        onClick = {
                            preferOnDevice = true
                            prefs.preferOnDevice = true
                        }
                    )
                }
                Text(
                    "Applies on next listen. Whisper-on-phone is later.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Speech language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text(
                    "Used by system STT and cloud ears. Pick en-IN / hi-IN for Indian English or Hindi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguagePolicy.SUPPORTED_LANGUAGES.forEach { opt ->
                        OpenChip(
                            label = opt.displayName,
                            isOn = languageTag == opt.tag,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                languageTag = opt.tag
                                prefs.languageTag = opt.tag
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BubbleSettings(prefs: FlowPrefs, onApplyBubble: () -> Unit) {
    val pal by prefs.appearance.collectAsState()
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(BubbleScaleSteps.nearest(prefs.bubbleScale)) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var shape by remember { mutableStateOf(prefs.bubbleShape) }
    var showText by remember { mutableStateOf(prefs.bubbleShowText) }
    var snap by remember { mutableStateOf(prefs.bubbleEdgeSnap) }
    var pulse by remember { mutableStateOf(prefs.bubblePulse) }
    var tint by remember { mutableStateOf(prefs.bubbleTint) }
    var roundness by remember { mutableStateOf(prefs.bubbleRoundness) }
    var roundPct by remember { mutableIntStateOf(prefs.bubbleRoundPct) }
    var showCancel by remember { mutableStateOf(prefs.bubbleShowCancel) }
    var showDone by remember { mutableStateOf(prefs.bubbleShowDone) }
    var shrinkIdle by remember { mutableStateOf(prefs.bubbleShrinkIdle) }
    var shrinkDot by remember { mutableStateOf(prefs.bubbleShrinkDot) }
    var shrinkSearch by remember { mutableStateOf(prefs.bubbleShrinkSearch) }
    var iconUri by remember { mutableStateOf(prefs.bubbleIconUri) }
    val pickIcon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val stored = persistBubbleIcon(ctx, uri)
        prefs.bubbleIconUri = stored
        iconUri = stored
        onApplyBubble()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Size, look, and feel of the floating bubble. Changes apply live.",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )
        Text(
            "Size",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SecUi.charcoal,
            softWrap = true
        )
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    BubbleScaleSteps.STEPS.forEach { step ->
                        val pct = (step * 100).toInt()
                        OpenChip(
                            label = "$pct%",
                            isOn = BubbleScaleSteps.nearest(scale) == step,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                scale = step
                                prefs.bubbleScale = step
                                onApplyBubble()
                            }
                        )
                    }
                }
                OpenButton(
                    text = "Reset size",
                    onClick = {
                        prefs.resetBubbleScale()
                        scale = prefs.bubbleScale
                        onApplyBubble()
                    },
                    variant = ButtonVariant.Outlined
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opacity", style = MaterialTheme.typography.bodyMedium, color = SecUi.charcoal)
                    Text(
                        "${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.ink
                    )
                }
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    onValueChangeFinished = {
                        prefs.bubbleOpacity = opacity
                        opacity = prefs.bubbleOpacity
                        onApplyBubble()
                    },
                    valueRange = 0.20f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = SecUi.charcoal,
                        activeTrackColor = SecUi.charcoal,
                        inactiveTrackColor = SecUi.stone
                    )
                )
            }
        }

        Text(
            "Look",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SecUi.charcoal,
            softWrap = true
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(vertical = Dimen.GAP_SM)
                .border(SecUi.hardBorder)
                .background(Color(BubbleTint.previewStageArgb(tint)))
                .testTag("bubble_preview"),
            contentAlignment = Alignment.Center
        ) {
            val previewShape = when {
                shape == "circle" || shape == "dot" -> CircleShape
                else -> RoundedCornerShape(percent = roundPct.coerceIn(0, 100))
            }
            val baseW = when (shape) {
                "dot" -> 22.dp
                "circle" -> 44.dp
                "square" -> 44.dp
                else -> 76.dp
            }
            val baseH = if (shape == "pill") 34.dp else baseW
            val customBmp = remember(iconUri) {
                val f = BubbleIconPolicy.localFile(ctx.filesDir)
                if (!f.isFile || f.length() <= 0L) {
                    null
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, bounds)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = BubbleIconPolicy.decodeSampleSize(bounds.outWidth, bounds.outHeight)
                    }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                }
            }
            Row(
                modifier = Modifier
                    .size(baseW * scale, baseH * scale)
                    .graphicsLayer { alpha = opacity }
                    .background(Color(pal.bubbleIdleArgb), previewShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (customBmp != null) {
                    Image(
                        bitmap = customBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp * scale)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color(pal.bubbleTextArgb),
                        modifier = Modifier.size(18.dp * scale)
                    )
                }
                if (showText && shape != "dot") {
                    Text(
                        "Listen",
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color(pal.bubbleTextArgb),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Shape",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    listOf(
                        "pill" to "Pill",
                        "circle" to "Circle",
                        "square" to "Squircle",
                        "dot" to "Dot"
                    ).forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = shape == v,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                shape = v
                                prefs.bubbleShape = v
                                onApplyBubble()
                            }
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    listOf(
                        BubbleTint.CHARCOAL to "Charcoal",
                        BubbleTint.CREAM to "Cream",
                        BubbleTint.INK to "Ink",
                        BubbleTint.STONE to "Stone",
                        BubbleTint.SKY to "Sky",
                        BubbleTint.FOREST to "Forest",
                        BubbleTint.CORAL to "Coral",
                        BubbleTint.GRAPE to "Grape",
                    ).forEach { (id, label) ->
                        OpenChip(
                            label = label,
                            isOn = tint == id,
                            showCheckWhenOn = true,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                tint = id
                                prefs.bubbleTint = id
                                prefs.colorBubbleIdle = HexColor.format(BubbleTint.argb(id))
                                prefs.colorBubbleListen = HexColor.format(BubbleTint.argb(id))
                                prefs.colorBubbleText = HexColor.format(BubbleTint.onArgb(id))
                                onApplyBubble()
                            }
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Corners",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                Text(
                    "Hard = sharp. Soft / Round = softer pill edges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    listOf(
                        BubbleChrome.ROUND_HARD to "Hard",
                        BubbleChrome.ROUND_SOFT to "Soft",
                        BubbleChrome.ROUND_ROUND to "Round",
                    ).forEach { (id, label) ->
                        OpenChip(
                            label = label,
                            isOn = roundness == id,
                            showCheckWhenOn = true,
                            modifier = Modifier
                                .wrapContentHeight()
                                .testTag("bubble_round_$id"),
                            onClick = {
                                roundness = id
                                prefs.bubbleRoundness = id
                                roundPct = BubbleChrome.pctFromLegacy(id)
                                prefs.bubbleRoundPct = roundPct
                                onApplyBubble()
                            }
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Roundness", style = MaterialTheme.typography.bodyMedium, color = SecUi.charcoal)
                    Text(
                        "$roundPct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.ink
                    )
                }
                Slider(
                    value = roundPct.toFloat(),
                    onValueChange = { roundPct = it.toInt() },
                    onValueChangeFinished = {
                        prefs.bubbleRoundPct = roundPct
                        onApplyBubble()
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = SecUi.charcoal,
                        activeTrackColor = SecUi.charcoal,
                        inactiveTrackColor = SecUi.stone
                    )
                )
            }
        }

        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Show text on bubble",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "Show live words on the bubble while listening",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true
                    )
                }
                OpenChip(
                    label = if (showText) "ON" else "OFF",
                    isOn = showText,
                    onClick = {
                        showText = !showText
                        prefs.bubbleShowText = showText
                        onApplyBubble()
                    }
                )
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Listen buttons",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    OpenChip(
                        label = "Show Cancel",
                        isOn = showCancel,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            showCancel = !showCancel
                            prefs.bubbleShowCancel = showCancel
                            onApplyBubble()
                        }
                    )
                    OpenChip(
                        label = "Show Done",
                        isOn = showDone,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            showDone = !showDone
                            prefs.bubbleShowDone = showDone
                            onApplyBubble()
                        }
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Shrink when idle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                OpenChip(
                    label = if (shrinkIdle) "ON" else "OFF",
                    isOn = shrinkIdle,
                    onClick = {
                        shrinkIdle = !shrinkIdle
                        prefs.bubbleShrinkIdle = shrinkIdle
                        onApplyBubble()
                    }
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    OpenChip(
                        label = "Dot",
                        isOn = shrinkDot,
                        enabled = shrinkIdle,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            shrinkDot = !shrinkDot
                            prefs.bubbleShrinkDot = shrinkDot
                            onApplyBubble()
                        }
                    )
                    OpenChip(
                        label = "Search",
                        isOn = shrinkSearch,
                        enabled = shrinkIdle,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            shrinkSearch = !shrinkSearch
                            prefs.bubbleShrinkSearch = shrinkSearch
                            onApplyBubble()
                        }
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Text(
                    "Custom icon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                Text(
                    if (BubbleIconPolicy.validUri(iconUri)) "Custom image set" else "Default mic",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted
                )
                OpenButton(
                    text = "Pick image",
                    onClick = { pickIcon.launch(arrayOf("image/*")) },
                    variant = ButtonVariant.Outlined
                )
                OpenButton(
                    text = "Clear icon",
                    onClick = {
                        clearBubbleIcon(ctx)
                        prefs.bubbleIconUri = ""
                        iconUri = ""
                        onApplyBubble()
                    },
                    variant = ButtonVariant.Text,
                    enabled = BubbleIconPolicy.validUri(iconUri) ||
                        BubbleIconPolicy.localFile(ctx.filesDir).isFile
                )
            }
        }

        Text(
            "Feel",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SecUi.charcoal,
            softWrap = true
        )
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Snap to edge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "On release, snap to the nearest left or right edge",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true
                        )
                    }
                    OpenChip(
                        label = if (snap) "ON" else "OFF",
                        isOn = snap,
                        onClick = {
                            snap = !snap
                            prefs.bubbleEdgeSnap = snap
                            onApplyBubble()
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Recording pulse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "Pulse with voice volume while recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true
                        )
                    }
                    OpenChip(
                        label = if (pulse) "ON" else "OFF",
                        isOn = pulse,
                        onClick = {
                            pulse = !pulse
                            prefs.bubblePulse = pulse
                            onApplyBubble()
                        }
                    )
                }
            }
        }

        OpenButton(
            text = "Wake / reset bubble",
            onClick = {
                prefs.clearSnooze()
                onApplyBubble()
            },
            variant = ButtonVariant.Outlined
        )
        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}

private fun persistBubbleIcon(ctx: Context, uri: Uri): String {
    try {
        ctx.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {
    }
    val dest = BubbleIconPolicy.localFile(ctx.filesDir)
    try {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        if (dest.isFile && dest.length() > 0L) return dest.toURI().toString()
    } catch (_: Exception) {
    }
    return uri.toString()
}

private fun clearBubbleIcon(ctx: Context) {
    BubbleIconPolicy.localFile(ctx.filesDir).delete()
}
