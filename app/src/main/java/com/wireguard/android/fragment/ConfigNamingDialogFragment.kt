/*
 * Copyright © 2018 Eric Kuck <eric@bluelinelabs.com>.
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.databinding.ConfigNamingDialogFragmentBinding
import com.wireguard.config.Config
import java.io.IOException
import java.util.Objects

class ConfigNamingDialogFragment : DialogFragment() {

    private var config: Config? = null
    private var binding: ConfigNamingDialogFragmentBinding? = null
    private var imm: InputMethodManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            config = Config.from(arguments!!.getString(KEY_CONFIG_TEXT))
        } catch (exception: IOException) {
            throw RuntimeException("Invalid config passed to " + javaClass.simpleName, exception)
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

        imm = getActivity()!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val alertDialogBuilder = AlertDialog.Builder(activity!!)
        alertDialogBuilder.setTitle(R.string.import_from_qrcode)

        binding = ConfigNamingDialogFragmentBinding.inflate(getActivity()!!.layoutInflater, null, false)
        binding!!.executePendingBindings()
        alertDialogBuilder.setView(binding!!.root)

        alertDialogBuilder.setPositiveButton(R.string.create_tunnel, null)
        alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> dismiss() }

        return alertDialogBuilder.create()
    }

    override fun dismiss() {
        setKeyboardVisible(false)
        super.dismiss()
    }

    private fun createTunnelAndDismiss() {
        if (binding != null) {
            val name = binding!!.tunnelNameText.text.toString()

            Application.tunnelManager.create(name, config).whenComplete { tunnel, throwable ->
                if (tunnel != null) {
                    dismiss()
                } else {
                    binding!!.tunnelNameTextLayout.error = throwable.message
                }
            }
        }
    }

    private fun setKeyboardVisible(visible: Boolean) {
        Objects.requireNonNull<InputMethodManager>(imm)

        if (visible) {
            imm!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        } else if (binding != null) {
            imm!!.hideSoftInputFromWindow(binding!!.tunnelNameText.windowToken, 0)
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
