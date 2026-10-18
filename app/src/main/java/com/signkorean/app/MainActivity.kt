package com.signkorean.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.signkorean.designsystem.components.GameCard
import com.signkorean.designsystem.components.GameChip
import com.signkorean.designsystem.layout.SignKoreanScreen
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.designsystem.theme.SignKoreanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignKoreanBootstrap()
        }
    }
}

@Composable
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
private fun SignKoreanBootstrap() {
    SignKoreanTheme {
        SignKoreanScreen {
            Column(
                modifier = Modifier.padding(SignKoreanSpacing.xLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = stringResource(R.string.foundation_ready),
                    style = MaterialTheme.typography.bodyLarge,
                )
                GameCard(modifier = Modifier.padding(top = SignKoreanSpacing.large)) {
                    GameChip(text = "Design system ready")
                }
            }
        }
    }
}
