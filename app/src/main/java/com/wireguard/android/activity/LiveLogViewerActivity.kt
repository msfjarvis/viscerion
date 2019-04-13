/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.R
import com.wireguard.android.databinding.LogViewerActivityBinding
import com.wireguard.android.util.LogExporter
import com.wireguard.android.util.isPermissionGranted
import com.wireguard.android.util.runShellCommand
import java.util.Timer
import java.util.TimerTask

class LiveLogViewerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<LogEntryAdapter.ViewHolder>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var timer: Timer
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
        timer = Timer()
        timer.scheduleAtFixedRate(LogUpdateTask { logEntries ->
            if (logEntries.isEmpty()) return@LogUpdateTask
            val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(logEntries, logcatDataset))
            diffResult.dispatchUpdatesTo(viewAdapter)
            logcatDataset.apply {
                clear()
                addAll(logEntries)
            }
        }, 0, 5000)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.export_log -> {
                if (!this.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    class LogUpdateTask(val onUpdateCallback: (ArrayList<LogEntry>) -> Unit) : TimerTask() {
        override fun run() {
            val ret = ArrayList<LogEntry>()
            "logcat -b all -d -v threadtime *:V".runShellCommand().forEach { line ->
                ret.add(LogEntry(line))
            }
            onUpdateCallback(ret)
        }
    }

    class DiffUtilCallback(
        private var newList: ArrayList<LogEntry>,
        private var oldList: ArrayList<LogEntry>
    ) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition] == oldList[oldItemPosition]
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return newList[newItemPosition].entry == oldList[oldItemPosition].entry
        }
    }

    class LogEntryAdapter(private val dataset: ArrayList<LogEntry>) :
        RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.log_viewer_entry, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = dataset[position].entry
        }

        override fun getItemCount() = dataset.size
    }

    data class LogEntry(val entry: String)
}
