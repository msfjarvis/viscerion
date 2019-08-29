/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.R
import com.wireguard.android.util.AuthenticationResult
import com.wireguard.android.util.Authenticator

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Authenticator(this) {
            when (it) {
                is AuthenticationResult.Success -> {
                    startMainActivity()
                }
                is AuthenticationResult.RecoverableError -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                is AuthenticationResult.UnrecoverableError -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AuthenticationResult.Cancelled -> {
                    Toast.makeText(this, getString(R.string.biometric_prompt_cancelled), Toast.LENGTH_SHORT).show()
                }
                else -> { }
            }
        }.authenticate()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
