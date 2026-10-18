package com.signkorean.data.asset

import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.LocalizedWordPart
import com.signkorean.domain.model.SignCard
import com.signkorean.domain.model.VoiceId
import com.signkorean.domain.port.output.CardCatalog
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class BundleContentAdapter(
    private val assetSource: AssetSource,
    private val json: Json = Json { ignoreUnknownKeys = false },
) : CardCatalog {
    private val validatedBundle: ValidatedBundle by lazy { validateAndLoad() }

    override fun getCards(): List<SignCard> = validatedBundle.cards

    override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> =
        validatedBundle.localizedCards[languageTag]
            ?: throw BundleValidationException("Unsupported bundled language: $languageTag")

    private fun validateAndLoad(): ValidatedBundle {
        val manifestText = assetSource.readText(MANIFEST_PATH)
        val manifest = decode<ContentManifest>(manifestText, MANIFEST_PATH)
        requireValidation(manifest.contentVersion > 0) { "contentVersion must be positive" }
        requireValidation(manifest.cardCount == REQUIRED_CARD_COUNT) { "expected $REQUIRED_CARD_COUNT cards" }
        requireValidation(manifest.languages.toSet() == REQUIRED_LANGUAGE_TAGS) { "supported languages must be en and ja" }
        requireValidation(manifest.checksums.keys == REQUIRED_CONTENT_PATHS) { "manifest must checksum every content file" }

        val contentTexts = REQUIRED_CONTENT_PATHS.associateWith(assetSource::readText)
        manifest.checksums.forEach { (path, checksum) ->
            requireValidation(sha256(contentTexts.getValue(path)) == checksum) { "checksum mismatch for $path" }
        }

        val baseCards = decode<List<BaseCardJson>>(contentTexts.getValue(BASE_CARDS_PATH), BASE_CARDS_PATH)
        validateBaseCards(baseCards, manifest.contentVersion)
        val localizedCards =
            manifest.languages.associateWith { languageTag ->
                val path = localizedPath(languageTag)
                decode<List<LocalizedCardCopyJson>>(contentTexts.getValue(path), path)
                    .also {
                        validateLocalizedCards(it, languageTag, baseCards.map(BaseCardJson::id).toSet(), manifest.contentVersion)
                    }.map(LocalizedCardCopyJson::toDomain)
            }
        return ValidatedBundle(baseCards.map(BaseCardJson::toDomain), localizedCards)
    }

    private fun validateBaseCards(
        cards: List<BaseCardJson>,
        contentVersion: Int,
    ) {
        requireValidation(cards.size == REQUIRED_CARD_COUNT) { "expected $REQUIRED_CARD_COUNT base cards" }
        requireValidation(cards.map(BaseCardJson::id).all(String::isNotBlank)) { "card id must not be blank" }
        requireValidation(cards.map(BaseCardJson::id).distinct().size == cards.size) { "duplicate card id" }
        requireValidation(cards.map(BaseCardJson::order) == (1..REQUIRED_CARD_COUNT).toList()) {
            "card order must be 1 through $REQUIRED_CARD_COUNT"
        }
        cards.forEach { card ->
            requireValidation(card.contentVersion == contentVersion) { "content version mismatch for ${card.id}" }
            requireValidation(card.korean.isNotBlank() && card.category.isNotBlank()) { "missing card text for ${card.id}" }
            validateFocus(card.id, card.focus)
            requireValidation(card.image.reviewStatus in IMAGE_REVIEW_STATUSES) { "invalid image review status for ${card.id}" }
            validateAsset(card.imageAsset, card.id, ".jpg")
            validateAsset(card.typecastAudioAsset, card.id, ".mp3")
            validateAsset(card.geminiAudioAsset, card.id, ".wav")
        }
        requireValidation(cards.map(BaseCardJson::imageAsset).distinct().size == cards.size) { "duplicate image asset" }
        requireValidation(cards.map(BaseCardJson::typecastAudioAsset).distinct().size == cards.size) { "duplicate Typecast asset" }
        requireValidation(cards.map(BaseCardJson::geminiAudioAsset).distinct().size == cards.size) { "duplicate Gemini asset" }
    }

    private fun validateLocalizedCards(
        copies: List<LocalizedCardCopyJson>,
        languageTag: String,
        cardIds: Set<String>,
        contentVersion: Int,
    ) {
        requireValidation(copies.size == REQUIRED_CARD_COUNT) { "expected $REQUIRED_CARD_COUNT $languageTag localized cards" }
        requireValidation(copies.map(LocalizedCardCopyJson::cardId).toSet() == cardIds) {
            "$languageTag localized card IDs do not match base cards"
        }
        requireValidation(copies.map(LocalizedCardCopyJson::cardId).distinct().size == copies.size) { "duplicate localized card id" }
        copies.forEach { copy ->
            requireValidation(copy.languageTag == languageTag) { "wrong languageTag for ${copy.cardId}" }
            requireValidation(copy.contentVersion == contentVersion) { "content version mismatch for ${copy.cardId}" }
            requireValidation(
                copy.displayMeaning.isNotBlank() && copy.streetNote.isNotBlank(),
            ) { "missing localized text for ${copy.cardId}" }
            requireValidation(copy.wordParts.isNotEmpty()) { "wordParts missing for ${copy.cardId}" }
            requireValidation(
                copy.wordParts.all { it.korean.isNotBlank() && it.meaning.isNotBlank() },
            ) { "wordParts incomplete for ${copy.cardId}" }
            requireValidation(
                copy.imageAltTextQuestion.isNotBlank() && copy.imageAltTextAnswer.isNotBlank(),
            ) { "alt text missing for ${copy.cardId}" }
        }
    }

    private fun validateFocus(
        cardId: String,
        focus: FocusRectJson,
    ) {
        requireValidation(
            focus.x in PERCENT_RANGE && focus.y in PERCENT_RANGE && focus.width in PERCENT_RANGE && focus.height in PERCENT_RANGE,
        ) {
            "focus must be within 0..100 for $cardId"
        }
        requireValidation(focus.x + focus.width <= 100 && focus.y + focus.height <= 100) { "focus extends beyond image for $cardId" }
    }

    private fun validateAsset(
        path: String,
        cardId: String,
        extension: String,
    ) {
        requireValidation(path.startsWith("assets/")) { "invalid asset URI for $cardId" }
        requireValidation(path.substringAfterLast('/').startsWith(cardId)) { "asset prefix must match card id for $cardId" }
        requireValidation(path.endsWith(extension)) { "invalid asset extension for $cardId" }
        val androidPath = path.removePrefix("assets/")
        requireValidation(assetSource.exists(androidPath)) { "missing asset: $androidPath" }
    }

    private inline fun <reified T> decode(
        text: String,
        path: String,
    ): T =
        try {
            json.decodeFromString(text)
        } catch (exception: Exception) {
            throw BundleValidationException("Invalid JSON in $path", exception)
        }

    private data class ValidatedBundle(
        val cards: List<SignCard>,
        val localizedCards: Map<String, List<LocalizedCardCopy>>,
    )

    private companion object {
        const val REQUIRED_CARD_COUNT = 20
        const val MANIFEST_PATH = "content/manifest.json"
        const val BASE_CARDS_PATH = "content/cards.base.json"
        const val EN_CARDS_PATH = "content/en/cards.json"
        const val JA_CARDS_PATH = "content/ja/cards.json"
        val REQUIRED_CONTENT_PATHS = setOf(BASE_CARDS_PATH, EN_CARDS_PATH, JA_CARDS_PATH)
        val REQUIRED_LANGUAGE_TAGS = setOf("en", "ja")
        val PERCENT_RANGE = 0f..100f
        val IMAGE_REVIEW_STATUSES = setOf("approved", "pending")
    }
}

class BundleValidationException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

private fun requireValidation(
    condition: Boolean,
    lazyMessage: () -> String,
) {
    if (!condition) throw BundleValidationException(lazyMessage())
}

private fun localizedPath(languageTag: String): String = "content/$languageTag/cards.json"

private fun sha256(text: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { "%02x".format(it) }

private fun androidAssetUri(webAssetPath: String): String = "file:///android_asset/${webAssetPath.removePrefix("assets/")}"

@Serializable
private data class ContentManifest(
    val contentVersion: Int,
    val cardCount: Int,
    val languages: List<String>,
    val checksums: Map<String, String>,
)

@Serializable
private data class BaseCardJson(
    val id: String,
    val order: Int,
    val korean: String,
    val category: String,
    val imageAsset: String,
    val image: ImageMetadataJson,
    val focus: FocusRectJson,
    val typecastAudioAsset: String,
    val geminiAudioAsset: String,
    val contentVersion: Int,
) {
    fun toDomain() =
        SignCard(
            id = id,
            order = order,
            korean = korean,
            category = category,
            imageAssetUri = androidAssetUri(imageAsset),
            focus = FocusRect(focus.x, focus.y, focus.width, focus.height),
            audioAssetUris =
                mapOf(
                    VoiceId.TYPECAST_EUNKYUNG to androidAssetUri(typecastAudioAsset),
                    VoiceId.GEMINI_KORE to androidAssetUri(geminiAudioAsset),
                ),
            contentVersion = contentVersion,
        )
}

@Serializable
private data class ImageMetadataJson(
    val source: String,
    val rights: String,
    val reviewStatus: String,
)

@Serializable
private data class FocusRectJson(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

@Serializable
private data class LocalizedCardCopyJson(
    val cardId: String,
    val languageTag: String,
    val displayMeaning: String,
    val pronunciationGuide: String,
    val wordParts: List<LocalizedWordPartJson>,
    val streetNote: String,
    val imageAltTextQuestion: String,
    val imageAltTextAnswer: String,
    val searchTerms: List<String>,
    val contentVersion: Int,
    val reviewer: String?,
    val reviewedAt: String?,
) {
    fun toDomain() =
        LocalizedCardCopy(
            cardId = cardId,
            languageTag = languageTag,
            displayMeaning = displayMeaning,
            pronunciationGuide = pronunciationGuide,
            wordParts = wordParts.map { LocalizedWordPart(it.korean, it.meaning) },
            streetNote = streetNote,
            imageAltTextQuestion = imageAltTextQuestion,
            imageAltTextAnswer = imageAltTextAnswer,
            searchTerms = searchTerms,
            contentVersion = contentVersion,
        )
}

@Serializable
private data class LocalizedWordPartJson(
    val korean: String,
    val meaning: String,
)
