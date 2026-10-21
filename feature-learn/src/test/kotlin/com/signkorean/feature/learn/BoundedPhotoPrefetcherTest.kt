package com.signkorean.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundedPhotoPrefetcherTest {
    @Test
    fun `enqueues only the next two unique images from a twenty card session`() {
        val enqueued = mutableListOf<String>()
        val prefetcher = BoundedPhotoPrefetcher(enqueued::add)

        prefetcher.prefetch((1..20).map { "asset://card-$it" })

        assertEquals(listOf("asset://card-1", "asset://card-2"), enqueued)
    }

    @Test
    fun `does not enqueue the same image twice`() {
        val enqueued = mutableListOf<String>()
        val prefetcher = BoundedPhotoPrefetcher(enqueued::add)

        prefetcher.prefetch(listOf("asset://same", "asset://same", "asset://next"))

        assertEquals(listOf("asset://same", "asset://next"), enqueued)
    }

    @Test
    fun `stops scanning once the bounded prefetch set is full`() {
        val enqueued = mutableListOf<String>()
        var visited = 0
        val images =
            object : AbstractList<String>() {
                override val size: Int = 20

                override fun get(index: Int): String {
                    visited += 1
                    return "asset://card-${index + 1}"
                }
            }

        BoundedPhotoPrefetcher(enqueued::add).prefetch(images)

        assertEquals(listOf("asset://card-1", "asset://card-2"), enqueued)
        assertEquals(2, visited)
    }
}
