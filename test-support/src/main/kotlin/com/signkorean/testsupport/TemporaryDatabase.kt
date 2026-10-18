package com.signkorean.testsupport

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

class TemporaryDatabase private constructor(
    private val directory: Path,
) : AutoCloseable {
    private var closed = false

    val databasePath: Path = directory.resolve("sign-korean-test.db")

    init {
        Files.createFile(databasePath)
    }

    override fun close() {
        if (closed) return

        if (Files.exists(directory)) {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
        closed = true
    }

    companion object {
        fun create(): TemporaryDatabase = TemporaryDatabase(Files.createTempDirectory("sign-korean-db-"))
    }
}
