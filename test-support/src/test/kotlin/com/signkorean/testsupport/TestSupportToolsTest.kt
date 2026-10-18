package com.signkorean.testsupport

import com.signkorean.domain.model.CardReviewState
import com.signkorean.testsupport.fixture.CardFixtureBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TestSupportToolsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `fake clock stays deterministic until advanced`() {
        val start = Instant.parse("2026-10-18T00:00:00Z")
        val fakeClock = FakeClock(start, ZoneId.of("Asia/Seoul"))

        fakeClock.advanceBy(Duration.ofMinutes(25))

        assertEquals(Instant.parse("2026-10-18T00:25:00Z"), fakeClock.now())
        assertEquals(ZoneId.of("Asia/Seoul"), fakeClock.zoneId())
    }

    @Test
    fun `locale fixture returns stable English and Japanese locales`() {
        assertEquals("en-US", LocaleFixture.englishUs.toLanguageTag())
        assertEquals("ja-JP", LocaleFixture.japaneseJapan.toLanguageTag())
    }

    @Test
    fun `card fixture builder creates an overrideable learning card`() {
        val card =
            CardFixtureBuilder()
                .apply {
                    id = "sign-20"
                    state = "answer"
                }.build()

        assertEquals("sign-20", card.id)
        assertEquals("약국", card.korean)
        assertEquals("answer", card.state)
    }

    @Test
    fun `stateful fake learning repository updates only the requested card`() {
        val repository = FakeLearningRepository()

        repository.setReviewState("sign-01", CardReviewState.Due)

        assertEquals(CardReviewState.Due, repository.getReviewState("sign-01"))
        assertEquals(CardReviewState.NotFound, repository.getReviewState("sign-02"))
    }

    @Test
    fun `temporary database helper removes its owned directory on close`() {
        val database = TemporaryDatabase.create()
        val databasePath = database.databasePath

        assertTrue(databasePath.toFile().exists())

        database.close()

        assertFalse(databasePath.toFile().exists())
    }

    @Test
    fun `main dispatcher rule runs queued main work deterministically`() =
        runTest {
            var completed = false

            launch(Dispatchers.Main) { completed = true }

            assertFalse(completed)
            mainDispatcherRule.scheduler.advanceUntilIdle()
            assertTrue(completed)
        }

    @Test
    fun `screenshot case validates stable artifact names`() {
        val artifact =
            ScreenshotTestHarness.capture(
                ScreenshotCase("learn-card-active", widthDp = 390, localeTag = "en-US"),
            ) { case -> ScreenshotArtifact(case.id, byteArrayOf(1, 2, 3)) }

        assertEquals("learn-card-active", artifact.caseId)
    }
}
