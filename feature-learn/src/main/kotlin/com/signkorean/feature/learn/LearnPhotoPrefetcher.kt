package com.signkorean.feature.learn

fun interface LearnPhotoPrefetcher {
    fun prefetch(imageUris: List<String>)
}

class BoundedPhotoPrefetcher(
    private val enqueue: (String) -> Unit,
) : LearnPhotoPrefetcher {
    override fun prefetch(imageUris: List<String>) {
        imageUris.distinct().take(MAX_PREFETCH_COUNT).forEach(enqueue)
    }

    private companion object {
        const val MAX_PREFETCH_COUNT = 2
    }
}
