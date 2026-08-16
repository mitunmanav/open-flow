package app.openflow.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import app.openflow.bubble.FlowAccessibilityService
import app.openflow.data.DictationEntity
import app.openflow.data.DictionaryWordEntity
import app.openflow.data.SnippetEntity
import app.openflow.export.HistoryExport
import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.LayoutPrefs
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
import app.openflow.stt.SttTuning
import app.openflow.ui.engine.EngineSettingsScreen
import app.openflow.ui.home.DictListPolicy
import app.openflow.ui.home.HubListPolicy
import app.openflow.ui.home.HistoryDays
import app.openflow.ui.home.HistorySearchPolicy
import app.openflow.ui.home.HomeBannerPolicy
import app.openflow.ui.home.ModuleEditorVisibility
import app.openflow.ui.home.UiScrollPolicy
import app.openflow.ui.privacy.PrivacyHonesty
import app.openflow.ui.setup.FirstRunPolicy
import app.openflow.ui.setup.SetupWizard
import app.openflow.bubble.AppCategory
import app.openflow.bubble.AppContextEngine
import app.openflow.bubble.AppOverride
import app.openflow.ui.shell.AppRoute
import app.openflow.ui.shell.AppShell
import app.openflow.ui.shell.NavStack
import app.openflow.ui.theme.BubbleTint
import app.openflow.ui.theme.Motion
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.theme.VisualSkin
import app.openflow.ui.theme.VisualSkin.*
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
            OpenFlowTheme(darkMode = darkMode, skin = skin) {
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
                                onOpenBubbleSettings = { goTo(AppRoute.BubbleSettings) },
                                onOpenAppearance = { goTo(AppRoute.Appearance) },
                                onOpenCleanup = { goTo(AppRoute.Cleanup) },
                                onOpenStyle = { goTo(AppRoute.Style) },
                                onOpenSpeechAi = { goTo(AppRoute.SpeechAi) },
                                onBattery = {
                                    try {
                                        startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        )
                                    } catch (_: Exception) {
                                        try {
                                            startActivity(Intent(Settings.ACTION_SETTINGS))
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            )
                            AppRoute.History -> HistoryScreen(app)
                            AppRoute.Dictionary -> DictionaryTab(app)
                            AppRoute.Snippets -> SnippetsTab(app)
                            AppRoute.Style -> StyleTab(app.prefs)
                            AppRoute.Settings -> SettingsHub(
                                onSpeechAi = { goTo(AppRoute.SpeechAi) },
                                onAppearance = { goTo(AppRoute.Appearance) },
                                onBubble = { goTo(AppRoute.BubbleSettings) },
                                onCleanup = { goTo(AppRoute.Cleanup) },
                                onPrivacy = { goTo(AppRoute.Privacy) },
                                onSounds = { goTo(AppRoute.Sounds) },
                                onHomeLayout = { goTo(AppRoute.HomeModules) }
                            )
                            AppRoute.SpeechAi -> {
                                val session = app.engineSession
                                EngineSettingsScreen(
                                    initialEar = app.enginePrefs.earId,
                                    initialBrain = app.enginePrefs.brainId,
                                    initialUrl = app.enginePrefs.customBaseUrl,
                                    initialSarvamMode = app.enginePrefs.sarvamMode,
                                    initialKeyMask = session.keyMask(),
                                    initialEarKeyMask = session.earKeyMask(),
                                    initialBrainKeyMask = session.brainKeyMask(),
                                    onPick = { e, b -> session.pick(e, b) },
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
                            AppRoute.Cleanup -> CleanupSettings(app.prefs)
                            AppRoute.Privacy -> PrivacySettings(app.prefs)
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
                                    try {
                                        startActivity(
                                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        )
                                    } catch (_: Exception) {
                                        try {
                                            startActivity(Intent(Settings.ACTION_SETTINGS))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHub(
    app: OpenFlowApp,
    bubbleOn: Boolean,
    micOn: Boolean,
    onEnableBubble: () -> Unit,
    onMic: () -> Unit,
    onOpenBubbleSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenCleanup: () -> Unit,
    onOpenStyle: () -> Unit,
    onOpenSpeechAi: () -> Unit,
    onBattery: () -> Unit
) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var statsText by remember { mutableStateOf("…") }
    var localNote by rememberSaveable { mutableStateOf("") }
    var cleanup by remember { mutableStateOf(app.prefs.cleanupLevel) }
    var showText by remember { mutableStateOf(app.prefs.bubbleShowText) }
    var lastClean by remember { mutableStateOf(app.prefs.lastCleanText) }
    var lastRaw by remember { mutableStateOf(app.prefs.lastRawText) }
    var snoozed by remember { mutableStateOf(app.prefs.isSnoozed()) }
    var seenHowTo by remember { mutableStateOf(app.prefs.seenHowTo) }
    var homeSearch by rememberSaveable { mutableStateOf("") }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                lastClean = app.prefs.lastCleanText
                lastRaw = app.prefs.lastRawText
                snoozed = app.prefs.isSnoozed()
                scope.launch {
                    val s = app.dictations.stats()
                    statsText = "${s.totalWords} words · ${s.totalSessions} sessions · ${s.streakDays}d streak"
                }
            }
        }
        owner.lifecycle.addObserver(obs)
        scope.launch {
            val s = app.dictations.stats()
            statsText = "${s.totalWords} words · ${s.totalSessions} sessions · ${s.streakDays}d streak"
        }
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val ready = bubbleOn && micOn
    val homeModules = app.prefs.homeModules()
    val visibleModules = homeModules.filter { it.visible }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState())
            .testTag("home_hub"),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        if (!seenHowTo) {
            OpenCard(modifier = Modifier.testTag("home_howto")) {
                Column(
                    Modifier
                        .padding(Dimen.MIN_PADDING)
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "How Open Flow works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "Not a keyboard. Keep yours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "Tap the bubble, then tap again to insert.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "X cancel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    Text(
                        "Dict = one word. Snippet = whole block.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    OpenButton(
                        text = "Got it",
                        onClick = {
                            app.prefs.seenHowTo = true
                            seenHowTo = true
                        },
                        modifier = Modifier.testTag("home_howto_got_it")
                    )
                }
            }
        }

        if (visibleModules.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Tune,
                title = "Home is empty",
                subtitle = "Turn cards on in Settings → Home layout.",
                modifier = Modifier.testTag("home_empty")
            )
        }

        when (val banner = HomeBannerPolicy.banner(bubbleOn = bubbleOn, micOn = micOn, snoozed = snoozed)) {
            HomeBannerPolicy.Banner.REPAIR_A11Y -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_repair")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        if (copy.body != null) {
                            Text(
                                copy.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        OpenButton(
                            text = copy.cta ?: "Open Accessibility",
                            onClick = onEnableBubble,
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_a11y")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.ALLOW_MIC -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_mic")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        if (copy.body != null) {
                            Text(
                                copy.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        OpenButton(
                            text = copy.cta ?: "Allow microphone",
                            onClick = onMic,
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_mic_btn")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.END_SNOOZE -> {
                val copy = HomeBannerPolicy.copy(banner)
                OpenCard(modifier = Modifier.testTag("home_banner_snooze")) {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true
                        )
                        OpenButton(
                            text = copy.cta ?: "End snooze",
                            onClick = {
                                app.prefs.clearSnooze()
                                snoozed = false
                                android.widget.Toast.makeText(
                                    ctx,
                                    "Snooze ended",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            contentDescription = copy.a11yLabel,
                            modifier = Modifier.testTag("home_banner_end_snooze")
                        )
                    }
                }
            }
            HomeBannerPolicy.Banner.NONE -> Unit
        }

        visibleModules.forEach { module ->
            when (module.id) {
                "setup" -> {
                    // Card title only — page title is AppShell top bar ("Open Flow").
                    OpenCard(modifier = Modifier.testTag("home_setup")) {
                        Column(
                            Modifier.padding(Dimen.MIN_PADDING),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (ready) "Ready to dictate" else "Finish setup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = true,
                                        modifier = Modifier.testTag("home_setup_title")
                                    )
                                    Text(
                                        "Floating bubble · local polish · any app",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                                // Hard badge: charcoal block when ON, cream + hard border when SETUP.
                                Surface(
                                    color = if (ready) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.testTag(
                                        if (ready) "home_setup_badge_on" else "home_setup_badge_setup"
                                    )
                                ) {
                                    Text(
                                        text = if (ready) "ON" else "SETUP",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ready) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                            Text(
                                when {
                                    ready ->
                                        "Focus a text field → tap the floating bubble → speak → tap again. Polished text inserts once."
                                    !bubbleOn && !micOn ->
                                        "Two steps left: turn on Accessibility (Open Flow Bubble), then allow the microphone. Force-stop or reinstall turns Accessibility off."
                                    !bubbleOn ->
                                        "Repair: Open Flow is not in Accessibility. Tap Enable bubble, turn it ON, then return here."
                                    else ->
                                        "Allow the microphone, then focus a field and tap the bubble."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true,
                                modifier = Modifier.testTag("home_setup_copy")
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_setup_chips"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OpenChip(
                                    label = if (bubbleOn) "Bubble on" else "Enable bubble",
                                    isOn = bubbleOn,
                                    showCheckWhenOn = true,
                                    modifier = Modifier.testTag("setup_chip_bubble"),
                                    onClick = onEnableBubble
                                )
                                OpenChip(
                                    label = if (micOn) "Mic on" else "Allow mic",
                                    isOn = micOn,
                                    showCheckWhenOn = true,
                                    modifier = Modifier.testTag("setup_chip_mic"),
                                    onClick = onMic
                                )
                            }
                            if (!bubbleOn) {
                                OpenButton(
                                    text = "Open Accessibility",
                                    onClick = onEnableBubble,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_btn_a11y")
                                )
                            }
                            if (!micOn) {
                                OpenButton(
                                    text = "Allow microphone",
                                    onClick = onMic,
                                    variant = if (bubbleOn) ButtonVariant.Filled else ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_btn_mic")
                                )
                            }
                            if (ready) {
                                OpenButton(
                                    text = "Bubble settings",
                                    onClick = onOpenBubbleSettings,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_btn_bubble_settings")
                                )
                                OpenButton(
                                    text = "Battery settings",
                                    onClick = onBattery,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_chip_battery")
                                )
                            } else {
                                OpenButton(
                                    text = "Battery settings",
                                    onClick = onBattery,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_chip_battery")
                                )
                            }
                        }
                    }
                }
                "test" -> {
                    OpenCard(modifier = Modifier.testTag("home_practice")) {
                        Column(
                            Modifier.padding(Dimen.MIN_PADDING),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Practice field",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Focus here, then use the floating bubble.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OpenTextField(
                                value = localNote,
                                onValueChange = { localNote = it },
                                placeholder = "Tap bubble · speak · tap stop…",
                                singleLine = false,
                                minLines = 2,
                                modifier = Modifier.testTag("practice_field")
                            )
                        }
                    }
                }
                "keys" -> {
                    OpenCard(modifier = Modifier.testTag("home_keys")) {
                        Column(
                            Modifier
                                .padding(Dimen.MIN_PADDING)
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Cleanup level",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                softWrap = true
                            )
                            // FlowRow: no overflow on narrow screens (weight Row clips).
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .testTag("home_cleanup_chips"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "none" to "None",
                                    "light" to "Light",
                                    "medium" to "Medium",
                                    "high" to "High"
                                ).forEach { (level, label) ->
                                    OpenChip(
                                        label = label,
                                        isOn = cleanup == level,
                                        modifier = Modifier
                                            .wrapContentHeight()
                                            .testTag("cleanup_$level"),
                                        onClick = {
                                            cleanup = level
                                            app.prefs.cleanupLevel = level
                                        }
                                    )
                                }
                            }
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .testTag("home_keys_chips"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OpenChip(
                                    label = "Speech on bubble",
                                    isOn = showText,
                                    modifier = Modifier
                                        .wrapContentHeight()
                                        .testTag("keys_speech_on_bubble"),
                                    onClick = {
                                        showText = !showText
                                        app.prefs.bubbleShowText = showText
                                        FlowAccessibilityService.instance?.applyPrefsVisual()
                                    }
                                )
                            }
                            Text(
                                "More",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                            ) {
                                OpenButton(
                                    text = "Speech + AI",
                                    onClick = onOpenSpeechAi,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_link_speech_ai")
                                )
                                OpenButton(
                                    text = "Cleanup",
                                    onClick = onOpenCleanup,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_link_cleanup")
                                )
                                OpenButton(
                                    text = "Style",
                                    onClick = onOpenStyle,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_link_style")
                                )
                                OpenButton(
                                    text = "Theme",
                                    onClick = onOpenAppearance,
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_link_theme")
                                )
                            }
                        }
                    }
                }
                "stats" -> {
                    val topRecent = dictations.firstOrNull()?.text?.trim().orEmpty()
                    val showLastCard = lastClean.isNotBlank() &&
                        lastClean.trim() != topRecent
                    if (showLastCard) {
                        OpenCard(modifier = Modifier.testTag("home_last_dictation")) {
                            Column(
                                Modifier.padding(Dimen.MIN_PADDING),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Last dictation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    lastClean.take(600),
                                    style = MaterialTheme.typography.bodyLarge,
                                    softWrap = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CopyButton(text = lastClean, label = "Copy clean")
                                    if (lastRaw.isNotBlank() && lastRaw != lastClean) {
                                        CopyButton(text = lastRaw, label = "Copy raw")
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .testTag("home_stats"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = true
                            )
                            Text(
                                statsText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                    }
                }
                "recent" -> {
                    OpenTextField(
                        value = homeSearch,
                        onValueChange = { homeSearch = it },
                        placeholder = "Search transcripts…",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = SecUi.muted
                            )
                        },
                        modifier = Modifier.testTag("home_history_search")
                    )
                    val shown = dictations.filter {
                        HubListPolicy.matches(homeSearch, it.text, it.rawText)
                    }
                    if (shown.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.MicNone,
                            title = if (homeSearch.isBlank()) "No dictations yet" else "No matching results",
                            subtitle = if (homeSearch.isBlank()) {
                                "Use the floating bubble in any app to build private history."
                            } else {
                                "Try a different search keyword."
                            },
                            modifier = Modifier.testTag("home_recent_empty")
                        )
                    } else {
                        val nowMs = System.currentTimeMillis()
                        val days = HistoryDays.group(
                            shown.map { HistoryDays.Row(it.id, it.createdAtEpochMs, it.text) },
                            nowMs = nowMs,
                            zoneOffsetMs = TimeZone.getDefault().getOffset(nowMs).toLong()
                        )
                        val byId = shown.associateBy { it.id }
                        days.forEach { day ->
                            Text(
                                day.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            day.rows.forEach { row ->
                                val d = byId[row.id] ?: return@forEach
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
                                                d.wordCount
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
        }

        Text(
            PrivacyHonesty.HOME_FOOTER,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            softWrap = true
        )
        Spacer(Modifier.height(Dimen.Space8))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryScreen(app: OpenFlowApp) {
    val dictations by app.dictations.observeDictations().collectAsState(initial = emptyList())
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
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
                OutlinedButton(
                    onClick = {
                        val rows = dictations.map { d ->
                            HistoryExport.Row(d.createdAtEpochMs, d.text, d.languageTag, d.wordCount)
                        }
                        val shareText = HistoryExport.toMarkdown(rows)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            ctx.startActivity(Intent.createChooser(send, "Export history (Markdown)"))
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
                    Text("Export", fontWeight = FontWeight.SemiBold)
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
                                    d.wordCount
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
    var draft by remember(d.id, d.text) { mutableStateOf(d.text) }
    val hasRaw = d.rawText.isNotBlank() && d.rawText != d.text
    val timeStr = remember(d.createdAtEpochMs) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(d.createdAtEpochMs))
    }

    OpenCard {
        Column(
            Modifier.padding(Dimen.MIN_PADDING),
            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$timeStr · ${d.wordCount}w",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecUi.muted,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(Dimen.MIN_TOUCH)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = SecUi.charcoal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
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
            }

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
                    }
                )
            } else {
                Text(
                    d.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecUi.charcoal,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            draft = d.text
                            editing = true
                        }
                )
            }

            // Compact labels so Copy | Raw | Copy raw fit one row on 411dp.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                CopyButton(text = d.text, label = "Copy")
                if (hasRaw) {
                    OpenChip(
                        label = if (showRaw) "Hide" else "Raw",
                        isOn = showRaw,
                        onClick = { showRaw = !showRaw }
                    )
                    CopyButton(text = d.rawText, label = "Raw copy")
                    OpenChip(
                        label = "Use raw",
                        isOn = false,
                        onClick = { onUseRaw(d.rawText) },
                        modifier = Modifier.testTag("history_use_raw"),
                    )
                }
            }
            if (hasRaw && showRaw) {
                Surface(
                    color = SecUi.stone,
                    shape = MaterialTheme.shapes.small,
                    border = SecUi.thinBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        d.rawText,
                        modifier = Modifier.padding(Dimen.GAP_SM),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyButton(text: String, label: String = "Copy") {
    val ctx = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    OutlinedButton(
        onClick = {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(android.content.ClipData.newPlainText("dictation", text))
            copied = true
        },
        modifier = Modifier
            .defaultMinSize(minWidth = Dimen.MIN_TOUCH, minHeight = Dimen.MIN_TOUCH)
            .heightIn(min = Dimen.MIN_TOUCH),
        shape = MaterialTheme.shapes.small,
        border = SecUi.hardBorder,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SecUi.charcoal,
            containerColor = SecUi.cream
        )
    ) {
        Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (copied) SecUi.ink else SecUi.charcoal
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (copied) "Copied!" else label,
            fontWeight = FontWeight.SemiBold
        )
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
                items(
                    items = shown,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleTab(prefs: FlowPrefs) {
    var selected by remember { mutableStateOf(prefs.style()) }
    var customEnd by remember { mutableStateOf(prefs.customEndPunct) }
    var customCaps by remember { mutableStateOf(prefs.customCaps) }
    var customExpand by remember { mutableStateOf(prefs.customExpandInformal) }
    var customRepl by remember { mutableStateOf(prefs.customStyleReplacements) }

    fun label(st: WritingStyle): String = when (st) {
        WritingStyle.FORMAL -> "Formal"
        WritingStyle.CASUAL -> "Casual"
        WritingStyle.VERY_CASUAL -> "Very casual"
        WritingStyle.EXCITED -> "Excited"
        WritingStyle.CUSTOM -> "Custom"
    }

    fun desc(st: WritingStyle): String = when (st) {
        WritingStyle.FORMAL ->
            "Sentence case, always ends with ., expands informal (gonna → going to)."
        WritingStyle.CASUAL ->
            "Everyday tone, sentence case, period on longer lines."
        WritingStyle.VERY_CASUAL ->
            "Chat-like: soft caps, no forced period."
        WritingStyle.EXCITED ->
            "High energy, prefers ! endings."
        WritingStyle.CUSTOM ->
            "Your end punctuation, caps, informal expand, and replace rules."
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
            "Pipeline: Dict → snippets → cleanup → style. Local rules only — no AI tone model.",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )

        WritingStyle.entries.forEach { st ->
            val on = selected == st
            OpenCard(
                selected = on,
                onClick = {
                    selected = st
                    prefs.styleName = st.name
                }
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
                            label(st),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            desc(st),
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

        if (selected == WritingStyle.CUSTOM) {
            OpenCard {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
                ) {
                    Text(
                        "Custom rules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.charcoal
                    )
                    Text(
                        "End punctuation",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                    ) {
                        listOf(
                            "auto" to "Auto",
                            "period" to "Period",
                            "bang" to "!",
                            "none" to "None"
                        ).forEach { (v, lab) ->
                            OpenChip(
                                label = lab,
                                isOn = customEnd == v,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    customEnd = v
                                    prefs.customEndPunct = v
                                }
                            )
                        }
                    }
                    Text(
                        "Capitalization",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecUi.charcoal,
                        softWrap = true
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                    ) {
                        listOf(
                            "sentence" to "Sentence",
                            "first" to "First",
                            "none" to "None"
                        ).forEach { (v, lab) ->
                            OpenChip(
                                label = lab,
                                isOn = customCaps == v,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    customCaps = v
                                    prefs.customCaps = v
                                }
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Expand informal",
                                style = MaterialTheme.typography.titleSmall,
                                color = SecUi.charcoal,
                                softWrap = true
                            )
                            Text(
                                "gonna → going to, don't → do not, …",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecUi.muted,
                                softWrap = true
                            )
                        }
                        OpenChip(
                            label = if (customExpand) "ON" else "OFF",
                            isOn = customExpand,
                            onClick = {
                                customExpand = !customExpand
                                prefs.customExpandInformal = customExpand
                            }
                        )
                    }
                    Text(
                        "Replacements (one per line: from=>to)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecUi.charcoal
                    )
                    OpenTextField(
                        value = customRepl,
                        onValueChange = {
                            customRepl = it
                            prefs.customStyleReplacements = it
                        },
                        placeholder = "cheers=>Thanks\nyeah=>yes",
                        singleLine = false,
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    )
                }
            }
        }

        AppContextCustomizer(prefs)

        Spacer(Modifier.height(Dimen.GAP_LG))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppContextCustomizer(prefs: FlowPrefs) {
    var appContextEnabled by remember { mutableStateOf(prefs.appContextEnabled) }
    var overrides by remember { mutableStateOf(prefs.getAppOverrides()) }

    var newPkg by remember { mutableStateOf("") }
    var newCat by remember { mutableStateOf(AppCategory.WORK_COLLAB) }
    var newStyle by remember { mutableStateOf(WritingStyle.CASUAL) }
    var newPrompt by remember { mutableStateOf("") }
    var showAddOverride by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Auto App Adaptation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "Wispr Flow style: dynamically adapts tone and prompt rules per app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true
                        )
                    }
                    OpenChip(
                        label = if (appContextEnabled) "ACTIVE" else "OFF",
                        isOn = appContextEnabled,
                        onClick = {
                            appContextEnabled = !appContextEnabled
                            prefs.appContextEnabled = appContextEnabled
                        }
                    )
                }
            }
        }

        if (appContextEnabled) {
            Text(
                "App Categories & Rules",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SecUi.charcoal,
                modifier = Modifier.padding(top = Dimen.GAP_SM)
            )

            val categories = listOf(
                AppCategory.MESSAGING to ("💬 Chat & Messaging" to "WhatsApp, Telegram, Signal, Discord, SMS"),
                AppCategory.EMAIL to ("✉️ Email" to "Gmail, Outlook, ProtonMail, Thunderbird"),
                AppCategory.WORK_COLLAB to ("💼 Work & Collab" to "Slack, Microsoft Teams, Jira, Linear"),
                AppCategory.DOCS_NOTES to ("📝 Notes & Docs" to "Notion, Obsidian, Google Docs, Keep"),
                AppCategory.DEV_TERMINAL to ("💻 Dev & Terminal" to "Termux, GitHub, GitJournal, Code"),
                AppCategory.AI_SEARCH to ("🔍 AI & Search" to "ChatGPT, Perplexity, Claude, Browsers"),
                AppCategory.GENERAL to ("🌐 General Apps" to "All other unclassified apps")
            )

            categories.forEach { (cat, info) ->
                val (title, examples) = info
                var catStyle by remember { mutableStateOf(prefs.getCategoryStyle(cat)) }
                var catPrompt by remember { mutableStateOf(prefs.getCategoryPrompt(cat)) }

                OpenCard {
                    Column(
                        Modifier.padding(Dimen.MIN_PADDING),
                        verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecUi.charcoal
                        )
                        Text(
                            "Recognized: $examples",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecUi.muted
                        )

                        Text(
                            "Writing Style",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecUi.charcoal
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                        ) {
                            listOf(
                                WritingStyle.CASUAL to "Casual",
                                WritingStyle.FORMAL to "Formal",
                                WritingStyle.VERY_CASUAL to "Very casual",
                                WritingStyle.EXCITED to "Excited"
                            ).forEach { (st, lbl) ->
                                OpenChip(
                                    label = lbl,
                                    isOn = catStyle == st,
                                    onClick = {
                                        catStyle = st
                                        prefs.setCategoryStyle(cat, st)
                                    }
                                )
                            }
                        }

                        Text(
                            "Custom Instructions (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecUi.charcoal
                        )
                        OpenTextField(
                            value = catPrompt,
                            onValueChange = {
                                catPrompt = it
                                prefs.setCategoryPrompt(cat, it)
                            },
                            placeholder = "e.g. Keep concise, bullet points, no emojis",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Per-App Overrides Section
            Text(
                "Per-App Package Overrides",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SecUi.charcoal,
                modifier = Modifier.padding(top = Dimen.GAP_SM)
            )

            OpenCard {
                Column(
                    Modifier.padding(Dimen.MIN_PADDING),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                ) {
                    Text(
                        "Custom rules for specific Android packages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted
                    )

                    if (overrides.isEmpty()) {
                        Text(
                            "No custom app overrides added yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted.copy(alpha = 0.8f)
                        )
                    } else {
                        overrides.forEach { ov ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(SecUi.stone, MaterialTheme.shapes.small)
                                    .padding(Dimen.GAP_SM),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ov.packageName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecUi.charcoal
                                    )
                                    Text(
                                        "${ov.category.label} • ${ov.style.name}" +
                                            if (ov.customPrompt.isNotBlank()) " • \"${ov.customPrompt}\"" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecUi.muted
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        prefs.deleteAppOverride(ov.packageName)
                                        overrides = prefs.getAppOverrides()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete override",
                                        tint = SecUi.error
                                    )
                                }
                            }
                        }
                    }

                    if (!showAddOverride) {
                        OpenButton(
                            text = "+ Add App Override",
                            onClick = { showAddOverride = true },
                            variant = ButtonVariant.Outlined
                        )
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .border(SecUi.thinBorder, MaterialTheme.shapes.small)
                                .padding(Dimen.GAP_SM),
                            verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                        ) {
                            Text(
                                "New App Override",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecUi.charcoal
                            )
                            OpenTextField(
                                value = newPkg,
                                onValueChange = { newPkg = it },
                                placeholder = "Package name (e.g. com.slack)",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Category", style = MaterialTheme.typography.labelSmall, color = SecUi.charcoal)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                            ) {
                                AppCategory.values().forEach { cat ->
                                    OpenChip(
                                        label = cat.label,
                                        isOn = newCat == cat,
                                        onClick = { newCat = cat }
                                    )
                                }
                            }

                            Text("Style", style = MaterialTheme.typography.labelSmall, color = SecUi.charcoal)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                            ) {
                                listOf(
                                    WritingStyle.CASUAL to "Casual",
                                    WritingStyle.FORMAL to "Formal",
                                    WritingStyle.VERY_CASUAL to "Very casual",
                                    WritingStyle.EXCITED to "Excited"
                                ).forEach { (st, lbl) ->
                                    OpenChip(
                                        label = lbl,
                                        isOn = newStyle == st,
                                        onClick = { newStyle = st }
                                    )
                                }
                            }

                            OpenTextField(
                                value = newPrompt,
                                onValueChange = { newPrompt = it },
                                placeholder = "Custom instructions (optional)",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
                            ) {
                                OpenButton(
                                    text = "Cancel",
                                    onClick = { showAddOverride = false },
                                    variant = ButtonVariant.Outlined,
                                    modifier = Modifier.weight(1f)
                                )
                                OpenButton(
                                    text = "Save Override",
                                    onClick = {
                                        if (newPkg.isNotBlank()) {
                                            prefs.saveAppOverride(
                                                AppOverride(
                                                    packageName = newPkg.trim(),
                                                    category = newCat,
                                                    style = newStyle,
                                                    customPrompt = newPrompt.trim()
                                                )
                                            )
                                            overrides = prefs.getAppOverrides()
                                            newPkg = ""
                                            newPrompt = ""
                                            showAddOverride = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHub(
    onSpeechAi: () -> Unit,
    onAppearance: () -> Unit,
    onBubble: () -> Unit,
    onCleanup: () -> Unit,
    onPrivacy: () -> Unit,
    onSounds: () -> Unit,
    onHomeLayout: () -> Unit
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
        SettingsRow("Flow Bubble & Gestures", "Shape, size, opacity, edge magnetic snap", onBubble)
        SettingsRow("Cleanup Pipeline", "Filler words, course corrections, lists", onCleanup)
        SettingsRow("Appearance", "Dark / light theme, visual skins", onAppearance)
        SettingsRow("Privacy & Retention", "Zero-cloud audit, auto-wipe policies", onPrivacy)
        SettingsRow("Haptics & Feedback", "Tactile clicks and audio feedback", onSounds)
        SettingsRow("Home layout", "Reorder and toggle Home cards", onHomeLayout)

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
            "none" to ("None" to "Exact speech — zero edits (Wispr None)."),
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
private fun PrivacySettings(prefs: FlowPrefs) {
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
private fun SoundsSettings(prefs: FlowPrefs) {
    var sounds by remember { mutableStateOf(prefs.bubbleSounds) }
    var feel by remember { mutableStateOf(prefs.hapticFeel) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Haptic and audio cues during dictation.",
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
                        "Play subtle tone when starting dictation",
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

        OpenCard {
            Column(
                Modifier
                    .padding(Dimen.MIN_PADDING)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tactile Haptics", style = MaterialTheme.typography.titleSmall, softWrap = true)
                Text(
                    "Off / Light (tick) / Full (confirm, reject, click).",
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
                        HapticFeel.OFF to "Off",
                        HapticFeel.LIGHT to "Light",
                        HapticFeel.FULL to "Full"
                    ).forEach { (id, label) ->
                        OpenChip(
                            label = label,
                            isOn = feel == id,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                feel = id
                                prefs.hapticFeel = id
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSettings(prefs: FlowPrefs) {
    val dark by prefs.darkMode.collectAsState()
    val skin by prefs.visualSkin.collectAsState()
    val context = LocalContext.current
    var refreshHz by remember { mutableIntStateOf(prefs.refreshHz) }
        var sttProfile by remember { mutableStateOf(prefs.sttProfile) }
        var languageTag by remember { mutableStateOf(prefs.languageTag) }
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
                Text("Visual skin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                Text(
                    "Brutal = hard borders, high contrast. Soft = rounded Material look.",
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
                    listOf(BRUTAL to "Brutal", M3 to "Soft").forEach { (v, label) ->
                        OpenChip(
                            label = label,
                            isOn = skin == v,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = { prefs.setVisualSkin(v) }
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
    var scale by remember { mutableFloatStateOf(prefs.bubbleScale) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var shape by remember { mutableStateOf(prefs.bubbleShape) }
    var showText by remember { mutableStateOf(prefs.bubbleShowText) }
    var snap by remember { mutableStateOf(prefs.bubbleEdgeSnap) }
    var feel by remember { mutableStateOf(prefs.hapticFeel) }
    var pulse by remember { mutableStateOf(prefs.bubblePulse) }
    var tint by remember { mutableStateOf(prefs.bubbleTint) }
    var roundness by remember { mutableStateOf(prefs.bubbleRoundness) }

    Column(
        Modifier
            .fillMaxSize()
            .background(SecUi.cream)
            .padding(horizontal = Dimen.PAGE_PAD, vertical = Dimen.GAP)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
    ) {
        Text(
            "Morph the shape, size, and interaction physics of your floating bubble.",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )
        Text(
            "Live preview",
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
                shape == "pill" -> when (roundness) {
                    BubbleChrome.ROUND_ROUND -> RoundedCornerShape(50)
                    BubbleChrome.ROUND_SOFT -> RoundedCornerShape(16.dp)
                    else -> RoundedCornerShape(12.dp)
                }
                else -> when (roundness) {
                    BubbleChrome.ROUND_ROUND -> RoundedCornerShape(16.dp)
                    BubbleChrome.ROUND_SOFT -> RoundedCornerShape(8.dp)
                    else -> RoundedCornerShape(2.dp)
                }
            }
            val baseW = when (shape) {
                "dot" -> 22.dp
                "circle" -> 44.dp
                "square" -> 44.dp
                else -> 76.dp
            }
            val baseH = if (shape == "pill") 34.dp else baseW
            Box(
                modifier = Modifier
                    .size(baseW * scale, baseH * scale)
                    .graphicsLayer { alpha = opacity }
                    .background(Color(BubbleTint.argb(tint)), previewShape),
                contentAlignment = Alignment.Center
            ) {
                if (showText && shape != "dot") {
                    Text(
                        "Hi",
                        color = Color(BubbleTint.onArgb(tint)),
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
                    "Overlay Shape",
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
                    "Hard = brutal. Soft / Round = friendlier pill edges.",
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
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
            ) {
                Text(
                    "Dimensions & Transparency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scale", style = MaterialTheme.typography.bodyMedium, color = SecUi.charcoal)
                    Text(
                        "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.ink
                    )
                }
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    onValueChangeFinished = {
                        prefs.bubbleScale = scale
                        onApplyBubble()
                    },
                    valueRange = 0.7f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = SecUi.charcoal,
                        activeTrackColor = SecUi.charcoal,
                        inactiveTrackColor = SecUi.stone
                    )
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
                        onApplyBubble()
                    },
                    valueRange = 0.3f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = SecUi.charcoal,
                        activeTrackColor = SecUi.charcoal,
                        inactiveTrackColor = SecUi.stone
                    )
                )
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP)
            ) {
                Text(
                    "Interactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Live Speech Caption",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "Display transcribed words directly on the bubble",
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

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Magnetic Edge Snapping",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "Snap bubble seamlessly to nearest screen edge on release",
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
                            "Active Recording Pulse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true
                        )
                        Text(
                            "Pulse glowing outer ring and scale dynamically to voice RMS volume",
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tactile Haptics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal
                    )
                    Text(
                        "Off / Light / Full on tap, save, cancel.",
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
                            HapticFeel.OFF to "Off",
                            HapticFeel.LIGHT to "Light",
                            HapticFeel.FULL to "Full"
                        ).forEach { (id, label) ->
                            OpenChip(
                                label = label,
                                isOn = feel == id,
                                modifier = Modifier.wrapContentHeight(),
                                onClick = {
                                    feel = id
                                    prefs.hapticFeel = id
                                    onApplyBubble()
                                }
                            )
                        }
                    }
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
