package com.signkorean.data.asset

import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.FocusRect
import com.signkorean.domain.model.LanguageTag
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
    private val allowMissingAssets: Boolean = false,
) : CardCatalog {
    private val validatedBundle: ValidatedBundle by lazy { validateAndLoad() }

    override fun getCards(): List<SignCard> = validatedBundle.cards

    override fun getCard(cardId: String): SignCard? = validatedBundle.cards.firstOrNull { it.id == cardId }

    override fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy> =
        validatedBundle.localizedCards[languageTag]
            ?: throw BundleValidationException("Unsupported bundled language: $languageTag")

    override fun validateBundle(): BundleValidationReport =
        BundleValidationReport(
            cardCount = validatedBundle.cards.size,
            imageCount =
                validatedBundle.cards
                    .map(SignCard::imageAssetUri)
                    .distinct()
                    .size,
            typecastAudioCount =
                validatedBundle.cards
                    .mapNotNull { it.audioAssetUris[VoiceId.TYPECAST_EUNKYUNG] }
                    .distinct()
                    .size,
            languages = validatedBundle.localizedCards.keys,
        )

    private fun validateAndLoad(): ValidatedBundle {
        val manifestText = assetSource.readText(MANIFEST_PATH)
        val manifest = decode<ContentManifest>(manifestText, MANIFEST_PATH)
        requireValidation(manifest.contentVersion > 0) { "contentVersion must be positive" }
        requireValidation(manifest.cardCount == REQUIRED_CARD_COUNT) { "expected $REQUIRED_CARD_COUNT cards" }
        requireValidation(manifest.languages.containsAll(REQUIRED_LANGUAGE_TAGS)) { "supported languages must include en and ja" }
        requireValidation(manifest.languages.distinct().size == manifest.languages.size) { "duplicate language in manifest" }
        requireValidation(manifest.languages.all { LanguageTag.parseOrNull(it) != null }) { "manifest contains an invalid BCP-47 language tag" }
        requireValidation(manifest.reviewStatus.keys == manifest.languages.toSet()) { "manifest must record review status for every language" }
        requireValidation(manifest.reviewStatus.getValue(ENGLISH_LANGUAGE_TAG) == NATIVE_ENGLISH_REVIEW_STATUS) {
            "English content must record native review status"
        }
        requireValidation(manifest.reviewStatus.getValue(JAPANESE_LANGUAGE_TAG) == NATIVE_JAPANESE_REVIEW_STATUS) {
            "Japanese review status must record native approval"
        }
        requireValidation(manifest.factReviewStatus.keys == manifest.languages.toSet()) {
            "manifest must record Korean fact review status for every language"
        }
        requireValidation(manifest.factReviewStatus.values.all { it == KOREAN_FACT_REVIEW_STATUS }) {
            "Korean fact review status must be approved for every language"
        }
        requireValidation(manifest.localizedContent.keys.all { it in manifest.languages }) {
            "localized content must only reference manifest languages"
        }
        val localizedPaths =
            manifest.languages.associateWith { languageTag ->
                manifest.localizedContent[languageTag] ?: localizedPath(languageTag)
            }
        requireValidation(localizedPaths.values.all { it.startsWith("content/") && it.endsWith(".json") }) {
            "manifest contains an invalid localized content path"
        }
        requireValidation(localizedPaths.values.distinct().size == localizedPaths.size) {
            "manifest must use a unique content path for every language"
        }
        requireValidation(manifest.checksums.keys == (setOf(BASE_CARDS_PATH) + localizedPaths.values)) {
            "manifest must checksum every content file"
        }

        val contentTexts = manifest.checksums.keys.associateWith(assetSource::readText)
        manifest.checksums.forEach { (path, checksum) ->
            requireValidation(sha256(contentTexts.getValue(path)) == checksum) { "checksum mismatch for $path" }
        }

        val baseCards = decode<List<BaseCardJson>>(contentTexts.getValue(BASE_CARDS_PATH), BASE_CARDS_PATH)
        validateBaseCards(baseCards, manifest.contentVersion)
        val cardsById = baseCards.associateBy(BaseCardJson::id)
        val localizedJson =
            manifest.languages.associateWith { languageTag ->
                val path = localizedPaths.getValue(languageTag)
                decode<List<LocalizedCardCopyJson>>(contentTexts.getValue(path), path)
            }
        val englishCopies = localizedJson.getValue(ENGLISH_LANGUAGE_TAG).associateBy(LocalizedCardCopyJson::cardId)
        val localizedCards =
            localizedJson.mapValues { (languageTag, copies) ->
                validateLocalizedCards(
                    copies = copies,
                    languageTag = languageTag,
                    cardsById = cardsById,
                    englishCopies = englishCopies,
                    contentVersion = manifest.contentVersion,
                )
                copies.map(LocalizedCardCopyJson::toDomain)
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
            card.typecastAudioAsset?.let { validateAsset(it, card.id, ".mp3") }
        }
        requireValidation(cards.map(BaseCardJson::imageAsset).distinct().size == cards.size) { "duplicate image asset" }
        requireValidation(cards.mapNotNull(BaseCardJson::typecastAudioAsset).distinct().size == cards.count { it.typecastAudioAsset != null }) {
            "duplicate Typecast asset"
        }
    }

    private fun validateLocalizedCards(
        copies: List<LocalizedCardCopyJson>,
        languageTag: String,
        cardsById: Map<String, BaseCardJson>,
        englishCopies: Map<String, LocalizedCardCopyJson>,
        contentVersion: Int,
    ) {
        requireValidation(copies.size == REQUIRED_CARD_COUNT) { "expected $REQUIRED_CARD_COUNT $languageTag localized cards" }
        requireValidation(copies.map(LocalizedCardCopyJson::cardId).toSet() == cardsById.keys) {
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
            requireValidation(!copy.imageAltTextQuestion.contains(cardsById.getValue(copy.cardId).korean)) {
                "question alt text reveals the answer for ${copy.cardId}"
            }
            requireValidation(copy.reviewedAt?.matches(REVIEW_DATE_PATTERN) == true) {
                "$languageTag review date missing for ${copy.cardId}"
            }
            if (languageTag == ENGLISH_LANGUAGE_TAG) {
                requireValidation(copy.reviewer == NATIVE_ENGLISH_REVIEWER) { "English reviewer missing for ${copy.cardId}" }
            } else if (languageTag == JAPANESE_LANGUAGE_TAG) {
                requireValidation(copy.reviewer == NATIVE_JAPANESE_REVIEWER) { "Japanese reviewer missing for ${copy.cardId}" }
                requireValidation(copy.pronunciationGuide.matches(JAPANESE_PRONUNCIATION_PATTERN)) {
                    "Japanese pronunciation guide must use Katakana for ${copy.cardId}"
                }
                requireValidation(copy.streetNote != englishCopies.getValue(copy.cardId).streetNote) {
                    "independent Japanese copy is required for ${copy.cardId}"
                }
            } else {
                requireValidation(!copy.reviewer.isNullOrBlank()) { "$languageTag reviewer missing for ${copy.cardId}" }
            }
            requireValidation(copy.factReviewer == KOREAN_FACT_REVIEWER) {
                "${if (languageTag == JAPANESE_LANGUAGE_TAG) "Japanese" else "English"} fact reviewer missing for ${copy.cardId}"
            }
            requireValidation(copy.factReviewedAt?.matches(REVIEW_DATE_PATTERN) == true) {
                "$languageTag fact review date missing for ${copy.cardId}"
            }
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
        if (!allowMissingAssets) {
            requireValidation(assetSource.exists(androidPath)) { "missing asset: $androidPath" }
        }
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
        const val REQUIRED_CARD_COUNT = 30
        const val MANIFEST_PATH = "content/manifest.json"
        const val BASE_CARDS_PATH = "content/cards.base.json"
        const val ENGLISH_LANGUAGE_TAG = "en"
        const val JAPANESE_LANGUAGE_TAG = "ja"
        const val NATIVE_ENGLISH_REVIEW_STATUS = "native-english-reviewed"
        const val NATIVE_JAPANESE_REVIEW_STATUS = "native-japanese-reviewed"
        const val KOREAN_FACT_REVIEW_STATUS = "korean-fact-reviewed"
        const val NATIVE_ENGLISH_REVIEWER = "native-english-review"
        const val NATIVE_JAPANESE_REVIEWER = "native-japanese-review"
        const val KOREAN_FACT_REVIEWER = "korean-fact-review"
        val REQUIRED_LANGUAGE_TAGS = setOf(ENGLISH_LANGUAGE_TAG, JAPANESE_LANGUAGE_TAG)
        val PERCENT_RANGE = 0f..100f
        val IMAGE_REVIEW_STATUSES = setOf("approved", "pending")
        val REVIEW_DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
        val JAPANESE_PRONUNCIATION_PATTERN = Regex("^[\\u30A0-\\u30FFー・\\s]+$")
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
    val localizedContent: Map<String, String> = emptyMap(),
    val reviewStatus: Map<String, String>,
    val factReviewStatus: Map<String, String> = emptyMap(),
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
    val typecastAudioAsset: String? = null,
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
                buildMap { typecastAudioAsset?.let { put(VoiceId.TYPECAST_EUNKYUNG, androidAssetUri(it)) } },
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
    val factReviewer: String? = null,
    val factReviewedAt: String? = null,
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
            reviewer = reviewer,
            reviewedAt = reviewedAt,
            factReviewer = factReviewer,
            factReviewedAt = factReviewedAt,
        )
}

@Serializable
private data class LocalizedWordPartJson(
    val korean: String,
    val meaning: String,
)
