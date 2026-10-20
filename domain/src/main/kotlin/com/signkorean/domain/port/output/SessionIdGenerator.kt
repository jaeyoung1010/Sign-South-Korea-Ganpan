package com.signkorean.domain.port.output

fun interface SessionIdGenerator {
    fun nextId(): String
}
