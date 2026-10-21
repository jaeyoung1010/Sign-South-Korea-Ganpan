import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

abstract class VerifyModuleBoundaries : DefaultTask() {
    @get:Input
    abstract val declaredModuleDependencies: MapProperty<String, Set<String>>

    @get:Input
    abstract val allowedModuleDependencies: MapProperty<String, Set<String>>

    @TaskAction
    fun verify() {
        val allowedDependencies = allowedModuleDependencies.get()
        val violations =
            declaredModuleDependencies
                .get()
                .toSortedMap()
                .flatMap { (module, dependencies) ->
                    val allowed = allowedDependencies.getValue(module)
                    dependencies
                        .minus(allowed)
                        .sorted()
                        .map { dependency -> "$module must not depend on $dependency" }
                }

        check(violations.isEmpty()) {
            "Module boundary violations:\n${violations.joinToString(separator = "\n")}"
        }
    }
}

abstract class VerifyReleaseQaExclusion : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mergedReleaseManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val releaseApk: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifest = mergedReleaseManifest.get().asFile.readText()
        val forbiddenComponents = listOf("DebugQaActivity", "DebugResetReceiver")
        val includedComponents = forbiddenComponents.filter(manifest::contains)

        check(includedComponents.isEmpty()) {
            "Release manifest must exclude debug QA components: ${includedComponents.joinToString()}"
        }

        val forbiddenApkStrings = forbiddenComponents + "Interactive QA menu"
        ZipFile(releaseApk.get().asFile).use { apk ->
            val releaseDex =
                apk
                    .entries()
                    .asSequence()
                    .filter { entry -> entry.name.startsWith("classes") && entry.name.endsWith(".dex") }
                    .joinToString(separator = "") { entry ->
                        apk.getInputStream(entry).use { input ->
                            String(input.readBytes(), StandardCharsets.UTF_8)
                        }
                    }
            val includedStrings = forbiddenApkStrings.filter(releaseDex::contains)

            check(includedStrings.isEmpty()) {
                "Release APK must exclude debug QA strings: ${includedStrings.joinToString()}"
            }
        }
    }
}

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    format("misc") {
        target("*.md", "docs/**/*.md", "**/*.toml", "**/*.yml", "**/*.yaml", ".gitignore", ".gitattributes")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val qualityToolTests =
    tasks.register<Exec>("qualityToolTests") {
        group = "verification"
        description = "Runs tests for the repository quality scanners."
        commandLine(
            "node",
            "--test",
            "scripts/quality/quality-rules.test.mjs",
            "scripts/quality/phase43-traceability.test.mjs",
            "scripts/quality/phase44-qa.test.mjs",
        )
    }

val validatePhase43Traceability =
    tasks.register<Exec>("validatePhase43Traceability") {
        group = "verification"
        description = "Verifies that every documented requirement and E2E scenario has code and test evidence."
        commandLine("node", "scripts/quality/phase43-traceability.mjs")
    }

val validatePhase44Qa =
    tasks.register<Exec>("validatePhase44Qa") {
        group = "verification"
        description = "Verifies the Phase 44 device QA matrix and release blocker record."
        commandLine("node", "scripts/quality/phase44-qa.mjs")
    }

val validateDependencyVersions =
    tasks.register<Exec>("validateDependencyVersions") {
        group = "verification"
        description = "Rejects dynamic or snapshot dependency versions."
        commandLine("node", "scripts/quality/check-dependency-versions.mjs", ".")
    }

val secretScan =
    tasks.register<Exec>("secretScan") {
        group = "verification"
        description = "Scans repository text files for likely committed credentials."
        commandLine("node", "scripts/quality/check-secrets.mjs", ".")
    }

tasks.register("qualityGate") {
    group = "verification"
    description = "Builds the app and runs repository quality and traceability checks."
    dependsOn(
        qualityToolTests,
        validatePhase43Traceability,
        validatePhase44Qa,
        validateDependencyVersions,
        secretScan,
        tasks.named("spotlessCheck"),
        ":app:detekt",
        ":app:lintDebug",
        ":app:testDebugUnitTest",
        ":domain:test",
        ":data:testDebugUnitTest",
        ":designsystem:testDebugUnitTest",
        ":test-support:test",
        ":app:assembleDebug",
    )
}

val verifyReleaseQaExclusion =
    tasks.register<VerifyReleaseQaExclusion>("verifyReleaseQaExclusion") {
        group = "verification"
        description = "Verifies that debug-only QA components are absent from the release manifest."
        dependsOn(":app:assembleRelease")
        mergedReleaseManifest.set(
            layout.projectDirectory.file(
                "app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml",
            ),
        )
        releaseApk.set(layout.projectDirectory.file("app/build/outputs/apk/release/app-release-unsigned.apk"))
    }

tasks.named("check") {
    dependsOn(
        qualityToolTests,
        validatePhase43Traceability,
        validatePhase44Qa,
        validateDependencyVersions,
        secretScan,
        tasks.named("spotlessCheck"),
        verifyReleaseQaExclusion,
    )
}

val allowedDependenciesByModule =
    mapOf(
        ":domain" to emptySet(),
        ":data" to setOf(":domain"),
        ":platform" to setOf(":domain"),
        ":designsystem" to emptySet(),
        ":feature-onboarding" to setOf(":domain", ":designsystem"),
        ":feature-learn" to setOf(":domain", ":designsystem"),
        ":feature-progress" to setOf(":domain", ":designsystem"),
        ":feature-settings" to setOf(":domain", ":designsystem"),
        ":test-support" to setOf(":domain"),
        ":app" to
            setOf(
                ":domain",
                ":data",
                ":platform",
                ":designsystem",
                ":feature-onboarding",
                ":feature-learn",
                ":feature-progress",
                ":feature-settings",
                ":test-support",
            ),
    )

gradle.projectsEvaluated {
    val declaredDependencies: Map<String, Set<String>> =
        subprojects.associate { subproject: Project ->
            val dependencies: Set<String> =
                buildSet {
                    subproject.configurations
                        .filterNot { configuration ->
                            configuration.name.contains("test", ignoreCase = true)
                        }.forEach { configuration ->
                            configuration.dependencies.forEach { dependency ->
                                if (dependency is ProjectDependency) add(dependency.path)
                            }
                        }
                }
            subproject.path to dependencies
        }

    val verifyModuleBoundaries =
        tasks.register<VerifyModuleBoundaries>("verifyModuleBoundaries") {
            group = "verification"
            description = "Rejects project dependencies that violate the hexagonal module boundaries."
            declaredModuleDependencies.putAll(declaredDependencies)
            allowedModuleDependencies.putAll(allowedDependenciesByModule)
        }

    tasks.named("qualityGate") {
        dependsOn(verifyModuleBoundaries, verifyReleaseQaExclusion)
    }
    tasks.named("check") {
        dependsOn(verifyModuleBoundaries)
    }
}
