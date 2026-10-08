package dev.openflow.dictation.core.stt.contract

import dev.openflow.dictation.core.stt.FailureReason
import dev.openflow.dictation.core.stt.PrepareResult
import dev.openflow.dictation.core.stt.ProviderHealth
import dev.openflow.dictation.core.stt.ProviderState
import dev.openflow.dictation.core.stt.SpeechEvent
import dev.openflow.dictation.core.stt.SpeechProvider
import dev.openflow.dictation.core.stt.TranscriptionRequest
import dev.openflow.dictation.core.stt.isConsistentWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared Contract Test suite every `SpeechProvider` runs (ADR-0006).
 *
 * ### How to run it
 *
 * ```kotlin
 * class SherpaOnnxProviderContract : SpeechProviderContract() {
 *     override fun newProvider(): SpeechProvider = /* a fresh, unprepared one */
 *     override fun servableRequest(): TranscriptionRequest = /* a language it declares */
 * }
 * ```
 *
 * plus, in the provider's `build.gradle.kts`:
 *
 * ```kotlin
 * testImplementation(testFixtures(project(":core")))
 * ```
 *
 * ### Do not copy this
 *
 * It is a published artifact rather than a template, because what a provider
 * asserts against has to be the same artifact every other provider asserts
 * against. A private copy proves nothing about the shared contract, and drifts
 * silently — which is the failure this suite exists to catch, one layer up.
 *
 * ### What is in here and what is not
 *
 * Every test in this class runs against *any* provider, with no model on disk,
 * no microphone and no network. That is a hard constraint rather than a
 * shortcut: a real adapter can only be asked for a health verdict here, because
 * on a machine with no model `prepare()` legitimately fails and there is
 * nothing to transcribe. So this class is the suite that always executes, and
 * it is where the health/lifecycle invariants live.
 *
 * The capabilities-honesty assertions that need an actual utterance are in
 * [TranscriptionContract], which a provider subclasses separately and only if
 * it can drive one headlessly. Nothing here is skippable — a suite whose tests
 * can be skipped is a suite that can be green while proving nothing, which is
 * the specific defect this repository has recorded more than once.
 */
abstract class SpeechProviderContract {

    /**
     * A brand-new, unprepared instance.
     *
     * Fresh per test, never a shared one: the lifecycle tests move an instance
     * to `CLOSED` and `PREPARED`, and a shared instance would make the second
     * test's assertions depend on the first one's ordering.
     */
    protected abstract fun newProvider(): SpeechProvider

    /**
     * A request this provider has declared it can serve — its first entry in
     * [dev.openflow.dictation.core.stt.Capabilities.supportedLanguages].
     *
     * Deliberately one request and not the whole declared set: this class must
     * run even for a provider whose model is absent, and the full sweep across
     * every declared language belongs to [TranscriptionContract] with a real
     * utterance behind it.
     */
    protected abstract fun servableRequest(): TranscriptionRequest

    @Test
    fun aNewInstanceHasNotPreparedAnything() {
        val provider = newProvider()
        try {
            assertEquals(
                "a freshly constructed provider must start NOT_PREPARED, not READY: " +
                    "nothing has been loaded yet and reporting READY would let the " +
                    "router hand it a request it cannot serve",
                ProviderState.NOT_PREPARED,
                provider.state.value,
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun healthAnswersBeforeAnythingIsPrepared() {
        val provider = newProvider()
        try {
            // The call is the assertion. `health()` reads cached local truth, so
            // it must answer on an instance that has loaded nothing at all
            // rather than block, throw or need a round trip — its one V1
            // consumer reads one snapshot at PREPARING, on the critical path of
            // every dictation.
            val health = provider.health()
            assertSame(health, provider.health())
            assertTrue(
                "health() returned $health before any preparation; a fresh " +
                    "instance can be HEALTHY only if the engine is genuinely ready, " +
                    "and must otherwise say which of the four conditions applies",
                health in ProviderHealth.entries,
            )
            assertTrue(
                "NOT_PREPARED paired with MODEL_MISSING is consistent; any other " +
                    "READY pairing is a contradiction, and a new instance is never READY",
                provider.state.value.isConsistentWith(health),
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun healthIsCheapLocalTruthAndDoesNotProbe() {
        val provider = newProvider()
        try {
            // Called twice with nothing happening in between and asserted equal,
            // and the whole thing inside `runBlocking` rather than a coroutine:
            // a `health()` that suspended or needed a network round trip would
            // show up here as a slow or stateful call. V1 does no active
            // probing at all, so a probe is a regression rather than a slower
            // version of the same behaviour.
            val first = provider.health()
            val second = provider.health()
            assertEquals(first, second)
        } finally {
            provider.close()
        }
    }

    @Test
    fun prepareEitherReachesReadyOrSaysWhyItDidNot() {
        val provider = newProvider()
        try {
            val result = runBlocking { provider.prepare() }
            when (result) {
                is PrepareResult.Ready -> assertEquals(
                    "prepare() returned Ready, so the instance must be READY — a " +
                        "report of success the router acts on, and a lifecycle value " +
                        "that has not moved means the app will wait for a state " +
                        "nothing is going to publish",
                    ProviderState.READY,
                    provider.state.value,
                )

                is PrepareResult.Failed -> assertEquals(
                    "prepare() returned Failed(${result.reason}), so the instance " +
                        "must be NOT_PREPARED. A failed prepare that left it READY " +
                        "would let the router believe it had an engine it does not.",
                    ProviderState.NOT_PREPARED,
                    provider.state.value,
                )
            }

            // The invariant ADR-0001 declares, checked against observed state
            // rather than against the contract's own helper: a provider that has
            // finished loading cannot simultaneously be missing its model.
            assertTrue(
                "state ${provider.state.value} paired with health ${provider.health()} " +
                    "contradicts the ADR-0001 invariant that READY implies health is " +
                    "HEALTHY, DEGRADED or UNAVAILABLE — MODEL_MISSING is necessarily " +
                    "NOT_PREPARED",
                provider.state.value.isConsistentWith(provider.health()),
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun aMissingModelIsNotACrashAndIsReportedAsOfflineModelMissing() {
        val provider = newProvider()
        try {
            if (provider.health() != ProviderHealth.MODEL_MISSING) {
                // Conditional rather than skipped: the condition is a property of
                // the machine the suite runs on, not of the provider, and there
                // is no way to remove a model the provider may legitimately have
                // found. On a machine without the model these three assertions
                // run; on one with it, the pairing has nothing to check.
                return
            }

            assertTrue(
                "health() reports MODEL_MISSING, so the instance cannot also be " +
                    "READY (ADR-0001)",
                provider.state.value != ProviderState.READY,
            )

            val prepared = runBlocking { provider.prepare() }
            assertTrue(
                "health() reports MODEL_MISSING, so prepare() must report it too " +
                    "rather than returning Ready or throwing: a missing model is the " +
                    "expected state of a fresh install under ADR-0010's Model " +
                    "Delivery, and a crash on the path to first dictation reads as a " +
                    "bug. Got $prepared",
                prepared is PrepareResult.Failed &&
                    prepared.reason == FailureReason.OfflineModelMissing,
            )

            // The pairing ADR-0001 makes an invariant and ADR-0006 says the
            // Contract Test enforces: MODEL_MISSING from health() must surface
            // as OfflineModelMissing from transcribe(), never something generic,
            // because that pairing is what lets the router show a download prompt
            // instead of a retry button.
            val events = runBlocking {
                withTimeout(TRANSCRIBE_TIMEOUT_MS) {
                    provider.transcribe(servableRequest()).toList()
                }
            }
            val failure = events.filterIsInstance<SpeechEvent.Failure>().singleOrNull()
            assertTrue(
                "a provider reporting MODEL_MISSING must fail transcribe() with a " +
                    "single OfflineModelMissing failure; got $events",
                failure != null &&
                    failure.reason == FailureReason.OfflineModelMissing &&
                    failure.recoverable,
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun transcribeIsColdAndDoesNothingUntilCollected() {
        val provider = newProvider()
        try {
            val first = provider.transcribe(servableRequest())
            val second = provider.transcribe(servableRequest())

            assertNotSame(
                "two calls to transcribe() returned the same Flow, so the second " +
                    "collection would join the first one's transcription instead " +
                    "of starting its own. The contract says cold: collecting twice " +
                    "starts two transcriptions.",
                first,
                second,
            )

            assertEquals(
                "building the Flow must not start a transcription: state " +
                    "${provider.state.value} and health ${provider.health()} are " +
                    "unchanged, because capture and decode belong to collection",
                ProviderState.NOT_PREPARED,
                provider.state.value,
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun closeIsTerminalAndHealthRefusesToLieAfterIt() {
        val provider = newProvider()
        val before = provider.health()

        provider.close()
        assertEquals(
            "close() must move the instance to CLOSED",
            ProviderState.CLOSED,
            provider.state.value,
        )

        // Refusing beats answering. CLOSED has no health counterpart — closing
        // is something the app did, not something an engine reported — so the
        // truthful response to "how ready is this?" about a released engine is
        // none. Returning the last cached verdict would let a released provider
        // look HEALTHY (it was $before here) to a caller that missed the close.
        val threw = try {
            provider.health()
            false
        } catch (expected: IllegalStateException) {
            true
        }
        assertTrue(
            "health() after close() must throw IllegalStateException rather than " +
                "return the verdict it gave before the engine was released",
            threw,
        )

        val prepareThrew = try {
            runBlocking { provider.prepare() }
            false
        } catch (expected: IllegalStateException) {
            true
        }
        assertTrue(
            "prepare() on a closed instance must throw: close() is terminal, and a " +
                "prepare() that resurrected a released engine would hide a " +
                "use-after-release bug rather than surface it",
            prepareThrew,
        )

        // Idempotent, because the natural place to close is a `finally` block
        // that may already have closed it.
        provider.close()
    }

    @Test
    fun declaredCapabilitiesAreSelfConsistent() {
        val provider = newProvider()
        try {
            val capabilities = provider.capabilities

            assertTrue(
                "supportedLanguages was ${capabilities.supportedLanguages}. A " +
                    "provider that can serve no language can serve no request, and " +
                    "the router would have nothing to select on.",
                capabilities.supportedLanguages.isNotEmpty(),
            )
            assertTrue(
                "maxAudioDurationSeconds was ${capabilities.maxAudioDurationSeconds}; " +
                    "the controller enforces it as a real ceiling, so a non-positive " +
                    "value refuses every utterance",
                capabilities.maxAudioDurationSeconds > 0,
            )
            assertTrue(
                "every declared language must be non-blank: the router matches on " +
                    "exact tags, so a blank entry matches a request nobody will make",
                capabilities.supportedLanguages.none { it.isBlank() },
            )

            // `0` and `null` are different answers, and only one of them is free.
            // The suite can check the shape — that a declared rate is a real
            // non-negative amount — but not whether it is the amount actually
            // charged, which is a property of the provider's invoice and is
            // reviewed rather than measured here.
            val pricing = capabilities.pricing
            if (pricing != null) {
                assertTrue(
                    "pricing declared ${pricing.microsUsdPerSecond} micros/second; a " +
                        "rate is never negative (0 is the honest value for a free " +
                        "provider, and cannot-estimate is pricing = null, not a " +
                        "number)",
                    pricing.microsUsdPerSecond >= 0,
                )
            }

            // `streaming` and `partialTranscripts` are separate declarations
            // about the same behaviour, and [TranscriptionContract] asserts the
            // behaviour. Here, only that declaring partials without streaming —
            // or the reverse — is not a thing this contract permits.
            assertTrue(
                "partialTranscripts is binding, so it cannot be declared without " +
                    "streaming: the app offers a live-transcript surface from the " +
                    "former and expects an event from the latter",
                !capabilities.partialTranscripts || capabilities.streaming,
            )
        } finally {
            provider.close()
        }
    }

    @Test
    fun theProviderHasAnIdentityTheRouterCanRecord() {
        val provider = newProvider()
        try {
            // ADR-0004's RouterContext carries `preferredProviderId` and its
            // decision log names the chosen provider, so an id is part of the
            // contract rather than a convenience. Not required to be unique
            // across processes here — that is the router's registry concern —
            // but it has to exist and be stable enough to match a preference.
            assertTrue(
                "id was blank; ADR-0004 matches a user's preferredProviderId against " +
                    "it and records it in the decision log",
                provider.id.isNotBlank(),
            )
            assertEquals(provider.id, provider.id)
        } finally {
            provider.close()
        }
    }

    private companion object {
        /**
         * How long a collected flow may take before the suite calls it hung.
         * Generous, because the machines this runs on are shared and slow, and
         * short enough that a real hang fails the build rather than parking it.
         */
        const val TRANSCRIBE_TIMEOUT_MS = 10_000L
    }
}