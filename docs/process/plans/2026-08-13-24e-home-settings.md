# Task E — Home + settings feel

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Caveman. DID / PASS-FAIL / NEXT.

**Goal:** First-open how-to, honest dict/snippet copy, live bubble preview, Brutal-only Appearance, STT apply without a11y toggle.

**Architecture:** Pref `seenHowTo` in FlowPrefs. Copy + module `what` map as pure helpers. Compose in MainActivity only. No new grammar. No INTERNET.

**Tech Stack:** Kotlin, Compose, SharedPreferences via PrefsStore, JVM tests.

---

## Files

- Modify: `app/src/main/java/app/openflow/prefs/FlowPrefs.kt` (`seenHowTo` only)
- Modify: `app/src/main/java/app/openflow/ui/MainActivity.kt`
- Modify: `app/build.gradle.kts` (`versionCode=4`, `versionName=0.1.3`)
- Test: `app/src/test/java/app/openflow/prefs/FlowPrefsSeenHowToTest.kt`

---

### Task E1: seenHowTo pref

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun seenHowTo_defaults_false() {
    val prefs = FlowPrefs(MemoryPrefsStore())
    assertThat(prefs.seenHowTo).isFalse()
}

@Test
fun seenHowTo_persists_true() {
    val store = MemoryPrefsStore()
    val prefs = FlowPrefs(store)
    prefs.seenHowTo = true
    assertThat(prefs.seenHowTo).isTrue()
    assertThat(store.getString("seen_how_to", "false")).isEqualTo("true")
}
```

- [ ] **Step 2: Implement `FlowPrefs.seenHowTo`** default false, key `seen_how_to`.
- [ ] **Step 3: Tests pass.**

### Task E2: Home how-to + copy + editors

Home: if `!seenHowTo`, card `testTag("home_howto")` title "How Open Flow works".
Lines: not a keyboard; tap then tap again; X cancel; Dictionary=one word; Snippet=whole block.
Button **Got it** sets `seenHowTo = true`.

Dictionary subtitle: `Change one word (say API, insert the long name).`
Snippets subtitle: `Say a short trigger. Paste a whole block.`

ModuleEditor: focused/moved row shows `what`:
setup=permissions, test=practice field, keys=cleanup chips, stats=last dictation, recent=history.

BubbleSettings: 72dp hard-border preview, cream/charcoal, shape/scale/opacity. Sliders already call `onApplyBubble()`.

Appearance: Brutal only. Remove M3/skin chips.

STT chips: after save `FlowAccessibilityService.instance?.applyPrefsVisual()`. Helper: `Applies on next listen.` No "re-enable Accessibility".

### Task E3: version + compile

`versionCode = 4` · `versionName = "0.1.3"`

```bash
export JAVA_HOME=$HOME/.local/jdk ANDROID_HOME=$HOME/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests app.openflow.prefs.FlowPrefsSeenHowToTest
```

### Security

No new perm. No INTERNET.

### Mitun device

Home first open → how-to card. Got it hides it. Dict vs snippet lines. Bubble sliders move preview. Appearance has no M3 chips. STT chip → next listen, no a11y toggle.
