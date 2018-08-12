/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.preference

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.preference.Preference
import android.util.AttributeSet
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.R

class VersionPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    private var versionSummary: String? = null

    init {

        Application.backendAsync.thenAccept { backend ->
            versionSummary = getContext().getString(R.string.version_summary_checking, backend.typeName.toLowerCase())
            Application.getAsyncWorker()!!.supplyAsync<String> { backend.version }.whenComplete { version, exception ->
                versionSummary = if (exception == null)
                    getContext().getString(R.string.version_summary, backend.typeName, version)
                else
                    getContext().getString(R.string.version_summary_unknown, backend.typeName.toLowerCase())
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
