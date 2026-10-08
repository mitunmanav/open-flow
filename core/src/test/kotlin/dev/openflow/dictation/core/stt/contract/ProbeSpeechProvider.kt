package dev.openflow.dictation.core.stt.contract

import dev.openflow.dictation.core.stt.Capabilities
import dev.openflow.dictation.core.stt.FailureReason
import dev.openflow.dictation.core.stt.PrepareResult
import dev.openflow.dictation.core.stt.Pricing
import dev.openflow.dictation.core.stt.ProviderHealth
import dev.openflow.dictation.core.stt.ProviderState
import dev.openflow.dictation.core.stt.SpeechEvent
import dev.openflow.dictation.core.stt.SpeechProvider
import dev.openflow.dictation.core.stt.TranscriptionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A deliberately minimal adapter that honours every clause of the contract, so
 * that [SpeechProviderContract] can be shown to pass against something.
 *
 * Not a `FakeProvider`. `GLOSSARY.md` defines that as the scripted
 * implementation that runs the whole dictation pipeline in tests, ticket 43 owns
 * it, and nothing here scripts an utterance, drives the refiner or models
 * latency -- this exists only so the published suite has something to run
 * against inside `core` itself.
 *
 * ### Why it is here at all
 *
 * Without it, `core`'s own build would compile the Contract Test suite and never
 * execute it, because the suite is abstract and no provider lives in `core` yet.
 * A suite that has never run is an untested test, and an untested suite is the
 * worst kind of published artifact: it looks like evidence. This class makes the
 * suite's *pass* case verifiable today, and
 * [ContractSuiteIsNotVacuous] makes its *failure* case verifiable too, which
 * together are what turn "the suite exists" into "the suite works".
 */
class ProbeSpeechProvider(
    override val id: String = "probe",
    override val capabilities: Capabilities = defaultCapabilities,
    private val modelPresent: Boolean = true,
) : SpeechProvider {

    private val _state = MutableStateFlow(ProviderState.NOT_PREPARED)
    override val state: StateFlow<ProviderState> = _state.asStateFlow()

    override suspend fun prepare(): PrepareResult {
        check(_state.value != ProviderState.CLOSED) { "closed" }
        if (_state.value == ProviderState.READY) return PrepareResult.Ready

        _state.value = ProviderState.PREPARING
        return if (modelPresent) {
            _state.value = ProviderState.READY
            PrepareResult.Ready
        } else {
            PrepareResult.Failed(FailureReason.OfflineModelMissing, "probe has no model")
        }
    }

    override fun transcribe(request: TranscriptionRequest): Flow<SpeechEvent> = flow {
        check(_state.value != ProviderState.CLOSED) { "closed" }
        if (!modelPresent) {
            emit(SpeechEvent.Failure(FailureReason.OfflineModelMissing, recoverable = true))
            return@flow
        }
        if (request.language !in capabilities.supportedLanguages) {
            emit(SpeechEvent.Failure(FailureReason.UnsupportedLanguage, recoverable = false))
            return@flow
        }

        emit(SpeechEvent.Preparing)
        emit(SpeechEvent.Listening)
        delay(PARTIAL_INTERVAL_MS)
        emit(SpeechEvent.Partial("hello"))
        delay(PARTIAL_INTERVAL_MS)
        emit(SpeechEvent.Partial("hello world"))
        delay(PARTIAL_INTERVAL_MS)
        emit(SpeechEvent.Final("hello world", 0L, UTTERANCE_MS))
    }

    override fun health(): ProviderHealth {
        check(_state.value != ProviderState.CLOSED) { "closed" }
        return if (modelPresent) ProviderHealth.HEALTHY else ProviderHealth.MODEL_MISSING
    }

    override fun close() {
        _state.value = ProviderState.CLOSED
    }

    companion object {
        /** Long enough to be separate events, short enough not to slow the suite. */
        const val PARTIAL_INTERVAL_MS = 5L

        const val UTTERANCE_MS = 500L

        val defaultCapabilities = Capabilities(
            streaming = true,
            offline = true,
            supportedLanguages = setOf("en-US"),
            autoDetectLanguage = false,
            partialTranscripts = true,
            timestamps = true,
            confidence = false,
            vocabularyBias = false,
            pricing = Pricing.FREE,
            maxAudioDurationSeconds = 60,
        )
    }
}