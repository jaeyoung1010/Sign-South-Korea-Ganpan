package com.signkorean.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.google.protobuf.InvalidProtocolBufferException
import com.signkorean.data.settings.proto.UserSettingsProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun interface SettingsDiagnostics {
    fun onCorruptionRecovered()
}

fun interface SystemReduceMotionSource {
    fun isReduceMotionEnabled(): Boolean
}

object NoopSettingsDiagnostics : SettingsDiagnostics {
    override fun onCorruptionRecovered() = Unit
}

object UserSettingsProtoSerializer : Serializer<UserSettingsProto> {
    override val defaultValue: UserSettingsProto = UserSettingsProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserSettingsProto =
        try {
            UserSettingsProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read user settings proto.", exception)
        }

    override suspend fun writeTo(
        t: UserSettingsProto,
        output: OutputStream,
    ) {
        t.writeTo(output)
    }
}

object UserSettingsDataStoreFactory {
    fun create(
        file: File,
        diagnostics: SettingsDiagnostics,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    ): DataStore<UserSettingsProto> {
        file.parentFile?.mkdirs()
        return DataStoreFactory.create(
            serializer = UserSettingsProtoSerializer,
            corruptionHandler =
                ReplaceFileCorruptionHandler {
                    diagnostics.onCorruptionRecovered()
                    UserSettingsProto.getDefaultInstance()
                },
            migrations = listOf(UserSettingsV1Migration),
            scope = scope,
            produceFile = { file },
        )
    }
}

object UserSettingsV1Migration : DataMigration<UserSettingsProto> {
    override suspend fun shouldMigrate(currentData: UserSettingsProto): Boolean = currentData.schemaVersion < VERSION

    override suspend fun migrate(currentData: UserSettingsProto): UserSettingsProto =
        currentData
            .toBuilder()
            .setSchemaVersion(VERSION)
            .build()

    override suspend fun cleanUp() = Unit

    private const val VERSION = 1
}
