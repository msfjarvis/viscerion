/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.fragment.TunnelDetailFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.Tunnel

/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */

class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    @Nullable
    private var actionBar: ActionBar? = null
    private var isTwoPaneLayout: Boolean = false
    @Nullable
    private var listFragment: TunnelListFragment? = null

    override fun onBackPressed() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        // If the action menu is visible and expanded, collapse it instead of navigating back.
        if (isTwoPaneLayout || backStackEntries == 0) {
            if (listFragment != null && listFragment!!.collapseActionMenu())
                return
        }
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
        actionBar!!.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }

    // We use onTouchListener here to avoid the UI click sound, hence
    // calling View#performClick defeats the purpose of it.
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        actionBar = supportActionBar
        isTwoPaneLayout = findViewById<View>(R.id.master_detail_wrapper) is LinearLayout
        listFragment = supportFragmentManager.findFragmentByTag("LIST") as TunnelListFragment
        supportFragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()
        val actionBarView = findViewById<View>(R.id.action_bar)
        actionBarView?.setOnTouchListener { _, _ -> listFragment != null && listFragment!!.collapseActionMenu() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        menu.findItem(R.id.menu_theme_change).isChecked =
            Application.sharedPreferences.getBoolean("dark_theme", true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // The back arrow in the action bar should act the same as the back button.
                onBackPressed()
                return true
            }
            R.id.menu_action_edit -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_container, TunnelEditorFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(null)
                    .commit()
                return true
            }
            R.id.menu_action_save ->
                // This menu item is handled by the editor fragment.
                return false
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.menu_theme_change -> {
                Application.sharedPreferences.edit().putBoolean("dark_theme", !item.isChecked).apply()
                item.isChecked = !item.isChecked
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        @Nullable oldTunnel: Tunnel?,
        @Nullable newTunnel: Tunnel?
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
            fragmentManager.beginTransaction()
                .add(R.id.detail_container, TunnelDetailFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit()
        }
    }
}
