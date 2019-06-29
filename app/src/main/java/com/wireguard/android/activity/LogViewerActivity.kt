/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.recyclical.datasource.dataSourceTypedOf
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import com.wireguard.android.databinding.LogViewerActivityBinding
import com.wireguard.android.databinding.LogViewerEntryBinding
import com.wireguard.android.util.runShellCommand
import timber.log.Timber
import java.io.FileOutputStream

class LogViewerActivity : AppCompatActivity() {

    private val dataSource = dataSourceTypedOf(readLogcat())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<LogViewerActivityBinding>(
                this,
                R.layout.log_viewer_activity
        )?.logviewer?.setup {
            withDataSource(dataSource)
            withLayoutManager(LinearLayoutManager(this@LogViewerActivity))
            withItem<LogEntry, LogViewHolder>(R.layout.log_viewer_entry) {
                onBind(::LogViewHolder) { _, item ->
                    binding.row.text = item.line
                }
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESULT_OK && requestCode == REQUEST_LOCATION) {
            data?.data?.also { uri ->
                Timber.d("Exporting logcat stream to ${uri.path}")
                exportLog(uri)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createLogFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "viscerion-log.txt")
        }
        startActivityForResult(intent, REQUEST_LOCATION)
    }

    private fun exportLog(fileUri: Uri) {
        contentResolver.openFileDescriptor(fileUri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                dataSource.forEach { entry ->
                    outputStream.write((entry.line + "\n").toByteArray())
                }
            }
            val message = getString(R.string.log_export_success, fileUri.path)
            findViewById<View>(android.R.id.content)?.let { view ->
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun readLogcat(): ArrayList<LogEntry> {
        val ret = ArrayList<LogEntry>()
        "logcat -b all -d -v threadtime *:V".runShellCommand().forEach { line ->
            ret.add(LogEntry(line))
        }
        return ret
    }

    class LogViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val binding = requireNotNull(DataBindingUtil.bind<LogViewerEntryBinding>(view))
    }

    data class LogEntry(val line: String)

    companion object {
        private const val REQUEST_LOCATION = 1000
    }
}
