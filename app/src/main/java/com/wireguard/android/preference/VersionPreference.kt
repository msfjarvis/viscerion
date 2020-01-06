/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
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
import com.wireguard.android.di.injector
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.BackendAsync
import java.util.Locale
import javax.inject.Inject

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var versionSummary: String? = null
    @Inject lateinit var backendAsync: BackendAsync
    @Inject lateinit var asyncWorker: AsyncWorker

    override fun onAttached() {
        injector.inject(this)
        super.onAttached()
        backendAsync.thenAccept { backend ->
            versionSummary =
                    getContext().getString(R.string.version_summary_checking, backend.getTypePrettyName().toLowerCase(Locale.ROOT))
            asyncWorker.supplyAsync {
                backend.getVersion()
            }.whenComplete { version, exception ->
                versionSummary = if (exception == null) {
                    getContext().getString(R.string.version_summary, backend.getTypePrettyName(), version)
                } else {
                    getContext().getString(
                        R.string.version_summary_unknown,
                        backend.getTypePrettyName().toLowerCase(Locale.ROOT)
                    )
                }
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
                if (BuildConfig.GIT_HASH.isNotEmpty()) BuildConfig.GIT_HASH else BuildConfig.VERSION_NAME
        )
    }

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://github.com/msfjarvis/viscerion")
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
