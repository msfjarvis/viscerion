/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.providers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.wireguard.android.R
import com.wireguard.android.di.getInjector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import javax.inject.Inject

/**
 * Implementation of App Widget functionality.
 */
class OneTapWidget : AppWidgetProvider() {

    @Inject lateinit var tunnelManager: TunnelManager

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val lastUsedTunnel = tunnelManager.getLastUsedTunnel() ?: return
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, lastUsedTunnel)
        }
    }

    override fun onEnabled(context: Context) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(ComponentName(context, this::class.java))
        onUpdate(context, widgetManager, ids)
    }

    override fun onDisabled(context: Context) {}

    override fun onReceive(context: Context, intent: Intent) {
        getInjector(context).inject(this)
        super.onReceive(context, intent)
        onEnabled(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        lastUsedTunnel: Tunnel
    ) {
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, lastUsedTunnel.createToggleIntent(), 0)
        val views = RemoteViews(context.packageName, R.layout.one_tap_widget)
        views.setTextViewText(R.id.appwidget_text, lastUsedTunnel.name)
        views.setTextViewText(
            R.id.appwidget_button, context.getString(
                if (lastUsedTunnel.state == Tunnel.State.UP) {
                    R.string.disable
                } else {
                    R.string.enable
                }
            )
        )
        views.setOnClickPendingIntent(R.id.appwidget_button, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
