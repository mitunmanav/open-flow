package dev.openflow.dictation.providers.sherpa.benchmark

import java.io.StringReader
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The run manifest's reader, exercised on the ways a person filling one in gets it wrong.
 *
 * The manifest is typed by a human holding a phone, once, before a run that takes tens of
 * minutes. So the reader's job is not to be forgiving — a benchmark that recovers from a typo
 * reports a clean run over a matrix nobody agreed to — and these tests are mostly about which
 * mistakes stop the run loudly instead.
 */
class BenchmarkManifestTest {

    private fun base(): Properties = Properties().apply {
        setProperty("device.name", "Pixel 6a")
        setProperty("device.tier", "mid")
        setProperty("device.abi", "arm64-v8a")
        setProperty("device.oem_skin", "stock")
        setProperty("device.android_major", "14")
        setProperty("eval_set.dir", "/sdcard/openflow-eval")
        setProperty("eval_set.unit", "WORD")
        setProperty("vad.model", "/sdcard/openflow-models/silero_vad.onnx")
        setProperty("threads", "2, 4")
        setProperty("model.1.id", "streaming-zipformer-en-20M-2023-02-17")
        setProperty("model.1.dir", "/sdcard/openflow-models/en-20M")
        setProperty("model.1.encoder", "encoder.int8.onnx")
        setProperty("model.1.decoder", "decoder.int8.onnx")
        setProperty("model.1.joiner", "joiner.int8.onnx")
        setProperty("model.1.tokens", "tokens.txt")
    }

    private fun parse(props: Properties): BenchmarkManifest =
        BenchmarkManifestReader.parse(StringReader(propsToText(props)))

    private fun propsToText(props: Properties): String =
        props.stringPropertyNames().sorted().joinToString("\n") { "$it = ${props.getProperty(it)}" }

    private fun expectRejected(props: Properties, because: String) {
        try {
            parse(props)
            fail("expected the manifest to be rejected because $because")
        } catch (e: BenchmarkConfigurationException) {
            assertTrue(
                "the message must say what to fix, was: ${e.message}",
                e.message!!.isNotBlank(),
            )
        }
    }

    // ---- the happy path -------------------------------------------------------------

    @Test
    fun aCompleteManifestParsesAndCarriesThePlanProtocol() {
        val manifest = parse(base())
        assertEquals("Pixel 6a", manifest.device.name)
        assertEquals(DeviceTier.MID, manifest.device.tier)
        assertEquals(listOf(2, 4), manifest.protocol.threadCounts)
        assertEquals(5, manifest.protocol.warmupIterations)
        assertEquals(20, manifest.protocol.measuredIterations)
        assertEquals(100, manifest.protocol.chunkMillis)
        assertEquals("cpu", manifest.protocol.executionProvider)
        assertEquals(10, manifest.batterySessionMinutes)
        assertEquals(1, manifest.models.size)
    }

    @Test
    fun modelPathsAreComposedFromTheDirectoryAndTheRole() {
        val manifest = parse(base())
        val entry = manifest.models.single()
        assertEquals(
            "/sdcard/openflow-models/en-20M/encoder.int8.onnx",
            entry.pathOf("encoder"),
        )
        assertEquals(512, manifest.vad.windowSize)
    }

    @Test
    fun duplicateThreadCountsCollapse() {
        val props = base()
        props.setProperty("threads", "4, 2, 4")
        assertEquals(listOf(4, 2), parse(props).protocol.threadCounts)
    }

    // ---- the protocol the plan fixes ------------------------------------------------

    @Test
    fun fewerThanTwentyMeasuredIterationsIsRejected() {
        // The thresholds were written against a median of ≥20. A median over 8 is a different
        // quantity, and the run would print the plan's sentence over it.
        val props = base()
        props.setProperty("measured_iterations", "8")
        expectRejected(props, "measured_iterations is below the plan's floor")
    }

    @Test
    fun anExecutionProviderOtherThanCpuIsRejected() {
        // QNN and RKNN are excluded from V1. Benchmarking them here would put a number in the
        // results document for a path no shipped device takes.
        val props = base()
        props.setProperty("provider", "qnn")
        expectRejected(props, "the plan fixes provider=cpu")
    }

    @Test
    fun trailingSilenceShorterThanTheEndpointRuleIsRejected() {
        // `EndpointRule.rule1.minTrailingSilence` is 2.4 s, verified with `javap -c` against the
        // artifact. Padding less than it and then reporting endpoint→Final would be measuring
        // how long it takes to manufacture an endpoint.
        val props = base()
        props.setProperty("trailing_silence_ms", "1000")
        expectRejected(props, "the tail is shorter than the endpoint rule allows")
    }

    @Test
    fun aChunkOfZeroMillisecondsIsRejected() {
        val props = base()
        props.setProperty("chunk_ms", "0")
        expectRejected(props, "chunk_ms must be positive")
    }

    // ---- the matrix ------------------------------------------------------------------

    @Test
    fun anUnknownModelIdNamesTheOnesThatExist() {
        base().setProperty("model.1.id", "whisper-medium")
        try {
            parse(base())
            fail("expected the manifest to be rejected")
        } catch (e: BenchmarkConfigurationException) {
            assertTrue(e.message!!.contains("whisper-medium"))
            assertTrue(
                "the message must list what does exist, was: ${e.message}",
                e.message!!.contains("parakeet-tdt-0.6b-v2"),
            )
        }
    }

    @Test
    fun aModelMissingARequiredRoleIsRejected() {
        // `joiner` is what makes a transducer a transducer; a recognizer built without it is
        // built wrong rather than built loosely.
        val props = base()
        props.remove("model.1.joiner")
        expectRejected(props, "the transducer's joiner is missing")
    }

    @Test
    fun aMoonshineEntryNeedsOneOfItsTwoDecoderShapes() {
        // `OfflineMoonshineModelConfig` takes five paths — preprocessor, encoder,
        // uncachedDecoder, cachedDecoder, mergedDecoder — and the releases use either the
        // split pair or the single merged file. A manifest naming one `decoder` file fits
        // neither, and a recognizer handed three of the five will not transcribe.
        fun moonshine(): Properties = base().apply {
            remove("model.1.id")
            setProperty("model.1.id", "moonshine-tiny-en-int8")
            remove("model.1.joiner")
            setProperty("model.1.preprocessor", "preprocessor.onnx")
        }

        expectRejected(moonshine(), "Moonshine needs a merged_decoder or the split pair")

        val halfSpecified = moonshine().apply { setProperty("model.1.uncached_decoder", "u.onnx") }
        try {
            parse(halfSpecified)
            fail("expected the manifest to be rejected")
        } catch (e: BenchmarkConfigurationException) {
            assertTrue(e.message!!.contains("uncached_decoder"))
            assertTrue("and it must say a single one is not enough", e.message!!.contains("neither of them"))
        }

        val split = moonshine().apply {
            setProperty("model.1.uncached_decoder", "u.onnx")
            setProperty("model.1.cached_decoder", "c.onnx")
        }
        val entry = parse(split).models.single()
        assertTrue(entry.files.keys.containsAll(BenchmarkManifestReader.MOONSHINE_SPLIT_DECODER_ROLES))
        assertEquals(RecognizerFamily.MOONSHINE, entry.candidate.family)

        val merged = moonshine().apply { setProperty("model.1.merged_decoder", "m.onnx") }
        assertTrue(parse(merged).models.single().files.keys.contains("merged_decoder"))
    }

    @Test
    fun modelIndicesMustStartAtOne() {
        val props = base()
        props.setProperty("model.2.id", "whisper-tiny.en")
        props.setProperty("model.2.dir", "/sdcard/openflow-models/whisper")
        props.setProperty("model.2.encoder", "e.onnx")
        props.setProperty("model.2.decoder", "d.onnx")
        props.setProperty("model.2.tokens", "tokens.txt")
        props.setProperty("model.2.language", "en")
        props.setProperty("model.2.task", "transcribe")
        expectRejected(props, "indices must start at 1")
    }

    @Test
    fun aManifestWithNoModelsIsRejected() {
        val props = base()
        props.stringPropertyNames().filter { it.startsWith("model.") }.forEach { props.remove(it) }
        expectRejected(props, "it lists no models")
    }

    @Test
    fun anOfflineFamilyNeedsNoStreamingOnlyRole() {
        val props = base()
        props.remove("model.1.joiner")
        props.setProperty("model.1.id", "sensevoice-zh-en-ja-ko-yue-2024-07-17")
        props.setProperty("model.1.model", "model.int8.onnx")
        props.setProperty("model.1.language", "auto")
        val entry = parse(props).models.single()
        assertEquals(ScoringStrategy.VAD_SEGMENTED, entry.candidate.family.scoringStrategy)
        assertTrue(entry.files.keys.contains("model"))
    }

    // ---- the eval set ----------------------------------------------------------------

    @Test
    fun theScoreUnitMustBeWordOrCharacter() {
        // The unit is the eval set's property. Two of the eight candidates are bilingual, so a
        // harness that inferred it would compare two different quantities in one table.
        base().setProperty("eval_set.unit", "syllable")
        expectRejected(base(), "eval_set.unit must be WORD or CHARACTER")
    }

    @Test
    fun theLibrispeechLayoutNeedsItsTranscriptNamed() {
        // LibriSpeech test-clean ships one `test-clean.trans.txt` for the whole set, not a
        // sidecar per audio file. Asking for that layout without naming the file would leave
        // every utterance unscorable.
        val missing = base()
        missing.setProperty("eval_set.reference", "librispeech")
        expectRejected(missing, "the librispeech layout needs eval_set.transcript")

        val props = base()
        props.setProperty("eval_set.reference", "librispeech")
        props.setProperty("eval_set.transcript", "/sdcard/openflow-eval/test-clean.trans.txt")
        val manifest = parse(props)
        assertEquals(ReferenceLayout.LIBRISPEECH_TRANSCRIPT, manifest.evalSet.reference)
        assertEquals("/sdcard/openflow-eval/test-clean.trans.txt", manifest.evalSet.transcriptPath)
    }

    @Test
    fun anUnknownReferenceLayoutNamesTheTwoThatExist() {
        base().setProperty("eval_set.reference", "kaldi")
        try {
            parse(base())
            fail("expected the manifest to be rejected")
        } catch (e: BenchmarkConfigurationException) {
            assertTrue(e.message!!.contains("per-file"))
            assertTrue(e.message!!.contains("librispeech"))
        }
    }

    @Test
    fun anUnknownTierNamesTheThreeThatExist() {
        base().setProperty("device.tier", "flagship")
        try {
            parse(base())
            fail("expected the manifest to be rejected")
        } catch (e: BenchmarkConfigurationException) {
            assertTrue(e.message!!.contains("weakest"))
        }
    }

    // ---- everything is required ------------------------------------------------------

    @Test
    fun everyMissingKeyIsAStopRatherThanADefault() {
        for (key in listOf("device.name", "device.tier", "device.abi", "device.oem_skin", "device.android_major", "eval_set.dir", "vad.model", "threads")) {
            val props = base()
            props.remove(key)
            expectRejected(props, "$key is missing")
        }
    }

    // ---- the template ----------------------------------------------------------------

    @Test
    fun theTemplateNamesEveryCandidateAndThePlansProtocol() {
        // Generated from the matrix rather than maintained beside it, so it cannot name a
        // candidate the harness does not know.
        val template = BenchmarkManifestReader.template()
        for (candidate in ModelMatrix.CANDIDATES) {
            assertTrue(
                "the manifest template does not name ${candidate.id}",
                Regex("^model\\.\\d+\\.id = ${Regex.escape(candidate.id)}$", RegexOption.MULTILINE).containsMatchIn(template),
            )
        }
        assertTrue(template.contains("threads = 2, 4"))
        assertTrue(template.contains("warmup_iterations = 5"))
        assertTrue(template.contains("measured_iterations = 20"))
        assertTrue(template.contains("provider = cpu"))
        assertTrue(template.contains("vad.window_size = 512"))
        assertTrue(template.contains("eval_set.reference = per-file"))
    }

    @Test
    fun theTemplateWarnsThatTheModelFileNamesAreDeliberatelyBlank() {
        // Four wrong filenames shipped in prose once already (ticket 32), one of which
        // described code that would not compile. The template refuses to guess.
        val template = BenchmarkManifestReader.template()
        assertTrue(template.contains("file names are deliberately BLANK"))
        assertTrue(template.contains("int8 only"))
        assertTrue(template.contains("adb push"))
    }

    @Test
    fun theTemplatePointsAtTheRunProcedure() {
        assertTrue(
            BenchmarkManifestReader.template().contains("docs/providers/model-benchmark-harness.md"),
        )
    }
}
