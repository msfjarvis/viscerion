/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.databinding.TaskerActivityBinding
import com.wireguard.android.model.TunnelManager

class TaskerActivity : AppCompatActivity() {

    private var binding: TaskerActivityBinding? = null
    val enableTunnel = ObservableBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.tasker_activity)
        binding?.activity = this
        binding?.taskerSaveButton?.setOnClickListener {
            saveTaskerAction()
        }

        intent.getBundleExtra(EXTRA_BUNDLE)?.let {
            binding?.taskerTunnelName?.setText(it.getString(TunnelManager.TUNNEL_NAME_INTENT_EXTRA))
            binding?.taskerSecret?.setText(it.getString(TunnelManager.INTENT_INTEGRATION_SECRET_EXTRA))
            enableTunnel.set(it.getString(TunnelManager.TUNNEL_STATE_INTENT_EXTRA) == "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_UP")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveTaskerAction() {
        hideKeyboard()
        val resultIntent = Intent()

        if (binding?.taskerTunnelName?.text.toString().isBlank()) {
            setResult(RESULT_CANCELED, resultIntent)
            super.finish()
            return
        }

        val bundle = Bundle()

        bundle.putString(TunnelManager.TUNNEL_NAME_INTENT_EXTRA, binding?.taskerTunnelName?.text.toString())
        bundle.putString(TunnelManager.INTENT_INTEGRATION_SECRET_EXTRA, binding?.taskerSecret?.text.toString())

        if (enableTunnel.get()) {
            bundle.putString(TunnelManager.TUNNEL_STATE_INTENT_EXTRA, "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_UP")
            resultIntent.putExtra(
                EXTRA_STRING_BLURB,
                getString(R.string.enable_tunnel, binding?.taskerTunnelName?.text)
            )
        } else {
            bundle.putString(TunnelManager.TUNNEL_STATE_INTENT_EXTRA, "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_DOWN")
            resultIntent.putExtra(
                EXTRA_STRING_BLURB,
                getString(R.string.disable_tunnel, binding?.taskerTunnelName?.text)
            )
        }

        resultIntent.putExtra(EXTRA_BUNDLE, bundle)
        setResult(RESULT_OK, resultIntent)
        super.finish()
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(
                it.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    companion object {
        private const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
        private const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    }
}
