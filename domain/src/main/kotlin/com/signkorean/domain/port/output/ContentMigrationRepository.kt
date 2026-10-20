package com.signkorean.domain.port.output

import com.signkorean.domain.model.AppContent
import com.signkorean.domain.model.ContentMigrationResult

fun interface ContentMigrationRepository {
    suspend fun migrate(content: AppContent): ContentMigrationResult
}
