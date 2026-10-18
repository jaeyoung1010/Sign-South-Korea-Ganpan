package com.signkorean.app.qa

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugQaActivityTest {
    @Test
    fun debugQaDeepLinkResolvesToDebugOnlyActivity() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("signkorean://debug/qa"))
                .setPackage(context.packageName)

        val resolved = context.packageManager.resolveActivity(intent, 0)

        assertNotNull(resolved)
        assertEquals(DebugQaActivity::class.java.name, resolved?.activityInfo?.name)

        ActivityScenario.launch<DebugQaActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(DebugQaActivity::class.java.name, activity::class.java.name)
            }
        }
    }
}
