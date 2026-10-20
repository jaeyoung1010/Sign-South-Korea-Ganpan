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
}
