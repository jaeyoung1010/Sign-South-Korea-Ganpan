package com.signkorean.testsupport

data class ScreenshotCase(
    val id: String,
    val widthDp: Int,
    val localeTag: String,
    val fontScale: Float = 1f,
) {
    init {
        require(id.matches(Regex("[a-z0-9-]+"))) { "Screenshot case ID must be stable: $id" }
        require(widthDp > 0) { "Screenshot width must be positive: $widthDp" }
    }
}

data class ScreenshotArtifact(
    val caseId: String,
    val bytes: ByteArray,
)

object ScreenshotTestHarness {
    fun capture(
        case: ScreenshotCase,
        capture: (ScreenshotCase) -> ScreenshotArtifact,
    ): ScreenshotArtifact =
        capture(case).also { artifact ->
            require(artifact.caseId == case.id) {
                "Screenshot artifact ID ${artifact.caseId} does not match case ${case.id}"
            }
        }
}
