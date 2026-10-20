package com.signkorean.domain.model

data class AppContent(
    val version: Int,
    val cardIds: Set<String>,
) {
    init {
        require(version > 0) { "Content version must be positive" }
        require(cardIds.isNotEmpty()) { "Content must contain cards" }
        require(cardIds.none(String::isBlank)) { "Card IDs must not be blank" }
    }
}

data class ContentMigrationResult(
    val migrated: Boolean,
    val activeSessionInvalidated: Boolean,
)

data class AppStartupEvent(
    val onboardingCompleted: Boolean,
    val hasResumableSession: Boolean,
    val usedDefaultSettings: Boolean,
    val contentMigrated: Boolean,
)

sealed interface AppInitializationResult {
    data class Ready(
        val destination: LaunchDestination,
        val hasResumableSession: Boolean,
        val usedDefaultSettings: Boolean,
    ) : AppInitializationResult

    sealed interface RecoverableFailure : AppInitializationResult {
        data object Database : RecoverableFailure

        data object Content : RecoverableFailure
    }
}
