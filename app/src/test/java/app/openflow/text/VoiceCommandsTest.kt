package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceCommandsTest {

    @Test
    fun phrase_map_loads_from_classpath_json() {
        val map = PhraseMap.default
        assertThat(map.insert["period"]).isEqualTo(".")
        assertThat(map.insert["full stop"]).isEqualTo(".")
        assertThat(map.insert["new line"]).isEqualTo("\n")
        assertThat(map.edit["backspace"]).isEqualTo(PhraseMap.EditOp.WORD)
        assertThat(map.edit["delete last word"]).isEqualTo(PhraseMap.EditOp.WORD)
    }

    @Test
    fun period_and_full_stop_become_dot() {
        assertThat(VoiceCommands.apply("hello period")).contains(".")
        assertThat(VoiceCommands.apply("hello period").lowercase()).doesNotContain("period")
        assertThat(VoiceCommands.apply("hello full stop").lowercase()).doesNotContain("full stop")
        assertThat(VoiceCommands.apply("hello full stop")).contains(".")
    }

    @Test
    fun comma_semicolon_exclamation() {
        val out = VoiceCommands.apply("yes comma no semicolon wow exclamation point")
        assertThat(out).contains(",")
        assertThat(out).contains(";")
        assertThat(out).contains("!")
        assertThat(out.lowercase()).doesNotContain("comma")
        assertThat(out.lowercase()).doesNotContain("semicolon")
        assertThat(out.lowercase()).doesNotContain("exclamation")
    }

    @Test
    fun symbols_from_map_ship_set() {
        val out = VoiceCommands.apply(
            "a question mark b colon c slash d hashtag e at sign f asterisk g ampersand " +
                "h percent sign i plus j minus k equals l tilde m underscore n ellipsis"
        )
        assertThat(out).contains("?")
        assertThat(out).contains(":")
        assertThat(out).contains("/")
        assertThat(out).contains("#")
        assertThat(out).contains("@")
        assertThat(out).contains("*")
        assertThat(out).contains("&")
        assertThat(out).contains("%")
        assertThat(out).contains("+")
        assertThat(out).contains("-")
        assertThat(out).contains("=")
        assertThat(out).contains("~")
        assertThat(out).contains("_")
        assertThat(out).contains("...")
    }

    @Test
    fun degrees_copyright_trademark() {
        val out = VoiceCommands.apply(
            "temp degrees celsius mark copyright trademark registered trademark"
        )
        assertThat(out).contains("°C")
        assertThat(out).contains("©")
        assertThat(out).contains("™")
        assertThat(out).contains("®")
    }

    @Test
    fun new_line_and_paragraph() {
        val nl = VoiceCommands.apply("hello new line world")
        assertThat(nl).contains("\n")
        assertThat(nl.lowercase()).doesNotContain("new line")
        val p = VoiceCommands.apply("a new paragraph b")
        assertThat(p).contains("\n\n")
        val next = VoiceCommands.apply("x next line y")
        assertThat(next).contains("\n")
        val skip = VoiceCommands.apply("x skip a line y")
        assertThat(skip).contains("\n\n")
    }

    @Test
    fun backspace_removes_last_word() {
        val out = VoiceCommands.apply("hello world backspace")
        assertThat(out.lowercase()).contains("hello")
        assertThat(out.lowercase()).doesNotContain("world")
        assertThat(out.lowercase()).doesNotContain("backspace")
    }

    @Test
    fun delete_last_word_alias() {
        val out = VoiceCommands.apply("one two three delete last word")
        assertThat(out.lowercase()).contains("one")
        assertThat(out.lowercase()).contains("two")
        assertThat(out.lowercase()).doesNotContain("three")
    }

    @Test
    fun delete_last_character() {
        val out = VoiceCommands.apply("hello delete last character")
        assertThat(out.lowercase()).isEqualTo("hell")
    }

    @Test
    fun delete_last_sentence() {
        val out = VoiceCommands.apply("First sentence. Second junk delete last sentence")
        assertThat(out.lowercase()).contains("first")
        assertThat(out.lowercase()).doesNotContain("second")
        assertThat(out.lowercase()).doesNotContain("junk")
    }

    @Test
    fun clear_all() {
        val out = VoiceCommands.apply("lots of words clear all")
        assertThat(out.trim()).isEmpty()
    }

    @Test
    fun parens_and_quotes() {
        val out = VoiceCommands.apply("open paren hi close paren quote yo quote")
        assertThat(out).contains("(")
        assertThat(out).contains(")")
        assertThat(out).contains("\"")
        assertThat(out.lowercase()).doesNotContain("open paren")
    }

    @Test
    fun longest_phrase_wins_over_shorter() {
        // "full stop" (2 words) must win; not leave "full" + fail on "stop"
        val full = VoiceCommands.apply("end full stop")
        assertThat(full).contains(".")
        assertThat(full.lowercase()).doesNotContain("full")
        assertThat(full.lowercase()).doesNotContain("stop")

        // "exclamation point" not partial tokens left behind
        val excl = VoiceCommands.apply("wow exclamation point")
        assertThat(excl).contains("!")
        assertThat(excl.lowercase()).doesNotContain("exclamation")
        assertThat(excl.lowercase()).doesNotContain("point")

        // "delete last word" (3) beats any shorter prefix; removes only last content word
        val del = VoiceCommands.apply("alpha beta delete last word")
        assertThat(del.lowercase().trim()).isEqualTo("alpha")
    }

    @Test
    fun multi_word_layout_and_punct_chain() {
        val out = VoiceCommands.apply("title new line body period")
        assertThat(out).contains("\n")
        assertThat(out).contains(".")
        assertThat(out.lowercase()).doesNotContain("period")
        assertThat(out.lowercase()).doesNotContain("new line")
    }

    @Test
    fun pipeline_light_applies_voice_commands() {
        val r = CleanupPipeline.run(
            "meet at five period backspace six period",
            CleanupLevel.LIGHT
        )
        assertThat(r.clean.lowercase()).doesNotContain("period")
        assertThat(r.clean.lowercase()).doesNotContain("backspace")
        assertThat(r.clean).contains(".")
    }

    @Test
    fun raw_keeps_period_word() {
        val r = CleanupPipeline.run("hello period", CleanupLevel.RAW)
        assertThat(r.clean.lowercase()).contains("period")
    }

    @Test
    fun new_aliases_map() {
        val out = VoiceCommands.apply(
            "a fullstop b semi colon c three dots d open square bracket x close square bracket"
        )
        assertThat(out).contains(".")
        assertThat(out).contains(";")
        assertThat(out).contains("...")
        assertThat(out).contains("[")
        assertThat(out).contains("]")
        assertThat(out.lowercase()).doesNotContain("fullstop")
        assertThat(out.lowercase()).doesNotContain("semi colon")
    }

    @Test
    fun dot_becomes_period_unless_tld() {
        assertThat(VoiceCommands.apply("end dot")).contains(".")
        val keep = VoiceCommands.apply("site dot com")
        assertThat(keep.lowercase()).contains("dot")
        assertThat(keep).doesNotContain(".")
    }
}
