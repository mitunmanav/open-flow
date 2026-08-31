package app.openflow.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.openflow.R
import app.openflow.bubble.BubbleChrome
import app.openflow.bubble.BubbleIconPolicy
import app.openflow.bubble.BubbleScaleSteps
import app.openflow.bubble.BubbleShapeCatalog
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.theme.BubbleTint
import app.openflow.ui.theme.HexColor
import app.openflow.ui.theme.SecUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BubbleSettings(prefs: FlowPrefs, onApplyBubble: () -> Unit) {
    val pal by prefs.appearance.collectAsState()
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(BubbleScaleSteps.nearest(prefs.bubbleScale)) }
    var opacity by remember { mutableFloatStateOf(prefs.bubbleOpacity) }
    var shape by remember { mutableStateOf(prefs.bubbleShape) }
    var showText by remember { mutableStateOf(prefs.bubbleShowText) }
    var showLangChip by remember { mutableStateOf(prefs.bubbleShowLangChip) }
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
    var showAdvanced by remember { mutableStateOf(false) }
    val pickIcon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val stored = persistBubbleIcon(ctx, uri)
        prefs.bubbleIconUri = stored
        iconUri = stored
        onApplyBubble()
    }

    SettingsPage(intro = "Size, look, and feel of the floating bubble. Changes apply live.") {
        SettingsSectionTitle("Size", "bubble_size")
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
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
                            },
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
                    variant = ButtonVariant.Outlined,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Opacity", style = MaterialTheme.typography.bodyMedium, color = SecUi.charcoal)
                    Text(
                        "${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.ink,
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
                        inactiveTrackColor = SecUi.stone,
                    ),
                )
            }
        }

        SettingsSectionTitle("Look", "bubble_look")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(vertical = Dimen.GAP_SM)
                .border(SecUi.hardBorder)
                .background(Color(BubbleTint.previewStageArgb(tint)))
                .testTag("bubble_preview"),
            contentAlignment = Alignment.Center,
        ) {
            val shapeSpec = BubbleShapeCatalog.fromId(shape)
            val previewShape = when {
                shapeSpec.oval -> CircleShape
                else -> RoundedCornerShape(percent = roundPct.coerceIn(0, 100))
            }
            val baseW = (shapeSpec.idleWidthDp * 0.8f).dp
            val baseH = (shapeSpec.idleHeightDp * 0.8f).dp
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
                horizontalArrangement = Arrangement.Center,
            ) {
                if (customBmp != null) {
                    Image(
                        bitmap = customBmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp * scale),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color(pal.bubbleTextArgb),
                        modifier = Modifier.size(18.dp * scale),
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
                        softWrap = false,
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Shape",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    BubbleShapeCatalog.ALL.forEach { spec ->
                        OpenChip(
                            label = spec.label,
                            isOn = shape == spec.id,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                shape = spec.id
                                prefs.bubbleShape = spec.id
                                onApplyBubble()
                            },
                        )
                    }
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    BubbleTint.ALL.forEach { tintSpec ->
                        OpenChip(
                            label = tintSpec.label,
                            isOn = tint == tintSpec.id,
                            showCheckWhenOn = true,
                            modifier = Modifier.wrapContentHeight(),
                            onClick = {
                                tint = tintSpec.id
                                prefs.bubbleTint = tintSpec.id
                                prefs.colorBubbleIdle = HexColor.format(tintSpec.fillArgb)
                                prefs.colorBubbleListen = HexColor.format(tintSpec.fillArgb)
                                prefs.colorBubbleText = HexColor.format(tintSpec.onArgb)
                                onApplyBubble()
                            },
                        )
                    }
                }
            }
        }

        OpenButton(
            text = if (showAdvanced) "Hide advanced" else "Advanced",
            onClick = { showAdvanced = !showAdvanced },
            variant = ButtonVariant.Outlined,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bubble_advanced"),
        )

        if (showAdvanced) {
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Corners",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                Text(
                    "Hard = sharp. Soft / Round = softer pill edges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
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
                            },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Roundness", style = MaterialTheme.typography.bodyMedium, color = SecUi.charcoal)
                    Text(
                        "$roundPct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecUi.ink,
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
                        inactiveTrackColor = SecUi.stone,
                    ),
                )
            }
        }

        OpenCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimen.MIN_PADDING),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Show text on bubble",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecUi.charcoal,
                        softWrap = true,
                    )
                    Text(
                        "Show live words on the bubble while listening",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecUi.muted,
                        softWrap = true,
                    )
                }
                OpenChip(
                    label = if (showText) "ON" else "OFF",
                    isOn = showText,
                    onClick = {
                        showText = !showText
                        prefs.bubbleShowText = showText
                        onApplyBubble()
                    },
                )
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Language badge",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    OpenChip(
                        label = if (showLangChip) "ON" else "OFF",
                        isOn = showLangChip,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            showLangChip = !showLangChip
                            prefs.bubbleShowLangChip = showLangChip
                            onApplyBubble()
                        },
                    )
                }
                Text(
                    "Show the dictation language (e.g. EN) on the idle bubble. Off = clean orb.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted,
                    softWrap = true,
                )
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Listen buttons",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                ) {
                    OpenChip(
                        label = "Show Cancel",
                        isOn = showCancel,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            showCancel = !showCancel
                            prefs.bubbleShowCancel = showCancel
                            onApplyBubble()
                        },
                    )
                    OpenChip(
                        label = "Show Done",
                        isOn = showDone,
                        modifier = Modifier.wrapContentHeight(),
                        onClick = {
                            showDone = !showDone
                            prefs.bubbleShowDone = showDone
                            onApplyBubble()
                        },
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Shrink when idle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                OpenChip(
                    label = if (shrinkIdle) "ON" else "OFF",
                    isOn = shrinkIdle,
                    onClick = {
                        shrinkIdle = !shrinkIdle
                        prefs.bubbleShrinkIdle = shrinkIdle
                        onApplyBubble()
                    },
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
                    verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
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
                        },
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
                        },
                    )
                }
            }
        }

        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM),
            ) {
                Text(
                    "Custom icon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecUi.charcoal,
                )
                Text(
                    if (BubbleIconPolicy.validUri(iconUri)) "Custom image set" else "Default mic",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecUi.muted,
                )
                OpenButton(
                    text = "Pick image",
                    onClick = { pickIcon.launch(arrayOf("image/*")) },
                    variant = ButtonVariant.Outlined,
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
                        BubbleIconPolicy.localFile(ctx.filesDir).isFile,
                )
            }
        }

        SettingsSectionTitle("Feel", "bubble_feel")
        OpenCard {
            Column(
                Modifier.padding(Dimen.MIN_PADDING),
                verticalArrangement = Arrangement.spacedBy(Dimen.GAP),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Snap to edge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true,
                        )
                        Text(
                            "On release, snap to the nearest left or right edge",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true,
                        )
                    }
                    OpenChip(
                        label = if (snap) "ON" else "OFF",
                        isOn = snap,
                        onClick = {
                            snap = !snap
                            prefs.bubbleEdgeSnap = snap
                            onApplyBubble()
                        },
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Recording pulse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecUi.charcoal,
                            softWrap = true,
                        )
                        Text(
                            "Pulse with voice volume while recording",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecUi.muted,
                            softWrap = true,
                        )
                    }
                    OpenChip(
                        label = if (pulse) "ON" else "OFF",
                        isOn = pulse,
                        onClick = {
                            pulse = !pulse
                            prefs.bubblePulse = pulse
                            onApplyBubble()
                        },
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
            variant = ButtonVariant.Outlined,
        )
        // L6: snooze discoverability — drag to top edge is hidden; surface it here and via TalkBack action.
        Text(
            "Tip: Drag bubble to top edge to snooze 10 min. Shake phone to wake. Or TalkBack → Actions → Snooze.",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
            modifier = Modifier
                .padding(top = Dimen.GAP_SM)
                .testTag("bubble_snooze_hint"),
        )
        } // showAdvanced
        // Always visible snooze hint outside advanced, for discoverability.
        Text(
            "Snooze: drag bubble to top edge for 10 min. Shake to wake. Accessibility → Snooze action also available.",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true,
            modifier = Modifier.testTag("bubble_snooze_hint_always"),
        )
    }
}

fun persistBubbleIcon(ctx: Context, uri: Uri): String {
    try {
        ctx.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
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

fun clearBubbleIcon(ctx: Context) {
    BubbleIconPolicy.localFile(ctx.filesDir).delete()
}
