package com.signkorean.domain.port.output

import com.signkorean.domain.model.BundleValidationReport
import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.SignCard

interface CardCatalog {
    fun getCards(): List<SignCard>

    fun getCard(cardId: String): SignCard?

    fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy>

    fun validateBundle(): BundleValidationReport
}
