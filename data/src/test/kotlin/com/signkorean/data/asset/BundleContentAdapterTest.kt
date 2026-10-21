package com.signkorean.data.asset

import com.signkorean.domain.model.VoiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundleContentAdapterTest {
    @Test
    fun `release bundle validates 20 cards 20 images 20 Typecast files and 20 Gemini files`() {
        val adapter = BundleContentAdapter(ClasspathAssetSource())

        val cards = adapter.getCards()
        val report = adapter.validateBundle()

        assertEquals(20, report.cardCount)
        assertEquals(20, report.imageCount)
        assertEquals(20, report.typecastAudioCount)
        assertEquals(20, report.geminiAudioCount)
        assertEquals(setOf("en", "ja"), report.languages)
        assertEquals(report.cardCount, cards.size)
        assertEquals((1..20).toList(), cards.map { it.order })
        assertTrue(cards.all { it.imageAssetUri.startsWith("file:///android_asset/") })
        assertEquals(20, cards.count { VoiceId.TYPECAST_EUNKYUNG in it.audioAssetUris })
        assertEquals(20, cards.count { VoiceId.GEMINI_KORE in it.audioAssetUris })
        assertTrue(cards.all { it.audioAssetUris.values.all { uri -> uri.startsWith("file:///android_asset/") } })
    }

    @Test
    fun `loads a complete localized copy for English and Japanese`() {
        val adapter = BundleContentAdapter(ClasspathAssetSource())
        val cardIds = adapter.getCards().map { it.id }.toSet()

        assertEquals(cardIds, adapter.getLocalizedCopy("en").map { it.cardId }.toSet())
        assertEquals(cardIds, adapter.getLocalizedCopy("ja").map { it.cardId }.toSet())
        assertTrue(adapter.getLocalizedCopy("en").all { it.wordParts.isNotEmpty() })
        assertTrue(adapter.getLocalizedCopy("ja").all { it.wordParts.isNotEmpty() })
        assertTrue(adapter.getLocalizedCopy("en").all { it.pronunciationGuide.isNotBlank() })
        assertTrue(adapter.getLocalizedCopy("en").all { it.imageAltTextQuestion.isNotBlank() && it.imageAltTextAnswer.isNotBlank() })
        assertTrue(adapter.getLocalizedCopy("en").all { !it.reviewer.isNullOrBlank() && it.reviewedAt == "2026-10-21" })
    }

    @Test
    fun `rejects an English card without native review metadata`() {
        val assets = validAssets().toMutableMap()
        assets[EN_CARDS_PATH] = assets.getValue(EN_CARDS_PATH).replaceFirst(
            "\"reviewer\":\"native-english-review\"",
            "\"reviewer\":null",
        )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "English reviewer")
    }

    @Test
    fun `rejects duplicate card IDs`() {
        val assets = validAssets().toMutableMap()
        assets[BASE_CARDS_PATH] = assets.getValue(BASE_CARDS_PATH).replaceFirst("02-gyeongbokgung", "01-hongdae-entrance")

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "duplicate card id")
    }

    @Test
    fun `rejects an out of bounds focus rectangle`() {
        val assets = validAssets().toMutableMap()
        assets[BASE_CARDS_PATH] = assets.getValue(BASE_CARDS_PATH).replaceFirst("\"x\":40", "\"x\":101")

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "focus")
    }

    @Test
    fun `rejects a locale card without word parts`() {
        val assets = validAssets().toMutableMap()
        assets[EN_CARDS_PATH] =
            assets
                .getValue(
                    EN_CARDS_PATH,
                ).replaceFirst(
                    "\"wordParts\":[{\"korean\":\"홍대\",\"meaning\":\"Hongik University · Hongdae\"},{\"korean\":\"입구\",\"meaning\":\"entrance\"}]",
                    "\"wordParts\":[]",
                )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "wordParts")
    }

    @Test
    fun `rejects question alt text that reveals the answer`() {
        val assets = validAssets().toMutableMap()
        assets[EN_CARDS_PATH] =
            assets.getValue(EN_CARDS_PATH).replaceFirst(
                "A Korean street sign with its text highlighted.",
                "A sign reading 홍대입구.",
            )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "reveals the answer")
    }

    @Test
    fun `rejects a manifest with a duplicate language`() {
        val assets = validAssets().toMutableMap()
        assets[MANIFEST_PATH] = assets.getValue(MANIFEST_PATH).replaceFirst("[\"en\", \"ja\"]", "[\"en\", \"ja\", \"en\"]")

        assertFailsValidation(assets, "duplicate language")
    }

    @Test
    fun `rejects a missing audio asset`() {
        val assets = validAssets().toMutableMap()
        assets.remove("audio/gemini/20-seoul-station.wav")

        assertFailsValidation(assets, "missing asset")
    }

    @Test
    fun `rejects a manifest checksum mismatch`() {
        val assets = validAssets().toMutableMap()
        assets[MANIFEST_PATH] =
            assets
                .getValue(
                    MANIFEST_PATH,
                ).replaceFirst(
                    "\"content/cards.base.json\": \"[0-9a-f]{64}\"".toRegex(),
                    "\"content/cards.base.json\": \"0000000000000000000000000000000000000000000000000000000000000000\"",
                )

        assertFailsValidation(assets, "checksum")
    }

    private fun assertFailsValidation(
        assets: Map<String, String>,
        expectedMessage: String,
    ) {
        val error = runCatching { BundleContentAdapter(FakeAssetSource(assets)).getCards() }.exceptionOrNull()

        assertTrue(error is BundleValidationException)
        assertTrue(error?.message.orEmpty().contains(expectedMessage, ignoreCase = true))
    }
}
