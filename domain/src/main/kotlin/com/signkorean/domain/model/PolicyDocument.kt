package com.signkorean.domain.model

enum class PolicyDocumentId {
    PRIVACY,
    TERMS,
}

data class PolicyDocument(
    val id: PolicyDocumentId,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val body: String,
    val onlineUrl: String,
    val bundledChecksum: String,
) {
    init {
        require(title.isNotBlank()) { "Policy title must not be blank" }
        require(version.isNotBlank()) { "Policy version must not be blank" }
        require(effectiveDate.isNotBlank()) { "Policy effective date must not be blank" }
        require(body.isNotBlank()) { "Policy body must not be blank" }
        require(onlineUrl.startsWith("https://")) { "Policy URL must use HTTPS" }
        require(bundledChecksum.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Policy checksum must be a SHA-256 hex digest"
        }
    }
}

data class GuestAccount(
    val storageScope: StorageScope = StorageScope.THIS_DEVICE,
    val email: String? = null,
    val cloudSyncAvailable: Boolean = false,
    val dataLostWhenAppDeleted: Boolean = true,
) {
    enum class StorageScope {
        THIS_DEVICE,
    }

    init {
        require(email == null) { "Guest accounts must not retain an email address" }
        require(!cloudSyncAvailable) { "Guest cloud sync must remain unavailable" }
        require(dataLostWhenAppDeleted) { "Guest data is scoped to the app installation" }
    }

    companion object {
        fun local(): GuestAccount = GuestAccount()
    }
}
