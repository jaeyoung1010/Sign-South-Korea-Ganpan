package com.signkorean.feature.learn

fun interface LearnPhotoPrefetcher {
    fun prefetch(imageUris: List<String>)
}

class BoundedPhotoPrefetcher(
    private val enqueue: (String) -> Unit,
) : LearnPhotoPrefetcher {
    override fun prefetch(imageUris: List<String>) {
        val selected = LinkedHashSet<String>(MAX_PREFETCH_COUNT)
        for (imageUri in imageUris) {
            selected += imageUri
            if (selected.size == MAX_PREFETCH_COUNT) break
        }
        selected.forEach(enqueue)
    }

    private companion object {
        const val MAX_PREFETCH_COUNT = 2
    }
}
