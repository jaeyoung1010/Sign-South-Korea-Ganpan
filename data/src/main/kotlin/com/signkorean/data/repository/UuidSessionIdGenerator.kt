package com.signkorean.data.repository

import com.signkorean.domain.port.output.SessionIdGenerator
import java.util.UUID

class UuidSessionIdGenerator : SessionIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}
