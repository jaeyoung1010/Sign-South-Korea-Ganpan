package com.signkorean.data.settings

import androidx.datastore.core.DataStore
import com.signkorean.data.settings.proto.AudioVoiceProto
import com.signkorean.data.settings.proto.InterestProto
import com.signkorean.data.settings.proto.LevelProto
import com.signkorean.data.settings.proto.UserSettingsProto
import com.signkorean.domain.model.AudioVoice
import com.signkorean.domain.model.Interest
import com.signkorean.domain.model.LearningLevel
import com.signkorean.domain.model.UserSettings
import com.signkorean.domain.port.output.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DateTimeException
import java.time.LocalTime

class ProtoUserSettingsRepository(
    private val dataStore: DataStore<UserSettingsProto>,
    private val systemReduceMotionSource: SystemReduceMotionSource,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> =
        dataStore.data.map { settings -> settings.toDomain(systemReduceMotionSource.isReduceMotionEnabled()) }

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
        dataStore
            .updateData { persisted ->
                transform(persisted.toDomain(systemReduceMotionSource.isReduceMotionEnabled())).toProto()
            }.toDomain(systemReduceMotionSource.isReduceMotionEnabled())
}

private fun UserSettingsProto.toDomain(systemReduceMotion: Boolean): UserSettings =
    UserSettings(
        onboardingCompleted = onboardingCompleted,
        level = level.toDomain(),
        interests = interestsList.mapNotNull(InterestProto::toDomain).toSet(),
        sessionLength = sessionLength.takeIf(UserSettings.SESSION_LENGTH_OPTIONS::contains) ?: UserSettings.DEFAULT_SESSION_LENGTH,
        appLanguageTag = appLanguageTag.supportedLanguageTagOrDefault(),
        learningExplanationLanguageTag =
            learningExplanationLanguageTag.supportedLanguageTagOrDefault(),
        showRomanization = showRomanization,
        autoPlayAudio = autoPlayAudio,
        audioVoice = voice.toDomain(),
        reduceMotion = if (hasExplicitReduceMotion) reduceMotion else systemReduceMotion,
        followSystemReduceMotion = !hasExplicitReduceMotion,
        reminderEnabled = reminderEnabled,
        reminderLocalTime = if (hasExplicitReminderLocalTime) reminderLocalTimeOrDefault() else LocalTime.of(19, 0),
    )

private fun UserSettings.toProto(): UserSettingsProto =
    UserSettingsProto
        .newBuilder()
        .setSchemaVersion(1)
        .setOnboardingCompleted(onboardingCompleted)
        .setLevel(level.toProto())
        .addAllInterests(interests.map(Interest::toProto))
        .setSessionLength(sessionLength)
        .setAppLanguageTag(appLanguageTag.supportedLanguageTagOrDefault())
        .setLearningExplanationLanguageTag(learningExplanationLanguageTag.supportedLanguageTagOrDefault())
        .setShowRomanization(showRomanization)
        .setAutoPlayAudio(autoPlayAudio)
        .setVoice(audioVoice.toProto())
        .setReduceMotion(reduceMotion)
        .setHasExplicitReduceMotion(!followSystemReduceMotion)
        .setReminderEnabled(reminderEnabled)
        .setReminderLocalHour(reminderLocalTime.hour)
        .setReminderLocalMinute(reminderLocalTime.minute)
        .setHasExplicitReminderLocalTime(true)
        .build()

private fun LevelProto.toDomain(): LearningLevel =
    when (this) {
        LevelProto.LEVEL_PROTO_ELEMENTARY -> LearningLevel.ELEMENTARY
        LevelProto.LEVEL_PROTO_NOT_SURE -> LearningLevel.NOT_SURE
        else -> LearningLevel.BEGINNER
    }

private fun LearningLevel.toProto(): LevelProto =
    when (this) {
        LearningLevel.BEGINNER -> LevelProto.LEVEL_PROTO_BEGINNER
        LearningLevel.ELEMENTARY -> LevelProto.LEVEL_PROTO_ELEMENTARY
        LearningLevel.NOT_SURE -> LevelProto.LEVEL_PROTO_NOT_SURE
    }

private fun InterestProto.toDomain(): Interest? =
    when (this) {
        InterestProto.INTEREST_PROTO_LANDMARK -> Interest.LANDMARK
        InterestProto.INTEREST_PROTO_NEIGHBORHOOD -> Interest.NEIGHBORHOOD
        InterestProto.INTEREST_PROTO_MARKET -> Interest.MARKET
        InterestProto.INTEREST_PROTO_TRANSPORT -> Interest.TRANSPORT
        InterestProto.INTEREST_PROTO_FOOD -> Interest.FOOD
        InterestProto.INTEREST_PROTO_SHOPPING -> Interest.SHOPPING
        else -> null
    }

private fun Interest.toProto(): InterestProto =
    when (this) {
        Interest.LANDMARK -> InterestProto.INTEREST_PROTO_LANDMARK
        Interest.NEIGHBORHOOD -> InterestProto.INTEREST_PROTO_NEIGHBORHOOD
        Interest.MARKET -> InterestProto.INTEREST_PROTO_MARKET
        Interest.TRANSPORT -> InterestProto.INTEREST_PROTO_TRANSPORT
        Interest.FOOD -> InterestProto.INTEREST_PROTO_FOOD
        Interest.SHOPPING -> InterestProto.INTEREST_PROTO_SHOPPING
    }

private fun AudioVoiceProto.toDomain(): AudioVoice =
    when (this) {
        AudioVoiceProto.AUDIO_VOICE_PROTO_GEMINI_KORE -> AudioVoice.GEMINI_KORE
        else -> AudioVoice.TYPECAST_EUNKYUNG
    }

private fun AudioVoice.toProto(): AudioVoiceProto =
    when (this) {
        AudioVoice.TYPECAST_EUNKYUNG -> AudioVoiceProto.AUDIO_VOICE_PROTO_TYPECAST_EUNKYUNG
        AudioVoice.GEMINI_KORE -> AudioVoiceProto.AUDIO_VOICE_PROTO_GEMINI_KORE
    }

private fun UserSettingsProto.reminderLocalTimeOrDefault(): LocalTime =
    try {
        LocalTime.of(reminderLocalHour, reminderLocalMinute)
    } catch (_: DateTimeException) {
        LocalTime.of(19, 0)
    }

private fun String.supportedLanguageTagOrDefault(): String =
    takeIf(UserSettings.SUPPORTED_LANGUAGE_TAGS::contains) ?: UserSettings.DEFAULT_LANGUAGE_TAG
