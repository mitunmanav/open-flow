package dev.openflow.dictation.core.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Capabilities]'s own invariants, plus the `0` / `null` distinction that has a
 * money consequence.
 *
 * `docs/providers/provider-testing.md` spends a section on the pricing trap, and
 * the trap's whole difficulty is that it is *not* a type error — an adapter that
 * coerces `null` to `0` compiles, passes every structural check here, and then
 * passes every cost-ceiling check while billing real money. So these tests
 * cannot fully close it; what they can do is pin the two answers apart so that
 * the coercion has somewhere visible to happen.
 */
class CapabilitiesTest {

    private fun capabilities(
        supportedLanguages: Set<String> = setOf("en-US"),
        pricing: Pricing? = Pricing.FREE,
        maxAudioDurationSeconds: Int = 30,
        partialTranscripts: Boolean = false,
        streaming: Boolean = false,
    ) = Capabilities(
        streaming = streaming,
        offline = true,
        supportedLanguages = supportedLanguages,
        autoDetectLanguage = false,
        partialTranscripts = partialTranscripts,
        timestamps = true,
        confidence = false,
        vocabularyBias = false,
        pricing = pricing,
        maxAudioDurationSeconds = maxAudioDurationSeconds,
    )

    @Test
    fun aProviderThatCanServeNoLanguageIsRejectedAtConstruction() {
        val thrown = assertThrows { capabilities(supportedLanguages = emptySet()) }
        assertTrue(
            "the message has to say what to do, not just that something was wrong: $thrown",
            thrown.contains("supportedLanguages"),
        )
    }

    @Test
    fun aNonPositiveDurationCeilingIsRejectedAtConstruction() {
        // The controller enforces this value as a real ceiling, so zero would
        // refuse every utterance and a negative one would be nonsense. Catching
        // it at construction rather than on the first dictation is the whole
        // difference between a build error and a user pressing a button.
        assertThrows { capabilities(maxAudioDurationSeconds = 0) }
        assertThrows { capabilities(maxAudioDurationSeconds = -1) }
    }

    @Test
    fun freeAndUnknownAreDifferentAnswers() {
        val free = capabilities(pricing = Pricing.FREE)
        val unknown = capabilities(pricing = null)

        assertEquals(Pricing(0L), free.pricing)
        assertNull(
            "pricing = null means cannot-estimate and must stay null. Defaulting " +
                "it to zero would make a metered cloud API look free and pass " +
                "every cost-ceiling check while billing real money (ADR-0001).",
            unknown.pricing,
        )
        assertFalse(
            "Pricing.FREE must not be equal to the absence of a price",
            free.pricing == unknown.pricing,
        )
    }

    @Test
    fun aNegativeRateIsRejected() {
        // 0 is the honest value for a free provider and null is the honest value
        // for one that cannot estimate; neither is reachable by arithmetic on a
        // price, so a negative is a bug rather than an answer.
        assertThrows { Pricing(-1L) }
    }

    @Test
    fun worstCaseIsTheDeclaredRateAgainstTheWholeCeiling() {
        // ADR-0004 rule 4 compares a bound rather than an estimate, because the
        // router commits before any sample exists. Multiplying by the declared
        // ceiling is what makes the bound hold from the moment a provider is
        // chosen.
        assertEquals(0L, Pricing.FREE.worstCaseMicrosUsd(maxAudioDurationSeconds = 3600))
        assertEquals(
            15_000L,
            Pricing(microsUsdPerSecond = 10L).worstCaseMicrosUsd(maxAudioDurationSeconds = 1500),
        )
    }

    @Test
    fun supportsMatchesDeclaredLanguagesExactly() {
        val provider = capabilities(supportedLanguages = setOf("en-US"))

        assertTrue(provider.supports("en-US"))
        assertFalse(
            "the router matches exact tags, so a provider serving en-US has not " +
                "declared en-GB — and declaring en-GB then failing it is the bug " +
                "provider-testing.md calls the common real shape of this failure",
            provider.supports("en-GB"),
        )
    }

    @Test
    fun everyCapabilityIsRequiredRatherThanDefaulted() {
        // Not a runtime test — a structural one. The reason `Capabilities` has no
        // default parameter values is that a default makes an undeclared
        // capability indistinguishable from a declared one, and Capabilities
        // Honesty is the rule the Contract Test suite exists to enforce. A
        // default would make that rule unenforceable by construction, so its
        // absence is a decision rather than an oversight.
        val declared = Capabilities::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(
            "ADR-0001 names ten capabilities; a rename or an addition changes what " +
                "every adapter declares and what the Contract Test suite asserts, " +
                "so it needs an ADR revision rather than arriving silently. Saw " +
                "$declared",
            setOf(
                "streaming",
                "offline",
                "supportedLanguages",
                "autoDetectLanguage",
                "partialTranscripts",
                "timestamps",
                "confidence",
                "vocabularyBias",
                "pricing",
                "maxAudioDurationSeconds",
            ),
            declared,
        )

        assertTrue(
            "every capability constructor parameter must be required. A default " +
                "value here is an undeclared capability that reads as declared, " +
                "which is the one error Capabilities Honesty cannot detect " +
                "downstream.",
            Capabilities::class.java.declaredConstructors
                .single { it.parameterCount == 10 }
                .parameters
                .none { it.isOptional },
        )
    }

    private fun assertThrows(block: () -> Unit): String {
        return try {
            block()
            throw AssertionError("expected this to be rejected and it was accepted")
        } catch (expected: IllegalArgumentException) {
            expected.message.orEmpty()
        }
    }
}