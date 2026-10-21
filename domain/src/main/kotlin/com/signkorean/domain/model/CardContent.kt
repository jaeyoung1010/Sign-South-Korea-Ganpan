package com.signkorean.domain.model

data class FocusRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

enum class VoiceId {
    TYPECAST_EUNKYUNG,
    GEMINI_KORE,
}

data class SignCard(
    val id: String,
    val order: Int,
    val korean: String,
    val category: String,
    val imageAssetUri: String,
    val focus: FocusRect,
    val audioAssetUris: Map<VoiceId, String>,
    val contentVersion: Int,
)

data class LocalizedWordPart(
    val korean: String,
    val meaning: String,
)

data class LocalizedCardCopy(
    val cardId: String,
    val languageTag: String,
    val displayMeaning: String,
    val pronunciationGuide: String,
    val wordParts: List<LocalizedWordPart>,
    val streetNote: String,
    val imageAltTextQuestion: String,
    val imageAltTextAnswer: String,
    val searchTerms: List<String>,
    val contentVersion: Int,
    val reviewer: String? = null,
    val reviewedAt: String? = null,
    val factReviewer: String? = null,
    val factReviewedAt: String? = null,
)

data class BundleValidationReport(
    val cardCount: Int,
    val imageCount: Int,
    val typecastAudioCount: Int,
    val geminiAudioCount: Int,
    val languages: Set<String>,
)
