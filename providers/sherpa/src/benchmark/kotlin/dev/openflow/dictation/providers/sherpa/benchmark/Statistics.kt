package dev.openflow.dictation.providers.sherpa.benchmark

/**
 * Summary statistics over the measured iterations of one benchmark cell.
 *
 * `model-selection.md`'s *Method* fixes the protocol this implements: 5 warmup
 * decodes discarded, then the **median** of at least 20 iterations. The median is
 * the headline because a phone thermally throttles partway through a 20-iteration
 * sweep and RTF climbs as it does — a mean would report that climb as if it were
 * the model's cost. The spread is kept next to the median anyway, because a cell
 * whose p90 is three times its median is a different measurement from one where
 * they agree, and a single number cannot say which.
 *
 * A percentile here is **nearest-rank**, never interpolated. With 20 samples,
 * an interpolated p90 is a value no iteration produced, and the whole point of
 * this harness is to report what was measured. Nearest-rank always returns a
 * sample that actually happened.
 *
 * The empty case returns `null` rather than a zeroed [Summary]. That is the same
 * rule the acceptance gate's status block follows — empty is the absence of
 * evidence, and a run that measured nothing has not measured zero.
 */
data class Summary(
    val n: Int,
    val min: Double,
    val median: Double,
    val p90: Double,
    val max: Double,
    val mean: Double,
) {
    companion object {
        /** `null` when [samples] is empty. */
        fun of(samples: List<Double>): Summary? {
            if (samples.isEmpty()) return null
            val sorted = samples.sorted()
            return Summary(
                n = sorted.size,
                min = sorted.first(),
                median = median(sorted),
                p90 = percentile(sorted, 0.90),
                max = sorted.last(),
                mean = sorted.sum() / sorted.size,
            )
        }

        /**
         * Median of an already-sorted list. Even counts average the two middle
         * samples — the one place a reported value is not an observation, and the
         * convention every reader of a median expects.
         */
        fun median(sorted: List<Double>): Double {
            require(sorted.isNotEmpty()) { "median of an empty sample" }
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) {
                sorted[mid]
            } else {
                (sorted[mid - 1] + sorted[mid]) / 2.0
            }
        }

        /**
         * Nearest-rank percentile of an already-sorted, non-empty list.
         *
         * `sorted[ceil(p * n) - 1]`, clamped into range, so `p = 0` is the
         * smallest sample and `p = 1` the largest.
         */
        fun percentile(sorted: List<Double>, p: Double): Double {
            require(sorted.isNotEmpty()) { "percentile of an empty sample" }
            require(p in 0.0..1.0) { "percentile $p is not in 0..1" }
            val rank = Math.ceil(p * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
    }
}
