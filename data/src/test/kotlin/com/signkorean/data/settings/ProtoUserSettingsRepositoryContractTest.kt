package com.signkorean.data.settings

import com.signkorean.domain.port.output.AppLocalePort
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.testsupport.contract.UserSettingsRepositoryContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class ProtoUserSettingsRepositoryContractTest : UserSettingsRepositoryContract() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() =
        runBlocking {
            scopes.forEach { scope -> scope.coroutineContext[Job]?.cancelAndJoin() }
        }

    override fun createRepository(): UserSettingsRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add)
        return ProtoUserSettingsRepository(
            dataStore =
                UserSettingsDataStoreFactory.create(
                    file = File(temporaryFolder.root, "${UUID.randomUUID()}.pb"),
                    diagnostics = NoopSettingsDiagnostics,
                    scope = scope,
                ),
            systemReduceMotionSource = SystemReduceMotionSource { false },
            appLocalePort =
                object : AppLocalePort {
                    override fun getSupported() = setOf("en", "ja")

                    override fun getCurrent() = "en"

                    override fun setCurrent(languageTag: String) = Unit
                },
        )
    }
}
