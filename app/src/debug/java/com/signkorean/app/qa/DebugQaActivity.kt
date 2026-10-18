package com.signkorean.app.qa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import com.signkorean.app.MainActivity
import com.signkorean.app.navigation.AppNavigationIntent
import com.signkorean.designsystem.components.GameButton
import com.signkorean.designsystem.components.GameButtonStyle
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameChip
import com.signkorean.designsystem.layout.SignKoreanScreen
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.designsystem.theme.SignKoreanTheme

class DebugQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignKoreanTheme {
                var selectedScreen by rememberSaveable { mutableStateOf(DebugQaPreferences.read(this)) }
                SignKoreanScreen {
                    Column(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(SignKoreanSpacing.large),
                    ) {
                        Text(
                            text = "Interactive QA menu",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Debug only · prototype screen jump and reset",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        GameCard(
                            modifier = Modifier.padding(top = SignKoreanSpacing.medium),
                        ) {
                            GameChip(text = "Selected: $selectedScreen")
                            DebugQaScreens.all.forEach { screen ->
                                GameButton(
                                    text = screen,
                                    onClick = {
                                        selectedScreen = screen
                                        DebugQaPreferences.write(this@DebugQaActivity, screen)
                                        startActivity(
                                            Intent(this@DebugQaActivity, MainActivity::class.java)
                                                .putExtra(AppNavigationIntent.EXTRA_DEBUG_SCREEN, screen),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = GameButtonStyle.Secondary,
                                )
                            }
                            GameButton(
                                text = "Reset prototype state",
                                onClick = {
                                    DebugQaPreferences.reset(this@DebugQaActivity)
                                    selectedScreen = DebugQaScreens.first()
                                },
                                style = GameButtonStyle.Review,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal object DebugQaScreens {
    val all: List<String> = AppNavigationIntent.prototypeScreenIds

    fun first(): String = all.first()
}

private object DebugQaPreferences {
    private const val PREFERENCES_NAME = "debug_qa"
    private const val SCREEN_KEY = "selected_screen"

    fun read(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getString(SCREEN_KEY, null)
            ?: DebugQaScreens.first()

    fun write(
        context: Context,
        screen: String,
    ) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putString(SCREEN_KEY, screen)
        }
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit { clear() }
    }
}
