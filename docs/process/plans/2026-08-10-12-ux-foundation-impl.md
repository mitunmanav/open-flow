# F12 — UI/UX Foundation Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Build reusable design system foundation — theme tokens, component library, motion primitives, WCAG AAA a11y primitives — for all 5 screens.

**Architecture:** Follow existing code patterns. No ViewModels, no Hilt. Add Turbine for Flow tests. Compose UI tests per component.

**Tech Stack:** Kotlin 17, Jetpack Compose BOM `2024.10.01`, Material 3, `androidx.compose.ui:ui-test-junit4`, Turbine, `HapticFeedbackConstants`, minSdk 26, targetSdk 35

---

## Global Constraints

- No INTERNET permission added
- Author: **Mitun only** — no Co-Authored-By
- All components: touch target ≥ 48dp, TalkBack contentDescription, text scale up to 200%
- Dark mode values: `"system"`, `"light"`, `"dark"` (StateFlow in FlowPrefs)
- Motion: respects `prefers-reduced-motion` via `configuration.uiMode` check
- All user-facing strings extracted to `strings.xml` with named IDs

---

## File Map

```
app/src/main/java/app/openflow/prefs/FlowPrefs.kt       # modify — add darkMode StateFlow
app/src/main/java/app/openflow/ui/theme/OpenFlowColors.kt # create
app/src/main/java/app/openflow/ui/theme/Motion.kt      # create
app/src/main/java/app/openflow/ui/theme/Theme.kt       # modify — dark mode param
app/src/main/java/app/openflow/ui/theme/Type.kt       # create
app/src/main/java/app/openflow/ui/components/OpenCard.kt     # create + test
app/src/main/java/app/openflow/ui/components/OpenChip.kt     # create + test
app/src/main/java/app/openflow/ui/components/OpenTextField.kt # create + test
app/src/main/java/app/openflow/ui/components/OpenButton.kt   # create
app/src/main/java/app/openflow/ui/components/OpenListItem.kt  # create
app/src/main/java/app/openflow/ui/components/EmptyState.kt   # create
app/src/main/java/app/openflow/ui/components/LoadingState.kt # create
app/src/main/java/app/openflow/ui/components/ErrorState.kt   # create
app/src/main/java/app/openflow/ui/a11y/OpenIcons.kt        # create
app/src/main/java/app/openflow/ui/a11y/Dimen.kt           # create
app/src/main/res/values/strings.xml                      # modify — add all user-facing strings
app/src/main/java/app/openflow/ui/MainActivity.kt         # modify — use components, dark toggle
app/build.gradle.kts                                      # modify — add Turbine
```

---

### Task 1: Dark mode StateFlow in FlowPrefs

**Files:**
- Modify: `app/src/main/java/app/openflow/prefs/FlowPrefs.kt`
- Test: `app/src/test/java/app/openflow/prefs/FlowPrefsDarkModeTest.kt` (create)

**Interfaces:**
- Produces: `darkMode: StateFlow<String>` — values `"system"`, `"light"`, `"dark"`, default `"system"`
- Write via: `setDarkMode(value: String)`

- [ ] **Step 1: Write failing test**

```kotlin
// app/src/test/java/app/openflow/prefs/FlowPrefsDarkModeTest.kt
package app.openflow.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FlowPrefsDarkModeTest {
    private lateinit var prefs: FlowPrefs

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("openflow_prefs_test", Context.MODE_PRIVATE)
            .edit().clear().apply()
        prefs = FlowPrefs(ctx)
    }

    @Test
    fun `darkMode defaults to system`() = runTest {
        prefs.darkMode.test {
            assertThat(awaitItem()).isEqualTo("system")
        }
    }

    @Test
    fun `darkMode emits new value on set`() = runTest {
        prefs.darkMode.test {
            assertThat(awaitItem()).isEqualTo("system")
            prefs.setDarkMode("dark")
            assertThat(awaitItem()).isEqualTo("dark")
        }
    }

    @Test
    fun `darkMode persists set value`() {
        prefs.setDarkMode("light")
        assertThat(prefs.darkMode.value).isEqualTo("light")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "app.openflow.prefs.FlowPrefsDarkModeTest" -q`
Expected: FAIL — `darkMode` not found

- [ ] **Step 3: Add darkMode StateFlow to FlowPrefs**

Add imports:
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

Add to class body:
```kotlin
private val _darkMode = MutableStateFlow("system")
val darkMode: StateFlow<String> = _darkMode.asStateFlow()

init {
    _darkMode.value = sp.getString("dark_mode", "system")!!
}

fun setDarkMode(value: String) {
    _darkMode.value = value
    sp.edit().putString("dark_mode", value).apply()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "app.openflow.prefs.FlowPrefsDarkModeTest" -q`
Expected: PASS

- [ ] **Step 5: Add Turbine dependency to build.gradle.kts**

```kotlin
// In testImplementation block, add:
testImplementation("app.cash.turbine:turbine:1.1.0")
```

Run: `./gradlew :app:dependencies --configuration testDebugCompileClasspath -q | grep turbine` to verify

---

### Task 2: OpenFlowColors — calm pro palette

**Files:**
- Create: `app/src/main/java/app/openflow/ui/theme/OpenFlowColors.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.openflow.ui.theme

import androidx.compose.ui.graphics.Color

object OpenFlowColors {
    val Primary = Color(0xFF1565C0)
    val PrimaryLight = Color(0xFF1976D2)
    val Secondary = Color(0xFF546E7A)
    val SecondaryLight = Color(0xFF78909C)

    val SurfaceLight = Color(0xFFFAFAFA)
    val SurfaceDark = Color(0xFF121212)
    val BackgroundLight = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF1E1E1E)

    val OnPrimaryLight = Color(0xFFFFFFFF)
    val OnPrimaryDark = Color(0xFFFFFFFF)
    val OnSurfaceLight = Color(0xFF1C1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)
    val OnBackgroundLight = Color(0xFF1C1B1F)
    val OnBackgroundDark = Color(0xFFE6E1E5)

    val SurfaceVariantLight = Color(0xFFE7E0EC)
    val SurfaceVariantDark = Color(0xFF49454F)
    val OnSurfaceVariantLight = Color(0xFF49454F)
    val OnSurfaceVariantDark = Color(0xFFCAC4D0)

    val Error = Color(0xFFB00020)
    val ErrorDark = Color(0xFFCF6679)
    val OnError = Color(0xFFFFFFFF)

    val ChipBubbleOn = Color(0xFF1B5E20)
    val ChipBubbleOnText = Color(0xFFFFFFFF)
    val ChipBubbleOff = Color(0xFFB71C1C)
    val ChipBubbleOffText = Color(0xFFFFFFFF)
}
```

Verify: `./gradlew :app:compileDebugKotlin -q`

---

### Task 3: A11y dimensions

**Files:**
- Create: `app/src/main/java/app/openflow/ui/a11y/Dimen.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.openflow.ui.a11y

import androidx.compose.ui.unit.dp

object Dimen {
    val TOUCH_TARGET = 48.dp
    val MIN_PADDING = 16.dp
    val CARD_ELEVATION = 2.dp
    val CARD_ROUNDING = 12.dp
    val BUTTON_ROUNDING = 8.dp
}
```

Verify: `./gradlew :app:compileDebugKotlin -q`

---

### Task 4: Motion primitives

**Files:**
- Create: `app/src/main/java/app/openflow/ui/theme/Motion.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.openflow.ui.theme

import android.content.Context
import android.content.res.Configuration

object Motion {
    const val TAB_SWITCH_MS = 150
    const val CHIP_COLOR_MS = 200

    fun shouldAnimate(context: Context): Boolean {
        return (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    }
}
```

Verify: `./gradlew :app:compileDebugKotlin -q`

---

### Task 5: OpenCard component + test

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/OpenCard.kt`
- Test: `app/src/androidTest/java/app/openflow/ui/components/OpenCardTest.kt` (create)

- [ ] **Step 1: Write the test**

```kotlin
// app/src/androidTest/java/app/openflow/ui/components/OpenCardTest.kt
package app.openflow.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class OpenCardTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `renders content`() {
        rule.setContent { OpenCard { Text("hello") } }
        rule.onNodeWithText("hello").assertExists()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "app.openflow.ui.components.OpenCardTest" -q`
Expected: FAIL — OpenCard not found

- [ ] **Step 3: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.openflow.ui.a11y.Dimen

@Composable
fun OpenCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    disabled: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (contentDescription != null)
                    Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
        shape = RoundedCornerShape(Dimen.CARD_ROUNDING),
        colors = CardDefaults.cardColors(
            containerColor = when {
                disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimen.CARD_ELEVATION),
        enabled = !disabled,
        content = content
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "app.openflow.ui.components.OpenCardTest" -q`
Expected: PASS

---

### Task 6: OpenChip component + test

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/OpenChip.kt`
- Test: `app/src/androidTest/java/app/openflow/ui/components/OpenChipTest.kt` (create)

- [ ] **Step 1: Write the test**

```kotlin
// app/src/androidTest/java/app/openflow/ui/components/OpenChipTest.kt
package app.openflow.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class OpenChipTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `renders label`() {
        rule.setContent { OpenChip(label = "Bubble ON", isOn = true, onClick = {}) }
        rule.onNodeWithText("Bubble ON").assertExists()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "app.openflow.ui.components.OpenChipTest" -q`
Expected: FAIL — OpenChip not found

- [ ] **Step 3: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.theme.OpenFlowColors

@Composable
fun OpenChip(
    label: String,
    isOn: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isOn) OpenFlowColors.ChipBubbleOn else OpenFlowColors.ChipBubbleOff,
        label = "chip_bg"
    )
    val textColor = if (isOn) OpenFlowColors.ChipBubbleOnText else OpenFlowColors.ChipBubbleOffText

    Surface(
        modifier = modifier
            .semantics { this.contentDescription = label; this.role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "app.openflow.ui.components.OpenChipTest" -q`
Expected: PASS

---

### Task 7: OpenButton component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/OpenButton.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.a11y.Dimen

enum class ButtonVariant { Filled, Outlined, Text }

@Composable
fun OpenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val view = LocalView.current
    val semanticsMod = if (contentDescription != null)
        Modifier.semantics { this.contentDescription = contentDescription }
    else Modifier

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
            modifier = modifier.fillMaxWidth().height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { Text(text) }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
            modifier = modifier.fillMaxWidth().height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled
        ) { Text(text) }

        ButtonVariant.Text -> TextButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            },
            modifier = modifier.height(Dimen.TOUCH_TARGET).then(semanticsMod),
            enabled = enabled
        ) { Text(text) }
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 8: OpenTextField component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/OpenTextField.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.openflow.ui.theme.OpenFlowColors

@Composable
fun OpenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    error: String? = null,
    minLines: Int = 1,
    contentDescription: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (contentDescription != null)
                    Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
        label = label?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OpenFlowColors.Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 9: OpenListItem component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/OpenListItem.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun OpenListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val semMod = if (contentDescription != null)
        Modifier.semantics { this.contentDescription = contentDescription }
    else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickMod)
            .then(semMod)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            actions?.invoke()
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 10: EmptyState component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/EmptyState.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 11: LoadingState component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/LoadingState.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun LoadingState(
    label: String = "Loading…",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp)
            .semantics { this.contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 12: ErrorState component

**Files:**
- Create: `app/src/main/java/app/openflow/ui/components/ErrorState.kt`

- [ ] **Step 1: Write the component**

```kotlin
package app.openflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp)
            .semantics { this.contentDescription = "Error: $message" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            OpenButton(
                text = "Retry",
                onClick = onRetry,
                variant = ButtonVariant.Outlined,
                contentDescription = "Retry after error"
            )
        }
    }
}
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 13: OpenIcons — semantic icons

**Files:**
- Create: `app/src/main/java/app/openflow/ui/a11y/OpenIcons.kt`

- [ ] **Step 1: Write the file**

```kotlin
package app.openflow.ui.a11y

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style

object OpenIcons {
    val Home = Icons.Default.Home
    val Book = Icons.Default.Book
    val ShortText = Icons.Default.ShortText
    val Style = Icons.Default.Style
    val Settings = Icons.Default.Settings
    val ContentCopy = Icons.Default.ContentCopy
    val Delete = Icons.Default.Delete
    val Share = Icons.Default.Share

    const val HomeDesc = "Home tab"
    const val BookDesc = "Dictionary tab"
    const val ShortTextDesc = "Snippets tab"
    const val StyleDesc = "Style tab"
    const val SettingsDesc = "Settings tab"
    const val CopyDesc = "Copy to clipboard"
    const val DeleteDesc = "Delete item"
    const val ShareDesc = "Share"
}
```

Verify: `./gradlew :app:compileDebugKotlin -q`

---

### Task 14: Theme update + Typography

**Files:**
- Modify: `app/src/main/java/app/openflow/ui/theme/Theme.kt`
- Create: `app/src/main/java/app/openflow/ui/theme/Type.kt`

- [ ] **Step 1: Write Type.kt**

```kotlin
package app.openflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 2: Rewrite Theme.kt**

Replace all content of `Theme.kt` with:

```kotlin
package app.openflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = OpenFlowColors.Primary,
    onPrimary = OpenFlowColors.OnPrimaryLight,
    primaryContainer = OpenFlowColors.PrimaryLight,
    secondary = OpenFlowColors.Secondary,
    secondaryContainer = OpenFlowColors.SecondaryLight,
    surface = OpenFlowColors.SurfaceLight,
    onSurface = OpenFlowColors.OnSurfaceLight,
    background = OpenFlowColors.BackgroundLight,
    onBackground = OpenFlowColors.OnBackgroundLight,
    surfaceVariant = OpenFlowColors.SurfaceVariantLight,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantLight,
    error = OpenFlowColors.Error,
    onError = OpenFlowColors.OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = OpenFlowColors.PrimaryLight,
    onPrimary = OpenFlowColors.OnPrimaryDark,
    primaryContainer = OpenFlowColors.Primary,
    secondary = OpenFlowColors.SecondaryLight,
    secondaryContainer = OpenFlowColors.Secondary,
    surface = OpenFlowColors.SurfaceDark,
    onSurface = OpenFlowColors.OnSurfaceDark,
    background = OpenFlowColors.BackgroundDark,
    onBackground = OpenFlowColors.OnBackgroundDark,
    surfaceVariant = OpenFlowColors.SurfaceVariantDark,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantDark,
    error = OpenFlowColors.ErrorDark,
    onError = OpenFlowColors.OnError
)

@Composable
fun OpenFlowTheme(
    darkMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 3: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`

---

### Task 15: String extraction to strings.xml

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

Add these string IDs to the existing `strings.xml` (merge with existing IDs):

```xml
<string name="home_tab">Home</string>
<string name="dictionary_tab">Dictionary</string>
<string name="snippets_tab">Snippets</string>
<string name="style_tab">Style</string>
<string name="settings_tab">Settings</string>
<string name="setup_title">Setup</string>
<string name="enable_flow_bubble">1. Enable Flow Bubble</string>
<string name="grant_microphone">2. Grant microphone</string>
<string name="accessibility_settings">Accessibility settings</string>
<string name="mic_granted">Microphone granted</string>
<string name="bubble_on">Bubble ON</string>
<string name="bubble_off">Bubble OFF</string>
<string name="mic_on">Mic ON</string>
<string name="mic_off">Mic OFF</string>
<string name="how_it_works">3. Focus field → tap 🎙 (or hold to talk) → release/stop\nDrag bubble to bottom edge = snooze 10 min</string>
<string name="test_field">Test field</string>
<string name="history_title">History</string>
<string name="no_dictations">No dictations yet. Use the bubble, then stop to save.</string>
<string name="words_format">%d words</string>
<string name="delete">Delete</string>
<string name="dictionary_title">Teach Open Flow your words (local)</string>
<string name="word_phrase">Word / phrase</string>
<string name="replace_with">Replace with (optional)</string>
<string name="add">Add</string>
<string name="snippets_title">Voice trigger → paste block (local)</string>
<string name="snippets_hint">Say the trigger alone as a full utterance after stop.</string>
<string name="trigger_placeholder">Trigger e.g. sig</string>
<string name="body_placeholder">Body</string>
<string name="add_snippet">Add snippet</string>
<string name="style_title">Writing style (local post-process)</string>
<string name="settings_title">Flow Bubble (Wispr parity)</string>
<string name="size_format">Size %d%%</string>
<string name="opacity_format">Opacity %d%%</string>
<string name="stt_language_label">STT language tag e.g. en-US, hi-IN</string>
<string name="default_locale">Default: %s</string>
<string name="end_snooze">End bubble snooze</string>
<string name="local_first_desc">Local-first. Wispr needs cloud; we do not. MIT FOSS.</string>
<string name="dark_mode">Dark mode</string>
<string name="dark_system">System</string>
<string name="dark_light">Light</string>
<string name="dark_dark">Dark</string>
```

- [ ] **Step 2: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q` — if any string ID is missing or wrong, fix and recompile.

---

### Task 16: MainActivity — use components + dark mode toggle

**Files:**
- Modify: `app/src/main/java/app/openflow/ui/MainActivity.kt`

- [ ] **Step 1: Add imports**

```kotlin
import app.openflow.ui.components.EmptyState
import app.openflow.ui.components.OpenCard
import app.openflow.ui.components.OpenChip
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenListItem
import app.openflow.ui.theme.OpenFlowTheme
import app.openflow.ui.a11y.OpenIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MicNone
```

- [ ] **Step 2: Update Theme + dark mode StateFlow observation**

In `setContent`, replace the `OpenFlowTheme` call:

```kotlin
val darkMode by app.prefs.darkMode.collectAsState(initial = app.prefs.darkMode.value)
OpenFlowTheme(darkMode = darkMode) {
    // existing content
}
```

- [ ] **Step 3: Add dark mode toggle row to SettingsTab**

After the opacity slider in `SettingsTab`, add:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Dark mode", style = MaterialTheme.typography.bodyMedium)
    Row {
        listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (v, label) ->
            TextButton(onClick = { prefs.setDarkMode(v) }) {
                Text(
                    label,
                    color = if (prefs.darkMode.value == v)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 4: Replace UI with components in HomeTab**

Replace `Chip` → `OpenChip`:
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OpenChip(label = if (bubbleOn) "Bubble ON" else "Bubble OFF", isOn = bubbleOn)
    OpenChip(label = if (micOn) "Mic ON" else "Mic OFF", isOn = micOn)
}
```

Replace `Card` → `OpenCard` for setup card and history cards.
Replace `Button` → `OpenButton` for setup buttons (variant = `.Filled`).
Replace `OutlinedTextField` → `OpenTextField` for test field.
Replace `Button` → `OpenButton` with `ButtonVariant.Outlined` for delete buttons.

Add copy button in history items:
```kotlin
OutlinedButton(
    onClick = {
        val cm = LocalContext.current.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        cm.setPrimaryClip(
            android.content.ClipData.newPlainText("dictation", d.text)
        )
    },
    modifier = Modifier.height(Dimen.TOUCH_TARGET)
) { Text("Copy") }
```

Add empty state for no dictations:
```kotlin
if (dictations.isEmpty()) {
    EmptyState(
        icon = Icons.Default.MicNone,
        title = "No dictations yet",
        subtitle = "Use the bubble, then stop to save."
    )
}
```

- [ ] **Step 5: Wrap tab content with AnimatedContent**

Wrap the `when(tab)` in `Scaffold`'s content with:
```kotlin
AnimatedContent(
    targetState = tab,
    transitionSpec = {
        fadeIn(androidx.compose.animation.core.tween(150)) togetherWith
            fadeOut(androidx.compose.animation.core.tween(150))
    },
    label = "tab_content"
) { targetTab ->
    when(targetTab) {
        // existing when content
    }
}
```

- [ ] **Step 6: Verify compiles**

Run: `./gradlew :app:compileDebugKotlin -q`
Expected: PASS

---

### Task 17: Build + verify

- [ ] `./gradlew :app:testDebugUnitTest -q`
- [ ] `./gradlew :app:assembleDebug -q`
- [ ] Verify APK at `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Report DID / PASS-FAIL / NEXT to Mitun

---

## Spec Coverage

| Spec item | Task |
|---|---|
| Calm pro palette | Task 2 |
| Dark mode toggle | Tasks 1, 14, 16 |
| WCAG AAA colors | Task 2 |
| OpenCard | Task 5 |
| OpenChip | Task 6 |
| OpenButton haptics | Task 7 |
| OpenTextField | Task 8 |
| OpenListItem | Task 9 |
| EmptyState | Task 10 |
| LoadingState | Task 11 |
| ErrorState | Task 12 |
| Motion fade 150ms | Task 16 |
| Touch target ≥48dp | Task 3, all components |
| String extraction | Task 15 |
| OpenIcons + contentDescription | Task 13 |
| Compose UI tests | Tasks 5, 6 |
| Dark mode StateFlow tests | Task 1 |
