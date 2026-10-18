package com.signkorean.testsupport.fixture

data class CardFixture(
    val id: String,
    val korean: String,
    val localeTag: String,
    val state: String,
)

class CardFixtureBuilder {
    var id: String = "sign-01"
    var korean: String = "약국"
    var localeTag: String = "en-US"
    var state: String = "question"

    fun build(): CardFixture = CardFixture(id, korean, localeTag, state)
}
