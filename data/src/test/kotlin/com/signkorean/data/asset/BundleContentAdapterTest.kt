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
        assertTrue(adapter.getLocalizedCopy("ja").all { it.pronunciationGuide.matches(JAPANESE_PRONUNCIATION_PATTERN) })
        assertTrue(adapter.getLocalizedCopy("ja").all { it.imageAltTextQuestion.isNotBlank() && it.imageAltTextAnswer.isNotBlank() })
        assertTrue(
            adapter.getLocalizedCopy("ja").all {
                it.reviewer == "native-japanese-review" &&
                    it.reviewedAt == "2026-10-21" &&
                    it.factReviewer == "korean-fact-review" &&
                    it.factReviewedAt == "2026-10-21"
            },
        )
        assertTrue(adapter.getLocalizedCopy("ja").all { it.contentVersion == 2 })
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
    fun `rejects a Japanese card without native review metadata`() {
        val assets = validAssets().toMutableMap()
        assets[JA_CARDS_PATH] = assets.getValue(JA_CARDS_PATH).replaceFirst(
            "\"reviewer\":\"native-japanese-review\"",
            "\"reviewer\":\"editor-draft\"",
        )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "Japanese reviewer")
    }

    @Test
    fun `rejects a Japanese card without Korean fact review metadata`() {
        val assets = validAssets().toMutableMap()
        assets[JA_CARDS_PATH] = assets.getValue(JA_CARDS_PATH).replaceFirst(
            "\"factReviewer\":\"korean-fact-review\"",
            "\"factReviewer\":null",
        )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "Japanese fact reviewer")
    }

    @Test
    fun `rejects Japanese copy that duplicates the English sentence structure`() {
        val assets = validAssets().toMutableMap()
        val englishStreetNote = "The gateway to Hongdae, a popular area for live music, cafés, shopping, and nightlife."
        assets[JA_CARDS_PATH] = assets.getValue(JA_CARDS_PATH).replaceFirst(
            "ライブハウスやカフェ、買い物でにぎわう弘大エリアの玄関口です。",
            englishStreetNote,
        )

        refreshManifestChecksums(assets)
        assertFailsValidation(assets, "independent Japanese")
    }

    @Test
    fun `rejects a manifest without Japanese and Korean approval statuses`() {
        val assets = validAssets().toMutableMap()
        assets[MANIFEST_PATH] = assets.getValue(MANIFEST_PATH)
            .replace("\"ja\": \"native-japanese-reviewed\"", "\"ja\": \"editor-reviewed\"")
            .replace("\"ja\": \"korean-fact-reviewed\"", "\"ja\": \"pending\"")

        assertFailsValidation(assets, "Japanese review status")
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
    fun `loads a future BCP 47 locale without changing domain use cases`() {
        val assets = validAssets().toMutableMap()
        assets["content/fr/cards.json"] = assets.getValue(JA_CARDS_PATH).replace("\"languageTag\":\"ja\"", "\"languageTag\":\"fr\"")
        assets[MANIFEST_PATH] =
            assets
                .getValue(MANIFEST_PATH)
                .replace("[\"en\", \"ja\"]", "[\"en\", \"ja\", \"fr\"]")
                .replace(
                    "\"ja\": \"native-japanese-reviewed\"\n  },",
                    "\"ja\": \"native-japanese-reviewed\",\n    \"fr\": \"future-native-reviewed\"\n  },",
                )
                .replace(
                    "\"ja\": \"korean-fact-reviewed\"\n  },",
                    "\"ja\": \"korean-fact-reviewed\",\n    \"fr\": \"korean-fact-reviewed\"\n  },",
                )
                .replace(
                    "\"languages\": [\"en\", \"ja\"],",
                    "\"languages\": [\"en\", \"ja\", \"fr\"],\n  \"localizedContent\": {\"en\": \"content/en/cards.json\", \"ja\": \"content/ja/cards.json\", \"fr\": \"content/fr/cards.json\"},",
                )
                .replace(
                    "\"content/ja/cards.json\": \"7b6ded7fddbbefbdeb2dc1da6721a4a3a9cf945436c872fe6cf52e30c36a195b\"",
                    "\"content/ja/cards.json\": \"7b6ded7fddbbefbdeb2dc1da6721a4a3a9cf945436c872fe6cf52e30c36a195b\",\n    \"content/fr/cards.json\": \"0000000000000000000000000000000000000000000000000000000000000000\"",
                )

        refreshManifestChecksums(assets)
        val adapter = BundleContentAdapter(FakeAssetSource(assets))

        assertEquals(20, adapter.getLocalizedCopy("fr").size)
        assertEquals(setOf("en", "ja", "fr"), adapter.validateBundle().languages)
    }

    @Test
    fun `rejects a missing audio asset`() {
        val assets = validAssets().toMutableMap()
        assets.remove("audio/gemini/20-seoul-station.wav")

        assertFailsValidation(assets, "missing asset")
    }

    @Test
    fun `offline mode keeps cards loadable when one photo and one audio asset are missing`() {
        val assets = validAssets().toMutableMap()
        assets.remove("signs/generated/20-seoul-station.jpg")
        assets.remove("audio/gemini/20-seoul-station.wav")

        val adapter = BundleContentAdapter(FakeAssetSource(assets), allowMissingAssets = true)

        assertEquals(20, adapter.getCards().size)
        val card = adapter.getCard("20-seoul-station")
        assertTrue(card?.imageAssetUri?.contains("20-seoul-station.jpg") == true)
        assertTrue(card?.audioAssetUris?.get(VoiceId.GEMINI_KORE)?.contains("20-seoul-station.wav") == true)
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

    private companion object {
        val JAPANESE_PRONUNCIATION_PATTERN = Regex("^[\\u30A0-\\u30FFー・\\s]+$")
    }
}
