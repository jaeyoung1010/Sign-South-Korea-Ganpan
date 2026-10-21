package com.signkorean.feature.learn

import com.signkorean.domain.model.CardReportReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportUiStateTest {
    @Test
    fun `send is disabled until a reason is selected`() {
        val state = ReportUiState()

        assertFalse(state.canSubmit)
        assertTrue(state.copy(selectedReason = CardReportReason.OTHER).canSubmit)
    }

    @Test
    fun `details input is truncated at 500 unicode code points`() {
        val state = ReportUiState().withDetails("😀".repeat(501))

        assertEquals(500, state.details.codePointCount(0, state.details.length))
    }

    @Test
    fun `saving and error states prevent another submission`() {
        val selected = ReportUiState(selectedReason = CardReportReason.WRONG_KOREAN)

        assertFalse(selected.copy(status = ReportStatus.Saving).canSubmit)
        assertTrue(selected.copy(status = ReportStatus.Error).canSubmit)
    }
}
