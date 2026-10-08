package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The eval set's two reference layouts, and its refusal to score a partial corpus.
 *
 * Both layouts exist because LibriSpeech test-clean — the set `model-selection.md` names for
 * English — ships a single `test-clean.trans.txt` of `utterance-id text` lines covering the whole
 * set, which is a different shape from the per-file `.txt` sidecar a hand-recorded zh/en
 * dictation set uses. Supporting one would leave the other unscoreable, and an unscoreable WER
 * column is worse than a missing one because it looks like a zero.
 *
 * Every failure here is a **throw**, and [aMissingSidecarStopsTheRunRatherThanShorteningTheCorpus]
 * is the one that matters: dropping an utterance and scoring the rest produces an arithmetically
 * valid WER over a corpus that is not the one the plan names.
 */
class EvalSetCatalogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun wav(name: String) = folder.newFile(name).writeBytes(ByteArray(64))

    private fun text(name: String, body: String) = folder.newFile(name).writeText(body)

    private fun catalog(
        reference: ReferenceLayout = ReferenceLayout.PER_FILE_SIDECAR,
        transcript: File? = null,
        pattern: String = "*.wav",
    ) = EvalSetCatalog(folder.root, pattern, reference, transcript, ScoreUnit.WORD)

    // ---- the per-file layout --------------------------------------------------------

    @Test
    fun sidecarLayoutPairsEachWavWithItsOwnTxt() {
        wav("1.wav"); wav("2.wav")
        text("1.txt", "the first line"); text("2.txt", "the second line")
        val utterances = catalog().utterances()
        assertEquals(listOf("1", "2"), utterances.map { it.id })
        assertEquals("the first line", utterances[0].reference)
        assertEquals("2.wav", utterances[1].audio.name)
    }

    @Test
    fun utterancesAreSortedSoTwoRunsScoreTheSameCorpusInTheSameOrder() {
        wav("c.wav"); wav("a.wav"); wav("b.wav")
        listOf("a", "b", "c").forEach { text("$it.txt", "line $it") }
        assertEquals(listOf("a", "b", "c"), catalog().utterances().map { it.id })
    }

    @Test
    fun thePatternIsHonouredRatherThanHardcodedToWav() {
        wav("keep.flac"); wav("skip.wav")
        text("keep.txt", "kept"); text("skip.txt", "skipped")
        val utterances = catalog(pattern = "*.flac").utterances()
        assertEquals(listOf("keep"), utterances.map { it.id })
    }

    @Test
    fun aGlobIsMatchedNotTreatedAsALiteral() {
        wav("a.wav"); text("a.txt", "a")
        wav("ab.wav"); text("ab.txt", "ab")
        // `?` matches one character. Treated as a literal it would match nothing at all.
        assertEquals(listOf("a"), catalog(pattern = "?.wav").utterances().map { it.id })
    }

    // ---- the LibriSpeech layout -----------------------------------------------------

    @Test
    fun theLibrispeechLayoutReadsOneTranscriptForTheWholeSet() {
        wav("1089-0000-0000.wav"); wav("1089-0000-0001.wav")
        text("test-clean.trans.txt", "1089-0000-0000 THE FIRST LINE\n1089-0000-0001 THE SECOND LINE\n")
        val utterances = catalog(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, folder.root.resolve("test-clean.trans.txt")).utterances()
        assertEquals(listOf("1089-0000-0000", "1089-0000-0001"), utterances.map { it.id })
        // The corpus text is not case-folded here. Normalization is the scorer's job and it does
        // it once; doing it twice would make the corpus disagree with what the scorer saw.
        assertEquals("THE FIRST LINE", utterances[0].reference)
    }

    @Test
    fun blankAndUnpaddedTranscriptLinesAreHandled() {
        wav("a.wav")
        text("t.txt", "\n   \na   some text   \n")
        assertEquals("some text", catalog(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, folder.root.resolve("t.txt")).utterances().single().reference)
    }

    // ---- the refusals ---------------------------------------------------------------

    @Test
    fun aMissingSidecarStopsTheRunRatherThanShorteningTheCorpus() {
        wav("1.wav"); text("1.txt", "present")
        wav("2.wav")
        try {
            catalog().utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("2.wav"))
            assertTrue(e.message!!.contains("2.txt"))
        }
    }

    @Test
    fun anAudioFileWithNoTranscriptLineStopsTheRun() {
        wav("a.wav"); wav("b.wav")
        text("t.txt", "a only this one\n")
        try {
            catalog(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, folder.root.resolve("t.txt")).utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("b.wav"))
            assertTrue(e.message!!.contains("incomplete copy"))
        }
    }

    @Test
    fun anEmptyDirectoryIsAStopRatherThanAnEmptyCorpus() {
        // A run over zero utterances would report a WER of null, which reads as a failure rather
        // than as the absence of a corpus.
        try {
            catalog().utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("no file matching"))
        }
    }

    @Test
    fun aDirectoryThatIsNotThereNamesItself() {
        val missing = EvalSetCatalog(File(folder.root, "nope"), "*.wav", ReferenceLayout.PER_FILE_SIDECAR, null, ScoreUnit.WORD)
        try {
            missing.utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("nope"))
        }
    }

    @Test
    fun aTranscriptLineWithNoTextAfterTheIdIsRejected() {
        wav("a.wav")
        text("t.txt", "a\n")
        try {
            catalog(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, folder.root.resolve("t.txt")).utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("no 'id text' split"))
        }
    }

    @Test
    fun aMissingTranscriptFileNamesItself() {
        wav("a.wav")
        try {
            catalog(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, File(folder.root, "absent.txt")).utterances()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("absent.txt"))
        }
    }

    @Test
    fun aSidecarIsNotReadAsAudio() {
        wav("1.wav"); text("1.txt", "the words"); text("2.txt", "no audio for this one")
        val utterances = catalog().utterances()
        // The `.txt` beside a `.wav` is a reference, never a second utterance.
        assertEquals(1, utterances.size)
        assertEquals("the words", utterances.single().reference)
    }
}
