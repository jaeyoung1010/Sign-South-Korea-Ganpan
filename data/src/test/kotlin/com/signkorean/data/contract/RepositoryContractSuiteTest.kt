package com.signkorean.data.contract

import com.signkorean.data.repository.BootstrapSessionRepository
import com.signkorean.data.repository.BootstrapUserSettingsRepository
import com.signkorean.data.repository.EmptyLearningRepository
import com.signkorean.domain.port.output.SessionRepository
import com.signkorean.domain.port.output.UserSettingsRepository
import com.signkorean.testsupport.FakeLearningRepository
import com.signkorean.testsupport.FakeSessionRepository
import com.signkorean.testsupport.FakeUserSettingsRepository
import com.signkorean.testsupport.contract.LearningRepositoryContract
import com.signkorean.testsupport.contract.SessionRepositoryContract
import com.signkorean.testsupport.contract.UserSettingsRepositoryContract

class EmptyLearningRepositoryContractTest : LearningRepositoryContract() {
    override fun createRepository() = EmptyLearningRepository()
}

class FakeLearningRepositoryContractTest : LearningRepositoryContract() {
    override fun createRepository() = FakeLearningRepository()
}

class BootstrapSessionRepositoryContractTest : SessionRepositoryContract() {
    override fun createRepository(activeSession: Boolean): SessionRepository = BootstrapSessionRepository(activeSession)
}

class FakeSessionRepositoryContractTest : SessionRepositoryContract() {
    override fun createRepository(activeSession: Boolean): SessionRepository = FakeSessionRepository(activeSession)
}

class BootstrapUserSettingsRepositoryContractTest : UserSettingsRepositoryContract() {
    override fun createRepository(): UserSettingsRepository = BootstrapUserSettingsRepository()
}

class FakeUserSettingsRepositoryContractTest : UserSettingsRepositoryContract() {
    override fun createRepository(): UserSettingsRepository = FakeUserSettingsRepository()
}
