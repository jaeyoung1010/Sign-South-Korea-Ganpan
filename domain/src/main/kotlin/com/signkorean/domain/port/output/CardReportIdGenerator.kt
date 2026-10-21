package com.signkorean.domain.port.output

fun interface CardReportIdGenerator {
    fun nextId(): String
}
