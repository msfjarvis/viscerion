/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.inlineactivityresult.startActivityForResult
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import com.wireguard.android.databinding.LogViewerActivityBinding
import com.wireguard.android.databinding.LogViewerEntryBinding
import com.wireguard.android.util.runShellCommand
import timber.log.Timber
import java.io.FileOutputStream
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
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.export_log -> {
                createLogFile()
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::timer.isInitialized) {
            timer.cancel()
        }
    }

    private fun createLogFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "viscerion-log.txt")
        }

        startActivityForResult(intent) { result, data ->
            if (result) {
                data.data?.also { uri ->
                    Timber.d("Exporting logcat stream to ${uri.path}")
                    exportLog(uri)
                }
            }
        }
    }

    private fun exportLog(fileUri: Uri) {
        contentResolver.openFileDescriptor(fileUri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                logcatDataset.forEach { entry ->
                    outputStream.write((entry.line + "\n").toByteArray())
                }
            }
            val message = getString(R.string.log_export_success, fileUri.path)
            findViewById<View>(android.R.id.content)?.let { view ->
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
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
            return newList[newItemPosition].line == oldList[oldItemPosition].line
        }
    }

    data class LogEntry(val line: String)

    class LogEntryAdapter(
        private val dataset: ArrayList<LogEntry>
    ) : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val binding = requireNotNull(DataBindingUtil.bind<LogViewerEntryBinding>(view))
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.row.text = dataset[position].line
        }

        override fun getItemCount() = dataset.size
    }
}
