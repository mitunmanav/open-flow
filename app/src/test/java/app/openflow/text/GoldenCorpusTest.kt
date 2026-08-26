package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Runs [CleanupPipeline] over the golden corpus and reports word-level
 * precision / recall / F1 per category (micro-averaged).
 *
 * Gate: every category must stay at or above its pinned value in
 * baseline.json, and `notouch` cases must come out byte-identical
 * (after whitespace normalization) — no invented or destroyed words.
 *
 * To (re)pin the baseline: run this test, copy the printed
 * `BASELINE` JSON into baseline.json, and re-run green.
 */
class GoldenCorpusTest {

    private val eps = 0.005f

    @Test
    fun corpus_scores_meet_baseline_and_notouch_is_untouched() {
        val cases = GoldenCorpus.load()
        assertThat(cases.size).isAtLeast(200)
        assertThat(cases.any { it.category == "notouch" }).isTrue()

        val byCategory = cases.groupBy { it.category }
        val baseline = loadBaseline()
        val scores = mutableMapOf<String, Score>()

        for ((cat, list) in byCategory) {
            var matched = 0
            var predicted = 0
            var gold = 0
            for (c in list) {
                val clean = CleanupPipeline.run(c.raw, level = c.level).clean
                if (System.getenv("OPENFLOW_GOLDEN_DEBUG") != null && clean != c.expected) {
                    println("DIFF|||$cat|||${c.raw}|||${c.expected}|||$clean")
                }
                val a = words(clean)
                val b = words(c.expected)
                val l = lcs(a, b)
                matched += l
                predicted += a.size
                gold += b.size
            }
            val score = Score(
                p = if (predicted == 0) 0f else matched.toFloat() / predicted,
                r = if (gold == 0) 0f else matched.toFloat() / gold,
                f1 = f1(matched, predicted, gold),
            )
            scores[cat] = score
        }

        var mMatched = 0; var mPredicted = 0; var mGold = 0
        for (c in cases.filter { it.category != "notouch" }) {
            val clean = CleanupPipeline.run(c.raw, level = c.level).clean
            val a = words(clean); val b = words(c.expected)
            val l = lcs(a, b)
            mMatched += l; mPredicted += a.size; mGold += b.size
        }
        val all = Score(
            p = if (mPredicted == 0) 0f else mMatched.toFloat() / mPredicted,
            r = if (mGold == 0) 0f else mMatched.toFloat() / mGold,
            f1 = f1(mMatched, mPredicted, mGold),
        )

        println("=== GOLDEN CORPUS REPORT (${cases.size} cases) ===")
        for (cat in byCategory.keys.sorted()) {
            println(String.format("%-14s p=%.3f r=%.3f f1=%.3f", cat, scores[cat]!!.p, scores[cat]!!.r, scores[cat]!!.f1))
        }
        println(String.format("%-14s p=%.3f r=%.3f f1=%.3f", "all", all.p, all.r, all.f1))
        println("BASELINE " + buildJson(scores, all))

        if (baseline != null) {
            for ((cat, score) in scores) {
                val pin = baseline[cat] ?: continue
                assertThat(score.f1).isAtLeast(pin - eps)
            }
            assertThat(all.f1).isAtLeast(baseline["all"]!! - eps)
        }

        for (c in cases.filter { it.category == "notouch" }) {
            val clean = CleanupPipeline.run(c.raw, level = c.level).clean
            val normalized = collapseWs(c.raw)
            assertThat(clean).isEqualTo(normalized)
        }
    }

    private fun loadBaseline(): Map<String, Float>? {
        val f = java.io.File("src/test/resources/golden/baseline.json")
        if (!f.isFile) return null
        val text = f.readText(Charsets.UTF_8).trim()
        if (text.isBlank() || text == "{}") return null
        return Regex("""(\w+)\s*:\s*([0-9.]+)""")
            .findAll(text.replace("\"", ""))
            .associate { it.groupValues[1] to it.groupValues[2].toFloat() }
    }

    private fun buildJson(scores: Map<String, Score>, all: Score): String {
        val sb = StringBuilder("{")
        for (cat in scores.keys.sorted()) {
            sb.append("\"$cat\": ${String.format("%.3f", scores[cat]!!.f1)}, ")
        }
        sb.append("\"all\": ").append(String.format("%.3f", all.f1)).append("}")
        return sb.toString()
    }

    private fun words(s: String): List<String> = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun collapseWs(s: String): String =
        s.replace(Regex("[ \t]+"), " ").trim()

    private fun lcs(a: List<String>, b: List<String>): Int {
        val n = a.size; val m = b.size
        val prev = IntArray(m + 1)
        val cur = IntArray(m + 1)
        for (i in 1..n) {
            for (j in 1..m) {
                cur[j] = if (a[i - 1] == b[j - 1]) prev[j - 1] + 1
                else maxOf(prev[j], cur[j - 1])
            }
            prev.indices.forEach { prev[it] = cur[it] }
            cur.indices.forEach { cur[it] = 0 }
        }
        return prev[m]
    }

    private fun f1(matched: Int, predicted: Int, gold: Int): Float {
        if (predicted == 0 || gold == 0) return 0f
        val p = matched.toFloat() / predicted
        val r = matched.toFloat() / gold
        return if (p + r == 0f) 0f else 2 * p * r / (p + r)
    }

    private data class Score(val p: Float, val r: Float, val f1: Float)
}
