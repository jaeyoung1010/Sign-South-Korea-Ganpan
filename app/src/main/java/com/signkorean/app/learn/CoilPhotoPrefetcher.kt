package com.signkorean.app.learn

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.signkorean.feature.learn.BoundedPhotoPrefetcher
import com.signkorean.feature.learn.LearnPhotoPrefetcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoilPhotoPrefetcher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LearnPhotoPrefetcher {
        private val boundedPrefetcher = BoundedPhotoPrefetcher(::enqueue)

        override fun prefetch(imageUris: List<String>) {
            boundedPrefetcher.prefetch(imageUris)
        }

        private fun enqueue(imageUri: String) {
            SingletonImageLoader
                .get(context)
                .enqueue(
                    ImageRequest
                        .Builder(context)
                        .data(imageUri)
                        .build(),
                )
        }
    }
