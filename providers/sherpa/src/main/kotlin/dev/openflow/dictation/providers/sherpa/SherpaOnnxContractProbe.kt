package dev.openflow.dictation.providers.sherpa

import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream

/**
 * Compile-time proof that the sherpa-onnx AAR is actually consumable by this
 * project's toolchain, and nothing more.
 *
 * Declaring the dependency only proves the coordinate resolves. This function
 * forces the Kotlin compiler to read the AAR's Kotlin metadata, resolve two of its
 * classes, and resolve one member on each. That is the specific risk worth catching
 * here: the AAR is compiled against Kotlin 1.7.20 and this project compiles with
 * 2.0.21, and a mismatch would otherwise surface much later as a failure inside the
 * provider rather than as a red build in the ticket that introduced the dependency.
 *
 * It is `internal`, it has no callers, and it is not a working recognizer. The real
 * SpeechProvider implementation is application code and arrives with the feature.
 */
internal fun sherpaOnnxContractIsVisible(
    config: OnlineRecognizerConfig,
    stream: OnlineStream,
): Boolean = config.enableEndpoint && stream.ptr >= 0L