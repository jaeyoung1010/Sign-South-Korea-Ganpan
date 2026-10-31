package com.signkorean.domain.model

/** A locally bundled profile image and the language it selects during onboarding. */
enum class ProfileAvatar(
    val id: String,
    val languageTag: String,
) {
    US_BLACK_MAN("us_black_man", "en"),
    US_BLACK_WOMAN("us_black_woman", "en"),
    US_WHITE_MAN("us_white_man", "en"),
    US_WHITE_WOMAN("us_white_woman", "en"),
    CHINESE_MAN("chinese_man", "zh"),
    CHINESE_WOMAN("chinese_woman", "zh"),
    JAPANESE_MAN("japanese_man", "ja"),
    JAPANESE_WOMAN("japanese_woman", "ja"),
    ;

    companion object {
        fun fromId(id: String): ProfileAvatar? = entries.firstOrNull { it.id == id }
    }
}
