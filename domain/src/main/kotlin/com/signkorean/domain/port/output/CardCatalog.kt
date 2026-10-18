package com.signkorean.domain.port.output

import com.signkorean.domain.model.LocalizedCardCopy
import com.signkorean.domain.model.SignCard

interface CardCatalog {
    fun getCards(): List<SignCard>

    fun getLocalizedCopy(languageTag: String): List<LocalizedCardCopy>
}
