/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.di.injector
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AuthenticationResult
import com.wireguard.android.util.Authenticator
import javax.inject.Inject

class LaunchActivity : AppCompatActivity() {

    @Inject lateinit var prefs: ApplicationPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        if (prefs.fingerprintAuth) {
            Authenticator(this) {
                when (it) {
                    is AuthenticationResult.Success -> {
                        startMainActivity(false)
                    }
                    is AuthenticationResult.UnrecoverableError -> {
                        startMainActivity(false)
                    }
                    else -> {
                    }
                }
            }.authenticate()
        } else {
            startMainActivity(true)
        }
    }

    private fun startMainActivity(noAuth: Boolean) {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        android.os.Handler().postDelayed({ finish() }, if (noAuth) 0L else 500L)
    }
}
