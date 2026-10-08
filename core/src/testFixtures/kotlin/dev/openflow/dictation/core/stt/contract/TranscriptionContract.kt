package dev.openflow.dictation.core.stt.contract

import dev.openflow.dictation.core.stt.FailureReason
import dev.openflow.dictation.core.stt.PrepareResult
import dev.openflow.dictation.core.stt.SpeechEvent
import dev.openflow.dictation.core.stt.SpeechProvider
import dev.openflow.dictation.core.stt.TranscriptionRequest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The half of the Contract Test suite that needs a real utterance.
 *
 * [SpeechProviderContract] is the part that always runs: health, lifecycle, the
 * `READY`/health invariant, the `MODEL_MISSING` pairing. This class is the part
 * that needs the engine to actually decode something, so it is a separate class
 * rather than more tests in that one — a provider whose model is not available
 * can still run the always-on suite, and folding these in would either fail it
 * for a machine problem or make them skippable, and a skippable assertion is an
 * assertion that proves nothing.
 *
 * ### Before you subclass this
 *
 * The provider must be constructible with a *fixture* audio source — a
 * waveform it reads instead of the microphone — because a unit test has no
 * microphone and the contract has no `AudioSource` type for it to be handed.
 * That is the same seam `docs/providers/provider-authoring.md` describes: where
 * the audio comes from is the adapter's construction, wired in `app`.
 *
 * Unlike the class above, nothing here degrades gracefully. A provider that
 * cannot prepare fails, because a module whose Contract Tests never ran is
 * exactly the defect ticket 27's compile-time probe was written to catch, one
 * layer up.
 *
 * ### What this suite cannot check
 *
 * [dev.openflow.dictation.core.stt.Capabilities.confidence] has no counterpart
 * anywhere in [SpeechEvent] — `Final` carries text and two timestamps and
 * nothing else (ADR-0001) — so a provider that declared `confidence = true`
 * would be making a declaration this contract gives no consumer a way to check.
 * Nothing here asserts it. That is a gap in the contract rather than an
 * oversight in the suite, and it is recorded in ADR-0001's amendment history:
 * whoever adds confidence support has to change `Final` first, which is a change
 * to every adapter and therefore a proposal.
 */
abstract class TranscriptionContract {

    /** A fresh provider, constructed over a fixture audio source. */
    protected abstract fun newProvider(): SpeechProvider

    /** A request this provider has declared it can serve. */
    protected abstract fun servableRequest(): TranscriptionRequest

    /**
     * Feed [seconds] seconds of fixture audio through [provider] for [request]
     * and return every event it emitted.
     *
     * The audio source is the only provider-specific thing here; the
     * expectations are not, which is why the assertions live in this class
     * rather than in the harness.
     */
    protected abstract fun collectUtteranceOfSeconds(
        provider: SpeechProvider,
        request: TranscriptionRequest,
        seconds: Int,
    ): List<SpeechEvent>

    /**
     * How many utterances this provider's audio source has actually delivered.
     *
     * The observable for the two things a provider cannot be asked about from
     * the outside: that collecting twice is two transcriptions, and that
     * cancelling one stops it.
     */
    protected abstract fun utterancesDelivered(): Int

    @Test
    fun aRequestTerminatesWithExactlyOneFinalOrFailure() {
        withPreparedProvider { provider ->
            val events = collect(provider, SHORT_UTTERANCE_SECONDS)
            val terminals = events.filter { it is SpeechEvent.Final || it is SpeechEvent.Failure }

            assertTrue(
                "a request produced ${terminals.size} terminal events ($terminals). " +
                    "ADR-0001 allows zero or one, and more than one means the " +
                    "Dictation would be recorded twice.",
                terminals.size <= 1,
            )
            assertEquals(
                "a fixture utterance is audio the provider claims to decode, so it " +
                    "must terminate rather than quietly complete. Events were $events",
                1,
                terminals.size,
            )
            assertTrue(
                "no Listening event in $events, so nothing was ever captured",
                events.any { it is SpeechEvent.Listening },
            )
        }
    }

    @Test
    fun aDeclaredStreamingProviderEmitsAPartialBeforeItsFinal() {
        withPreparedProvider { provider ->
            if (!provider.capabilities.partialTranscripts) return@withPreparedProvider

            val events = collect(provider, SHORT_UTTERANCE_SECONDS)
            val firstPartial = events.indexOfFirst { it is SpeechEvent.Partial }
            val finalAt = events.indexOfFirst { it is SpeechEvent.Final }

            assertTrue(
                "capabilities declared partialTranscripts = true but no Partial " +
                    "arrived in $events. An engine that only produces final output " +
                    "must declare it false — a correct answer the router can act on. " +
                    "Declaring it true is a false promise the UI believes, and it " +
                    "surfaces later as a live transcript that never updates.",
                firstPartial >= 0,
            )
            assertTrue(
                "a Partial arrived at index $firstPartial and the Final at $finalAt, " +
                    "so a snapshot the consumer has already replaced is not a " +
                    "partial. Events were $events",
                finalAt < 0 || firstPartial < finalAt,
            )
        }
    }

    @Test
    fun partialsAreCumulativeSnapshotsRatherThanDeltas() {
        withPreparedProvider { provider ->
            val partials = collect(provider, SHORT_UTTERANCE_SECONDS)
                .filterIsInstance<SpeechEvent.Partial>()
            if (partials.size < 2) return@withPreparedProvider

            partials.zipWithNext { earlier, later ->
                assertTrue(
                    "Partial snapshots must be cumulative — each one the whole " +
                        "utterance so far, not the words added since the last. " +
                        "\"${later.text}\" dropped text that \"${earlier.text}\" " +
                        "already contained, which reads correctly in isolation and " +
                        "produces a garbled live transcript once conflated " +
                        "downstream.",
                    later.text.startsWith(earlier.text),
                )
            }
        }
    }

    @Test
    fun everyDeclaredLanguageIsServable() {
        withPreparedProvider { provider ->
            // The whole declared set, not a sample: an engine that handles en-US
            // and quietly fails en-GB is the common real shape of this bug, and
            // the router matches on exact tags, so a declared tag that fails is a
            // capability the app believed and did not get.
            provider.capabilities.supportedLanguages.forEach { language ->
                val request = servableRequest().copy(language = language)
                val events = collect(provider, SHORT_UTTERANCE_SECONDS, request)
                val failure = events.filterIsInstance<SpeechEvent.Failure>().singleOrNull()
                assertTrue(
                    "capabilities declared \"$language\" as supported but " +
                        "transcribing it failed with ${failure?.reason} " +
                        "(recoverable = ${failure?.recoverable}). A declared language " +
                        "must not fail UnsupportedLanguage; events were $events",
                    failure?.reason != FailureReason.UnsupportedLanguage,
                )
            }
        }
    }

    @Test
    fun finalTimestampsBracketTheAudioActuallySupplied() {
        withPreparedProvider { provider ->
            val events = collect(provider, SHORT_UTTERANCE_SECONDS)
            val final = events.filterIsInstance<SpeechEvent.Final>().singleOrNull()
                ?: return@withPreparedProvider

            assertTrue(
                "Final ended at ${final.endedAtMs}ms, before it started at " +
                    "${final.startedAtMs}ms",
                final.endedAtMs >= final.startedAtMs,
            )
            if (provider.capabilities.timestamps) {
                assertTrue(
                    "capabilities declared timestamps = true, so the reported span " +
                        "(${final.startedAtMs}..${final.endedAtMs}ms) must be " +
                        "consistent with the $SHORT_UTTERANCE_SECONDS s of audio that " +
                        "was supplied. Final Latency is derived from these two " +
                        "numbers, and a synthesised pair makes the Latency Guard " +
                        "watch a value that was never measured.",
                    final.endedAtMs - final.startedAtMs >=
                        SHORT_UTTERANCE_SECONDS * 1_000L,
                )
            }
            assertTrue(
                "Final carried empty text. A capture that heard nothing should end " +
                    "with no terminal event at all rather than hand the refiner and " +
                    "the inserter an empty transcript.",
                final.text.isNotEmpty(),
            )
        }
    }

    @Test
    fun collectingTwiceStartsTwoTranscriptions() {
        withPreparedProvider { provider ->
            val request = servableRequest()
            val before = utterancesDelivered()

            val first = runBlocking {
                withTimeout(UTTERANCE_TIMEOUT_MS) { provider.transcribe(request).toList() }
            }
            val afterFirst = utterancesDelivered()

            val second = runBlocking {
                withTimeout(UTTERANCE_TIMEOUT_MS) { provider.transcribe(request).toList() }
            }
            val afterSecond = utterancesDelivered()

            assertEquals(
                "the first collection consumed ${afterFirst - before} utterances; a " +
                    "collection that is never wired to an audio source cannot prove " +
                    "anything about this provider",
                1,
                afterFirst - before,
            )
            assertEquals(
                "collecting a second time consumed ${afterSecond - afterFirst} " +
                    "utterances. That is what cold means, and the check is on audio " +
                    "actually delivered rather than on two Flow objects being " +
                    "distinct instances.",
                1,
                afterSecond - afterFirst,
            )
            assertFalse(
                "the two collections disagreed: first was $first, second was $second",
                first.isEmpty() || second.isEmpty(),
            )
        }
    }

    @Test
    fun cancellingTheCollectionStopsTheProvider() {
        withPreparedProvider { provider ->
            val before = utterancesDelivered()

            // Take the first event and stop: the rest of the utterance is never
            // collected. The engine must notice, because the native side holds
            // audio buffers and a decode thread that a listener which merely
            // closed would leave running.
            runBlocking {
                withTimeout(UTTERANCE_TIMEOUT_MS) {
                    provider.transcribe(servableRequest()).take(1).toList()
                }
            }
            val afterCancel = utterancesDelivered()

            assertTrue(
                "cancelling the collection consumed ${afterCancel - before} " +
                    "utterances; the engine kept transcribing with nobody collecting",
                afterCancel - before <= 1,
            )

            // And the instance is still usable, which is what "cancelled" has to
            // mean rather than "broken".
            val events = runBlocking {
                withTimeout(UTTERANCE_TIMEOUT_MS) {
                    provider.transcribe(servableRequest()).toList()
                }
            }
            assertFalse(
                "after a cancelled collection the next request failed with $events; " +
                    "cancelling must free the engine, not exhaust it",
                events.any { it is SpeechEvent.Failure },
            )
        }
    }

    @Test
    fun maxAudioDurationSecondsIsACeilingTheProviderCanActuallyServe() {
        withPreparedProvider { provider ->
            val ceiling = provider.capabilities.maxAudioDurationSeconds
            val events = collect(provider, ceiling)
            val failure = events.filterIsInstance<SpeechEvent.Failure>().singleOrNull()

            assertTrue(
                "capabilities declared maxAudioDurationSeconds = $ceiling, but " +
                    "transcribing that many seconds of audio failed with " +
                    "${failure?.reason}. The controller enforces this as a real " +
                    "ceiling and the router computes the cost worst case from it, so " +
                    "a ceiling the provider cannot reach is a number the rest of the " +
                    "app acts on and cannot honour.",
                failure == null,
            )
        }
    }

    private fun collect(
        provider: SpeechProvider,
        seconds: Int,
        request: TranscriptionRequest = servableRequest(),
    ): List<SpeechEvent> = runBlocking {
        withTimeout(UTTERANCE_TIMEOUT_MS) {
            collectUtteranceOfSeconds(provider, request, seconds)
        }
    }

    /**
     * Run [block] against a freshly constructed provider that prepared
     * successfully.
     *
     * Fails rather than skipping when preparation does not succeed: a provider
     * whose Contract Tests never executed is the defect this suite exists to
     * prevent, and the fix is to make the fixture model available — not to have
     * the suite report success without having run.
     */
    private fun withPreparedProvider(block: (SpeechProvider) -> Unit) {
        val provider = newProvider()
        try {
            val prepared = runBlocking { provider.prepare() }
            check(prepared is PrepareResult.Ready) {
                "TranscriptionContract needs a provider that prepares, and this one " +
                    "returned $prepared. Make the fixture model available to the unit " +
                    "test — for a downloaded model that means the test provides it, " +
                    "or the harness points at a known fixture copy. Do not weaken " +
                    "the suite to accommodate this."
            }
            block(provider)
        } finally {
            provider.close()
        }
    }

    private companion object {
        /** Long enough to be an utterance, short enough to keep the suite quick. */
        const val SHORT_UTTERANCE_SECONDS = 1

        /** Generous: shared CI machines are slow, and a hang should fail not park. */
        const val UTTERANCE_TIMEOUT_MS = 60_000L
    }
}