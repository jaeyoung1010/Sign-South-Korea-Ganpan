import org.gradle.api.tasks.Exec

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt) apply false
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
        commandLine("node", "--test", "scripts/quality/quality-rules.test.mjs")
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
    description = "Builds the debug APK and runs all Phase 1 quality checks."
    dependsOn(
        qualityToolTests,
        validateDependencyVersions,
        secretScan,
        tasks.named("spotlessCheck"),
        ":app:detekt",
        ":app:lintDebug",
        ":app:testDebugUnitTest",
        ":app:assembleDebug",
    )
}

tasks.named("check") {
    dependsOn(qualityToolTests, validateDependencyVersions, secretScan, tasks.named("spotlessCheck"))
}
