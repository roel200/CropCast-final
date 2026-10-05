package com.cropcast.app.ui.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {
    @Test
    fun `hiligaynon translates core navigation and settings labels`() {
        assertEquals("Balay", localize("Home", "Hiligaynon"))
        assertEquals("Kahandaan sang binulan nga datos", localize("Monthly data readiness", "Hiligaynon"))
        assertEquals("Mga Setting", localize("Settings", "Hiligaynon"))
        assertEquals("Rekomendasyon sang Tanom", localize("Crop Recommendation", "Hiligaynon"))
    }

    @Test
    fun `english and unknown text remain unchanged`() {
        assertEquals("Settings", localize("Settings", "English"))
        assertEquals("CropCast", localize("CropCast", "Hiligaynon"))
    }

    @Test
    fun `filipino translates fixed labels and crop names`() {
        assertEquals("Bahay", localize("Home", "Filipino"))
        assertEquals("Halumigmig ng Lupa", localize("Soil Moisture", "Filipino"))
        assertEquals("Kamatis", localize("Tomato", "Filipino"))
        assertEquals("Okt", localize("Oct", "Filipino"))
    }

    @Test
    fun `filipino translates engine sentences that carry numbers`() {
        assertEquals(
            "Panahon 24.1–27.3 °C (pinakamainam 20–27 °C)",
            localize("Season 24.1–27.3 °C (best 20–27 °C)", "Filipino")
        )
        assertEquals(
            "Bumaba ang halumigmig ng lupa sa 35%",
            localize("Moisture dropped to 35%", "Filipino")
        )
        assertEquals(
            "Kamatis: Maulan at maalinsangang panahon: mataas ang panganib ng bacterial wilt at late blight. " +
                "Gumamit ng nakataas na taniman at lumalaban na uri.",
            localize(
                "Tomato: Wet, warm season: high risk of bacterial wilt and late blight. Use raised beds and resistant varieties.",
                "Filipino"
            )
        )
        assertEquals(
            "Maayos ang antas: magbigay ng karaniwang dosis. Bumili ng Urea (46-0-0): 8 kg kada 1,000 m² " +
                "(mga ½ sako na 50 kg). Katumbas ito ng 80 kg kada ektarya. Ihalo sa lupa bago magtanim.",
            localize(
                "Level is fine: give a normal dose. Buy Urea (46-0-0): 8 kg per 1,000 m² (about ½ sack of 50 kg). " +
                    "That is 80 kg per hectare. Mix it into the soil before planting.",
                "Filipino"
            )
        )
    }

    @Test
    fun `filipino leaves unknown text unchanged`() {
        assertEquals("CropCast", localize("CropCast", "Filipino"))
        assertEquals("Soil: unknown", localize("Soil: unknown", "Filipino"))
    }
}
