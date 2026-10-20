package com.signkorean.domain.port.output

import com.signkorean.domain.model.HomeActivitySnapshot
import java.time.Instant

fun interface HomeActivityRepository {
    suspend fun loadHomeActivity(now: Instant): HomeActivitySnapshot
}
