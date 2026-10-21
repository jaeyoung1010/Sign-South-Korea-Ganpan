package com.signkorean.app.legal

import android.content.Context
import com.signkorean.app.R
import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.model.PolicyDocumentId
import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.PolicyDocumentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject

private const val JAPANESE_LANGUAGE_TAG = "ja"

class BundledPolicyDocumentRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val ioDispatcher: CoroutineDispatcher,
        private val appLocalePort: AppLocalePort,
    ) : PolicyDocumentRepository {
        override suspend fun load(id: PolicyDocumentId): PolicyDocument =
            withContext(ioDispatcher) {
                val metadata = metadataFor(id)
                val bytes = context.assets.open(metadata.assetPath).use { it.readBytes() }
                val checksum = sha256(bytes)
                check(checksum.equals(metadata.expectedChecksum, ignoreCase = true)) {
                    "Bundled ${id.name.lowercase()} policy checksum does not match its public resource"
                }
                PolicyDocument(
                    id = id,
                    title = metadata.title,
                    version = context.getString(metadata.versionRes),
                    effectiveDate = context.getString(R.string.policy_effective_date),
                    body = String(bytes, StandardCharsets.UTF_8),
                    onlineUrl = context.getString(metadata.urlRes),
                    bundledChecksum = checksum,
                )
            }

        private fun metadataFor(id: PolicyDocumentId): PolicyMetadata =
            if (appLocalePort.getCurrent().equals(JAPANESE_LANGUAGE_TAG, ignoreCase = true)) {
                japaneseMetadataFor(id)
            } else {
                englishMetadataFor(id)
            }

        private fun englishMetadataFor(id: PolicyDocumentId): PolicyMetadata =
            when (id) {
                PolicyDocumentId.PRIVACY ->
                    PolicyMetadata(
                        assetPath = "legal/privacy-policy.md",
                        title = context.getString(R.string.legal_privacy_policy),
                        versionRes = R.string.policy_privacy_version,
                        urlRes = R.string.policy_privacy_url,
                        expectedChecksum = context.getString(R.string.policy_privacy_checksum),
                    )

                PolicyDocumentId.TERMS ->
                    PolicyMetadata(
                        assetPath = "legal/terms-of-use.md",
                        title = context.getString(R.string.legal_terms_of_use),
                        versionRes = R.string.policy_terms_version,
                        urlRes = R.string.policy_terms_url,
                        expectedChecksum = context.getString(R.string.policy_terms_checksum),
                    )
            }

        private fun japaneseMetadataFor(id: PolicyDocumentId): PolicyMetadata =
            when (id) {
                PolicyDocumentId.PRIVACY ->
                    PolicyMetadata(
                        assetPath = "legal/ja/privacy-policy.md",
                        title = context.getString(R.string.legal_privacy_policy),
                        versionRes = R.string.policy_privacy_version,
                        urlRes = R.string.policy_privacy_url,
                        expectedChecksum = context.getString(R.string.policy_privacy_checksum_ja),
                    )

                PolicyDocumentId.TERMS ->
                    PolicyMetadata(
                        assetPath = "legal/ja/terms-of-use.md",
                        title = context.getString(R.string.legal_terms_of_use),
                        versionRes = R.string.policy_terms_version,
                        urlRes = R.string.policy_terms_url,
                        expectedChecksum = context.getString(R.string.policy_terms_checksum_ja),
                    )
            }

        private data class PolicyMetadata(
            val assetPath: String,
            val title: String,
            val versionRes: Int,
            val urlRes: Int,
            val expectedChecksum: String,
        )

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    }
