/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.util.AuthenticationResult
import com.wireguard.android.util.Authenticator

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getPrefs().fingerprintAuth) {
            Authenticator(this) {
                when (it) {
                    is AuthenticationResult.Success -> {
                        startMainActivity()
                    }
                    is AuthenticationResult.UnrecoverableError -> {
                        finish()
                    }
                    else -> {
                    }
                }
            }.authenticate()
        } else {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
