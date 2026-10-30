package com.cropcast.app.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCropCastLanguage = staticCompositionLocalOf { "Filipino" }

private val hiligaynon = mapOf(
    "Home" to "Balay",
    "Welcome" to "Maayong pag-abot",
    "Monthly data readiness" to "Kahandaan sang binulan nga datos",
    "Important notices" to "Importante nga mga pahibalo",
    "Test data is simulated and is not field evidence" to "Ang test nga datos gin-simulate kag indi ebidensya halin sa aktuwal nga latagon",
    "View full recommendation" to "Tan-awa ang bug-os nga rekomendasyon",
    "of" to "sa",
    "required readings collected" to "kinahanglan nga mga pagbasa ang natipon",
    "Ready for recommendation" to "Handa na para sa rekomendasyon",
    "More readings needed" to "Kinahanglan pa sang dugang nga mga pagbasa",
    "No active alerts" to "Wala sang aktibo nga alerto",
    "Sensor is offline; using the latest available data" to "Offline ang sensor; ginagamit ang pinakabag-o nga datos nga ara",
    "Recommendation" to "Rekomendasyon",
    "Sensor details" to "Detalye sang sensor",
    "Crop Recommendation" to "Rekomendasyon sang Tanom",
    "ML crop recommendation" to "Rekomendasyon sang tanom paagi sa ML",
    "Top recommendation" to "Panguna nga rekomendasyon",
    "Top match from 22 crops" to "Pinakamaayo nga kaangay sa 22 ka tanom",
    "experimental model score" to "eksperimental nga iskor sang modelo",
    "model agreement" to "pag-uyon sang modelo",
    "model agreement, not field success probability" to "pag-uyon sang modelo, indi posibilidad sang kalampusan sa latagon",
    "High model agreement" to "Mataas nga pag-uyon sang modelo",
    "Moderate model agreement" to "Kasagarang pag-uyon sang modelo",
    "Low model agreement" to "Manubo nga pag-uyon sang modelo",
    "Agreement unavailable" to "Wala sang iskor sang pag-uyon",
    "At least eight valid monthly readings are needed for the ML recommendation" to "Kinahanglan ang labing gamay walo ka husto nga binulan nga pagbasa para sa rekomendasyon sang ML",
    "Public-data ML recommendations" to "Mga rekomendasyon sang ML halin sa publiko nga datos",
    "The public crop model could not be loaded" to "Indi ma-load ang publiko nga modelo sang tanom",
    "Collect at least" to "Magtipon sang labing gamay",
    "valid readings for the 22-crop model" to "ka husto nga pagbasa para sa modelo nga may 22 ka tanom",
    "model score" to "iskor sang modelo",
    "Experimental 22-crop model using" to "Eksperimental nga modelo sang 22 ka tanom nga nagagamit sang",
    "latest-month readings; rainfall is excluded because the device does not measure it" to "mga pagbasa sang pinakabag-o nga bulan; wala ginlakip ang ulan kay wala ini ginakuha sang device",
    "Sensor conditions are outside the public dataset range" to "Ang kondisyon sang sensor yara sa guwa sang range sang publiko nga dataset",
    "Check these readings" to "Usisaa ini nga mga pagbasa",
    "The sensor readings are invalid for crop recommendation" to "Indi balido ang mga pagbasa sang sensor para sa rekomendasyon sang tanom",
    "No reliable crop match was found for these readings" to "Wala sang masaligan nga kaangay nga tanom para sini nga mga pagbasa",
    "No reliable crop match was found; the closest experimental matches are shown below" to "Wala sang masaligan nga kaangay; ginapakita sa idalom ang pinakamalapit nga eksperimento nga mga resulta",
    "Rainfall is needed before this model can recommend a crop" to "Kinahanglan ang datos sang ulan antes makarekomendar ini nga modelo",
    "No recommendation: readings are outside the public dataset range" to "Wala sang rekomendasyon: yara sa guwa sang range sang publiko nga dataset ang mga pagbasa",
    "30-day weather rainfall" to "Ulan sa sulod sang 30 ka adlaw",
    "Loading rainfall estimate" to "Ginakuha ang banabana sang ulan",
    "Add valid farm coordinates in Settings to include rainfall" to "Ibutang ang husto nga koordinado sang uma sa Settings agod malakip ang ulan",
    "Weather unavailable; using the six-input offline model" to "Wala ang datos sang panahon; ginagamit ang offline nga modelo nga may anum ka input",
    "Using the six-input offline model" to "Ginagamit ang offline nga modelo nga may anum ka input",
    "Weather data by Open-Meteo; rainfall is an estimate, not a field gauge reading" to "Datos sang panahon halin sa Open-Meteo; banabana lamang ang ulan kag indi pagbasa sang rain gauge sa latagon",
    "Next month recommendation" to "Rekomendasyon para sa masunod nga bulan",
    "Forecast for" to "Tagna para sa",
    "past months used" to "nagligad nga mga bulan ang gin-gamit",
    "Based on historical sensor data" to "Base sa makasaysayan nga datos sang sensor",
    "More closed-month history is needed before forecasting" to "Kinahanglan pa sang kasaysayan sang mga natapos nga bulan antes magtagna",
    "Monthly recommendation history" to "Kasaysayan sang binulan nga rekomendasyon",
    "Forecast results" to "Mga resulta sang tagna",
    "Why this crop" to "Ngaa ini nga tanom",
    "Best matching fields" to "Pinakamaayo nga nagakaangay nga mga field",
    "Fields to monitor" to "Mga field nga bantayan",
    "Used" to "Gin-gamit ang",
    "latest-month readings" to "mga pagbasa sang pinakabag-o nga bulan",
    "NPK and moisture ranges are provisional until locally calibrated" to "Pansamantala pa ang range sang NPK kag kaumog tubtob ma-calibrate sa lokal",
    "Forecast" to "Tagna",
    "Observed" to "Naobserbahan",
    "Observed month" to "Naobserbahan nga bulan",
    "Forecast matched" to "Nag-angay ang tagna",
    "Forecast differed" to "Nagkalain ang tagna",
    "High confidence" to "Mataas nga kumpiyansa",
    "Moderate confidence" to "Kasagarang kumpiyansa",
    "Low confidence" to "Manubo nga kumpiyansa",
    "Limited history" to "Limitado nga kasaysayan",
    "Decision support only: confirm the result with local weather, field conditions, and farming advice before planting" to "Giya lamang ini sa pagdesisyon: kumpirmaha anay sa lokal nga panahon, kondisyon sang latagon, kag laygay sa pagpanguma antes magtanom",
    "Second choice" to "Ikaduha nga pilian",
    "Reading stability" to "Katatagan sang mga pagbasa",
    "Stable readings" to "Matatag nga mga pagbasa",
    "Some variability" to "May pila ka pagbag-o",
    "High variability" to "Daku nga pagbag-o",
    "Unknown stability" to "Wala nahibaluan ang katatagan",
    "Variable fields" to "Mga field nga nagabag-o",
    "Measured ranges" to "Nasukod nga mga range",
    "Change from previous month" to "Pagbag-o halin sa nagligad nga bulan",
    "General Philippine wet season" to "Kasagarang tig-ululan sa Pilipinas",
    "General Philippine dry season" to "Kasagarang tig-ilinit sa Pilipinas",
    "Rainfall is often higher from May to October; confirm drainage and check a local forecast." to "Kasagarang mas madamo ang ulan halin Mayo tubtob Oktubre; kumpirmaha ang drainage kag tan-awa ang lokal nga tagna sang panahon.",
    "Rainfall is often lower from November to April; confirm irrigation and check a local forecast." to "Kasagarang mas diutay ang ulan halin Nobyembre tubtob Abril; kumpirmaha ang irigasyon kag tan-awa ang lokal nga tagna sang panahon.",
    "local farm outcomes considered" to "lokal nga resulta sang uma ang ginkonsiderar",
    "Sensor" to "Sensor",
    "Smart Farming Assistant" to "Maalam nga Manugbulig sa Pagpanguma",
    "Demo" to "Demo",
    "Settings" to "Mga Setting",
    "Back" to "Balik",
    "Farm & Device" to "Uma kag Device",
    "Farm or field name" to "Ngalan sang uma ukon latagon",
    "ESP32 device name" to "Ngalan sang ESP32 device",
    "Farm latitude" to "Latitude sang uma",
    "Farm longitude" to "Longitude sang uma",
    "Farm coordinates let CropCast obtain a 30-day rainfall estimate" to "Ang koordinado sang uma nagatugot sa CropCast nga magkuha sang 30 ka adlaw nga banabana sang ulan",
    "Connection status" to "Kahimtangan sang koneksyon",
    "Online" to "Online",
    "Demo mode" to "Demo mode",
    "Offline" to "Offline",
    "Last synchronization" to "Katapusan nga pag-sync",
    "Waiting for data" to "Nagahulat sang datos",
    "Test connection" to "Tilawi ang koneksyon",
    "Restart ESP32" to "Sugdi liwat ang ESP32",
    "Alerts & Notifications" to "Mga Alerto kag Pahibalo",
    "All farm alerts" to "Tanan nga alerto sa uma",
    "Master control for sensor alerts" to "Panguna nga kontrol sang mga alerto sang sensor",
    "Low soil moisture" to "Manubo nga kaumog sang duta",
    "Warn when moisture is below the threshold" to "Magpahibalo kon manubo sa limitasyon ang kaumog",
    "Soil moisture threshold" to "Limitasyon sang kaumog sang duta",
    "High temperature" to "Mataas nga temperatura",
    "Minimum soil pH" to "Pinakanubo nga pH sang duta",
    "Maximum soil pH" to "Pinakamataas nga pH sang duta",
    "Device offline" to "Device nga offline",
    "Repeat alerts" to "Liwata ang mga alerto",
    "minutes" to "minutos",
    "Every" to "Kada",
    "Sound" to "Tingog",
    "Play a sound for important farm alerts" to "Magpatukar sang tingog para sa importante nga alerto sa uma",
    "Vibration" to "Pag-uyog",
    "Vibrate for important farm alerts" to "Mag-uyog para sa importante nga alerto sa uma",
    "Open Android notification settings" to "Abrihi ang notification settings sang Android",
    "Crop Profile" to "Profile sang Tanom",
    "Current crop" to "Karon nga tanom",
    "Crop variety" to "Sahi sang tanom",
    "Planting date" to "Petsa sang pagtanom",
    "Planting date (YYYY-MM-DD)" to "Petsa sang pagtanom (YYYY-MM-DD)",
    "Not set" to "Wala pa ginbutang",
    "Language & Offline Data" to "Lengguwahe kag Offline nga Datos",
    "Language" to "Lengguwahe",
    "Dark mode" to "Madulom nga mode",
    "Use a darker theme throughout the app" to "Gamita ang madulom nga tema sa bug-os nga app",
    "Offline data availability" to "Pagkaanaa sang offline nga datos",
    "Enabled · Firebase disk cache" to "Aktibo · Firebase disk cache",
    "Cloud history" to "Kasaysayan sa cloud",
    "Keep sensor history synchronized" to "Tipigi nga naka-sync ang kasaysayan sang sensor",
    "Account & Privacy" to "Account kag Pribasidad",
    "Farmer or administrator" to "Mangunguma ukon administrador",
    "Display name" to "Ngalan nga ipakita",
    "Role" to "Papel",
    "Farmer" to "Mangunguma",
    "Administrator" to "Administrador",
    "Sign-in status" to "Kahimtangan sang pag-sign in",
    "Guest / anonymous" to "Bisita / wala nakilala",
    "Change password" to "Ilisi ang password",
    "New password" to "Bag-o nga password",
    "Share diagnostic information" to "Ipaambit ang diagnostic information",
    "Helps troubleshoot sensor connection issues" to "Nagabulig sa pagpangita sang problema sa koneksyon sang sensor",
    "Sign out" to "Mag-sign out",
    "Save" to "Tipigi",
    "Cancel" to "Kanselahon",
    "Change" to "Ilisi",
    "Grow period" to "Panahon sang pagtubo",
    "days" to "mga adlaw",
    "Suitability" to "Pagkaangay",
    "Sensor Status" to "Kahimtangan sang Sensor",
    "Current Conditions" to "Karon nga Kahimtangan",
    "Monthly Sensor Collection" to "Binulan nga Pagtipon sang Datos sang Sensor",
    "Monthly Sensor Readings" to "Binulan nga mga Pagbasa sang Sensor",
    "Current month" to "Karon nga bulan",
    "readings" to "mga pagbasa",
    "Waiting for this month's sensor samples" to "Nagahulat sang mga pagbasa sang sensor sini nga bulan",
    "Waiting for monthly sensor samples" to "Nagahulat sang binulan nga mga pagbasa sang sensor",
    "Monthly averages" to "Binulan nga mga average",
    "Crop recommendations use these monthly averages" to "Ginagamit ang sini nga binulan nga mga average sa pagrekomendar sang tanom",
    "Based on" to "Base sa",
    "sensor readings collected this month" to "mga pagbasa sang sensor nga natipon sini nga bulan",
    "sensor readings from the latest available month" to "mga pagbasa sang sensor halin sa pinakabag-o nga bulan nga may datos",
    "A recommendation will appear after monthly sensor data is collected" to "Magaguwa ang rekomendasyon pagkatapos matipon ang binulan nga datos sang sensor",
    "Soil Moisture" to "Kaumog sang Duta",
    "Temperature" to "Temperatura",
    "Humidity" to "Kaumog sang Hangin",
    "Soil pH" to "pH sang Duta",
    "Nitrogen" to "Nitrogen",
    "Phosphorus" to "Phosphorus",
    "Potassium" to "Potassium",
    "Light Intensity" to "Kusog sang Kahayag",
    "Last sync" to "Katapusan nga sync",
    "waiting" to "nagahulat",
    "Settings saved in demo mode" to "Natipigan ang mga setting sa demo mode",
    "Settings synchronized" to "Na-sync ang mga setting",
    "Could not save settings" to "Indi matipigan ang mga setting"
    ,"Build your local dataset" to "Maghimo sang lokal nga dataset"
    ,"Save what was planted and the harvest result for future local validation" to "Tipigi ang gintanom kag resulta sang ani para sa masunod nga lokal nga pag-validate"
    ,"Current recommendation" to "Karon nga rekomendasyon"
    ,"saved local outcomes" to "natipigan nga lokal nga resulta"
    ,"Record crop outcome" to "Irekord ang resulta sang tanom"
    ,"Planted crop" to "Gintanom nga tanom"
    ,"Harvest weight (kg)" to "Kabug-aton sang ani (kg)"
    ,"Outcome rating (1-5)" to "Marka sang resulta (1-5)"
    ,"Problems or notes (optional)" to "Mga problema ukon nota (opsyonal)"
    ,"Crop outcome saved in demo mode" to "Natipigan ang resulta sang tanom sa demo mode"
    ,"Crop outcome saved" to "Natipigan ang resulta sang tanom"
    ,"Could not save crop outcome" to "Indi matipigan ang resulta sang tanom"
    ,"My Crop" to "Akon nga Tanom"
    ,"No crop set" to "Wala sing natumbok nga tanom"
    ,"Pick the crop growing in your field and its planting date in Settings." to "Pilia sa Settings ang tanom sa inyo uma kag ang petsa sang pagtanom."
    ,"Open Settings" to "Buksi ang Settings"
    ,"Nothing needs doing right now." to "Wala sing kinahanglan buhaton subong."
    ,"What to do" to "Mga dapat himuon"
    ,"Fertilizer plan" to "Plano sa abono"
    ,"Your readings vs crop needs" to "Ang inyo mga pagbasa kontra sa kinahanglan sang tanom"
    ,"Readings: average of the latest month from your device." to "Mga pagbasa: average sang pinakabag-o nga bulan halin sa inyo device."
    ,"Readings: the latest live reading." to "Mga pagbasa: ang pinakabag-o nga live nga pagbasa."
    ,"Demo data: soil values are simulated." to "Demo nga datos: gin-simulate lang ang mga kantidad sang duta."
    ,"Ranges: FAO ECOCROP (temperature, pH) and the CropCast crop table (humidity, moisture). N, P and K use the soil-test levels of the fertilizer plan." to "Mga sakop: FAO ECOCROP (temperatura, pH) kag talaan sang tanom sang CropCast (kalamigon, moisture). Ang N, P kag K nagagamit sang soil-test levels sang plano sa abono."
    ,"planted" to "gintanom"
    ,"Planting date not set" to "Wala pa natumbok ang petsa sang pagtanom"
    ,"Day" to "Adlaw"
    ,"harvest" to "ani"
    ,"Act now" to "Himua na"
    ,"Watch" to "Bantayi"
    ,"Tip" to "Tambag"
    ,"OK" to "OK"
    ,"No range" to "Wala sing sakop"
    ,"Seedling" to "Bag-ong tubo"
    ,"Growing" to "Nagatubo"
    ,"Flowering and fruiting" to "Nagabulak kag nagaunga"
    ,"Ready to harvest" to "Handa na anihon"
    ,"Air temperature" to "Temperatura sang hangin"
    ,"Soil moisture" to "Moisture sang duta"
    ,"Low" to "Mababa"
    ,"High" to "Mataas"
    ,"Normal" to "Normal"
    ,"Best" to "Pinakamaayo"
    ,"Tolerates" to "Matiis"
    ,"Your farm" to "Inyo uma"
    ,"soil test level" to "lebel sa soil test"
    ,"No range for this crop yet" to "Wala pa sing sakop para sa sini nga tanom"
    ,"Field should be drained now" to "Dapat na patuyuon ang uma"
    ,"Ask for" to "Pangayuha"
    ,"Gives" to "Nagahatag sang"
    ,"Common mix" to "Pirmi nga sapaw"
    ,"Single fertilizers" to "Isa-isa nga abono"
    ,"Fewest sacks. Complete and Urea are the fertilizers most farmers already buy." to "Pinakadikit nga sako. Ang Complete kag Urea ang mga abono nga ginabakal na sang kalabanan nga mangunguma."
    ,"One fertilizer per nutrient. Exact doses, but more sacks to buy." to "Isa ka abono kada sustansya. Husto ang dosis, pero mas madamo nga sako ang bakalon."
    ,"Harvest is close: skip fertilizer now. This plan is for the next planting." to "Malapit na ang ani: indi anay mag-abono. Para sa masunod nga pagtanom ang plano nga ini."
    ,"Buy one of these sets:" to "Magbakal sang isa sa sini nga mga set:"
    ,"Nothing is low. These are normal maintenance doses." to "Wala sing mababa. Ini ang mga normal nga dosis sa pag-atipan."
    ,"Soil pH is high, so the nitrogen is Ammosul (21-0-0). It lowers pH a little. Urea (46-0-0) also works if Ammosul is not in stock." to "Mataas ang pH sang duta, gani Ammosul (21-0-0) ang nitrogen. Ginapaubos sini gamay ang pH. Mahimo man ang Urea (46-0-0) kon wala Ammosul."
    ,"Have chicken manure or vermicast? Mix it in too. It feeds the soil, but it does not replace the doses above." to "May tai sang manok ukon vermicast? Sapawa man. Ginapakaon sini ang duta, pero indi ini ang kapuli sang mga dosis sa ibabaw."
    ,"Amounts are per 1,000 m² (10 × 100 m). 1 sack = 50 kg." to "Ang kadamuon kada 1,000 m² (10 × 100 m). 1 sako = 50 kg."
    ,"Now, in a small hole 5 cm beside the roots, then cover" to "Subong, sa gamay nga buho 5 cm sa higad sang ugat, dayon tabunan"
    ,"Now, together with the nitrogen" to "Subong, kaupod sang nitrogen"
    ,"Half now, half in 3–4 weeks, beside the plants" to "Tunga subong, tunga sa 3–4 ka semana, sa higad sang tanom"
    ,"Now, beside the plants, then water it in" to "Subong, sa higad sang tanom, dayon tubigi"
    ,"Flood the field" to "Lunopa ang uma"
    ,"Soil moisture is below what rice needs." to "Mas mababa ang moisture sang duta sangsa kinahanglan sang humay."
    ,"Raise the water to 3–5 cm. Check the bunds for leaks." to "Pasaka ang tubig sa 3–5 cm. Usisaa kon may tagas ang pilapil."
    ,"Water today" to "Magtubig subong"
    ,"Soil moisture is below what this crop needs." to "Mas mababa ang moisture sang duta sangsa kinahanglan sang sini nga tanom."
    ,"Water slowly at the base until the top 15 cm is moist. Water in the early morning. Check again tomorrow." to "Hinay-hinay nga magtubig sa puno tubtob mamasa ang ibabaw nga 15 cm. Magtubig sa aga. Usisaa liwat buas."
    ,"Drain the field" to "Patuyua ang uma"
    ,"Soil moisture is far above what this crop tolerates." to "Mas mataas gid ang moisture sang duta sangsa matiis sang sini nga tanom."
    ,"Open a drainage canal between rows. Stop watering until it drops." to "Maghukay sang kanal sa tunga sang laray. Hunongi ang pagtubig tubtob mag-ubos."
    ,"No more fertilizer" to "Wala na abono"
    ,"Some nutrients read low, but harvest is close." to "May mga sustansya nga mababa ang basa, pero malapit na ang ani."
    ,"Skip fertilizer now. Use the fertilizer plan below before the next planting." to "Indi anay mag-abono. Gamita ang plano sa abono sa idalom antes ang masunod nga pagtanom."
    ,"Add fertilizer" to "Dugangi sang abono"
    ,"Some nutrients read low while the crop is growing, so feed it now." to "May mga sustansya nga mababa ang basa samtang nagatubo ang tanom, gani pakaon na subong."
    ,"See the fertilizer plan below." to "Tan-awa ang plano sa abono sa idalom."
    ,"Humid: blast and sheath blight risk" to "Mabasa ang hangin: peligro sang blast kag sheath blight"
    ,"Humidity and heat are both high." to "Mataas ang kalamigon kag kainit."
    ,"Do not add extra nitrogen. Walk the field and look for diamond-shaped leaf spots." to "Indi magdugang sang nitrogen. Lakata ang uma kag pangitaa ang mantsa nga porma diamante sa dahon."
    ,"Humid: leaf blight risk" to "Mabasa ang hangin: peligro sang leaf blight"
    ,"Look for long grey-brown leaf spots. Remove badly hit lower leaves." to "Pangitaa ang mataas nga abo-kayumanggi nga mantsa sa dahon. Kuhaa ang grabe nga natamaan nga idalom nga dahon."
    ,"Humid: soft rot risk" to "Mabasa ang hangin: peligro sang soft rot"
    ,"Harvest on time. Do not leave cut leaves in the bed." to "Mag-ani sa husto nga oras. Indi pabay-i ang gin-utod nga dahon sa tanaman."
    ,"Warm and humid: wilt and blight risk" to "Mainit kag mabasa ang hangin: peligro sang lanta kag blight"
    ,"Remove yellow lower leaves. Water the soil, not the leaves. Keep space between plants for air." to "Kuhaa ang dilaw nga idalom nga dahon. Tubigi ang duta, indi ang dahon. Mag-iwan sang puwang sa tunga sang tanom para sa hangin."
    ,"Cool for this crop" to "Mabugnaw para sa sini nga tanom"
    ,"Air temperature is below the best range, so growth slows." to "Mas mababa ang temperatura sang hangin sangsa pinakamaayo, gani nagahinay ang pagtubo."
    ,"Cover young plants at night. Do not over-water." to "Taklubi ang bata nga tanom sa gab-i. Indi sobrahi ang tubig."
    ,"Hot for leafy crops" to "Mainit para sa mga tanom nga madahon"
    ,"Air temperature is above the best range. Leafy crops may flower early and turn bitter." to "Mas mataas ang temperatura sang hangin sangsa pinakamaayo. Mahimo nga agud magbulak kag mapait ang tanom nga madahon."
    ,"Give afternoon shade, or harvest a few days early." to "Hatagi sang landong sa hapon, ukon anihon sang pila ka adlaw nga mas aga."
    ,"Warm for this crop" to "Mainit para sa sini nga tanom"
    ,"Air temperature is above the best range. Hot days reduce flowering and fruit set." to "Mas mataas ang temperatura sang hangin sangsa pinakamaayo. Ginapaubos sang mainit nga adlaw ang pagbulak kag pag-unga."
    ,"Mulch with rice straw to keep roots cool. Water in the morning." to "Butangi sang mulch nga dagami agud mabugnaw ang ugat. Magtubig sa aga."
    ,"Soil pH is high for this crop" to "Mataas ang pH sang duta para sa sini nga tanom"
    ,"The probe reads above the best range. Check the probe first." to "Mas mataas ang basa sang probe sangsa pinakamaayo. Usisaa anay ang probe."
    ,"Dip the probe in pH 7 buffer. If the reading holds, use ammonium sulfate (21-0-0) instead of urea and mix in compost." to "Isawsaw ang probe sa pH 7 buffer. Kon indi nagbag-o ang basa, gamita ang ammonium sulfate (21-0-0) imbes nga urea kag sapawa sang compost."
    ,"Soil pH is low for this crop" to "Mababa ang pH sang duta para sa sini nga tanom"
    ,"The probe reads below the best range. Check the probe first." to "Mas mababa ang basa sang probe sangsa pinakamaayo. Usisaa anay ang probe."
    ,"Dip the probe in pH 7 buffer. If the reading holds, mix in agricultural lime (dolomite) before the next planting." to "Isawsaw ang probe sa pH 7 buffer. Kon indi nagbag-o ang basa, sapawa ang agricultural lime (dolomite) antes ang masunod nga pagtanom."
    ,"Drain before harvest" to "Patuyua antes anihon"
    ,"Your rice is ripening." to "Nagahinog na ang inyo humay."
    ,"Keep the field drained 1–2 weeks before harvest so grain dries evenly." to "Pabay-i nga mamala ang uma 1–2 ka semana antes anihon agud pareho ang pagpamala sang binhi."
    ,"Do not let it dry out now" to "Indi pabay-i nga mamala subong"
    ,"Your corn is tasseling." to "Nagabulak na ang inyo mais."
    ,"Tasseling is when corn needs water most. A dry week now cuts the harvest." to "Sa sini nga yugto pinakakinahanglan sang mais ang tubig. Ang isa ka semana nga mamala magapaubos sang ani."
    ,"Keep moisture steady" to "Padayona ang moisture"
    ,"Your crop is flowering and fruiting." to "Nagabulak kag nagaunga ang inyo tanom."
    ,"Dry-then-wet swings cause blossom-end rot (black fruit bottoms). Water a little every day." to "Ang salit-salit nga mamala kag basa nagapahinabo sang blossom-end rot (itom nga idalom sang bunga). Magtubig sang gamay kada adlaw."
    ,"Protect young plants" to "Panalipdi ang bata nga tanom"
    ,"Your crop is a seedling." to "Bag-o pa nga tubo ang inyo tanom."
    ,"Shade at noon on hot days. Check for cutworms at night." to "Landongi sa udto kon mainit. Usisaa kon may ulod sa gab-i."
    ,"Plan the harvest" to "Plano-a ang ani"
    ,"Harvest is close." to "Malapit na ang ani."
    ,"Line up buyers and sacks this week." to "Ihanda ang mga manugbakal kag sako sa sini nga semana."
)

val SUPPORTED_LANGUAGES = listOf("Filipino", "English", "Hiligaynon")

fun localize(english: String, language: String): String = when {
    language.equals("Filipino", ignoreCase = true) -> translateFilipino(english)
    language.equals("Hiligaynon", ignoreCase = true) -> hiligaynon[english] ?: english
    else -> english
}

@Composable
fun tr(english: String): String = localize(english, LocalCropCastLanguage.current)
