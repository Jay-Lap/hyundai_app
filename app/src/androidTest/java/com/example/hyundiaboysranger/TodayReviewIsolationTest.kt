package com.example.hyundiaboysranger

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Uses uniquely named stores; existing user settings and review data are untouched. */
class TodayReviewIsolationTest {
    @Test fun twoExplicitStopsAndGuidanceAcceptanceUseOnlyIsolatedStores() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val reviews = ReviewRepository(context, "today_review_$suffix")
        val settings = SettingsRepository(context, "today_settings_$suffix")
        val initial = UserSettings(UserProfile.SENIOR, true, true, true, true, false, DisplayMode.NIGHT, 180)
        assertTrue(settings.save(initial))
        assertNull(reviews.overview())
        assertTrue(reviews.setConsent(true))
        val first = UUID.randomUUID().toString(); val second = UUID.randomUUID().toString()
        assertTrue(reviews.saveAttempt(first, "start", "test"))
        assertTrue(reviews.saveAttempt(first, "stop", "test"))
        assertTrue(reviews.saveAttempt(second, "start", "test"))
        assertTrue(reviews.saveAttempt(second, "stop", "test"))
        assertTrue(hasRepeatedExplicitStops(reviews.overview()!!.events))
        val before = reviews.overview()
        assertTrue(settings.enableDetailedGuidance())
        assertEquals(initial.copy(detailedGuidance = true), settings.load())
        assertEquals(before, reviews.overview())
    }
}
