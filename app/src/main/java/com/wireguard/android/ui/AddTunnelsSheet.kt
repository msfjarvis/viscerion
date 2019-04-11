/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.zxing.integration.android.IntentIntegrator
import com.wireguard.android.R
import com.wireguard.android.activity.TunnelCreatorActivity
import com.wireguard.android.fragment.TunnelListFragment
import com.google.android.material.R as materialR

class AddTunnelsSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) dismiss()
        return inflater.inflate(R.layout.add_tunnels_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialog = dialog as BottomSheetDialog?
                val bottomSheet = dialog?.findViewById<FrameLayout>(materialR.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            dismiss()
                        }
                    }
                })
            }
        })
        dialog?.findViewById<MaterialButton>(R.id.create_empty)?.setOnClickListener {
            onRequestCreateConfig()
            dismiss()
        }
        dialog?.findViewById<MaterialButton>(R.id.create_from_file)?.setOnClickListener {
            onRequestImportConfig()
            dismiss()
        }
        dialog?.findViewById<MaterialButton>(R.id.create_from_qrcode)?.setOnClickListener {
            onRequestScanQRCode()
            dismiss()
        }
    }

    private fun onRequestCreateConfig() {
        startActivity(Intent(activity, TunnelCreatorActivity::class.java))
    }

    private fun onRequestImportConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(Intent.createChooser(intent, "Choose ZIP or conf"), TunnelListFragment.REQUEST_IMPORT)
    }

    private fun onRequestScanQRCode() {
        val intentIntegrator = IntentIntegrator.forSupportFragment(this).apply {
            setOrientationLocked(false)
            setBeepEnabled(false)
            setPrompt(getString(R.string.qr_code_hint))
        }
        intentIntegrator.initiateScan(listOf(IntentIntegrator.QR_CODE))
    }
}
