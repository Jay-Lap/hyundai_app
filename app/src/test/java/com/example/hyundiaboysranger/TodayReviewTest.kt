package com.example.hyundiaboysranger

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TodayReviewTest {
    private fun id() = UUID.randomUUID().toString()

    @Test fun consentEmptyAndProfileLimitsNeverWriteOnRead() {
        val prefs = FakePreferences()
        val repo = ReviewRepository(prefs)
        assertNull(repo.overview())
        assertTrue(todayItems(repo.overview(), UserProfile.SENIOR).isEmpty())
        assertEquals(0, prefs.commits)
        assertTrue(repo.setConsent(true))
        val commits = prefs.commits
        assertTrue(todayItems(repo.overview(), UserProfile.BEGINNER).isEmpty())
        assertEquals(commits, prefs.commits)
        listOf("engine", "brake", "tire", "washer").forEach { assertTrue(repo.addWarning(it)) }
        val snapshot = repo.overview()
        assertEquals(1, todayItems(snapshot, UserProfile.SENIOR).size)
        assertEquals(3, todayItems(snapshot, UserProfile.BEGINNER).size)
        assertEquals(3, todayItems(snapshot, UserProfile.CUSTOM).size)
    }

    @Test fun onlyTwoDistinctConsecutiveExplicitStopsQualify() {
        val prefs = FakePreferences(); val repo = ReviewRepository(prefs)
        assertTrue(repo.setConsent(true))
        val first = id(); val second = id()
        assertTrue(repo.saveAttempt(first, "start", "test"))
        assertTrue(repo.saveAttempt(first, "stop", "test"))
        assertFalse(hasRepeatedExplicitStops(repo.overview()!!.events))
        assertTrue(repo.saveAttempt(second, "start", "test"))
        assertTrue(repo.saveAttempt(second, "stop", "test"))
        assertTrue(hasRepeatedExplicitStops(repo.overview()!!.events))
        assertFalse(repo.saveAttempt(first, "start", "test"))
        val recent = repo.overview()!!.events
        val ambiguousDuplicate = recent.last().copy(original = "{\"attempt_id\":\"$first\",\"attempt_phase\":\"unknown\"}")
        assertFalse(hasRepeatedExplicitStops(recent + ambiguousDuplicate))
        assertFalse(hasRepeatedExplicitStops(listOf(recent[1], recent[0], recent[2], recent[3])))
        val third = id()
        assertTrue(repo.saveAttempt(third, "start", "test"))
        assertTrue(repo.saveAttempt(third, "complete", "test"))
        assertFalse(hasRepeatedExplicitStops(repo.overview()!!.events))
        assertFalse(repo.saveAttempt(third, "stop", "test"))
    }

    @Test fun todayItemsChooseNewestWithoutCreatingNewRecords() {
        val items = listOf(
            ReviewItem("warning", "engine", "saved", 10),
            ReviewItem("wiper", "front_wiper", "started", 40),
            ReviewItem("warning", "brake", "saved", 20),
            ReviewItem("warning", "tire", "saved", 30),
        )
        val overview = ReviewOverview(items, emptyList())
        assertEquals(listOf("front_wiper"), todayItems(overview, UserProfile.SENIOR).map { it.id })
        assertEquals(listOf("front_wiper", "tire", "brake"), todayItems(overview, UserProfile.BEGINNER).map { it.id })
    }

    @Test fun legacyOrAmbiguousStartsAndCompletionsNeverQualify() {
        val prefs = FakePreferences(); val repo = ReviewRepository(prefs)
        assertTrue(repo.setConsent(true))
        repeat(4) { assertTrue(repo.save(false, "legacy")) }
        assertFalse(hasRepeatedExplicitStops(repo.overview()!!.events))
        val session = id()
        assertTrue(repo.saveAttempt(session, "start", "test"))
        assertFalse(repo.saveAttempt(id(), "stop", "test"))
        assertTrue(repo.saveAttempt(session, "complete", "test"))
        assertFalse(hasRepeatedExplicitStops(repo.overview()!!.events))
        val invalid = ReviewEvent("wiper", "front_wiper", "started", 1, "x", "{\"attempt_id\":\"not-uuid\",\"attempt_phase\":\"stop\"}")
        assertFalse(hasRepeatedExplicitStops(List(4) { invalid }))
    }

    @Test fun explicitAcceptPreservesOtherFieldsAndFailureIsUnknown() {
        val settingsPrefs = FakePreferences(); val settings = SettingsRepository(settingsPrefs)
        val reviewPrefs = FakePreferences(); val review = ReviewRepository(reviewPrefs)
        val initial = UserSettings(UserProfile.SENIOR, true, true, true, true, false, DisplayMode.NIGHT, 180)
        assertTrue(settings.save(initial)); assertTrue(review.setConsent(true)); assertTrue(review.addWarning("engine"))
        val reviewBefore = reviewPrefs.disk.toMap(); val settingsBefore = settingsPrefs.disk.toMap()
        // Dismiss/later: do not invoke the acceptance operation.
        assertEquals(settingsBefore, settingsPrefs.disk); assertEquals(reviewBefore, reviewPrefs.disk)
        settingsPrefs.reportSuccess = false
        assertFalse(settings.enableDetailedGuidance())
        assertEquals(settingsBefore, settingsPrefs.disk)
        assertEquals(SettingsStorageKind.WRITE_UNKNOWN, settings.status().kind)
        assertEquals(reviewBefore, reviewPrefs.disk)
        settingsPrefs.reportSuccess = true
        assertTrue(settings.retryWrite())
        assertEquals(initial.copy(detailedGuidance = true), settings.load())
        assertEquals(reviewBefore, reviewPrefs.disk)
    }

    @Test fun successfulAcceptanceChangesOnlyDetailedGuidance() {
        val prefs = FakePreferences(); val repo = SettingsRepository(prefs)
        val original = UserSettings(UserProfile.BEGINNER, false, true, false, true, false, DisplayMode.DAY, 150)
        assertTrue(repo.save(original))
        assertTrue(repo.enableDetailedGuidance())
        assertEquals(original.copy(detailedGuidance = true), repo.load())
    }

    @Test fun uncertainStartCannotBecomeStopUntilTheSameStartIsRetried() {
        val prefs = FakePreferences(); val repo = ReviewRepository(prefs)
        assertTrue(repo.setConsent(true))
        val attempt = id()
        prefs.reportSuccess = false
        assertFalse(repo.saveAttempt(attempt, "start", "test"))
        assertFalse(repo.saveAttempt(attempt, "stop", "test"))
        assertNull(repo.overview())
        prefs.reportSuccess = true
        assertTrue(repo.retryWrite())
        assertTrue(repo.saveAttempt(attempt, "stop", "test"))
        assertEquals(listOf("stop", "start"), repo.overview()!!.events.map { attemptMarker(it)?.phase })
        assertFalse(hasRepeatedExplicitStops(repo.overview()!!.events))
    }

    @Test fun damagedOrFutureStoresRemainProtected() {
        for (values in listOf(mapOf<String, Any>("schema_version" to 99, "consent" to true),
            mapOf<String, Any>("consent" to true, "updated" to 3L))) {
            val prefs = FakePreferences(values); val repo = ReviewRepository(prefs)
            assertNull(repo.overview())
            assertFalse(repo.saveAttempt(id(), "start", "test"))
            assertEquals(values, prefs.disk); assertEquals(0, prefs.commits)
        }
        val futureSettings = FakePreferences(mapOf("schema_version" to 99))
        assertFalse(SettingsRepository(futureSettings).enableDetailedGuidance())
        assertEquals(0, futureSettings.commits)
    }
}
