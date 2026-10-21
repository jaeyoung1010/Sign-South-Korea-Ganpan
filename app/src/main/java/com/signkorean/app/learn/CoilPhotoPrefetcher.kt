package com.signkorean.app.learn

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.signkorean.feature.learn.BoundedPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoilPhotoPrefetcher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LearnPhotoPrefetcher {
        private val boundedPrefetcher = BoundedPhotoPrefetcher(::enqueue)
        private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun prefetch(imageUris: List<String>) {
            prefetchScope.launch {
                runCatching { boundedPrefetcher.prefetch(imageUris) }
            }
        }

        private fun enqueue(imageUri: String) {
            SingletonImageLoader
                .get(context)
                .enqueue(
                    ImageRequest
                        .Builder(context)
                        .data(imageUri)
                        .size(PREFETCH_IMAGE_SIZE_PX)
                        .build(),
                )
        }

        private companion object {
            const val PREFETCH_IMAGE_SIZE_PX = 720
        }
    }
