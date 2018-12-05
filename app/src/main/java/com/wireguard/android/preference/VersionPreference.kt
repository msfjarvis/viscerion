/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.preference

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.R

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var versionSummary: String? = null

    init {
        Application.backendAsync.thenAccept { backend ->
            versionSummary =
                getContext().getString(R.string.version_summary_checking, backend.getTypeName().toLowerCase())
            Application.asyncWorker.supplyAsync { backend.getVersion() }
                .whenComplete { version, exception ->
                    versionSummary = if (exception == null)
                        getContext().getString(R.string.version_summary, backend.getTypeName(), version)
                    else
                        getContext().getString(R.string.version_summary_unknown, backend.getTypeName().toLowerCase())
                    notifyChanged()
                }
        }
    }

    override fun getSummary(): CharSequence? {
        return versionSummary
    }

    override fun getTitle(): CharSequence {
        return context.getString(R.string.version_title, BuildConfig.VERSION_NAME)
    }

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.wireguard.com/")
        try {
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}
