/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityOptionsCompat.makeCustomAnimation
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.fragment.TunnelDetailFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ApplicationPreferencesChangeCallback
import com.wireguard.android.util.updateAppTheme

/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */

class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private var listFragment: TunnelListFragment? = null
    private val prefCallback = ApplicationPreferencesChangeCallback(this)

    override fun onDestroy() {
        Application.appPrefs.unregisterCallback()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        // If the two-pane layout does not have an editor open, going back should exit the app.
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finish()
            return
        }
        // Deselect the current tunnel on navigating back from the detail pane to the one-pane list.
        if (!isTwoPaneLayout && backStackEntries == 1) {
            selectedTunnel = null
            return
        }
        super.onBackPressed()
    }

    override fun onBackStackChanged() {
        if (actionBar == null)
            return
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val backStackEntries = supportFragmentManager.backStackEntryCount
        val minBackStackEntries = if (isTwoPaneLayout) 2 else 1
        actionBar?.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        actionBar = supportActionBar
        isTwoPaneLayout = findViewById<View>(R.id.master_detail_wrapper) is LinearLayout
        listFragment = supportFragmentManager.findFragmentByTag("LIST") as TunnelListFragment
        supportFragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()
        Application.appPrefs.registerCallback(prefCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        menu.findItem(R.id.menu_theme_switch).title = if (Application.appPrefs.useDarkTheme)
            getString(R.string.dark_mode_disable)
        else
            getString(R.string.dark_mode_enable)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // The back arrow in the action bar should act the same as the back button.
                onBackPressed()
                return true
            }
            R.id.menu_logviewer -> {
                startActivity(Intent(this, LiveLogViewerActivity::class.java))
                return true
            }
            R.id.menu_action_edit -> {
                supportFragmentManager.commit {
                    replace(R.id.detail_container, TunnelEditorFragment())
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    addToBackStack(null)
                }
                return true
            }
            R.id.menu_theme_switch -> {
                Application.appPrefs.apply {
                    useDarkTheme = !useDarkTheme
                }
                updateAppTheme()
                val bundle = makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out).toBundle()
                finish()
                startActivity(intent, bundle)
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
                add(R.id.detail_container, TunnelDetailFragment())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
        }
    }

    companion object {
        var isTwoPaneLayout: Boolean = false
            private set
    }
}
