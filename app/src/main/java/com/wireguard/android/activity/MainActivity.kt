/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import com.wireguard.android.di.injector
import com.wireguard.android.fragment.TunnelDetailFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.ApplicationPreferencesChangeCallback
import com.wireguard.android.util.humanReadablePath
import com.wireguard.android.util.runShellCommand
import java.io.FileOutputStream
import javax.inject.Inject
import timber.log.Timber

/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */

class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private var listFragment: TunnelListFragment? = null
    @Inject lateinit var prefs: ApplicationPreferences

    override fun onDestroy() {
        prefs.unregisterCallback()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        // If the two-pane layout does not have an editor open, going back should exit the app.
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finishAfterTransition()
            return
        }
        // Deselect the current tunnel on navigating back from the detail pane to the one-pane list.
        if (!isTwoPaneLayout && backStackEntries == 1) {
            supportFragmentManager.popBackStack()
            selectedTunnel = null
            return
        }
        if (isTaskRoot) {
            if (backStackEntries == 2) {
                supportFragmentManager.popBackStack()
            } else if (backStackEntries == 0) {
                finishAfterTransition()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onBackStackChanged() {
        if (actionBar == null) {
            return
        }
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val backStackEntries = supportFragmentManager.backStackEntryCount
        val minBackStackEntries = if (isTwoPaneLayout) {
            2
        } else {
            1
        }
        actionBar?.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_LOG_FILE_LOCATION) {
            data?.data?.also { uri ->
                Timber.d("Exporting logcat stream to ${uri.path}")
                exportLog(uri)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        actionBar = supportActionBar
        isTwoPaneLayout = findViewById<View>(R.id.master_detail_wrapper) is LinearLayout
        listFragment = supportFragmentManager.findFragmentByTag("LIST") as TunnelListFragment
        supportFragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()
        prefs.registerCallback(ApplicationPreferencesChangeCallback(this, tunnelManager))
        // Dispatch insets on back stack changed
        findViewById<ViewGroup>(android.R.id.content).setOnApplyWindowInsetsListener { _, insets ->
            supportFragmentManager.let {
                it.addOnBackStackChangedListener {
                    it.fragments.last().view?.dispatchApplyWindowInsets(insets)
                }
                insets
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // The back arrow in the action bar should act the same as the back button.
                onBackPressed()
                return true
            }
            R.id.export_log -> {
                createLogFile()
                return true
            }
            R.id.menu_action_edit -> {
                supportFragmentManager.commit {
                    replace(R.id.detail_container, TunnelEditorFragment::class.java, null)
                    setCustomAnimations(0, 0)
                    addToBackStack(null)
                }
                return true
            }
            R.id.menu_action_save ->
                // This menu item is handled by the editor fragment.
                return false
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        oldTunnel: Tunnel?,
        newTunnel: Tunnel?
    ) {
        val fragmentManager = supportFragmentManager
        val backStackEntries = fragmentManager.backStackEntryCount
        if (newTunnel == null) {
            // Clear everything off the back stack (all editors and detail fragments).
            fragmentManager.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return
        }
        if (backStackEntries == 2) {
            // Pop the editor off the back stack to reveal the detail fragment. Use the immediate
            // method to avoid the editor picking up the new tunnel while it is still visible.
            fragmentManager.popBackStackImmediate()
        } else if (backStackEntries == 0) {
            // Create and show a new detail fragment.
            fragmentManager.commit {
                add(R.id.detail_container, TunnelDetailFragment::class.java, null)
                setCustomAnimations(0, 0)
                addToBackStack(null)
            }
        }
    }

    private fun createLogFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "viscerion-log.txt")
        }
        startActivityForResult(intent, REQUEST_LOG_FILE_LOCATION)
    }

    private fun exportLog(fileUri: Uri) {
        contentResolver.openFileDescriptor(fileUri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                readLogcat().forEach { line ->
                    outputStream.write((line + "\n").toByteArray())
                }
            }
            val message = getString(R.string.log_export_success, fileUri.humanReadablePath)
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun readLogcat(): ArrayList<String> {
        val ret = ArrayList<String>()
        "logcat -b all -d -v threadtime *:V".runShellCommand().forEach { line ->
            ret.add(line)
        }
        return ret
    }

    companion object {
        var isTwoPaneLayout: Boolean = false
            private set
        private const val REQUEST_LOG_FILE_LOCATION = 1000
    }
}
