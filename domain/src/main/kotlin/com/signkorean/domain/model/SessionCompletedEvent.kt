package com.signkorean.domain.model

data class SessionCompletedEvent(
    val gotItCount: Int,
    val reviewCount: Int,
    val durationMillis: Long,
) {
    init {
        require(gotItCount >= 0)
        require(reviewCount >= 0)
        require(gotItCount + reviewCount > 0)
        require(durationMillis >= 0)
    }
}
