package com.signkorean.domain.model

/** A locally bundled profile image and the language it selects during onboarding. */
enum class ProfileAvatar(
    val id: String,
    val languageTag: String,
    val assetPath: String,
) {
    US_BLACK_MAN("us_black_man", "en", "profile-avatars/with-flags/01-us-black-man-flag.png"),
    US_BLACK_WOMAN("us_black_woman", "en", "profile-avatars/with-flags/02-us-black-woman-flag.png"),
    US_WHITE_MAN("us_white_man", "en", "profile-avatars/with-flags/03-us-white-man-flag.png"),
    US_WHITE_WOMAN("us_white_woman", "en", "profile-avatars/with-flags/04-us-white-woman-flag.png"),
    CHINESE_MAN("chinese_man", "zh", "profile-avatars/with-flags/05-chinese-man-flag.png"),
    CHINESE_WOMAN("chinese_woman", "zh", "profile-avatars/with-flags/06-chinese-woman-flag.png"),
    JAPANESE_MAN("japanese_man", "ja", "profile-avatars/with-flags/07-japanese-man-flag.png"),
    JAPANESE_WOMAN("japanese_woman", "ja", "profile-avatars/with-flags/08-japanese-woman-flag.png"),
    ;

    companion object {
        fun fromId(id: String): ProfileAvatar? = entries.firstOrNull { it.id == id }
    }
}
