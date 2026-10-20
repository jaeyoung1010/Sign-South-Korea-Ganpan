package com.signkorean.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signkorean.data.db.SignKoreanDatabase
import com.signkorean.designsystem.theme.SignKoreanTheme
import com.signkorean.feature.onboarding.OnboardingScreen
import com.signkorean.feature.onboarding.OnboardingStep
import com.signkorean.feature.onboarding.OnboardingUiState
import com.signkorean.feature.onboarding.TutorialTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TutorialPersistenceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Inject
    lateinit var database: SignKoreanDatabase

    @Test
    fun tutorial_practice_keeps_learning_database_unchanged() {
        hiltRule.inject()
        val progressCount = database.cardProgressDao().count()

        composeRule.setContent {
            SignKoreanTheme {
                OnboardingScreen(
                    state = OnboardingUiState(OnboardingStep.Tutorial),
                    onAction = {},
                )
            }
        }
        composeRule.onNodeWithTag(TutorialTestTags.SHOW_ANSWER).performClick()
        composeRule.onNodeWithTag(TutorialTestTags.GOT_IT).performClick()

        assertEquals(progressCount, database.cardProgressDao().count())
    }
}
