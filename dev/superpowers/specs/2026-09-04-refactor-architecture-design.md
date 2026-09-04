# Open Flow Architecture Refactor — Design Spec

**Date:** 2026-09-04  
**Status:** DRAFT — awaiting user review  
**Author:** Mitun

---

## 1. Executive Summary

This spec defines a **feature-based modularization with Clean Architecture layers** for the Open Flow codebase. The goal: transform the current 17-package monolith into a maintainable, testable, secure multi-module project — **without removing, adding, or cutting any features**.

### Current State (Pain Points)
- `FlowAccessibilityService.kt`: 2,024 lines — god class mixing accessibility, STT, audio, UI, text processing
- 17 packages under `app.openflow` with tangled cross-imports
- Business logic coupled to Android framework (hard to unit test)
- Single `app` module — any change triggers full rebuild
- No explicit module boundaries — unclear ownership

### Target State
- **~23 Gradle modules** organized by feature + layer (1 app + 3 core + 4 abstraction + 4 domain + 3 data + 8 feature)
- **Domain layer** (pure Kotlin) = testable without Android
- **Abstraction modules** = contracts only, zero implementation
- **Feature modules** = self-contained, independently buildable
- **Explicit dependency graph** — no cycles, inversion via abstractions

### Non-Goals
- ❌ No feature removal/addition
- ❌ No UI redesign
- ❌ No runtime behavior changes
- ❌ No big-bang rewrite — incremental migration only

---

## 2. Module Architecture

### 2.1 Module Taxonomy

| Type | Suffix | Purpose | Android Deps? |
|------|--------|---------|---------------|
| **App** | `app` | Entry point, DI graph, Navigation graph | Yes |
| **Feature** | `feature-*` | Single user-facing capability (bubble, stt, text, etc.) | Yes (UI layer) |
| **Data** | `data-*` | Repository implementations, Room, DataStore, network | Yes |
| **Domain** | `domain-*` | Use cases, domain models, repository interfaces | **No** |
| **Abstraction** | `*-api` | Pure interfaces/contracts (no impl) | **No** |
| **Core** | `core-*` | Shared utilities, UI components, testing support | Yes (minimal) |

### 2.2 Proposed Modules (15 total)

```
open-flow/
├── app/                          # :app (1)
│   ├── DI (Hilt modules)
│   ├── Navigation graph
│   └── Application class
│
├── core/                         # 3 modules
│   ├── core-common/              # :core:common
│   │   ├── extensions, constants, result types
│   │   └── Dimen, Dp, Color constants
│   ├── core-ui/                  # :core:ui
│   │   ├── Shared Compose: BubbleChrome, WaveformBars, chips
│   │   ├── Theming: OpenFlowTheme, typography, shapes
│   │   └── Haptics, HapticFeel, HapticPick
│   └── core-testing/             # :core:testing
│       ├── Test rules, coroutines test dispatcher
│       ├── Fakes: FakeSpeechEngine, FakeAudioCapture
│       └── Compose test utilities
│
├── abstraction/                  # 4 modules — Pure contracts (Kotlin-only)
│   ├── bubble-api/               # :abstraction:bubble-api
│   │   ├── BubbleController, BubbleVisuals, BubbleEvents
│   │   ├── BubbleShape, BubbleSize, BubbleState
│   │   └── FieldContext, InsertCallback
│   ├── stt-api/                  # :abstraction:stt-api
│   │   ├── SpeechEngine, EarId, EarGate
│   │   ├── SttConfig, SttResult, SttError
│   │   └── LanguagePolicy, MicPolicy
│   ├── text-api/                 # :abstraction:text-api
│   │   ├── TextProcessor, CleanupPipeline
│   │   ├── ItnProcessor, CommandProcessor
│   │   ├── WritingStyle, StyleConfig
│   │   └── LearnEngine, Correction
│   └── audio-api/                # :abstraction:audio-api
│       ├── AudioCapture, AudioConsumer
│       ├── PcmFormat, SampleRate
│       ├── WavWriter, AudioProbe
│       └── TeeController, GenerationToken
│
├── domain/                       # 4 modules — Pure business logic (Kotlin-only)
│   ├── domain-bubble/            # :domain:bubble
│   │   ├── Use cases: StartListening, StopListening, InsertText
│   │   ├── BubbleStateMachine, VisibilityPolicy
│   │   ├── DragController, PulseController
│   │   └── Models: BubbleMode, SessionState
│   ├── domain-stt/               # :domain:stt
│   │   ├── Use cases: Transcribe, SwitchEar, Fallback
│   │   ├── SttRouter, ProviderHealthMonitor
│   │   ├── Models: EarCapabilities, RouteDecision
│   │   └── Policies: LanguagePolicy, ContinuousPolicy
│   ├── domain-text/              # :domain:text
│   │   ├── Use cases: CleanupText, ApplyITN, ExecuteCommand
│   │   ├── PipelineOrchestrator, StageRunner
│   │   ├── Models: CleanupResult, ItnResult, CommandResult
│   │   └── Learning: LearnFromEdit, AutoCorrect
│   └── domain-audio/             # :domain:audio
│       ├── Use cases: CaptureAudio, WriteWav, ProbeLatency
│       ├── AudioGenerationManager (M3-A invariant)
│       ├── TeeOrchestrator, ResamplePolicy
│       └── Models: AudioSession, CaptureConfig
│
├── data/                         # 3 modules — Implementations
│   ├── data-dictation/           # :data:dictation
│   │   ├── Room database, DAOs, entities
│   │   ├── DictationRepositoryImpl
│   │   ├── Migrations, FTS, Snippets, VoiceProfile
│   │   └── ProcessStatus, DictationMapper
│   ├── data-prefs/               # :data:prefs
│   │   ├── FlowPrefs (DataStore), EnginePrefs
│   │   ├── LayoutPrefs, RetentionPrefs
│   │   └── Keys, Defaults, Migration
│   └── data-privacy/             # :data:privacy
│       ├── RetentionPolicyImpl, RetentionProof
│       ├── EncryptionPolicy (FBE only, per M7)
│       ├── BackupRules, DataExtractionRules
│       └── PrivacyDefaults
│
└── feature/                      # 8 modules — UI + feature orchestration
    ├── feature-bubble/           # :feature:bubble
    │   ├── FlowAccessibilityService (refactored, ~300 lines)
    │   ├── BubbleViewModel, BubbleUiState
    │   ├── Compose: BubbleOverlay, BubbleCanvas, Chips
    │   ├── WindowController, DragHandler
    │   └── DI: BubbleModule
    ├── feature-stt/              # :feature:stt
    │   ├── SttViewModel, SttUiState
    │   ├── Engine factories (Cloud, OnDevice, System)
    │   ├── Providers: CloudEar, OnDeviceEar, AndroidSpeechEngine
    │   ├── SttRouterImpl, ProviderHealthImpl
    │   └── DI: SttModule
    ├── feature-text/             # :feature:text
    │   ├── TextViewModel, TextUiState
    │   ├── Processors: CleanupPipelineImpl, ItnProcessorImpl
    │   ├── CommandExecutor, VoiceCommandParser
    │   ├── LearnEngineImpl, CorrectionClassifierImpl
    │   └── DI: TextModule
    ├── feature-audio/            # :feature:audio
    │   ├── AudioViewModel, AudioUiState
    │   ├── AppAudioCapture (M3-A tee), SessionAudioCapture
    │   ├── WhisperRuntime, JniWhisperRuntime
    │   ├── AudioProbe, AudioFileManager
    │   ├── Consumers: WhisperConsumer, CloudConsumer, WavConsumer
    │   └── DI: AudioModule
    ├── feature-home/             # :feature:home
    │   ├── HomeViewModel, HomeUiState
    │   ├── HomeFeed, HistorySearch, ModuleCatalog
    │   ├── DictationCard, DayHeader, LoadMore
    │   └── DI: HomeModule
    ├── feature-insights/         # :feature:insights
    │   ├── InsightsViewModel, InsightsUiState
    │   ├── Tiles, Heatmap, ShareCard
    │   ├── InsightsAggregatePolicy (pure, tested)
    │   └── DI: InsightsModule
    ├── feature-setup/            # :feature:setup
    │   ├── SetupViewModel, SetupUiState
    │   ├── Steps: A11yStep, MicStep, BatteryStep
    │   ├── FirstRunPolicy, OemBatteryHint
    │   ├── SetupProgressDots, SetupStepCard
    │   └── DI: SetupModule
    └── feature-settings/         # :feature:settings
        ├── SettingsViewModel, SettingsUiState
        ├── Screens: Bubble, Privacy, Speech, Appearance, Cleanup
        ├── SettingsCatalog, SettingsHub
        └── DI: SettingsModule
```

---

## 3. Dependency Graph

### 3.1 Layer Rules (Enforced by Gradle)

```
app
  → feature-* (all)
  → core-ui, core-common
  → data-* (for DI binding only)

feature-*
  → domain-* (same domain)
  → *-api (abstractions)
  → core-ui, core-common
  ❌ NOT → other feature-*
  ❌ NOT → data-* directly (via domain use cases)

domain-*
  → *-api (same domain)
  → core-common
  ❌ NOT → Android
  ❌ NOT → data-*
  ❌ NOT → feature-*

data-*
  → domain-* (implements repository interfaces)
  → *-api (implements contracts)
  → core-common
  ❌ NOT → feature-*
  ❌ NOT → other data-*

*-api
  → core-common (only)
  ❌ NOT → Android
  ❌ NOT → any implementation

core-*
  → core-common (only)
  ❌ NOT → feature/domain/data/abstraction
```

### 3.2 Dependency Inversion Examples

**STT Domain → STT Abstraction**
```kotlin
// domain-stt/src/main/kotlin/.../SttRouter.kt
interface SttRouter {
    fun route(config: SttConfig): Flow<SttResult>
    fun switchEar(earId: EarId)
}

// stt-api/src/main/kotlin/.../SpeechEngine.kt
interface SpeechEngine {
    fun start(listener: Listener)
    fun stop()
    val isAvailable: Boolean
}

// feature-stt/src/main/kotlin/.../SttRouterImpl.kt
class SttRouterImpl @Inject constructor(
    private val registry: ProviderRegistry,  // from stt-api
    private val health: ProviderHealth       // from stt-api
) : SttRouter { ... }
```

**Bubble Feature → Bubble Domain → Bubble API**
```kotlin
// feature-bubble: BubbleViewModel uses domain use cases
class BubbleViewModel @Inject constructor(
    private val startListening: StartListeningUseCase,  // domain-bubble
    private val insertText: InsertTextUseCase,          // domain-bubble
    private val bubbleController: BubbleController      // bubble-api
) : ViewModel() { ... }

// domain-bubble: Use case implements business logic
class StartListeningUseCase @Inject constructor(
    private val audioCapture: AudioCapture,      // audio-api
    private val sttRouter: SttRouter,            // stt-api
    private val bubbleVisuals: BubbleVisuals     // bubble-api
) { ... }
```

---

## 4. Migration Strategy (Incremental)

### Phase 0: Preparation (Week 1)
- [ ] Add `core-common`, `core-ui`, `core-testing` modules
- [ ] Extract shared constants, Dimen, theming to `core-ui`
- [ ] Set up **Hilt** in `app` module (per §11 decision)
- [ ] Configure `buildSrc` / convention plugins for module standards

### Phase 1: Abstraction Modules (Week 2)
- [ ] Create `bubble-api`, `stt-api`, `text-api`, `audio-api`
- [ ] Define interfaces for all cross-module contracts
- [ ] **No implementation code** — pure Kotlin interfaces + data classes
- [ ] Configure composite build (`includeBuild`) for consumption (per §11 decision)

### Phase 2: Domain Modules (Week 3)
- [ ] Create `domain-bubble`, `domain-stt`, `domain-text`, `domain-audio`
- [ ] Move pure business logic from `FlowAccessibilityService`, `SttEngine`, `TextPostProcessor`, `AppAudioCapture`
- [ ] Write unit tests for every use case (target: 90%+ coverage)
- [ ] Zero Android dependencies — verify with `android-lint`

### Phase 3: Data Modules (Week 4)
- [ ] Create `data-dictation`, `data-prefs`, `data-privacy`
- [ ] Move Room, DataStore, Repository implementations
- [ ] Implement repository interfaces from domain modules
- [ ] Integration tests with Room test DB

### Phase 4: Feature Modules (Weeks 5-8)
- [ ] **feature-bubble**: Extract `FlowAccessibilityService` → `BubbleViewModel` + Compose UI + `BubbleWindowController`
  - Target: Service < 300 lines (delegates to ViewModel + domain use cases)
- [ ] **feature-stt**: Extract STT providers, router, health monitor
- [ ] **feature-text**: Extract processors, pipeline, commands, learning
- [ ] **feature-audio**: Extract `AppAudioCapture` (M3-A tee), Whisper, consumers
- [ ] **feature-home/insights/setup/settings**: Extract UI + ViewModels

### Phase 5: App Module Wiring (Week 9)
- [ ] Wire DI graph in `app` module
- [ ] Configure Navigation 3 graph
- [ ] Remove old package imports from `OpenFlowApp`
- [ ] Verify `gate.sh --quick` passes

### Phase 6: Cleanup (Week 10)
- [ ] Delete old `app.openflow.*` packages
- [ ] Remove unused dependencies
- [ ] Update `AGENTS.md` with new module map
- [ ] Document module ownership in `docs/specs/module-ownership.md`

---

## 5. Key Refactoring Patterns

### 5.1 God Class Decomposition: `FlowAccessibilityService`

**Before:** 2,024 lines — accessibility + STT + audio + UI + text + persistence

**After:**
```
feature-bubble/
├── FlowAccessibilityService.kt          # ~200 lines
│   ├── onServiceConnected() → bubbleViewModel.initialize()
│   ├── onAccessibilityEvent() → bubbleViewModel.onAccessibilityEvent()
│   ├── onInterrupt/onDestroy() → bubbleViewModel.cleanup()
│   └── Delegates ALL logic to ViewModel
├── BubbleViewModel.kt                   # ~300 lines
│   ├── uiState: StateFlow<BubbleUiState>
│   ├── startListening(), stopListening(), insertText()
│   └── Uses: StartListeningUseCase, StopListeningUseCase, InsertTextUseCase
├── BubbleUiState.kt                     # Sealed class: Idle, Listening, Processing, Error
├── usecase/
│   ├── StartListeningUseCase.kt
│   ├── StopListeningUseCase.kt
│   ├── InsertTextUseCase.kt
│   ├── UpdateBubbleVisibilityUseCase.kt
│   └── HandleAccessibilityEventUseCase.kt
├── ui/
│   ├── BubbleOverlay.kt (Compose)
│   ├── BubbleCanvas.kt (Custom draw → BubbleVisualPainter)
│   ├── PostStopChips.kt
│   └── WaveformBars.kt
└── window/
    ├── BubbleWindowController.kt
    └── BubbleDragHandler.kt
```

### 5.2 STT Engine Decomposition: `SttEngine`

**Before:** 26,834 lines — routing, providers, fallbacks, hypotheses, WAV, metrics

**After:**
```
domain-stt/
├── usecase/
│   ├── TranscribeUseCase.kt
│   ├── SwitchEarUseCase.kt
│   └── FallbackUseCase.kt
├── policy/
│   ├── LanguagePolicy.kt
│   ├── ContinuousPolicy.kt
│   └── EarGate.kt
├── model/
│   ├── SttConfig.kt
│   ├── SttResult.kt (Partial, Final, Error)
│   └── EarCapabilities.kt
└── SttRouter.kt (interface)

feature-stt/
├── SttViewModel.kt
├── provider/
│   ├── CloudEar.kt (implements SpeechEngine)
│   ├── OnDeviceEar.kt (implements SpeechEngine)
│   └── AndroidSpeechEngine.kt (implements SpeechEngine)
├── SttRouterImpl.kt (implements SttRouter)
└── ProviderHealthImpl.kt
```

### 5.3 Text Processing: `TextPostProcessor` (13,855 lines)

**After:**
```
domain-text/
├── usecase/
│   ├── CleanupTextUseCase.kt
│   ├── ApplyItnUseCase.kt
│   ├── ExecuteCommandUseCase.kt
│   └── LearnFromEditUseCase.kt
├── pipeline/
│   ├── PipelineStage.kt (interface)
│   ├── StageRunner.kt (orchestrates stages)
│   └── CleanupBudget.kt
├── model/
│   ├── CleanupResult.kt
│   ├── ItnResult.kt
│   └── CommandResult.kt
└── TextProcessor.kt (interface)

feature-text/
├── processor/
│   ├── CleanupPipelineImpl.kt (implements TextProcessor)
│   ├── ItnProcessorImpl.kt
│   ├── CommandExecutorImpl.kt
│   └── LearnEngineImpl.kt
└── TextViewModel.kt
```

### 5.4 Audio Tee (M3-A): `AppAudioCapture`

**After:**
```
domain-audio/
├── usecase/
│   ├── CaptureAudioUseCase.kt
│   ├── WriteWavUseCase.kt
│   └── ProbeLatencyUseCase.kt
├── model/
│   ├── AudioSession.kt (generation token)
│   ├── CaptureConfig.kt (rate, source, buffer)
│   └── PcmFormat.kt
├── AudioCapture.kt (interface)
├── AudioConsumer.kt (interface)
├── TeeController.kt (interface)
└── GenerationManager.kt (M3-A invariant)

feature-audio/
├── AppAudioCapture.kt (implements AudioCapture + TeeController)
│   ├── Single AudioRecord + CopyOnWriteArrayList<AudioConsumer>
│   ├── Generation token propagation
│   └── Device-aware buffer sizing (§5.1 audio-tee-architecture.md)
├── consumer/
│   ├── WhisperConsumer.kt
│   ├── CloudConsumer.kt
│   └── WavFileConsumer.kt
├── WhisperRuntime.kt / JniWhisperRuntime.kt
├── AudioProbe.kt
└── AudioFileManager.kt
```

---

## 6. Testing Strategy

| Layer | Test Type | Target | Tools |
|-------|-----------|--------|-------|
| **Domain** | Unit | 95%+ | JUnit, MockK, Turbine (Flow) |
| **Data** | Integration | 80%+ | Room test DB, DataStore test, Turbine |
| **Feature (ViewModel)** | Unit | 90%+ | MockK, Turbine, Compose Test |
| **Feature (UI)** | Screenshot | Critical paths | Paparazzi (later), Compose Preview |
| **Abstraction** | Contract | 100% | Interface verification tests |
| **E2E** | Instrumented | Smoke | `gate.sh --instrument` |

### Test Organization
```
domain-bubble/
├── src/test/
│   ├── usecase/StartListeningUseCaseTest.kt
│   ├── policy/VisibilityPolicyTest.kt
│   └── statemachine/BubbleStateMachineTest.kt

feature-bubble/
├── src/test/
│   ├── BubbleViewModelTest.kt
│   └── window/BubbleDragHandlerTest.kt
├── src/androidTest/
│   └── FlowAccessibilityServiceTest.kt (minimal — delegates tested in domain)

data-dictation/
├── src/androidTest/
│   ├── DictationRepositoryTest.kt
│   └── DictationDaoTest.kt
```

---

## 7. Security & Privacy Boundaries

### 7.1 Module Isolation for Privacy

| Module | Privacy Responsibility | Data Access |
|--------|------------------------|-------------|
| `data-privacy` | **Single source of truth** for retention, encryption, backup | Only module with `RetentionPolicy` impl |
| `data-dictation` | Stores dictations; **no encryption logic** | Delegates to `data-privacy` |
| `domain-*` | **No raw data access** — uses repository interfaces | Never touches Room/DataStore directly |
| `feature-*` | UI only — **no persistence logic** | Calls domain use cases |

### 7.2 M7 Compliance (Storage/Privacy)
- `data-privacy` implements `RetentionPolicy` per `PrivacyDefaults`
- `allowBackup=false` + `dataExtractionRules` in `app` manifest
- **No `EncryptedFile`**, **no SQLCipher** — FBE only (per architecture-gates.md)
- `RetentionProof` generated in `data-privacy`, verified in tests

### 7.3 M3-A Audio Security
- `domain-audio.GenerationManager` enforces: **no PCM from generation g reaches g+1**
- `feature-audio.AppAudioCapture` holds single `AudioRecord` — no orphan recorders
- `AudioProbe` logs actual sample rate, buffer size, frames — no PII

---

## 8. Build Configuration

### 8.1 Convention Plugins (`build-logic/convention/`)
```kotlin
// android-library.kt
plugins { id("com.android.library") id("kotlin-android") id("kotlin-kapt") }
android {
    compileSdk = 36
    defaultConfig { minSdk = 26; targetSdk = 36 }
    // Shared: namespace, javaVersion, composeOptions, kotlinOptions
}
dependencies {
    // Shared: constraints, platform BOM
}

// android-feature.kt (extends android-library)
plugins { id("android-library") id("kotlin-android") id("kotlin-kapt") id("hilt") }
dependencies {
    api(project(":abstraction:*-api")) // feature exports its API
    implementation(project(":domain:*")) // feature uses domain
}

// kotlin-pure.kt (domain, abstraction)
plugins { id("kotlin") }
dependencies {
    // Zero Android deps
    api(project(":core:common"))
}
```

### 8.2 Version Catalog (`gradle/libs.versions.toml`)
- Centralized: Kotlin, Compose, Coroutines, Room, Hilt, Whisper NDK
- Single source for all modules

---

## 9. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Incremental migration breaks build** | High | High | Phase-gated: each phase passes `gate.sh --quick` before next |
| **Circular dependencies emerge** | Medium | High | `api`/`implementation` enforcement; `dependencyVerify` task |
| **Performance regression (DI overhead)** | Low | Medium | Benchmark `gate.sh --release` before/after; Hilt compile-time |
| **Whisper/NDK integration complexity** | Medium | High | Keep `feature-audio` self-contained; isolate JNI in `whisper` package |
| **Accessibility service refactor breaks overlay** | High | Critical | Keep `FlowAccessibilityService` as thin delegate; test on device each phase |
| **Team unfamiliar with multi-module** | Medium | Medium | Document patterns in `docs/specs/module-patterns.md`; pair programming |

---

## 10. Success Criteria (Definition of Done)

### Per Phase
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — **PASS**
- [ ] `bash scripts/qa/gate.sh --quick` — **PASS**
- [ ] No new lint warnings
- [ ] Unit test coverage ≥ 80% for new domain/feature code

### Final (Phase 6)
- [ ] All 17 old packages deleted
- [ ] `gate.sh --release` — **PASS**
- [ ] `gate.sh --instrument` — **PASS** (6/6 functional checks)
- [ ] `play-check.sh` — **17/17 PASS**
- [ ] `visual-capture.sh` — 4 PNGs match baseline
- [ ] APK size ≤ current + 5%
- [ ] Build time (clean) ≤ current × 1.2 (modularization should improve incremental)

---

## 11. Open Questions (Require Decisions)

1. **DI Framework**: Hilt (Google standard, compile-time) vs Koin (lightweight, runtime)?  
   → *Recommendation: Hilt — better for multi-module, compile-time validation, Android standard*

2. **Navigation**: Navigation 3 (type-safe, Compose) vs manual?  
   → *Recommendation: Navigation 3 — single-activity, deep links, type-safe args*

3. **Whisper NDK Module**: Separate `:whisper` module or part of `:feature:audio`?  
   → *Recommendation: Keep in `:feature:audio` — single feature ownership; NDK config in convention plugin*

4. **Compose Compiler**: Upgrade to Compose Compiler 1.5+ (Kotlin 1.9+)?  
   → *Recommendation: Yes — required for Compose BOM 2024.x, better incremental*

5. **Module Publishing**: Local Maven vs composite build?  
   → *Recommendation: Composite build (`includeBuild`) — faster, no publishing step*

---

## 12. Appendix: Current → Target Package Mapping

| Current Package | Target Module(s) |
|-----------------|------------------|
| `app.openflow.bubble` | `feature-bubble`, `domain-bubble`, `bubble-api` |
| `app.openflow.stt` | `feature-stt`, `domain-stt`, `stt-api` |
| `app.openflow.text` | `feature-text`, `domain-text`, `text-api` |
| `app.openflow.audio` | `feature-audio`, `domain-audio`, `audio-api` |
| `app.openflow.whisper` | `feature-audio` (internal) |
| `app.openflow.engine` | `feature-stt` (ProviderRegistry → SttRouterImpl) |
| `app.openflow.orchestrate` | `domain-stt` (SttRouter), `domain-bubble` (BrainRouter) |
| `app.openflow.data` | `data-dictation` |
| `app.openflow.prefs` | `data-prefs` |
| `app.openflow.privacy` | `data-privacy` |
| `app.openflow.ui.home` | `feature-home` |
| `app.openflow.ui.insights` | `feature-insights` |
| `app.openflow.ui.setup` | `feature-setup` |
| `app.openflow.ui.settings` | `feature-settings` |
| `app.openflow.ui.*` (shared) | `core-ui` |
| `app.openflow.display` | `feature-bubble` (DisplayRefreshController) |
| `app.openflow.runtime` | `domain-audio` (TrimPolicy) |
| `app.openflow.secrets` | `data-prefs` (SecretStore) |
| `app.openflow.ai` | `feature-stt` (Brain providers) |
| `app.openflow.notify` | `core-common` (DictationNotifier) |
| `app.openflow.help` | `core-common` (HelpLinks) |
| `app.openflow.export` | `feature-home` / `feature-insights` |
| `app.openflow.metrics` | `domain-audio` / `feature-audio` |

---

*End of Design Spec*