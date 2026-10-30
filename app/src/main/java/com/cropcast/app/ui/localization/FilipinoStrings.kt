package com.cropcast.app.ui.localization

/** Filipino (Tagalog) text for every fixed English UI string. Numbers and dates are handled in FilipinoDynamic.kt. */
internal val FILIPINO: Map<String, String> = mapOf(
    // Navigation and home
    "Home" to "Bahay",
    "Welcome" to "Maligayang pagdating",
    "Monthly data readiness" to "Kahandaan ng buwanang datos",
    "Important notices" to "Mahahalagang paalala",
    "Test data is simulated and is not field evidence" to "Ang test na datos ay gawa-gawa lamang at hindi patunay mula sa bukid",
    "View full recommendation" to "Tingnan ang buong rekomendasyon",
    "of" to "sa",
    "required readings collected" to "kinakailangang pagbasa ang nakolekta",
    "Ready for recommendation" to "Handa na para sa rekomendasyon",
    "More readings needed" to "Kailangan pa ng mas maraming pagbasa",
    "No active alerts" to "Walang aktibong alerto",
    "Sensor is offline; using the latest available data" to "Offline ang sensor; ginagamit ang pinakabagong datos na mayroon",
    "Recommendation" to "Rekomendasyon",
    "Sensor details" to "Detalye ng sensor",
    "Crop Recommendation" to "Rekomendasyon sa Pananim",
    "Best crop for your farm" to "Pinakamainam na pananim para sa iyong bukid",
    "Best crops for your farm" to "Pinakamainam na mga pananim para sa iyong bukid",
    "Sensor" to "Sensor",
    "Smart Farming Assistant" to "Matalinong Katulong sa Pagsasaka",
    "Demo" to "Demo",
    "Notifications" to "Mga Abiso",
    "Settings" to "Mga Setting",
    "Back" to "Bumalik",
    "Close" to "Isara",
    // ML and recommendation
    "ML crop recommendation" to "Rekomendasyon sa pananim gamit ang ML",
    "Top recommendation" to "Pangunahing rekomendasyon",
    "Top match from 22 crops" to "Pinakaangkop mula sa 22 pananim",
    "experimental model score" to "eksperimental na marka ng modelo",
    "model agreement" to "pagsang-ayon ng modelo",
    "model agreement, not field success probability" to "pagsang-ayon ng modelo, hindi tsansa ng tagumpay sa bukid",
    "High model agreement" to "Mataas na pagsang-ayon ng modelo",
    "Moderate model agreement" to "Katamtamang pagsang-ayon ng modelo",
    "Low model agreement" to "Mababang pagsang-ayon ng modelo",
    "Agreement unavailable" to "Walang marka ng pagsang-ayon",
    "At least eight valid monthly readings are needed for the ML recommendation" to "Kailangan ng hindi bababa sa walong wastong buwanang pagbasa para sa rekomendasyon ng ML",
    "Public-data ML recommendations" to "Mga rekomendasyon ng ML mula sa pampublikong datos",
    "Comparison: 22-crop ML model" to "Paghahambing: ML model na may 22 pananim",
    "The public crop model could not be loaded" to "Hindi ma-load ang pampublikong modelo ng pananim",
    "Collect at least" to "Mangolekta ng hindi bababa sa",
    "valid readings for the 22-crop model" to "wastong pagbasa para sa modelong may 22 pananim",
    "model score" to "marka ng modelo",
    "Experimental 22-crop model using" to "Eksperimental na modelong may 22 pananim na gumagamit ng",
    "latest-month readings; rainfall is excluded because the device does not measure it" to "pagbasa ng pinakabagong buwan; hindi isinama ang ulan dahil hindi ito sinusukat ng device",
    "Sensor conditions are outside the public dataset range" to "Ang kondisyon ng sensor ay labas sa saklaw ng pampublikong dataset",
    "Check these readings" to "Suriin ang mga pagbasang ito",
    "The sensor readings are invalid for crop recommendation" to "Hindi wasto ang mga pagbasa ng sensor para sa rekomendasyon ng pananim",
    "No reliable crop match was found for these readings" to "Walang mapagkakatiwalaang katugmang pananim para sa mga pagbasang ito",
    "No reliable crop match was found; the closest experimental matches are shown below" to "Walang mapagkakatiwalaang katugma; ipinapakita sa ibaba ang pinakamalapit na eksperimental na resulta",
    "Rainfall is needed before this model can recommend a crop" to "Kailangan ang datos ng ulan bago makapagrekomenda ng pananim ang modelong ito",
    "No recommendation: readings are outside the public dataset range" to "Walang rekomendasyon: labas sa saklaw ng pampublikong dataset ang mga pagbasa",
    "30-day weather rainfall" to "Ulan sa nakaraang 30 araw",
    "Loading rainfall estimate" to "Kinukuha ang tantiya ng ulan",
    "Add valid farm coordinates in Settings to include rainfall" to "Maglagay ng wastong koordinado ng bukid sa Settings para maisama ang ulan",
    "Weather unavailable; using the six-input offline model" to "Walang datos ng panahon; ginagamit ang offline na modelong may anim na input",
    "Using the six-input offline model" to "Ginagamit ang offline na modelong may anim na input",
    "Weather data by Open-Meteo; rainfall is an estimate, not a field gauge reading" to "Datos ng panahon mula sa Open-Meteo; tantiya lamang ang ulan at hindi pagbasa ng rain gauge sa bukid",
    "Rainfall" to "Ulan",
    "Next month recommendation" to "Rekomendasyon para sa susunod na buwan",
    "Forecast for" to "Tagna para sa",
    "past months used" to "nakaraang buwan ang ginamit",
    "Based on historical sensor data" to "Batay sa nakaraang datos ng sensor",
    "More closed-month history is needed before forecasting" to "Kailangan pa ng kasaysayan ng mga natapos na buwan bago makapagtagna",
    "Monthly recommendation history" to "Kasaysayan ng buwanang rekomendasyon",
    "Forecast results" to "Mga resulta ng tagna",
    "Why this crop" to "Bakit ang pananim na ito",
    "Why" to "Bakit",
    "Best matching fields" to "Pinakaangkop na mga field",
    "Fields to monitor" to "Mga field na dapat bantayan",
    "Used" to "Ginamit ang",
    "latest-month readings" to "pagbasa ng pinakabagong buwan",
    "NPK and moisture ranges are provisional until locally calibrated" to "Pansamantala pa ang saklaw ng NPK at halumigmig hangga't hindi pa nakakalibrate sa lokal",
    "Forecast" to "Tagna",
    "Observed" to "Naobserbahan",
    "Observed month" to "Naobserbahang buwan",
    "Forecast matched" to "Tumugma ang tagna",
    "Forecast differed" to "Hindi tumugma ang tagna",
    "High confidence" to "Mataas na kumpiyansa",
    "Moderate confidence" to "Katamtamang kumpiyansa",
    "Low confidence" to "Mababang kumpiyansa",
    "Limited history" to "Limitadong kasaysayan",
    "Decision support only: confirm the result with local weather, field conditions, and farming advice before planting" to "Gabay lamang sa pagpapasya: kumpirmahin muna sa lokal na panahon, kondisyon ng bukid, at payo sa pagsasaka bago magtanim",
    "Second choice" to "Pangalawang pinili",
    "Reading stability" to "Katatagan ng mga pagbasa",
    "Stable readings" to "Matatag na mga pagbasa",
    "Some variability" to "May kaunting pagbabago",
    "High variability" to "Malaking pagbabago",
    "Unknown stability" to "Hindi alam ang katatagan",
    "Variable fields" to "Mga field na nagbabago",
    "Measured ranges" to "Nasukat na saklaw",
    "Change from previous month" to "Pagbabago mula sa nakaraang buwan",
    "General Philippine wet season" to "Karaniwang tag-ulan sa Pilipinas",
    "General Philippine dry season" to "Karaniwang tag-init sa Pilipinas",
    "Season unavailable" to "Walang impormasyon sa panahon",
    "Add a valid target month to show general seasonal guidance." to "Maglagay ng wastong buwan para makita ang pangkalahatang gabay sa panahon.",
    "Rainfall is often higher from May to October; confirm drainage and check a local forecast." to "Karaniwang mas malakas ang ulan mula Mayo hanggang Oktubre; tiyaking maayos ang drainage at tingnan ang lokal na pagtataya ng panahon.",
    "Rainfall is often lower from November to April; confirm irrigation and check a local forecast." to "Karaniwang mas mahina ang ulan mula Nobyembre hanggang Abril; tiyaking may irigasyon at tingnan ang lokal na pagtataya ng panahon.",
    "local farm outcomes considered" to "lokal na resulta ng bukid ang isinaalang-alang",
    // Farm recommendation cards
    "Loading climate and soil maps for your farm…" to "Kinukuha ang mapa ng klima at lupa para sa iyong bukid…",
    "Waiting for soil readings or farm location data" to "Naghihintay ng pagbasa sa lupa o lokasyon ng bukid",
    "All checked conditions are in the optimal range" to "Lahat ng sinuring kondisyon ay nasa pinakamainam na saklaw",
    "Main limit" to "Pangunahing limitasyon",
    "gross" to "kabuuang kita",
    "Best income among suitable crops" to "Pinakamalaking kita sa mga angkop na pananim",
    "FAO suitability classes" to "Mga klase ng kaangkupan ng FAO",
    "planting this month" to "pagtatanim ngayong buwan",
    "income: PSA national yield × farmgate price, before costs" to "kita: pambansang ani ng PSA × presyo sa bukid, bago ang gastos",
    "AI explanation" to "Paliwanag ng AI",
    "Explain with AI" to "Ipaliwanag gamit ang AI",
    "Explain again" to "Ipaliwanag muli",
    "AI advice is unavailable right now. The ranking and plan above still apply." to "Hindi available ang payo ng AI ngayon. Nananatiling may bisa ang ranggo at plano sa itaas.",
    "Written by an AI from the numbers above. It can make mistakes; confirm with a BSWM soil test or your local agriculturist." to "Isinulat ng AI batay sa mga numero sa itaas. Maaari itong magkamali; kumpirmahin sa soil test ng BSWM o sa inyong lokal na agrikulturista.",
    "Best months to plant" to "Pinakamainam na buwan ng pagtatanim",
    "Soil and fertilizer plan for" to "Plano sa lupa at pataba para sa",
    "growing now" to "tumutubo ngayon",
    "What to buy and how much to use. Amounts are for every 1,000 m² of field, for example a 25 m × 40 m plot." to "Ano ang bibilhin at gaano karami ang gagamitin. Ang dami ay para sa bawat 1,000 m² ng bukid, halimbawa ay lote na 25 m × 40 m.",
    "Low" to "Mababa",
    "High" to "Mataas",
    "OK" to "Ayos",
    "Can't find these? Ask the agri-supply store for the same numbers (like 46-0-0) or a complete fertilizer such as 14-14-14. N, P and K do not change the crop ranking because fertilizer can correct them. Confirm with a BSWM soil test or your municipal agriculturist." to "Hindi makita ang mga ito? Itanong sa agri-supply store ang parehong numero (tulad ng 46-0-0) o kumpletong pataba tulad ng 14-14-14. Hindi binabago ng N, P at K ang ranggo ng pananim dahil maaari itong ituwid ng pataba. Kumpirmahin sa soil test ng BSWM o sa inyong agrikulturista sa munisipyo.",
    "Before planting" to "Bago magtanim",
    "Data used" to "Mga datos na ginamit",
    "Also suitable" to "Angkop din",
    "Alugbati crop" to "Pananim na alugbati",
    "Highly suitable" to "Lubhang angkop",
    "Moderately suitable" to "Katamtamang angkop",
    "Marginally suitable" to "Bahagyang angkop",
    "Not suitable" to "Hindi angkop",
    // Settings
    "Farm & Device" to "Bukid at Device",
    "Farm or field name" to "Pangalan ng bukid o taniman",
    "ESP32 device name" to "Pangalan ng ESP32 device",
    "Farm latitude" to "Latitude ng bukid",
    "Farm longitude" to "Longitude ng bukid",
    "Farm coordinates let CropCast obtain a 30-day rainfall estimate" to "Sa koordinado ng bukid, makakakuha ang CropCast ng tantiya ng ulan sa nakaraang 30 araw",
    "Pick farm on map or use my location" to "Pumili ng bukid sa mapa o gamitin ang aking lokasyon",
    "Farm location" to "Lokasyon ng bukid",
    "Tap the map to place your farm." to "Pindutin ang mapa para ilagay ang iyong bukid.",
    "Use my current location" to "Gamitin ang kasalukuyan kong lokasyon",
    "Save location" to "I-save ang lokasyon",
    "Finding your location…" to "Hinahanap ang iyong lokasyon…",
    "Could not get your location. Turn on GPS and try again, or tap the map." to "Hindi makuha ang iyong lokasyon. I-on ang GPS at subukan muli, o pindutin ang mapa.",
    "Location permission was denied. Tap the map to place your farm instead." to "Tinanggihan ang pahintulot sa lokasyon. Pindutin na lang ang mapa para ilagay ang iyong bukid.",
    "Connection status" to "Kalagayan ng koneksyon",
    "Online" to "Online",
    "Demo mode" to "Demo mode",
    "Offline" to "Offline",
    "Last synchronization" to "Huling pag-sync",
    "Waiting for data" to "Naghihintay ng datos",
    "Test connection" to "Subukan ang koneksyon",
    "Restart ESP32" to "I-restart ang ESP32",
    "Pair a device" to "Ipares ang device",
    "Power the ESP32. If it was paired before, hold its BOOT button for 5 seconds." to "Buksan ang ESP32. Kung naipares na ito dati, pindutin at hawakan ang BOOT button nito sa loob ng 5 segundo.",
    "On your phone, join the Wi-Fi network named CropCast-XXXX." to "Sa iyong telepono, kumonekta sa Wi-Fi na may pangalang CropCast-XXXX.",
    "In the setup page, choose your Wi-Fi, enter its password and this pairing code:" to "Sa setup page, piliin ang iyong Wi-Fi, ilagay ang password nito at ang pairing code na ito:",
    "Waiting for the device to come online" to "Hinihintay na mag-online ang device",
    "Alerts & Notifications" to "Mga Alerto at Abiso",
    "All farm alerts" to "Lahat ng alerto sa bukid",
    "Master control for sensor alerts" to "Pangunahing kontrol ng mga alerto ng sensor",
    "Low soil moisture" to "Mababang halumigmig ng lupa",
    "Warn when moisture is below the threshold" to "Magbabala kapag mas mababa sa limitasyon ang halumigmig",
    "Soil moisture threshold" to "Limitasyon ng halumigmig ng lupa",
    "High temperature" to "Mataas na temperatura",
    "Minimum soil pH" to "Pinakamababang pH ng lupa",
    "Maximum soil pH" to "Pinakamataas na pH ng lupa",
    "Device offline" to "Offline ang device",
    "Repeat alerts" to "Ulitin ang mga alerto",
    "minutes" to "minuto",
    "Every" to "Bawat",
    "Sound" to "Tunog",
    "Play a sound for important farm alerts" to "Magpatunog para sa mahahalagang alerto sa bukid",
    "Vibration" to "Pag-vibrate",
    "Vibrate for important farm alerts" to "Mag-vibrate para sa mahahalagang alerto sa bukid",
    "Open Android notification settings" to "Buksan ang notification settings ng Android",
    "Crop Profile" to "Profile ng Pananim",
    "Current crop" to "Kasalukuyang pananim",
    "Crop variety" to "Uri ng pananim",
    "Planting date" to "Petsa ng pagtatanim",
    "Planting date (YYYY-MM-DD)" to "Petsa ng pagtatanim (YYYY-MM-DD)",
    "Not set" to "Hindi pa naitakda",
    "None" to "Wala",
    "pH advice only" to "payo sa pH lamang",
    "Irrigation available" to "May irigasyon",
    "Dry months will not lower crop scores; too much rain still does" to "Hindi ibababa ng tuyong buwan ang marka ng pananim; ang sobrang ulan ay bumababa pa rin",
    "Language & Offline Data" to "Wika at Offline na Datos",
    "Language" to "Wika",
    "Dark mode" to "Madilim na mode",
    "Light mode" to "Maliwanag na mode",
    "Use a darker theme throughout the app" to "Gumamit ng mas madilim na tema sa buong app",
    "Switch to dark mode" to "Lumipat sa madilim na mode",
    "Switch to light mode" to "Lumipat sa maliwanag na mode",
    "Offline data availability" to "Availability ng offline na datos",
    "Enabled · Firebase disk cache" to "Naka-on · Firebase disk cache",
    "Cloud history" to "Kasaysayan sa cloud",
    "Keep sensor history synchronized" to "Panatilihing naka-sync ang kasaysayan ng sensor",
    "Account & Privacy" to "Account at Privacy",
    "Account name" to "Pangalan ng account",
    "Display name" to "Pangalan na ipapakita",
    "Farmer" to "Magsasaka",
    "Guest Farmer" to "Bisitang Magsasaka",
    "Sign-in status" to "Kalagayan ng pag-sign in",
    "Guest / anonymous" to "Bisita / hindi kilala",
    "Change password" to "Palitan ang password",
    "New password" to "Bagong password",
    "Share diagnostic information" to "Ibahagi ang diagnostic information",
    "Helps troubleshoot sensor connection issues" to "Nakakatulong sa paghanap ng problema sa koneksyon ng sensor",
    "Sign out" to "Mag-sign out",
    "Save" to "I-save",
    "Cancel" to "Kanselahin",
    "Change" to "Palitan",
    "Grow period" to "Panahon ng pagtubo",
    "days" to "araw",
    "Suitability" to "Kaangkupan",
    // Sensor screen
    "Sensor Status" to "Kalagayan ng Sensor",
    "Current Conditions" to "Kasalukuyang Kondisyon",
    "Monthly Sensor Collection" to "Buwanang Koleksyon ng Sensor",
    "Monthly Sensor Readings" to "Buwanang Pagbasa ng Sensor",
    "Current month" to "Kasalukuyang buwan",
    "readings" to "pagbasa",
    "Waiting for this month's sensor samples" to "Naghihintay ng sample ng sensor ngayong buwan",
    "Waiting for monthly sensor samples" to "Naghihintay ng buwanang sample ng sensor",
    "Monthly averages" to "Buwanang average",
    "Crop recommendations use these monthly averages" to "Ginagamit ang buwanang average na ito sa rekomendasyon ng pananim",
    "Based on" to "Batay sa",
    "sensor readings collected this month" to "pagbasa ng sensor na nakolekta ngayong buwan",
    "sensor readings from the latest available month" to "pagbasa ng sensor mula sa pinakabagong buwan na may datos",
    "A recommendation will appear after monthly sensor data is collected" to "Lalabas ang rekomendasyon pagkatapos makolekta ang buwanang datos ng sensor",
    "Soil Moisture" to "Halumigmig ng Lupa",
    "Temperature" to "Temperatura",
    "Humidity" to "Halumigmig ng Hangin",
    "Soil pH" to "pH ng Lupa",
    "Nitrogen" to "Nitrogen",
    "Phosphorus" to "Phosphorus",
    "Potassium" to "Potassium",
    "Light Intensity" to "Lakas ng Liwanag",
    "Last sync" to "Huling sync",
    "waiting" to "naghihintay",
    "Field soil sample" to "Sample ng lupa sa bukid",
    "Walk the field in a zig-zag. At each of 5-10 spots, push the probe 15-20 cm deep, wait for a new reading, then tap Record spot." to "Maglakad sa bukid nang paikot-ikot. Sa bawat isa sa 5-10 puwesto, isaksak ang probe nang 15-20 cm ang lalim, hintayin ang bagong pagbasa, saka pindutin ang Itala ang puwesto.",
    "Record spot" to "Itala ang puwesto",
    "Save sample" to "I-save ang sample",
    "Clear" to "Burahin",
    "spots" to "puwesto",
    "Last sample" to "Huling sample",
    "spread" to "pagitan",
    "pH varies more than 1.0 across the field; consider managing the areas separately." to "Mahigit 1.0 ang pagkakaiba ng pH sa buong bukid; isaalang-alang ang hiwalay na pangangalaga sa bawat bahagi.",
    "Tap a card to see what it means" to "Pindutin ang card para malaman ang kahulugan nito",
    // Outcome feedback
    "Build your local dataset" to "Buuin ang iyong lokal na dataset",
    "Save what was planted and the harvest result for future local validation" to "I-save ang itinanim at ang resulta ng ani para sa susunod na lokal na pagpapatunay",
    "Current recommendation" to "Kasalukuyang rekomendasyon",
    "saved local outcomes" to "nai-save na lokal na resulta",
    "Record crop outcome" to "Itala ang resulta ng pananim",
    "Planted crop" to "Itinanim na pananim",
    "Harvest weight (kg)" to "Timbang ng ani (kg)",
    "Outcome rating (1-5)" to "Marka ng resulta (1-5)",
    "Problems or notes (optional)" to "Mga problema o tala (opsyonal)",
    "Crop outcome saved in demo mode" to "Na-save ang resulta ng pananim sa demo mode",
    "Crop outcome saved" to "Na-save ang resulta ng pananim",
    "Could not save crop outcome" to "Hindi ma-save ang resulta ng pananim",
    "Settings saved in demo mode" to "Na-save ang mga setting sa demo mode",
    "Settings synchronized" to "Na-sync ang mga setting",
    "Could not save settings" to "Hindi ma-save ang mga setting",
    // Login and register
    "Welcome to CropCast" to "Maligayang pagdating sa CropCast",
    "Sign in to monitor your farm and sensor data." to "Mag-sign in para mabantayan ang iyong bukid at datos ng sensor.",
    "Create your account to start monitoring your farm." to "Gumawa ng account para simulang bantayan ang iyong bukid.",
    "Email address" to "Email address",
    "Password" to "Password",
    "Confirm password" to "Kumpirmahin ang password",
    "Full name" to "Buong pangalan",
    "Show password" to "Ipakita ang password",
    "Hide password" to "Itago ang password",
    "Log in" to "Mag-log in",
    "Register" to "Mag-rehistro",
    "Create account" to "Gumawa ng account",
    "Continue with Google" to "Magpatuloy gamit ang Google",
    "Continue as guest" to "Magpatuloy bilang bisita",
    "Continue in demo mode" to "Magpatuloy sa demo mode",
    "or" to "o",
    "New here? Create an account" to "Bago dito? Gumawa ng account",
    "Already have an account? Log in" to "May account na? Mag-log in",
    "Firebase is not configured. Log in starts a local demo session; cloud verification and synchronization are unavailable." to "Hindi pa naka-configure ang Firebase. Ang pag-log in ay magsisimula ng lokal na demo; hindi available ang cloud verification at pag-sync.",
    "CropCast logo" to "Logo ng CropCast",
    "Enter your name" to "Ilagay ang iyong pangalan",
    "Passwords do not match" to "Hindi magkatugma ang mga password",
    "Enter your email address" to "Ilagay ang iyong email address",
    "Enter a valid email address" to "Maglagay ng wastong email address",
    "Password must contain at least 6 characters" to "Dapat hindi bababa sa 6 na karakter ang password",
    "Login failed" to "Hindi nakapag-log in",
    "Wrong email or password" to "Maling email o password",
    "This email is already registered. Log in instead." to "Nakarehistro na ang email na ito. Mag-log in na lang.",
    "Check your internet connection and try again" to "Suriin ang iyong internet at subukan muli",
    "Registration failed" to "Hindi nakapag-rehistro",
    "Account created" to "Nagawa na ang account",
    "Registration needs a Firebase account. Add google-services.json first." to "Kailangan ng Firebase account sa pagrerehistro. Idagdag muna ang google-services.json.",
    "Google sign-in requires Firebase configuration" to "Kailangan ng Firebase configuration para sa Google sign-in",
    "Add a Google account to your device, then try again" to "Magdagdag ng Google account sa iyong device, saka subukan muli",
    "Google sign-in failed. Please try again." to "Hindi nagtagumpay ang Google sign-in. Pakisubukan muli.",
    "Could not continue as guest" to "Hindi makapagpatuloy bilang bisita",
    "Signed out" to "Naka-sign out na",
    "Sign out failed" to "Hindi nakapag-sign out",
    // Messages from the view model
    "Pairing needs a signed-in Firebase account" to "Kailangan ng naka-sign in na Firebase account para sa pagpapares",
    "Device paired" to "Naipares na ang device",
    "Pairing failed" to "Hindi nagtagumpay ang pagpapares",
    "Demo mode is working; Firebase is not configured" to "Gumagana ang demo mode; hindi pa naka-configure ang Firebase",
    "ESP32 connection verified" to "Napatunayan ang koneksyon sa ESP32",
    "Firebase connected, but the ESP32 status is missing" to "Nakakonekta ang Firebase, ngunit wala ang status ng ESP32",
    "Connection test failed" to "Hindi nagtagumpay ang pagsubok ng koneksyon",
    "Restart command simulated in demo mode" to "Ginaya ang restart command sa demo mode",
    "Restart command sent to the ESP32" to "Naipadala ang restart command sa ESP32",
    "Restart command failed" to "Hindi nagtagumpay ang restart command",
    "Profile saved in demo mode" to "Na-save ang profile sa demo mode",
    "Profile updated" to "Na-update ang profile",
    "Could not update profile" to "Hindi ma-update ang profile",
    "Password changes require an email account" to "Kailangan ng email account para mapalitan ang password",
    "Password changed" to "Napalitan ang password",
    "Password change failed" to "Hindi napalitan ang password",
    "Enter the crop that was planted" to "Ilagay ang pananim na itinanim",
    "Harvest weight cannot be negative" to "Hindi puwedeng negatibo ang timbang ng ani",
    "Choose an outcome rating from 1 to 5" to "Pumili ng marka ng resulta mula 1 hanggang 5",
    "Wait for a valid probe reading, then record the spot" to "Hintayin ang wastong pagbasa ng probe, saka itala ang puwesto",
    "Move the probe to a new spot and wait for a fresh reading" to "Ilipat ang probe sa bagong puwesto at hintayin ang bagong pagbasa",
    "The recorded spots were not valid readings" to "Hindi wasto ang mga naitalang pagbasa",
    "Field sample saved in demo mode" to "Na-save ang sample sa bukid sa demo mode",
    "Field sample saved" to "Na-save ang sample sa bukid",
    "Could not save the field sample" to "Hindi ma-save ang sample sa bukid",
    // Sensor explanations
    "Current reading" to "Kasalukuyang pagbasa",
    "What is it?" to "Ano ito?",
    "Why it matters" to "Bakit ito mahalaga",
    "Good range" to "Magandang saklaw",
    "If too low" to "Kapag masyadong mababa",
    "If too high" to "Kapag masyadong mataas",
    "How much water is in the soil around the probe, shown as a percentage." to "Dami ng tubig sa lupa sa paligid ng probe, ipinapakita bilang porsyento.",
    "Roots drink water from the soil. This tells you if your crop needs watering now or if the soil is too wet." to "Sumisipsip ng tubig ang mga ugat mula sa lupa. Ipinapakita nito kung kailangan nang diligan ang pananim o kung sobrang basa na ang lupa.",
    "Most vegetables grow best at 50–70%. Rice likes wet, flooded soil." to "Karamihan ng gulay ay lumalago nang maayos sa 50–70%. Ang palay ay mas gusto ang basa at binabahang lupa.",
    "Below 40%: the soil is dry and plants wilt. Water the field." to "Mas mababa sa 40%: tuyo ang lupa at nalalanta ang halaman. Diligan ang bukid.",
    "Above 80%: the soil is waterlogged. Roots can rot, so improve drainage." to "Mas mataas sa 80%: babad sa tubig ang lupa. Maaaring mabulok ang ugat, kaya ayusin ang drainage.",
    "How hot or cool the air is around the sensor, in degrees Celsius (°C)." to "Gaano kainit o kalamig ang hangin sa paligid ng sensor, sa degrees Celsius (°C).",
    "Each crop grows in a certain temperature. Too hot or too cold slows growth and flowering." to "May angkop na temperatura ang bawat pananim. Kapag masyadong mainit o malamig, bumabagal ang paglaki at pamumulaklak.",
    "Most crops here grow best at 20–30 °C." to "Karamihan ng pananim dito ay lumalago nang maayos sa 20–30 °C.",
    "Below 15 °C: growth slows down." to "Mas mababa sa 15 °C: bumabagal ang paglaki.",
    "Above 35 °C: flowers drop and plants get stressed. Give shade and water." to "Mas mataas sa 35 °C: nalalagas ang bulaklak at nai-stress ang halaman. Bigyan ng lilim at tubig.",
    "How much water vapor is in the air, shown as a percentage." to "Dami ng singaw ng tubig sa hangin, ipinapakita bilang porsyento.",
    "Damp air helps fungus and leaf diseases spread. Very dry air makes plants lose water fast." to "Ang mamasa-masang hangin ay nagpapadali sa pagkalat ng amag at sakit sa dahon. Ang sobrang tuyong hangin ay mabilis na nag-aalis ng tubig sa halaman.",
    "50–80% suits most crops." to "Angkop sa karamihan ng pananim ang 50–80%.",
    "Below 40%: plants lose water fast. Water more often." to "Mas mababa sa 40%: mabilis na nawawalan ng tubig ang halaman. Dagdagan ang pagdidilig.",
    "Above 85%: fungal diseases like blight and mildew spread. Keep good airflow." to "Mas mataas sa 85%: kumakalat ang sakit na amag tulad ng blight at mildew. Panatilihing maayos ang daloy ng hangin.",
    "How acidic or alkaline the soil is, on a scale of 0 to 14. 7 is neutral." to "Gaano kaasim o ka-alkaline ang lupa, sa sukat na 0 hanggang 14. Ang 7 ay neutral.",
    "pH decides whether plants can take up nutrients. A wrong pH locks nutrients in the soil even when you add fertilizer." to "Ang pH ang nagpapasya kung kayang kunin ng halaman ang sustansya. Kapag mali ang pH, nakakulong ang sustansya sa lupa kahit magdagdag ng pataba.",
    "Most crops grow best at pH 5.5–7.0." to "Karamihan ng pananim ay lumalago nang maayos sa pH 5.5–7.0.",
    "Below 5.5: the soil is too acidic. Apply agricultural lime (dolomite)." to "Mas mababa sa 5.5: masyadong maasim ang lupa. Maglagay ng agricultural lime (dolomite).",
    "Above 7.5: the soil is too alkaline. Mix in compost or use ammonium sulfate." to "Mas mataas sa 7.5: masyadong alkaline ang lupa. Haluan ng compost o gumamit ng ammonium sulfate.",
    "N, a nutrient in the soil that makes leaves and stems grow, measured in mg/kg." to "Ang N ay sustansya sa lupa na nagpapalago ng dahon at tangkay, sinusukat sa mg/kg.",
    "Nitrogen keeps plants green and leafy." to "Pinapanatili ng nitrogen na luntian at malago ang halaman.",
    "About 125–250 mg/kg is a good level." to "Ang 125–250 mg/kg ay magandang antas.",
    "Below 125: pale yellow leaves and slow growth. Add urea (46-0-0)." to "Mas mababa sa 125: namumutlang dilaw ang dahon at mabagal ang paglaki. Magdagdag ng urea (46-0-0).",
    "Above 250: too many leaves, weak fruit and more pests." to "Mas mataas sa 250: sobrang dahon, mahinang bunga at mas maraming peste.",
    "P, a nutrient that helps roots, flowers and fruit, measured in mg/kg." to "Ang P ay sustansyang tumutulong sa ugat, bulaklak at bunga, sinusukat sa mg/kg.",
    "Phosphorus builds strong roots and helps plants flower and set fruit." to "Pinapatibay ng phosphorus ang ugat at tinutulungan ang halaman na mamulaklak at mamunga.",
    "About 10–25 mg/kg is a good level." to "Ang 10–25 mg/kg ay magandang antas.",
    "Below 10: purple-tinted leaves and weak roots. Add Solophos (0-18-0) before planting." to "Mas mababa sa 10: lilang kulay ng dahon at mahinang ugat. Magdagdag ng Solophos (0-18-0) bago magtanim.",
    "Above 25: more than the plant needs. Skip phosphorus fertilizer this season." to "Mas mataas sa 25: higit sa kailangan ng halaman. Huwag munang maglagay ng phosphorus ngayong season.",
    "K, a nutrient that helps fruit fill out and keeps plants healthy, measured in mg/kg." to "Ang K ay sustansyang tumutulong na mabuo ang bunga at nagpapanatiling malusog ang halaman, sinusukat sa mg/kg.",
    "Potassium makes strong stems, sweet fruit and better resistance to disease and drought." to "Pinapatibay ng potassium ang tangkay, pinatatamis ang bunga at pinalalakas laban sa sakit at tagtuyot.",
    "About 78–156 mg/kg is a good level." to "Ang 78–156 mg/kg ay magandang antas.",
    "Below 78: weak stems, yellow leaf edges and poor fruit. Add muriate of potash (0-0-60)." to "Mas mababa sa 78: mahinang tangkay, madilaw na gilid ng dahon at pangit na bunga. Magdagdag ng muriate of potash (0-0-60).",
    "Above 156: already enough. Extra potassium can block calcium and magnesium." to "Mas mataas sa 156: sapat na. Ang sobrang potassium ay maaaring humarang sa calcium at magnesium.",
    "How bright the light is at the sensor, measured in lux." to "Gaano kaliwanag ang ilaw sa sensor, sinusukat sa lux.",
    "Plants use light to make food (photosynthesis). It also shows if your crop is shaded." to "Gumagamit ang halaman ng liwanag para gumawa ng pagkain (photosynthesis). Ipinapakita rin nito kung nasisilungan ang pananim.",
    "Open sun is about 30,000–100,000 lux. Most vegetables need many hours of bright light a day." to "Ang bukas na sikat ng araw ay mga 30,000–100,000 lux. Karamihan ng gulay ay nangangailangan ng maraming oras ng maliwanag na ilaw bawat araw.",
    "Too dark: slow, leggy growth. Trim nearby shade or move the crop." to "Masyadong madilim: mabagal at payat ang paglaki. Putulin ang nakasilong o ilipat ang pananim.",
    "Very strong light with heat can scorch leaves. Give shade in the hottest hours." to "Ang matinding liwanag na may init ay maaaring makasunog ng dahon. Bigyan ng lilim sa pinakamainit na oras.",
    "Jan" to "Ene", "Feb" to "Peb", "Mar" to "Mar", "Apr" to "Abr", "May" to "May", "Jun" to "Hun",
    "Jul" to "Hul", "Aug" to "Ago", "Sep" to "Set", "Oct" to "Okt", "Nov" to "Nob", "Dec" to "Dis",
    // Crops
    "Tomato" to "Kamatis",
    "Okra" to "Okra",
    "Alugbati" to "Alugbati",
    "Potato" to "Patatas",
    "Rice" to "Palay",
    "Corn" to "Mais",
    "Eggplant" to "Talong",
    "Cucumber" to "Pipino",
    "Cabbage" to "Repolyo",
    "Sweet Potato" to "Kamote",
    "Lettuce" to "Letsugas",
    "Spinach" to "Spinach",
    "Pechay" to "Pechay",
    "Kangkong" to "Kangkong",
    "Ampalaya" to "Ampalaya",
    "Sitaw" to "Sitaw",
    "Kalabasa" to "Kalabasa",
    "Chili" to "Sili",
    "Onion" to "Sibuyas",
    "Garlic" to "Bawang",
    "Mungbean" to "Munggo",
    "Peanut" to "Mani",
    "Ginger" to "Luya",
    "Gabi" to "Gabi",
    "Patola" to "Patola",
    "Corn (Maize)" to "Mais",
    "Banana" to "Saging",
    "Mango" to "Mangga",
    "Coconut" to "Niyog",
    "Coffee" to "Kape",
    "Cotton" to "Bulak",
    "Grapes" to "Ubas",
    "Jute" to "Yute",
    "Papaya" to "Papaya",
    "Orange" to "Dalandan",
    "Apple" to "Mansanas",
    "Watermelon" to "Pakwan",
    "Muskmelon" to "Milon",
    "Pomegranate" to "Granada",
    "Chickpea" to "Garbansos",
    "Lentil" to "Lentil",
    "Kidney Beans" to "Kidney beans",
    "Moth Beans" to "Moth beans",
    "Mung Bean" to "Munggo",
    "Pigeon Peas" to "Pigeon peas",
    "Black Gram" to "Black gram",
    // Crop varieties and soils
    "Roma" to "Roma",
    "Native" to "Native",
    "Green stem" to "Berdeng tangkay",
    "Common" to "Karaniwan",
    "Lowland" to "Lowland",
    "Yellow" to "Dilaw",
    "Slicing" to "Panghiwa",
    "Green" to "Berde",
    "Leaf" to "Dahon",
    "Upland" to "Upland",
    "Hybrid" to "Hybrid",
    "Pole" to "Pole",
    "Siling labuyo" to "Siling labuyo",
    "Red" to "Pula",
    "Ilocos" to "Ilocos",
    "Loamy" to "Lupang-buhaghag",
    "Well-drained loam" to "Lupang-buhaghag na maayos ang drainage",
    "Rich loam" to "Mayamang lupang-buhaghag",
    "Loose, well-drained loam" to "Maluwag na lupang may maayos na drainage",
    "Clay loam / paddy" to "Luwad-buhaghag / palayan",
    "Loose sandy loam" to "Maluwag na mabuhanging lupa",
    "Fertile loam" to "Matabang lupang-buhaghag",
    "Moist loam to clay" to "Mamasa-masang lupang-buhaghag hanggang luwad",
    "Light, sandy loam" to "Magaan at mabuhanging lupa",
    "Loose, well-drained loam" to "Maluwag na lupang may maayos na drainage",
    // Data sources
    "7-in-1 soil probe" to "7-in-1 na soil probe",
    "DHT11 air sensor" to "DHT11 na sensor ng hangin",
    "FAO ECOCROP crop ranges" to "Saklaw ng pananim ng FAO ECOCROP",
    "Open-Meteo rainfall history" to "Kasaysayan ng ulan ng Open-Meteo",
    "PSA yields and farmgate prices" to "Ani at presyo sa bukid ng PSA",
    "NASA POWER climate normals" to "Karaniwang klima ng NASA POWER",
    "Open-Meteo 16-day forecast" to "16-araw na tagna ng Open-Meteo",
    "ISRIC SoilGrids soil map" to "Mapa ng lupa ng ISRIC SoilGrids",
    "ECMWF seasonal forecast" to "Pana-panahong tagna ng ECMWF",
    // Engine factor names, risks and advice that never change
    "Soil pH limit" to "Limitasyon ng pH ng lupa",
    "Season rainfall" to "Ulan sa panahon ng pagtatanim",
    "Soil texture" to "Tekstura ng lupa",
    "Soil salinity" to "Alat ng lupa",
    "Wet-season disease" to "Sakit sa tag-ulan",
    "Heavy rain at planting" to "Malakas na ulan sa pagtatanim",
    "Fertilizer" to "Pataba",
    "Dry or cool season: low disease pressure" to "Tuyo o malamig na panahon: mababa ang banta ng sakit",
    "Typhoon season (Jul–Nov) falls on flowering and ripening, when wind damage is heaviest. Stake or trellis firmly, or plant so the crop flowers outside July–November." to "Ang panahon ng bagyo (Hul–Nob) ay tumatapat sa pamumulaklak at pagkahinog, kung kailan pinakamalala ang pinsala ng hangin. Itukod o ilatag nang matibay ang pananim, o magtanim nang hindi sasabay sa Hulyo–Nobyembre ang pamumulaklak.",
    "high risk of bacterial wilt and late blight. Use raised beds and resistant varieties." to "mataas ang panganib ng bacterial wilt at late blight. Gumamit ng nakataas na taniman at lumalaban na uri.",
    "high risk of downy mildew. Trellis for airflow and avoid overhead watering." to "mataas ang panganib ng downy mildew. Gumamit ng trellis para sa daloy ng hangin at iwasan ang pagdidilig mula sa itaas.",
    "high risk of black rot and soft rot. Ensure drainage and rotate fields." to "mataas ang panganib ng black rot at soft rot. Tiyaking maayos ang drainage at magpalit-palit ng taniman.",
    "Spread agricultural lime (dolomite) and mix it into the soil 2-3 weeks before planting. Ask the agri-supply store or your municipal agriculturist how many sacks your soil needs." to "Magkalat ng agricultural lime (dolomite) at ihalo sa lupa 2-3 linggo bago magtanim. Itanong sa agri-supply store o sa agrikulturista ng munisipyo kung ilang sako ang kailangan ng inyong lupa.",
    "Mix in compost, and use ammonium sulfate (21-0-0) as your nitrogen fertilizer." to "Haluan ng compost, at gumamit ng ammonium sulfate (21-0-0) bilang pataba ng nitrogen.",
    "No pH correction needed" to "Hindi kailangang ayusin ang pH",
    "makes leaves and stems grow" to "nagpapalago ng dahon at tangkay",
    "helps roots, flowers and fruit" to "tumutulong sa ugat, bulaklak at bunga",
    "helps fruit fill out and keeps plants healthy" to "tumutulong na mabuo ang bunga at nagpapanatiling malusog ang halaman",
    "Add a full dose." to "Maglagay ng buong dosis.",
    "Level is fine: give a normal dose." to "Maayos ang antas: magbigay ng karaniwang dosis.",
    "Soil already has plenty: a small top-up is enough." to "Marami nang sustansya ang lupa: sapat na ang kaunting dagdag.",
    "Crop is already growing: top-dress now, beside the plants and not on the leaves, then water it in." to "Tumutubo na ang pananim: mag-top-dress ngayon sa tabi ng halaman at hindi sa dahon, saka diligan.",
    "Phosphorus is meant for before planting. If you skipped it, place it beside the roots now." to "Ang phosphorus ay para sa bago magtanim. Kung hindi ito nailagay, ilagay ito ngayon sa tabi ng ugat.",
    "Spread half at planting and half 3-4 weeks later." to "Ikalat ang kalahati sa pagtatanim at ang kalahati 3-4 na linggo pagkatapos.",
    "Mix it into the soil before planting." to "Ihalo sa lupa bago magtanim.",
    "Climate and soil maps are not loaded yet. Set the farm latitude and longitude in Settings and connect to the internet once." to "Hindi pa nakukuha ang mapa ng klima at lupa. Itakda ang latitude at longitude ng bukid sa Settings at kumonekta sa internet kahit minsan."
    ,"My Crop" to "Aking Pananim"
    ,"No crop set" to "Walang itinakdang pananim"
    ,"Pick the crop growing in your field and its planting date in Settings." to "Piliin sa Settings ang pananim sa inyong bukid at ang petsa ng pagtatanim."
    ,"Open Settings" to "Buksan ang Settings"
    ,"Nothing needs doing right now." to "Walang kailangang gawin sa ngayon."
    ,"What to do" to "Mga dapat gawin"
    ,"Fertilizer plan" to "Plano sa pataba"
    ,"Your readings vs crop needs" to "Ang inyong mga pagbasa laban sa kailangan ng pananim"
    ,"Readings: average of the latest month from your device." to "Mga pagbasa: average ng pinakabagong buwan mula sa inyong device."
    ,"Readings: the latest live reading." to "Mga pagbasa: ang pinakabagong live na pagbasa."
    ,"Demo data: soil values are simulated." to "Demo na datos: gawa-gawa lamang ang mga halaga ng lupa."
    ,"Ranges: FAO ECOCROP (temperature, pH) and the CropCast crop table (humidity, moisture). N, P and K use the soil-test levels of the fertilizer plan." to "Mga saklaw: FAO ECOCROP (temperatura, pH) at talaan ng pananim ng CropCast (halumigmig, moisture). Ang N, P at K ay gumagamit ng soil-test levels ng plano sa pataba."
    ,"planted" to "itinanim"
    ,"Planting date not set" to "Hindi pa naitakda ang petsa ng pagtatanim"
    ,"Day" to "Araw"
    ,"harvest" to "ani"
    ,"Act now" to "Kumilos na"
    ,"Watch" to "Bantayan"
    ,"Tip" to "Payo"
    ,"No range" to "Walang saklaw"
    ,"Seedling" to "Punla"
    ,"Growing" to "Lumalaki"
    ,"Flowering and fruiting" to "Namumulaklak at namumunga"
    ,"Ready to harvest" to "Handa nang anihin"
    ,"Air temperature" to "Temperatura ng hangin"
    ,"Soil moisture" to "Moisture ng lupa"
    ,"Normal" to "Normal"
    ,"Best" to "Pinakamainam"
    ,"Tolerates" to "Kayang tiisin"
    ,"Your farm" to "Inyong bukid"
    ,"soil test level" to "antas sa soil test"
    ,"No range for this crop yet" to "Wala pang saklaw para sa pananim na ito"
    ,"Field should be drained now" to "Dapat nang patuyuin ang bukid"
    ,"Ask for" to "Hanapin"
    ,"Gives" to "Nagbibigay ng"
    ,"Common mix" to "Karaniwang halo"
    ,"Single fertilizers" to "Isa-isang pataba"
    ,"Fewest sacks. Complete and Urea are the fertilizers most farmers already buy." to "Pinakakaunting sako. Ang Complete at Urea ang mga pataba na binibili na ng karamihan ng magsasaka."
    ,"One fertilizer per nutrient. Exact doses, but more sacks to buy." to "Isang pataba bawat sustansya. Eksakto ang dosis, pero mas maraming sakong bibilhin."
    ,"Harvest is close: skip fertilizer now. This plan is for the next planting." to "Malapit na ang ani: huwag munang magpataba. Para sa susunod na pagtatanim ang planong ito."
    ,"Buy one of these sets:" to "Bumili ng isa sa mga set na ito:"
    ,"Nothing is low. These are normal maintenance doses." to "Walang mababa. Ito ang mga normal na dosis sa pangangalaga."
    ,"Soil pH is high, so the nitrogen is Ammosul (21-0-0). It lowers pH a little. Urea (46-0-0) also works if Ammosul is not in stock." to "Mataas ang pH ng lupa, kaya Ammosul (21-0-0) ang nitrogen. Bahagya nitong ibinababa ang pH. Puwede rin ang Urea (46-0-0) kung walang Ammosul."
    ,"Have chicken manure or vermicast? Mix it in too. It feeds the soil, but it does not replace the doses above." to "May dumi ng manok o vermicast? Ihalo rin. Pinapakain nito ang lupa, pero hindi nito pinapalitan ang mga dosis sa itaas."
    ,"Amounts are per 1,000 m² (10 × 100 m). 1 sack = 50 kg." to "Ang dami ay kada 1,000 m² (10 × 100 m). 1 sako = 50 kg."
    ,"Now, in a small hole 5 cm beside the roots, then cover" to "Ngayon, sa maliit na butas 5 cm sa tabi ng ugat, saka tabunan"
    ,"Now, together with the nitrogen" to "Ngayon, kasabay ng nitrogen"
    ,"Half now, half in 3–4 weeks, beside the plants" to "Kalahati ngayon, kalahati sa 3–4 na linggo, sa tabi ng halaman"
    ,"Now, beside the plants, then water it in" to "Ngayon, sa tabi ng halaman, saka diligan"
    ,"Flood the field" to "Lunurin ang bukid"
    ,"Soil moisture is below what rice needs." to "Mas mababa ang moisture ng lupa kaysa kailangan ng palay."
    ,"Raise the water to 3–5 cm. Check the bunds for leaks." to "Itaas ang tubig sa 3–5 cm. Suriin kung may tagas ang pilapil."
    ,"Water today" to "Magdilig ngayon"
    ,"Soil moisture is below what this crop needs." to "Mas mababa ang moisture ng lupa kaysa kailangan ng pananim na ito."
    ,"Water slowly at the base until the top 15 cm is moist. Water in the early morning. Check again tomorrow." to "Dahan-dahang magdilig sa puno hanggang mamasa ang itaas na 15 cm. Magdilig sa madaling-araw. Suriin muli bukas."
    ,"Drain the field" to "Patuyuin ang bukid"
    ,"Soil moisture is far above what this crop tolerates." to "Higit na mataas ang moisture ng lupa kaysa kaya ng pananim na ito."
    ,"Open a drainage canal between rows. Stop watering until it drops." to "Magbukas ng kanal sa pagitan ng hanay. Itigil ang pagdilig hanggang bumaba."
    ,"No more fertilizer" to "Wala nang pataba"
    ,"Some nutrients read low, but harvest is close." to "May mga sustansyang mababa ang basa, pero malapit na ang ani."
    ,"Skip fertilizer now. Use the fertilizer plan below before the next planting." to "Huwag munang magpataba. Gamitin ang plano sa pataba sa ibaba bago ang susunod na pagtatanim."
    ,"Add fertilizer" to "Magdagdag ng pataba"
    ,"Some nutrients read low while the crop is growing, so feed it now." to "May mga sustansyang mababa ang basa habang lumalaki ang pananim, kaya pakainin na ngayon."
    ,"See the fertilizer plan below." to "Tingnan ang plano sa pataba sa ibaba."
    ,"Humid: blast and sheath blight risk" to "Mahalumigmig: panganib ng blast at sheath blight"
    ,"Humidity and heat are both high." to "Mataas ang halumigmig at init."
    ,"Do not add extra nitrogen. Walk the field and look for diamond-shaped leaf spots." to "Huwag magdagdag ng nitrogen. Lakarin ang bukid at hanapin ang batik na hugis-diyamante sa dahon."
    ,"Humid: leaf blight risk" to "Mahalumigmig: panganib ng leaf blight"
    ,"Look for long grey-brown leaf spots. Remove badly hit lower leaves." to "Hanapin ang mahahabang kulay-abo-kayumangging batik sa dahon. Alisin ang malubhang tinamaang ibabang dahon."
    ,"Humid: soft rot risk" to "Mahalumigmig: panganib ng soft rot"
    ,"Harvest on time. Do not leave cut leaves in the bed." to "Mag-ani sa tamang oras. Huwag iwanan ang pinutol na dahon sa taniman."
    ,"Warm and humid: wilt and blight risk" to "Mainit at mahalumigmig: panganib ng lanta at blight"
    ,"Remove yellow lower leaves. Water the soil, not the leaves. Keep space between plants for air." to "Alisin ang dilaw na ibabang dahon. Diligan ang lupa, hindi ang dahon. Mag-iwan ng pagitan ng halaman para sa hangin."
    ,"Cool for this crop" to "Malamig para sa pananim na ito"
    ,"Air temperature is below the best range, so growth slows." to "Mas mababa ang temperatura ng hangin kaysa pinakamainam, kaya bumabagal ang paglaki."
    ,"Cover young plants at night. Do not over-water." to "Takpan ang mga batang halaman sa gabi. Huwag sobrahan ang tubig."
    ,"Hot for leafy crops" to "Mainit para sa madahong pananim"
    ,"Air temperature is above the best range. Leafy crops may flower early and turn bitter." to "Mas mataas ang temperatura ng hangin kaysa pinakamainam. Maaaring maagang mamulaklak at mapait ang madahong pananim."
    ,"Give afternoon shade, or harvest a few days early." to "Bigyan ng lilim sa hapon, o anihin nang ilang araw na mas maaga."
    ,"Warm for this crop" to "Mainit para sa pananim na ito"
    ,"Air temperature is above the best range. Hot days reduce flowering and fruit set." to "Mas mataas ang temperatura ng hangin kaysa pinakamainam. Binabawasan ng mainit na araw ang pamumulaklak at pamumunga."
    ,"Mulch with rice straw to keep roots cool. Water in the morning." to "Maglagay ng mulch na dayami para malamig ang ugat. Magdilig sa umaga."
    ,"Soil pH is high for this crop" to "Mataas ang pH ng lupa para sa pananim na ito"
    ,"The probe reads above the best range. Check the probe first." to "Mas mataas ang basa ng probe kaysa pinakamainam. Suriin muna ang probe."
    ,"Dip the probe in pH 7 buffer. If the reading holds, use ammonium sulfate (21-0-0) instead of urea and mix in compost." to "Isawsaw ang probe sa pH 7 buffer. Kung hindi nagbago ang basa, gumamit ng ammonium sulfate (21-0-0) sa halip na urea at maghalo ng compost."
    ,"Soil pH is low for this crop" to "Mababa ang pH ng lupa para sa pananim na ito"
    ,"The probe reads below the best range. Check the probe first." to "Mas mababa ang basa ng probe kaysa pinakamainam. Suriin muna ang probe."
    ,"Dip the probe in pH 7 buffer. If the reading holds, mix in agricultural lime (dolomite) before the next planting." to "Isawsaw ang probe sa pH 7 buffer. Kung hindi nagbago ang basa, ihalo ang agricultural lime (dolomite) bago ang susunod na pagtatanim."
    ,"Drain before harvest" to "Patuyuin bago anihin"
    ,"Your rice is ripening." to "Humihinog na ang inyong palay."
    ,"Keep the field drained 1–2 weeks before harvest so grain dries evenly." to "Panatilihing tuyo ang bukid 1–2 linggo bago anihin para pantay ang pagkatuyo ng butil."
    ,"Do not let it dry out now" to "Huwag hayaang matuyo ngayon"
    ,"Your corn is tasseling." to "Namumulaklak na ang inyong mais."
    ,"Tasseling is when corn needs water most. A dry week now cuts the harvest." to "Sa yugtong ito pinakakailangan ng mais ang tubig. Ang isang linggong tuyo ay magpapababa ng ani."
    ,"Keep moisture steady" to "Panatilihing tuloy ang moisture"
    ,"Your crop is flowering and fruiting." to "Namumulaklak at namumunga ang inyong pananim."
    ,"Dry-then-wet swings cause blossom-end rot (black fruit bottoms). Water a little every day." to "Ang salit-salit na tuyo at basa ay nagdudulot ng blossom-end rot (itim na ilalim ng bunga). Magdilig nang kaunti araw-araw."
    ,"Protect young plants" to "Protektahan ang mga batang halaman"
    ,"Your crop is a seedling." to "Punla pa ang inyong pananim."
    ,"Shade at noon on hot days. Check for cutworms at night." to "Lilim sa tanghali kapag mainit. Tingnan kung may uod sa gabi."
    ,"Plan the harvest" to "Planuhin ang ani"
    ,"Harvest is close." to "Malapit na ang ani."
    ,"Line up buyers and sacks this week." to "Ihanda ang mga mamimili at sako ngayong linggo."
)
