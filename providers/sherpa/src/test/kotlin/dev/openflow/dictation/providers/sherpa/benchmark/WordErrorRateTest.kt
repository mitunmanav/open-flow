package dev.openflow.dictation.providers.sherpa.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The WER scorer, including the family-specific normalization it exists for.
 *
 * These are the numbers the model default is chosen from, so each case here is one a family
 * actually produces: capitals from the zipformers, punctuation from Whisper, rich-transcription
 * tags from SenseVoice, and no spaces anywhere for the Chinese and Japanese candidates.
 */
class WordErrorRateTest {

    @Test
    fun anExactMatchScoresZero() {
        val rate = WordErrorRate.score("the quick brown fox", "the quick brown fox", ScoreUnit.WORD)!!
        assertEquals(0.0, rate.errorPercent, 1e-9)
        assertEquals(0, rate.errors)
        assertEquals(4, rate.referenceUnits)
    }

    @Test
    fun zipformerCapitalsDoNotCountAsFourSubstitutions() {
        // `stream/zipformer` models ship an UPPERCASE tokens.txt and return `HELLO WORLD`.
        // LibriSpeech references are lower case. Unnormalized, this scores 200% on a model that
        // is working perfectly — which is the failure mode a scorer with no normalization has.
        val rate = WordErrorRate.score("HELLO WORLD", "hello world", ScoreUnit.WORD)!!
        assertEquals(0.0, rate.errorPercent, 1e-9)
    }

    @Test
    fun whisperPunctuationIsRemovedRatherThanNormalized() {
        // There is no single canonical punctuation to normalize *to*, so it goes. A comma that
        // is in the hypothesis and absent from the reference is not a recognition error.
        assertEquals(0.0, WordErrorRate.score("Hello, world.", "hello world", ScoreUnit.WORD)!!.errorPercent, 1e-9)
    }

    @Test
    fun senseVoiceRichTranscriptionTagsAreStripped() {
        // SenseVoice decorates its output: `<|en|><|NEUTRAL|><|Speech|><|woitn|>the text`.
        // sherpa-onnx strips these in some code paths and not others depending on version, so
        // leaving them in would count four "words" of markup per utterance.
        val rate = WordErrorRate.score("<|en|><|NEUTRAL|><|Speech|><|woitn|>the text", "the text", ScoreUnit.WORD)!!
        assertEquals(0.0, rate.errorPercent, 1e-9)
        assertEquals(2, rate.referenceUnits)
    }

    @Test
    fun chineseIsScoredPerCharacterBecauseItHasNoWordSpaces() {
        // `你好世界` is one whitespace-delimited token. Split on spaces, it scores as a single
        // substitution or deletion no matter what was said, so every reference comes out 0% or
        // 100% — a score column that looks populated and says nothing.
        val rate = WordErrorRate.score("你好世男", "你好世界", ScoreUnit.CHARACTER)!!
        assertEquals(1, rate.substitutions)
        assertEquals(25.0, rate.errorPercent, 1e-9)
        assertEquals(4, rate.referenceUnits)
        assertEquals("CER", rate.unit.label)
    }

    @Test
    fun chinesePunctuationIsDroppedRatherThanTreatedAsASeparator() {
        // `你好, 世界` and `你好世界` are the same utterance. Under character scoring a
        // separator would become a deletion; dropping it is the convention for the script.
        assertEquals(0.0, WordErrorRate.score("你好, 世界", "你好世界", ScoreUnit.CHARACTER)!!.errorPercent, 1e-9)
    }

    @Test
    fun digitsAreLeftAlone() {
        // `3` is not converted to `three`. No published WER applies that normalization, and a
        // score nobody else can reproduce is worth less than a slightly cruder one.
        val rate = WordErrorRate.score("room 3", "room three", ScoreUnit.WORD)!!
        assertEquals(1, rate.substitutions)
        assertEquals(50.0, rate.errorPercent, 1e-9)
    }

    @Test
    fun substitutionsDeletionsAndInsertionsAreCountedSeparately() {
        // A dictation app feels a dropped word and a hallucinated one differently: one loses the
        // user's content, the other puts words in their mouth, which is worse in a tool whose
        // claim is that it does not editorialize. A single edit distance hides which happened.
        assertEquals(1, WordErrorRate.score("a c", "a b c", ScoreUnit.WORD)!!.deletions)
        assertEquals(1, WordErrorRate.score("a b c d", "a b c", ScoreUnit.WORD)!!.insertions)
        assertEquals(1, WordErrorRate.score("a x c", "a b c", ScoreUnit.WORD)!!.substitutions)
    }

    @Test
    fun theMixIsRightWhenAllThreeHappen() {
        val rate = WordErrorRate.score("a x c e", "a b c", ScoreUnit.WORD)!!
        assertEquals(1, rate.substitutions)
        assertEquals(1, rate.insertions)
        assertEquals(0, rate.deletions)
        assertEquals(2, rate.errors)
    }

    @Test
    fun errorPercentIsAPercentageNotARatio() {
        // `model-selection.md` allows "+2 absolute", and "absolute" means two percentage points.
        // Storing a ratio here would make that comparison off by a factor of a hundred.
        assertEquals(100.0, WordErrorRate.score("b", "a", ScoreUnit.WORD)!!.errorPercent, 1e-9)
    }

    @Test
    fun anEmptyReferenceIsAbsentRatherThanZero() {
        // A division by zero. Any value invented for it — 0, 100, infinity — is a number nobody
        // measured, so it is null and the caller has to decide what an unscorable utterance means.
        assertNull(WordErrorRate.score("anything", "   ...   ", ScoreUnit.WORD))
    }

    @Test
    fun anEmptyHypothesisIsAllDeletions() {
        val rate = WordErrorRate.score("", "one two three", ScoreUnit.WORD)!!
        assertEquals(3, rate.deletions)
        assertEquals(0, rate.substitutions)
        assertEquals(0, rate.insertions)
        assertEquals(100.0, rate.errorPercent, 1e-9)
    }

    @Test
    fun theCorpusRateIsTotalErrorsOverTotalReferenceUnits() {
        // Not the mean of the per-utterance rates. A mean weights a two-word utterance equally
        // with a thirty-second one and is dominated by short utterances and by any single one
        // that went badly; the aggregate is the number every published WER reports.
        val utterances = listOf(
            WordErrorRate.score("a", "a", ScoreUnit.WORD)!!,             // 0/1
            WordErrorRate.score("x y z", "a b c d e f g h", ScoreUnit.WORD)!!, // 5 sub / 8
        )
        val aggregate = WordErrorRate.aggregate(utterances)!!
        assertEquals(9, aggregate.referenceUnits)
        assertEquals(5, aggregate.errors)
        assertEquals(5.0 * 100.0 / 9.0, aggregate.errorPercent, 1e-9)
        // The mean of the rates would be (0 + 62.5) / 2 = 31.25%, which is a different number.
        assertTrue("the aggregate must not be the mean of the rates", aggregate.errorPercent < 31.25)
    }

    @Test
    fun aggregatingNothingIsAbsent() {
        assertNull(WordErrorRate.aggregate(emptyList()))
    }

    @Test
    fun mixingUnitsIsRefused() {
        val word = WordErrorRate.score("a", "a", ScoreUnit.WORD)!!
        val character = WordErrorRate.score("a", "a", ScoreUnit.CHARACTER)!!
        try {
            WordErrorRate.aggregate(listOf(word, character))
            throw AssertionError("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("WER"))
        }
    }

    @Test
    fun theEditDistanceIsLevenshtein() {
        // Verified against the textbook cases so the rolling-row optimisation and the
        // attribution pass cannot disagree with the definition they implement.
        assertEquals(Triple(0, 0, 0), WordErrorRate.editDistance(listOf("a"), listOf("a")))
        assertEquals(Triple(1, 0, 0), WordErrorRate.editDistance(listOf("a"), listOf("b")))
        assertEquals(Triple(0, 1, 0), WordErrorRate.editDistance(listOf("a"), listOf()))
        assertEquals(Triple(0, 0, 1), WordErrorRate.editDistance(listOf(), listOf("a")))
        // kitten → sitting is 3 substitutions.
        assertEquals(3, WordErrorRate.editDistance("kitten".map(Char::toString), "sitting".map(Char::toString)).let {
            it.first + it.second + it.third
        })
        // flaw → lawn is 2 substitutions in one alignment.
        assertEquals(2, WordErrorRate.editDistance("flaw".map(Char::toString), "lawn".map(Char::toString)).let {
            it.first + it.second + it.third
        })
    }

    @Test
    fun normalizationIsExposedSoAScoreCanBeReproduced() {
        // A WER nobody can reproduce is not a measurement, and reproducing it means seeing
        // exactly what the scorer saw.
        assertEquals("hello world", WordErrorRate.normalize("  <|en|>HELLO,  World! ", ScoreUnit.WORD))
        assertEquals("你好世界", WordErrorRate.normalize("你好, 世界。", ScoreUnit.CHARACTER))
        assertEquals("WER", ScoreUnit.WORD.label)
    }
}
