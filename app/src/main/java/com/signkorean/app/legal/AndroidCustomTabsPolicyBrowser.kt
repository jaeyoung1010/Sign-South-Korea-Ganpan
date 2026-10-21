package com.signkorean.app.legal

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.signkorean.domain.model.PolicyDocument
import com.signkorean.domain.port.output.PolicyBrowserPort
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidCustomTabsPolicyBrowser
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PolicyBrowserPort {
        override fun open(document: PolicyDocument): Boolean {
            val uri = document.onlineUrl.toUri()
            if (uri.scheme != "https" || uri.host.isNullOrBlank() || !hasValidatedNetwork()) return false
            return try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                if (context !is android.app.Activity) {
                    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                customTabsIntent.launchUrl(context, uri)
                true
            } catch (_: Exception) {
                false
            }
        }

        private fun hasValidatedNetwork(): Boolean {
            val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
