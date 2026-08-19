package app.openflow.ui.dictionary

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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent

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
import app.openflow.ui.theme.SecUi

@Composable
fun PairImportBlock(
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
    Column(verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)) {
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
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
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
