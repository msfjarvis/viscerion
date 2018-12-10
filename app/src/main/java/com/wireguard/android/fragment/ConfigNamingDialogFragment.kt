/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.databinding.ConfigNamingDialogFragmentBinding
import com.wireguard.android.widget.NameInputFilter
import com.wireguard.config.Config
import java.io.IOException

class ConfigNamingDialogFragment : DialogFragment() {

    private var config: Config? = null
    private var binding: ConfigNamingDialogFragmentBinding? = null
    private var imm: InputMethodManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            config = Config.from(arguments?.getString(KEY_CONFIG_TEXT))
        } catch (exception: IOException) {
            throw RuntimeException("Invalid config passed to ${javaClass.simpleName}", exception)
        }
    }

    override fun onResume() {
        super.onResume()

        val dialog = dialog as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { createTunnelAndDismiss() }

        setKeyboardVisible(true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity

        imm = context?.getSystemService<InputMethodManager>()

        // Allow throwing with a null activity, there's not much to do anyway
        val alertDialogBuilder = AlertDialog.Builder(activity!!)
        alertDialogBuilder.setTitle(R.string.import_from_qrcode)

        binding = ConfigNamingDialogFragmentBinding.inflate(activity.layoutInflater, null, false)
        binding?.executePendingBindings()
        alertDialogBuilder.setView(binding?.root)
        binding?.tunnelNameText?.filters = arrayOf(NameInputFilter())

        alertDialogBuilder.setPositiveButton(R.string.create_tunnel, null)
        alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> dismiss() }

        return alertDialogBuilder.create()
    }

    override fun dismiss() {
        setKeyboardVisible(false)
        super.dismiss()
    }

    private fun createTunnelAndDismiss() {
        binding?.let {
            val name = it.tunnelNameText.text.toString()

            Application.tunnelManager.create(name, config).whenComplete { tunnel, throwable ->
                if (tunnel != null) {
                    dismiss()
                } else {
                    it.tunnelNameTextLayout.error = throwable.message
                }
            }
        }
    }

    private fun setKeyboardVisible(visible: Boolean) {
        if (visible) {
            imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else if (binding != null) {
            imm?.hideSoftInputFromWindow(binding?.tunnelNameText?.windowToken, 0)
        }
    }

    companion object {

        private const val KEY_CONFIG_TEXT = "config_text"

        fun newInstance(configText: String): ConfigNamingDialogFragment {
            val extras = Bundle()
            extras.putString(KEY_CONFIG_TEXT, configText)
            val fragment = ConfigNamingDialogFragment()
            fragment.arguments = extras
            return fragment
        }
    }
}
