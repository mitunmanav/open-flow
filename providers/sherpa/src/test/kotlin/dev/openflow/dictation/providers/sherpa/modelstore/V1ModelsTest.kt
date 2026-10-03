package dev.openflow.dictation.providers.sherpa.modelstore

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * The V1 default model's pinned facts, as ratchets: each field that a
 * future edit must not change silently, and the constraints every
 * [ModelSpec] enforces at construction.
 */
class V1ModelsTest {

    @Test
    fun theV1DefaultIsTheMatrixFirstCandidate() {
        // ADR-0010 downloads the model the benchmark matrix's first row
        // measures ("streaming-zipformer-en-20M-2023-02-17", the
        // `sherpa-onnx-` release asset of that candidate id). The two
        // must stay the same model; the benchmark harness (ticket 40)
        // owns the matrix in code, so the identity is asserted here
        // rather than cross-referenced — a test that compiles against
        // another ticket's in-flight source set is a test that cannot
        // run until that ticket lands.
        assertEquals(
            "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
            V1Models.STREAMING_EN_20M.name
        )
    }

    @Test
    fun theV1DefaultLoadsTheInt8Variant() {
        // Measured from the release archive (ticket 46): the tarball
        // carries fp32 and int8 variants, and the V1 default loads the
        // int8 trio plus the tokenizer.
        assertEquals(
            setOf(
                "encoder-epoch-99-avg-1.int8.onnx",
                "decoder-epoch-99-avg-1.int8.onnx",
                "joiner-epoch-99-avg-1.int8.onnx",
                "tokens.txt",
            ),
            V1Models.STREAMING_EN_20M.requiredEntries
                .map { V1Models.STREAMING_EN_20M.baseName(it) }
                .toSet()
        )
    }

    @Test
    fun thePinnedUrlIsTheAsrModelsRelease() {
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
                "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2",
            V1Models.STREAMING_EN_20M.archiveUrl
        )
    }

    @Test
    fun thePinnedHashIsTheMeasuredArchive() {
        // Computed from the 127,887,156-byte release asset on
        // 2026-10-03 (ticket 46). Changing this value is a supply-chain
        // decision: re-verify against the release and record it wherever
        // the ticket's answer lives.
        assertEquals(
            "9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9",
            V1Models.STREAMING_EN_20M.archiveSha256
        )
    }

    @Test
    fun aSpecRejectsEntriesOutsideTheModelDirectory() {
        try {
            ModelSpec(
                name = "m",
                archiveUrl = "https://example.test/m.tar.bz2",
                archiveSha256 = "a".repeat(64),
                requiredEntries = setOf("../escape"),
            )
            fail("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // the entry escapes the model's top-level directory
        }
    }

    @Test
    fun aSpecRejectsAMalformedHash() {
        try {
            ModelSpec(
                name = "m",
                archiveUrl = "https://example.test/m.tar.bz2",
                archiveSha256 = "not-a-hash",
                requiredEntries = setOf("m/tokens.txt"),
            )
            fail("expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // the hash is not 64 hex characters
        }
    }
}
