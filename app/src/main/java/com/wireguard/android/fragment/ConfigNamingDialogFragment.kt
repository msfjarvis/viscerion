/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wireguard.android.R
import com.wireguard.android.databinding.ConfigNamingDialogFragmentBinding
import com.wireguard.android.di.injector
import com.wireguard.android.model.TunnelManager
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class ConfigNamingDialogFragment : DialogFragment() {

    private var config: Config? = null
    private var binding: ConfigNamingDialogFragmentBinding? = null
    private var imm: InputMethodManager? = null
    @Inject lateinit var tunnelManager: TunnelManager

    override fun onAttach(context: Context) {
        injector.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configText = arguments?.getString(KEY_CONFIG_TEXT)?.toByteArray(StandardCharsets.UTF_8)
        try {
            config = Config.parse(ByteArrayInputStream(configText))
        } catch (exception: IOException) {
            throw IllegalArgumentException("Invalid config passed to ${javaClass.simpleName}", exception)
        } catch (exception: BadConfigException) {
            throw IllegalArgumentException("Invalid config passed to ${javaClass.simpleName}", exception)
        }
    }

    override fun onResume() {
        super.onResume()

        val dialog = dialog as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { createTunnelAndDismiss() }

        setKeyboardVisible(true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        imm = requireContext().getSystemService()

        val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
        alertDialogBuilder.setTitle(R.string.import_from_qr_code)
        alertDialogBuilder.setPositiveButton(R.string.create_tunnel, null)
        alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> dismiss() }

        binding = ConfigNamingDialogFragmentBinding.inflate(requireActivity().layoutInflater, null, false)
        binding?.apply {
            executePendingBindings()
            alertDialogBuilder.setView(root)
            tunnelNameText.requestFocus()
        }

        return alertDialogBuilder.create()
    }

    override fun dismiss() {
        setKeyboardVisible(false)
        super.dismiss()
    }

    private fun createTunnelAndDismiss() {
        binding?.apply {
            val name = tunnelNameText.text.toString()

            tunnelManager.create(name, config).whenComplete { tunnel, throwable ->
                if (tunnel == null) {
                    tunnelNameTextLayout.error = throwable.message
                } else {
                    dismiss()
                }
            }
        }
    }

    private fun setKeyboardVisible(visible: Boolean) {
        imm?.apply {
            if (visible) {
                toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            } else if (binding != null) {
                hideSoftInputFromWindow(binding?.tunnelNameText?.windowToken, 0)
            }
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
