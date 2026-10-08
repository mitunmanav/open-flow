package dev.openflow.dictation.core.stt.contract

import dev.openflow.dictation.core.stt.FailureReason
import dev.openflow.dictation.core.stt.ProviderState
import dev.openflow.dictation.core.stt.SpeechEvent
import dev.openflow.dictation.core.stt.SpeechProvider
import dev.openflow.dictation.core.stt.TranscriptionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the published suite against a probe that honours the contract, so that
 * `core`'s own build has actually executed the suite it publishes.
 *
 * ### What this test is for
 *
 * The suite is two abstract classes, so nothing in `core` runs them by itself:
 * `./gradlew test` stays green whether the suite passes, fails, or was never
 * compiled at all. That is this repository's most-recorded failure — a green
 * check that ran zero tests — and a shared suite nobody ever runs is that failure
 * wearing a publishing hat.
 *
 * Subclassing the suite rather than calling it is deliberate. A helper that ran
 * the assertions through reflection would report one passing test whichever way
 * they went, and would still be just one green test; subclassing means the
 * suite's tests are discovered by JUnit exactly as a provider module discovers
 * them, including any added later.
 */
class ProbeHonoursTheContract : SpeechProviderContract() {
    override fun newProvider() = ProbeSpeechProvider()
    override fun servableRequest() = TranscriptionRequest(language = "en-US")
}

/**
 * The other half of the same argument: the suite has to be able to fail.
 *
 * Assertions that cannot fail are indistinguishable from an absent suite, and
 * the provider modules that run this are the only things in the build that would
 * otherwise notice a regression in it. Each test below breaks exactly one clause
 * of ADR-0001 and asserts the published suite catches it — the `MODEL_MISSING`
 * pairing and the `READY`/health invariant, both named in ADR-0006 as the reason
 * the suite exists.
 */
class ContractSuiteIsNotVacuous {

    @Test
    fun theSuiteCatchesAMissingModelReportedAsSomethingGeneric() {
        val violations = violationsFrom { GenericModelFailureContract() }

        assertTrue(
            "ADR-0001 makes this pairing an invariant and ADR-0006 says the shared " +
                "suite enforces it: a provider reporting MODEL_MISSING must fail " +
                "transcribe() with OfflineModelMissing, never something generic, " +
                "because that pairing is what lets the router show a download " +
                "prompt rather than a retry button. The suite passed a provider " +
                "that broke it. Violations seen: $violations",
            violations.any { it.contains("OfflineModelMissing") },
        )
    }

    @Test
    fun theSuiteCatchesAReadyProviderClaimingItsModelIsMissing() {
        val violations = violationsFrom { ReadyButMissingModelContract() }

        assertTrue(
            "READY paired with MODEL_MISSING is the one contradiction ADR-0001 " +
                "declares: a provider cannot have finished loading a model it says " +
                "is missing. The suite passed one. Violations seen: $violations",
            violations.any { it.contains("MODEL_MISSING") },
        )
    }

    @Test
    fun theSuiteCatchesAnUndersuppliedUtterance() {
        // The suite asserts "exactly one terminal event", which is a real
        // constraint on a provider and not a restatement of its own code. A flow
        // that returns without a Final and without a Failure strands the Dictation
        // in Transcribing forever, and nothing else in the build would notice.
        val violations = violationsFrom { SilentTranscriptionContract() }

        assertTrue(
            "a request that terminates without a Final or a Failure leaves the " +
                "Dictation running with nothing coming. The suite passed one. " +
                "Violations seen: $violations",
            violations.any { it.contains("terminal event") },
        )
    }

    /**
     * Run every `@Test` in [contractClass] and collect the assertion messages it
     * produced.
     *
     * JUnit 4 offers no entry point for running a suite from inside a test, so
     * this drives the same method detection JUnit uses and invokes each method on
     * one instance. Sound here because the suite's tests are independent by
     * construction — each builds its own provider in a `finally` — and it is the
     * only way to observe a *failing* shared suite from an otherwise green build.
     */
    private fun violationsFrom(contractClass: () -> SpeechProviderContract): List<String> {
        val instance = contractClass()
        return instance.javaClass.methods
            .filter { it.isAnnotationPresent(org.junit.Test::class.java) }
            .sortedBy { it.name }
            .mapNotNull { method ->
                try {
                    method.invoke(instance)
                    null
                } catch (thrown: java.lang.reflect.InvocationTargetException) {
                    (thrown.targetException ?: thrown).message.orEmpty()
                }
            }
    }
}

/**
 * Reports `MODEL_MISSING` from `health()` but fails a transcription with a
 * generic reason — the shape ADR-0001's pairing forbids.
 */
private class GenericModelFailureContract : SpeechProviderContract() {
    private val broken = object : SpeechProvider by ProbeSpeechProvider(modelPresent = false) {
        override fun transcribe(request: TranscriptionRequest) = flow {
            emit(SpeechEvent.Failure(FailureReason.Unknown, recoverable = true))
        }
    }

    override fun newProvider(): SpeechProvider = broken
    override fun servableRequest() = TranscriptionRequest(language = "en-US")
}

/**
 * Claims `READY` while reporting the model missing: the one (state, health) pair
 * ADR-0001 declares impossible.
 */
private class ReadyButMissingModelContract : SpeechProviderContract() {
    private val forcedReady = MutableStateFlow(ProviderState.READY)
    private val ready = object : SpeechProvider by ProbeSpeechProvider(modelPresent = false) {
        override val state: StateFlow<ProviderState> = forcedReady.asStateFlow()
    }

    override fun newProvider(): SpeechProvider = ready
    override fun servableRequest() = TranscriptionRequest(language = "en-US")
}

/**
 * Completes a request with no terminal event at all, which would leave the
 * Dictation in `Transcribing` with nothing coming.
 */
private class SilentTranscriptionContract : SpeechProviderContract() {
    private val silent = object : SpeechProvider by ProbeSpeechProvider() {
        override fun transcribe(request: TranscriptionRequest) = flow {
            emit(SpeechEvent.Preparing)
            emit(SpeechEvent.Listening)
        }
    }

    override fun newProvider(): SpeechProvider = silent
    override fun servableRequest() = TranscriptionRequest(language = "en-US")
}

