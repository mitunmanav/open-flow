package app.openflow.text

/**
 * Spoken dates and times → written forms. Unambiguous shapes only.
 * Dates: "<month> <ordinal>" → "May 3"; optional year → "January 1, 2024";
 * "the tenth of december" → "10th of December"; bare "on the twenty third" → "23rd".
 * Times: H:MM (+AM/PM), o'clock, half/quarter past/to, "at seven" → 7:00,
 * noon/midnight untouched. Month/weekday words get capitalized.
 */
object ItnDateTime {

    internal val months = setOf(
        "january", "february", "march", "april", "may", "june", "july",
        "august", "september", "october", "november", "december",
    )
    private val weekdays = setOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    )
    private val ordinals = mapOf(
        "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
        "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10,
        "eleventh" to 11, "twelfth" to 12, "thirteenth" to 13, "fourteenth" to 14,
        "fifteenth" to 15, "sixteenth" to 16, "seventeenth" to 17,
        "eighteenth" to 18, "nineteenth" to 19, "twentieth" to 20,
        "twenty first" to 21, "twenty second" to 22, "twenty third" to 23,
        "twenty fourth" to 24, "twenty fifth" to 25, "twenty sixth" to 26,
        "twenty seventh" to 27, "twenty eighth" to 28, "twenty ninth" to 29,
        "thirtieth" to 30, "thirty first" to 31,
    )

    fun apply(text: String): String = applyTime(applyDate(text))

    // ---- Dates ----

    private fun ordinalAt(tokens: List<String>, i: Int): Pair<Int, Int>? {
        val one = ordinals[tokens[i].lowercase()]
        if (one != null) return one to 1
        if (i + 1 < tokens.size) {
            val two = ordinals["${tokens[i].lowercase()} ${tokens[i + 1].lowercase()}"]
            if (two != null) return two to 2
        }
        return null
    }

    private fun suffix(day: Int): String = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }

    internal fun applyDate(text: String): String {
        val tokens = text.split(" ")
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val low = tokens[i].lowercase()

            // "monday the third of june" handled by generic paths below plus
            // weekday capitalization; months drive the main conversions.
            if (low in months && i + 1 < tokens.size) {
                val ord = ordinalAt(tokens, i + 1)
                if (ord != null) {
                    val (day, used) = ord
                    var consumed = 1 + used
                    val sb = StringBuilder(cap(low)).append(' ').append(day)
                    // Optional year: "<month> <day> twenty twenty four"
                    ItnNumber.yearPair(tokens, i + consumed)?.let { (n, y) ->
                        sb.append(", ").append(y)
                        consumed += n
                    }
                    out.add(sb.toString())
                    i += consumed
                    continue
                }
            }
            // "the tenth of december" / "on the twenty third" / "is the thirteenth"
            if (low == "the" || low == "on the") {
                val base = i + if (low == "on the") 2 else 1
                if (base < tokens.size) {
                    val ord = ordinalAt(tokens, base)
                    if (ord != null) {
                        val (day, used) = ord
                        val after = base + used
                        if (after < tokens.size &&
                            tokens[after].equals("of", ignoreCase = true) &&
                            after + 1 < tokens.size &&
                            tokens[after + 1].lowercase() in months
                        ) {
                            // "<weekday>? the Dth of <Month>" → "<Weekday>, <Month> D"
                            val prevW = out.lastOrNull()?.lowercase()
                            if (prevW in weekdays) {
                                out.removeAt(out.size - 1)
                                out.add(cap(prevW!!) + ", " + cap(tokens[after + 1].lowercase()) + " $day")
                            } else {
                                out.add("the")
                                out.add("$day${suffix(day)}")
                                out.add("of")
                                out.add(cap(tokens[after + 1].lowercase()))
                            }
                            i = after + 2
                            continue
                        }
                        val clauseEnd = after >= tokens.size ||
                            tokens[after].lastOrNull()?.isLetterOrDigit() == false
                        if (low == "on the" || clauseEnd || out.lastOrNull()?.lowercase() == "on") {
                            out.add(if (low == "on the") "on the" else "the")
                            out.add("$day${suffix(day)}")
                            i = base + used
                            continue
                        }
                    }
                }
            }
            when (low) {
                in weekdays -> out.add(cap(low))
                in months -> out.add(cap(low))
                else -> out.add(tokens[i])
            }
            i++
        }
        return out.joinToString(" ")
    }

    private fun cap(w: String) = w.replaceFirstChar { it.uppercase() }

    // ---- Times ----

    private val hourWords: Map<String, Int> = ItnNumber.units.entries
        .filter { it.value in 1L..12L }
        .associate { it.key to it.value.toInt() }

    private fun minutesAt(tokens: List<String>, i: Int): Pair<Int, Int>? {
        if (i >= tokens.size) return null
        val t = tokens[i].lowercase()
        ItnNumber.tens[t]?.let { tens ->
            var v = tens.toInt()
            var used = 1
            if (i + 1 < tokens.size) {
                ItnNumber.units[tokens[i + 1].lowercase()]?.let { u ->
                    if (u in 1..9) {
                        v += u.toInt()
                        used = 2
                    }
                }
            }
            return v to used
        }
        ItnNumber.units[t]?.let { u ->
            if (u in 0L..59L) return u.toInt() to 1
        }
        return null
    }

    internal fun applyTime(text: String): String {
        val tokens = text.split(" ")
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val low = tokens[i].lowercase().trim(',', '.')

            // half past / quarter past|to / N past|to
            if ((low == "half" || low == "quarter" || minutesAt(tokens, i) != null) &&
                i + 1 < tokens.size
            ) {
                val next = tokens[i + 1].lowercase()
                if (next == "past" || next == "to") {
                    val hWord = tokens.getOrNull(i + 2)?.lowercase()?.trim(',', '.')
                    val h = hourWords[hWord]
                    if (h != null) {
                        val mins: Int = when (low) {
                            "half" -> 30
                            "quarter" -> 15
                            else -> minutesAt(tokens, i)!!.first
                        }
                        val hour = if (next == "to") h - 1 else h
                        val min = if (next == "to") 60 - mins else mins
                        out.add(render(hour, min))
                        i += 3
                        continue
                    }
                }
            }
            // H:MM [am|pm] — "four thirty pm", "it's three thirty"
            hourWords[low]?.let { h ->
                // Decimal context ("three point one four") is not a clock.
                if (out.lastOrNull()?.lowercase() == "point") {
                    out.add(tokens[i]); return@let
                }
                val min = minutesAt(tokens, i + 1)
                    ?.takeIf { it.first <= 59 }
                    ?.takeIf {
                        // domain-ish continuation ("thirty dot com") is not a clock
                        tokens.getOrNull(i + 1 + it.second)?.lowercase() != "dot"
                    }
                if (min != null) {
                    var meridiem = ""
                    var used = 2 + min.second
                    when (tokens.getOrNull(i + 1 + min.second)?.lowercase()) {
                        "am" -> meridiem = " AM"
                        "pm" -> meridiem = " PM"
                        else -> {
                            // "in the morning/afternoon/evening"
                            val j = i + 1 + min.second
                            if (tokens.getOrNull(j)?.lowercase() == "in" &&
                                tokens.getOrNull(j + 1)?.lowercase() == "the"
                            ) {
                                when (tokens.getOrNull(j + 2)?.lowercase()) {
                                    "morning" -> {
                                        meridiem = " AM"; used += 3
                                    }
                                    "afternoon", "evening" -> {
                                        meridiem = " PM"; used += 3
                                    }
                                }
                            }
                        }
                    }
                    out.add(render(h, min.first) + meridiem)
                    i += used
                    return@let
                }
                // "nine o'clock" / "at seven" / "eight sharp"
                val nxt = tokens.getOrNull(i + 1)?.lowercase()?.trim(',', '.')
                val after2 = tokens.getOrNull(i + 2)?.lowercase()
                val hourIsTimey = when {
                    nxt == "o'clock" || nxt == "oclock" || nxt == "sharp" -> true
                    nxt == "am" || nxt == "pm" -> true
                    // "at seven" only when the hour ends the clause (not "at ten dot …")
                    out.lastOrNull()?.equals("at", ignoreCase = true) == true ->
                        after2 == null || after2 in setOf("am", "pm", "sharp") ||
                            after2.startsWith(".") || after2.all { !it.isLetterOrDigit() }
                    else -> false
                }
                when {
                    nxt == "o'clock" || nxt == "oclock" -> {
                        out.add(render(h, 0)); i += 2
                    }
                    nxt == "sharp" -> {
                        out.add(render(h, 0)); out.add("sharp"); i += 2
                    }
                    hourIsTimey -> {
                        out.add(render(h, 0)); i++
                    }
                    else -> {
                        out.add(tokens[i]); i++
                    }
                }
            } ?: run { out.add(tokens[i]); i++ }
        }
        return out.joinToString(" ")
    }

    private fun render(h: Int, m: Int): String =
        "$h:${m.toString().padStart(2, '0')}"
}
