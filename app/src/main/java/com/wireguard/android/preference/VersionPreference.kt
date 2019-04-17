/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.preference

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.BackendAsync
import org.koin.core.KoinComponent
import org.koin.core.inject

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), KoinComponent {
    private var versionSummary: String? = null

    init {
        inject<BackendAsync>().value.thenAccept { backend ->
            versionSummary =
                    getContext().getString(R.string.version_summary_checking, backend.getTypePrettyName().toLowerCase())
            inject<AsyncWorker>().value.supplyAsync {
                backend.getVersion()
            }.whenComplete { version, exception ->
                versionSummary = if (exception == null)
                    getContext().getString(R.string.version_summary, backend.getTypePrettyName(), version)
                else
                    getContext().getString(
                                    R.string.version_summary_unknown,
                                    backend.getTypePrettyName().toLowerCase()
                            )
                notifyChanged()
            }
        }
    }

    override fun getSummary(): CharSequence? {
        return versionSummary
    }

    override fun getTitle(): CharSequence {
        return context.getString(
                R.string.version_title,
                if (BuildConfig.DEBUG) BuildConfig.GIT_HASH else BuildConfig.VERSION_NAME
        )
    }

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://github.com/MSF-Jarvis/viscerion")
        try {
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}
