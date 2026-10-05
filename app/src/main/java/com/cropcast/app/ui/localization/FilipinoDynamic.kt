package com.cropcast.app.ui.localization

/**
 * Filipino text for engine sentences that carry numbers, dates or crop names, so they cannot be
 * looked up whole. Each rule matches the exact English template the engines produce.
 */
private class Rule(val regex: Regex, val build: (List<String>) -> String)

private fun rule(pattern: String, build: (List<String>) -> String) =
    Rule(Regex(pattern, RegexOption.DOT_MATCHES_ALL), build)

private val MONTHS = mapOf(
    "Jan" to "Ene", "Feb" to "Peb", "Mar" to "Mar", "Apr" to "Abr", "May" to "May", "Jun" to "Hun",
    "Jul" to "Hul", "Aug" to "Ago", "Sep" to "Set", "Oct" to "Okt", "Nov" to "Nob", "Dec" to "Dis"
)
private val MONTH_PATTERN = Regex("\\b(" + MONTHS.keys.joinToString("|") + ")\\b")

/** "Oct 5" -> "Okt 5". */
internal fun filipinoMonths(text: String): String = MONTH_PATTERN.replace(text) { MONTHS.getValue(it.value) }

private fun f(text: String): String = translateFilipino(text)

private val RAIN_CATEGORY = mapOf(
    "heavy to intense" to "matindi hanggang napakatindi",
    "moderate to heavy" to "katamtaman hanggang malakas",
    "Heavy to intense" to "Matindi hanggang napakatindi",
    "Moderate to heavy" to "Katamtaman hanggang malakas"
)

private val TEXTURE = mapOf(
    "heavy (clay)" to "mabigat (luwad)",
    "light (sandy)" to "magaan (mabuhangin)",
    "medium (loam)" to "katamtaman (lupang-buhaghag)"
)

private val CROP_NAMES = setOf(
    "Tomato", "Okra", "Alugbati", "Potato", "Rice", "Corn", "Eggplant", "Cucumber", "Cabbage", "Sweet Potato",
    "Lettuce", "Spinach", "Pechay", "Kangkong", "Ampalaya", "Sitaw", "Kalabasa", "Chili", "Onion", "Garlic",
    "Mungbean", "Peanut", "Ginger", "Gabi", "Patola"
)

private fun sacks(text: String): String = when {
    text.startsWith("less than ") -> text.replace("less than ", "wala pang ").replace(" sack of 50 kg", " sako na 50 kg")
    else -> text.replace("about ", "mga ").replace(Regex(" sacks? of 50 kg"), " sako na 50 kg")
}

private fun product(text: String): String = text.replace("also called", "tinatawag ding")

private val RULES = listOf(
    rule("^Season ([\\d.]+)–([\\d.]+) °C \\(best (\\d+)–(\\d+) °C\\)$") { g ->
        "Panahon ${g[1]}–${g[2]} °C (pinakamainam ${g[3]}–${g[4]} °C)"
    },
    rule("^pH ([\\d.]+) \\(best ([\\d.]+)–([\\d.]+)\\)$") { g -> "pH ${g[1]} (pinakamainam ${g[2]}–${g[3]})" },
    rule("^pH ([\\d.]+) is outside the crop's limits \\(([\\d.]+)–([\\d.]+)\\); liming may not be enough$") { g ->
        "Ang pH na ${g[1]} ay labas sa limitasyon ng pananim (${g[2]}–${g[3]}); maaaring hindi sapat ang paglalagay ng apog"
    },
    rule("^(\\d+)–(\\d+) mm/month expected \\(best (\\d+)–(\\d+)\\)(.*)$") { g ->
        val hint = when (g[5]) {
            " – irrigated, so dry months are fine" -> " – may irigasyon, kaya ayos lang ang tuyong buwan"
            " – plan irrigation" -> " – magplano ng irigasyon"
            " – needs good drainage" -> " – kailangan ng maayos na drainage"
            else -> ""
        }
        "Inaasahang ${g[1]}–${g[2]} mm/buwan (pinakamainam ${g[3]}–${g[4]})$hint"
    },
    rule("^Mapped soil is (heavy \\(clay\\)|light \\(sandy\\)|medium \\(loam\\))(?:, (\\d+)% clay)?( – add organic matter)?$") { g ->
        "Ang lupa sa mapa ay ${TEXTURE.getValue(g[1])}" +
            (if (g[2].isNotEmpty()) ", ${g[2]}% luwad" else "") +
            (if (g[3].isNotEmpty()) " – dagdagan ng organikong bagay" else "")
    },
    rule("^ECe ≈ ([\\d.]+) dS/m; yield falls ([\\d.]+) % per dS/m above ([\\d.]+) \\((.+)\\)$") { g ->
        "ECe ≈ ${g[1]} dS/m; bumababa ng ${g[2]} % ang ani kada dS/m na lampas sa ${g[3]} (${g[4]})"
    },
    rule("^Wet, warm season: about (\\d+) % yield lost to disease – plant in the dry season$") { g ->
        "Maulan at maalinsangang panahon: mga ${g[1]} % ng ani ang nawawala dahil sa sakit – magtanim sa tag-init"
    },
    rule("^(\\d+) mm forecast on ([A-Za-z]+ \\d+) \\(PAGASA: (heavy to intense|moderate to heavy)\\)(; tolerates waterlogging| can drown or wash out seedlings – plant from ([A-Za-z]+ \\d+) or use raised beds)$") { g ->
        val tail = if (g[4].startsWith(";")) "; kayang tiisin ang pagbaha ng tubig"
        else " ay maaaring maglunod o tumangay sa mga punla – magtanim mula ${filipinoMonths(g[5])} o gumamit ng nakataas na taniman"
        filipinoMonths("Tinatayang ${g[1]} mm na ulan sa ${g[2]} (PAGASA: ${RAIN_CATEGORY.getValue(g[3])})$tail")
    },
    rule("^Wet, warm season: (high risk of .*)$") { g -> "Maulan at maalinsangang panahon: ${f(g[1])}" },
    rule("^Follows (.+) \\(same family, (.+)\\)\\. Rotate to another family to break soil-borne disease\\.$") { g ->
        "Kasunod ng ${f(g[1])} (parehong pamilya, ${g[2]}). Magpalit sa ibang pamilya ng pananim para maputol ang sakit na galing sa lupa."
    },
    rule("^([\\d.]+) is (too acidic|too alkaline|good) \\(best for (.+): ([\\d.]+)–([\\d.]+)\\)$") { g ->
        val state = when (g[2]) { "too acidic" -> "masyadong maasim"; "too alkaline" -> "masyadong alkaline"; else -> "maayos" }
        "${g[1]} ay $state (pinakamainam para sa ${f(g[3])}: ${g[4]}–${g[5]})"
    },
    rule("^No verified per-hectare rate for (.+)$") { g -> "Walang napatunayang dosis kada ektarya para sa ${f(g[1])}" },
    rule("^Ask your municipal agriculturist for the DA production guide for (.+), or get a BSWM soil test$") { g ->
        "Humingi sa agrikulturista ng munisipyo ng production guide ng DA para sa ${f(g[1])}, o magpa-soil test sa BSWM"
    },
    rule("^(\\d+) mg/kg is (low|enough|high) \\(good level: (\\d+)–(\\d+)\\)\\. (Nitrogen|Phosphorus|Potassium) (.+)\\.$") { g ->
        val state = when (g[2]) { "low" -> "mababa"; "enough" -> "sapat"; else -> "mataas" }
        "${g[1]} mg/kg ay $state (magandang antas: ${g[3]}–${g[4]}). ${f(g[5])} ${f(g[6])}."
    },
    rule("^(Add a full dose\\.|Level is fine: give a normal dose\\.|Soil already has plenty: a small top-up is enough\\.) Buy (.+?): (\\d+) kg per 1,000 m² \\((.+?)\\)\\. That is (\\d+) kg per hectare\\. (Crop is already growing: .+? water it in\\.|Phosphorus is meant for before planting\\. .+? now\\.|Spread half at planting and half 3-4 weeks later\\.|Mix it into the soil before planting\\.)(?: Ammonium sulfate \\(21-0-0\\) also works: (\\d+) kg per 1,000 m²\\.)?$") { g ->
        "${f(g[1])} Bumili ng ${product(g[2])}: ${g[3]} kg kada 1,000 m² (${sacks(g[4])}). Katumbas ito ng ${g[5]} kg kada ektarya. ${f(g[6])}" +
            (if (g[7].isNotEmpty()) " Puwede rin ang ammonium sulfate (21-0-0): ${g[7]} kg kada 1,000 m²." else "")
    },
    rule("^Probe pH ([\\d.]+) differs from the SoilGrids map \\(([\\d.]+)\\) by more than 1\\.0\\. Check the probe with a pH buffer or a lab test\\.$") { g ->
        "Ang pH ng probe na ${g[1]} ay higit 1.0 ang layo sa mapa ng SoilGrids (${g[2]}). Suriin ang probe gamit ang pH buffer o lab test."
    },
    rule("^Soil EC suggests saline soil \\(ECe ≈ ([\\d.]+) dS/m\\)\\. Confirm with a laboratory ECe test and leach with good-quality water\\.$") { g ->
        "Ipinapahiwatig ng EC ng lupa na maalat ito (ECe ≈ ${g[1]} dS/m). Kumpirmahin sa laboratory ECe test at hugasan ang lupa gamit ang malinis na tubig."
    },
    rule("^Climate normals were adjusted by ([+-][\\d.]+) °C for the farm's elevation\\.$") { g ->
        "Inayos ang karaniwang klima nang ${g[1]} °C ayon sa taas ng bukid."
    },
    rule("^ECMWF seasonal forecast: drier than normal \\((.+) rain\\)\\. Plan irrigation, or favour crops that need less water\\.$") { g ->
        filipinoMonths("Pana-panahong tagna ng ECMWF: mas tuyo kaysa karaniwan (ulan ${g[1]}). Magplano ng irigasyon, o piliin ang mga pananim na hindi gaanong nangangailangan ng tubig.")
    },
    rule("^ECMWF seasonal forecast: wetter than normal \\((.+) rain\\)\\. Favour crops that tolerate waterlogging \\(rice, kangkong, sitaw\\) and use raised beds for the rest\\.$") { g ->
        filipinoMonths("Pana-panahong tagna ng ECMWF: mas maulan kaysa karaniwan (ulan ${g[1]}). Piliin ang mga pananim na kayang tiisin ang pagbaha (palay, kangkong, sitaw) at gumamit ng nakataas na taniman para sa iba.")
    },
    rule("^ECMWF seasonal forecast: up to ([+-][\\d.]+) °C warmer than normal in the coming months\\.$") { g ->
        "Pana-panahong tagna ng ECMWF: hanggang ${g[1]} °C na mas mainit kaysa karaniwan sa mga susunod na buwan."
    },
    rule("^(Moderate to heavy|Heavy to intense) rain forecast: (\\d+) mm on ([A-Za-z]+ \\d+)\\. Crops that need well-drained soil: plant from ([A-Za-z]+ \\d+) or on raised beds, and clear drainage canals\\.$") { g ->
        filipinoMonths("Tagna ng ulan na ${RAIN_CATEGORY.getValue(g[1])}: ${g[2]} mm sa ${g[3]}. Para sa mga pananim na nangangailangan ng lupang maayos ang drainage: magtanim mula ${g[4]} o sa nakataas na taniman, at linisin ang mga kanal.")
    },
    rule("^Little rain forecast: (\\d+) mm in the next (\\d+) days\\. Irrigate after planting\\.$") { g ->
        "Kaunting ulan ang tagna: ${g[1]} mm sa susunod na ${g[2]} araw. Diligan pagkatapos magtanim."
    },
    rule("^Gale-force gusts up to (\\d+) km/h forecast on ([A-Za-z]+ \\d+) \\(PAGASA Wind Signal No\\. 2 level\\)\\. Stake tall crops, delay transplanting, and harvest mature rice and corn early if you can\\.$") { g ->
        filipinoMonths("Tagna ng malalakas na hangin hanggang ${g[1]} km/h sa ${g[2]} (antas ng PAGASA Wind Signal No. 2). Itukod ang matatangkad na pananim, ipagpaliban ang paglilipat-tanim, at anihin nang maaga ang hinog na palay at mais kung kaya.")
    },
    rule("^High humidity ahead \\((\\d+)%\\)\\. Watch tomato, eggplant, potato and cucumber for fungal disease\\.$") { g ->
        "Mataas na halumigmig ang paparating (${g[1]}%). Bantayan ang kamatis, talong, patatas at pipino laban sa sakit na amag."
    },
    rule("^Only (\\d+) mm of rain in the last 30 days and the soil is dry\\. Water before planting\\.$") { g ->
        "${g[1]} mm lang ang ulan sa nakaraang 30 araw at tuyo ang lupa. Diligan bago magtanim."
    },
    // Sources, alerts and view-model messages
    rule("^(NASA POWER climate normals) \\+ (ECMWF seasonal forecast)$") { g -> "${f(g[1])} + ${f(g[2])}" },
    rule("^(.+) \\(saved copy\\)$") { g -> "${f(g[1])} (naka-save na kopya)" },
    rule("^Moisture dropped to (\\d+)%$") { g -> "Bumaba ang halumigmig ng lupa sa ${g[1]}%" },
    rule("^Temperature reached ([\\d.]+)°C$") { g -> "Umabot sa ${g[1]}°C ang temperatura" },
    rule("^Soil pH is ([\\d.]+)$") { g -> "Ang pH ng lupa ay ${g[1]}" },
    rule("^(\\d+) spots recorded; save the sample$") { g -> "${g[1]} puwesto ang naitala; i-save na ang sample" },
    rule("^Record at least (\\d+) spots across the field$") { g -> "Magtala ng hindi bababa sa ${g[1]} puwesto sa buong bukid" },
    // Warnings that start with the crop they are about, e.g. "Tomato: Typhoon season ..."; last so it never shadows a template.
    rule("^([A-Za-z ]+): (.+)$") { g -> if (g[1] in CROP_NAMES) "${f(g[1])}: ${f(g[2])}" else "${g[1]}: ${g[2]}" }
)

private val RULE_FALLBACK_SACKS = Regex("^(less than ¼ sack of 50 kg|about .+ sacks? of 50 kg)$")

/** Exact Filipino text if the app has it, else a matching template, else the English text unchanged. */
fun translateFilipino(english: String): String {
    FILIPINO[english]?.let { return it }
    if (RULE_FALLBACK_SACKS.matches(english)) return sacks(english)
    for (rule in RULES) {
        val match = rule.regex.matchEntire(english) ?: continue
        return rule.build(match.groupValues)
    }
    return english
}
