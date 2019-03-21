/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.R
import com.wireguard.android.databinding.LogViewerActivityBinding
import com.wireguard.android.util.LogExporter
import eu.chainfire.libsuperuser.Shell

class LiveLogViewerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var rootSession: Shell.Interactive
    private val logcatDataset: ArrayList<LogEntry> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: LogViewerActivityBinding =
                DataBindingUtil.setContentView(this, R.layout.log_viewer_activity)
        viewManager = LinearLayoutManager(this)
        viewAdapter = LogEntryAdapter(logcatDataset)
        recyclerView = binding.logviewer.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rootSession = Shell.Builder().useSH().open().apply {
            addCommand(
                arrayOf("logcat", "-b", "all", "-v", "threadtime", "*:V"),
                0, object : Shell.OnCommandLineListener {
                    override fun onLine(line: String?) {
                        line?.let {
                            logcatDataset.add(LogEntry(it))
                            runOnUiThread { viewAdapter.notifyDataSetChanged() }
                        }
                    }

                    override fun onCommandResult(commandCode: Int, exitCode: Int) {}
                }
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.export_log -> {
                if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000
                    )
                } else {
                    LogExporter.exportLog(this)
                }
                return true
            }
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1000 -> LogExporter.exportLog(this)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rootSession.isRunning) {
            rootSession.kill()
        }
    }

    class LogEntryAdapter(private val dataset: ArrayList<LogEntry>) :
            RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        class ViewHolder(val textView: TextView, var isSingleLine: Boolean = true) :
                RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): LogEntryAdapter.ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.apply {
                setSingleLine()
                text = dataset[position].entry
                setOnClickListener {
                    setSingleLine(!holder.isSingleLine)
                    holder.isSingleLine = !holder.isSingleLine
                }
            }
        }

        override fun getItemCount() = dataset.size
    }

    data class LogEntry(val entry: String)
}
