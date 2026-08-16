package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PairImportTest {

    @Test
    fun csv_from_to_rows() {
        val r = PairImport.parse("wisper,Wispr\nmitun,Mitun\n")
        assertThat(r.rows).containsExactly(
            PairImport.Row("wisper", "Wispr"),
            PairImport.Row("mitun", "Mitun"),
        ).inOrder()
        assertThat(r.skipped).isEqualTo(0)
    }

    @Test
    fun skips_blank_comments_and_header() {
        val r = PairImport.parse(
            """
            from,to
            # comment
            foo,bar

            ,empty
            """.trimIndent()
        )
        assertThat(r.rows).containsExactly(PairImport.Row("foo", "bar"))
        assertThat(r.skipped).isAtLeast(2)
    }

    @Test
    fun quoted_comma_in_to() {
        val r = PairImport.parse("sig,\"Best regards, Mitun\"")
        assertThat(r.rows).containsExactly(PairImport.Row("sig", "Best regards, Mitun"))
    }

    @Test
    fun tsv_and_single_column_same_from_to() {
        val r = PairImport.parse("Acme\tAcme Corp\nOpenFlow\n")
        assertThat(r.rows).containsExactly(
            PairImport.Row("Acme", "Acme Corp"),
            PairImport.Row("OpenFlow", "OpenFlow"),
        )
    }

    @Test
    fun conflict_when_word_already_snippet() {
        val d = PairImport.decide(
            from = "sig",
            existingDict = emptySet(),
            existingSnip = setOf("sig"),
            kind = PairImport.Kind.DICT,
        )
        assertThat(d).isEqualTo(PairImport.Decision.CONFLICT)
    }

    @Test
    fun skip_dup_same_kind() {
        val d = PairImport.decide(
            from = "foo",
            existingDict = setOf("foo"),
            existingSnip = emptySet(),
            kind = PairImport.Kind.DICT,
        )
        assertThat(d).isEqualTo(PairImport.Decision.SKIP_DUP)
    }

    @Test
    fun add_when_free() {
        val d = PairImport.decide(
            from = "bar",
            existingDict = emptySet(),
            existingSnip = emptySet(),
            kind = PairImport.Kind.SNIPPET,
        )
        assertThat(d).isEqualTo(PairImport.Decision.ADD)
    }
}
