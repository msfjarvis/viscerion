/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.app.SearchManager
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import com.wireguard.android.configStore.FileConfigStore.Companion.CONFIGURATION_FILE_SUFFIX
import com.wireguard.android.databinding.ObservableKeyedRecyclerViewAdapter
import com.wireguard.android.databinding.TunnelListFragmentBinding
import com.wireguard.android.databinding.TunnelListItemBinding
import com.wireguard.android.di.ext.getAsyncWorker
import com.wireguard.android.di.ext.injectPrefs
import com.wireguard.android.di.ext.injectTunnelManager
import com.wireguard.android.model.Tunnel
import com.wireguard.android.ui.EdgeToEdge
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ImportEventsListener
import com.wireguard.android.util.KotlinCompanions
import com.wireguard.android.widget.MultiselectableRelativeLayout
import com.wireguard.android.widget.fab.FloatingActionButtonRecyclerViewScrollListener
import com.wireguard.config.Config
import java9.util.concurrent.CompletableFuture
import timber.log.Timber
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TunnelListFragment : BaseFragment(), SearchView.OnQueryTextListener {

    private val actionModeListener = ActionModeListener()
    private val tunnelManager by injectTunnelManager()
    private val prefs by injectPrefs()
    private val savedTunnelsList: ArrayList<Tunnel> = arrayListOf()
    private var actionMode: ActionMode? = null
    private var binding: TunnelListFragmentBinding? = null
    private lateinit var searchItem: MenuItem
    private val bottomSheetActionListener = object : ImportEventsListener {
        override fun onQrImport(result: String) {
            importTunnel(result)
        }

        override fun onRequestImport() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(
                    Intent.createChooser(intent, "Choose ZIP or conf"),
                    REQUEST_IMPORT
            )
        }
    }

    private fun importTunnel(configText: String) {
        try {
            // Ensure the config text is parseable before proceeding…
            Config.parse(ByteArrayInputStream(configText.toByteArray(StandardCharsets.UTF_8)))

            // Config text is valid, now create the tunnel…
            ConfigNamingDialogFragment.newInstance(configText).show(parentFragmentManager, null)
        } catch (exception: Exception) {
            onTunnelImportFinished(emptyList(), listOf<Throwable>(exception))
        }
    }

    private fun importTunnel(uri: Uri?) {
        val activity = activity
        if (activity == null || uri == null)
            return
        val contentResolver = activity.contentResolver

        val futureTunnels = ArrayList<CompletableFuture<Tunnel>>()
        val throwables = ArrayList<Throwable>()
        getAsyncWorker().supplyAsync {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
            var name = ""
            contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0))
                    name = cursor.getString(0)
                cursor.close()
            }
            if (name.isEmpty())
                name = Uri.decode(uri.lastPathSegment)
            var idx = name.lastIndexOf('/')
            if (idx >= 0) {
                require(idx < name.length - 1) { "Illegal file name: $name" }
                name = name.substring(idx + 1)
            }
            val isZip = name.toLowerCase(Locale.ROOT).endsWith(".zip")
            if (name.toLowerCase(Locale.ROOT).endsWith(CONFIGURATION_FILE_SUFFIX))
                name = name.substring(0, name.length - CONFIGURATION_FILE_SUFFIX.length)
            else require(isZip) { "File must be .conf or .zip" }

            if (isZip) {
                ZipInputStream(contentResolver.openInputStream(uri)).use { zip ->
                    val reader = BufferedReader(InputStreamReader(zip, StandardCharsets.UTF_8))
                    var entry: ZipEntry?
                    while (true) {
                        entry = zip.nextEntry
                        if (entry == null)
                            break
                        name = entry.name
                        idx = name.lastIndexOf('/')
                        if (idx >= 0) {
                            if (idx >= name.length - 1)
                                continue
                            name = name.substring(name.lastIndexOf('/') + 1)
                        }
                        if (name.toLowerCase(Locale.ROOT).endsWith(CONFIGURATION_FILE_SUFFIX))
                            name = name.substring(0, name.length - CONFIGURATION_FILE_SUFFIX.length)
                        else
                            continue
                        val config: Config? = try {
                            Config.parse(reader)
                        } catch (e: Exception) {
                            throwables.add(e)
                            null
                        }

                        if (config != null)
                            futureTunnels.add(tunnelManager.create(name, config).toCompletableFuture())
                    }
                }
            } else {
                futureTunnels.add(
                        tunnelManager.create(
                                name,
                                Config.parse(contentResolver.openInputStream(uri))
                        ).toCompletableFuture()
                )
            }

            if (futureTunnels.isEmpty()) {
                if (throwables.size == 1)
                    throw throwables[0]
                else require(throwables.isNotEmpty()) { "No configurations found" }
            }

            CompletableFuture.allOf(*futureTunnels.toTypedArray())
        }.whenComplete { future, exception ->
            if (exception != null) {
                onTunnelImportFinished(emptyList(), listOf(exception))
            } else {
                future.whenComplete { _, _ ->
                    val tunnels = ArrayList<Tunnel>(futureTunnels.size)
                    for (futureTunnel in futureTunnels) {
                        val tunnel: Tunnel? = try {
                            futureTunnel.getNow(null)
                        } catch (e: Exception) {
                            throwables.add(e)
                            null
                        }

                        if (tunnel != null)
                            tunnels.add(tunnel)
                    }
                    onTunnelImportFinished(tunnels, throwables)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_IMPORT -> {
                if (resultCode == AppCompatActivity.RESULT_OK)
                    data?.data?.also { uri ->
                        Timber.tag("TunnelImport").i("Import uri: $uri")
                        importTunnel(uri)
                    }
                return
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tunnelManager.getTunnels().thenAccept { observableSortedKeyedList ->
            observableSortedKeyedList.forEach {
                savedTunnelsList.add(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = TunnelListFragmentBinding.inflate(inflater, container, false)
        binding?.apply {
            createFab.setOnClickListener {
                val bottomSheet = AddTunnelsSheet(bottomSheetActionListener)
                bottomSheet.show(parentFragmentManager, "BOTTOM_SHEET")
            }
            tunnelList.addOnScrollListener(FloatingActionButtonRecyclerViewScrollListener(createFab))
            executePendingBindings()
            tunnels?.clear()
            tunnels?.addAll(savedTunnelsList)
        }
        // Collapse searchview on fragment transaction
        parentFragmentManager.addOnBackStackChangedListener {
            if (searchItem.isActionViewExpanded) {
                searchItem.collapseActionView()
            }
        }
        binding?.let {
            EdgeToEdge.setUpRoot(it.root as ViewGroup)
            EdgeToEdge.setUpFAB(it.createFab)
            EdgeToEdge.setUpScrollingContent(it.tunnelList, it.createFab)
        }
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun viewForTunnel(tunnel: Tunnel, tunnels: List<Tunnel>): MultiselectableRelativeLayout? {
        var view: MultiselectableRelativeLayout? = null
        if (binding != null) {
            view = binding!!.tunnelList.findViewHolderForAdapterPosition(
                    tunnels.indexOf(tunnel)
            )?.itemView as? MultiselectableRelativeLayout
        }
        return view
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        if (binding == null)
            return
        tunnelManager.getTunnels().thenAccept { tunnels ->
            newTunnel?.let {
                viewForTunnel(it, tunnels)?.setSingleSelected(true)
            }
            oldTunnel?.let {
                viewForTunnel(it, tunnels)?.setSingleSelected(false)
            }
        }
    }

    private fun onTunnelDeletionFinished(count: Int, throwable: Throwable?) {
        val message: String
        if (throwable == null) {
            message = resources.getQuantityString(R.plurals.delete_success, count, count)
        } else {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = resources.getQuantityString(R.plurals.delete_error, count, count, error)
            Timber.e(throwable)
        }
        if (binding != null) {
            Snackbar.make(binding!!.mainContainer, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onTunnelImportFinished(tunnels: List<Tunnel>, throwables: Collection<Throwable>) {
        var message = ""

        for (throwable in throwables) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.import_error, error)
            Timber.e(throwable)
        }

        savedTunnelsList.addAll(tunnels)
        when {
            tunnels.size == 1 && throwables.isEmpty() -> message = getString(R.string.import_success, tunnels[0].name)
            tunnels.isEmpty() && throwables.size == 1 -> {
            }
            throwables.isEmpty() -> message = resources.getQuantityString(
                    R.plurals.import_total_success,
                    tunnels.size, tunnels.size
            )
            throwables.isNotEmpty() -> {
                /* Use the exception message from above. */
                message = resources.getQuantityString(
                        R.plurals.import_partial_success,
                        tunnels.size + throwables.size,
                        tunnels.size, tunnels.size + throwables.size
                )
            }
        }

        if (prefs.exclusions.isNotEmpty()) {
            val excludedApps = prefs.exclusionsArray
            tunnels.forEach { tunnel ->
                val oldConfig = tunnel.getConfig()
                oldConfig?.let {
                    it.`interface`.excludedApplications.addAll(excludedApps)
                    tunnel.setConfig(it)
                }
            }
        }

        if (binding != null && message.isNotEmpty()) {
            Snackbar.make(binding!!.mainContainer, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        binding?.tunnels?.let { tunnelList ->
            tunnelList.clear()
            if (!newText.isNullOrEmpty()) {
                tunnelList.addAll(savedTunnelsList.filter {
                    it.name.contains(newText, true)
                })
            } else {
                tunnelList.addAll(savedTunnelsList)
            }
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        onQueryTextChange(query)
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView
        val searchManager = requireActivity().getSystemService<SearchManager>()
        searchView.setSearchableInfo(searchManager?.getSearchableInfo(activity?.componentName))
        searchView.setOnQueryTextListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntegerArrayList("CHECKED_ITEMS", actionModeListener.getCheckedItems())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedInstanceState?.let { bundle ->
            val checkedItems = bundle.getIntegerArrayList("CHECKED_ITEMS")
            checkedItems?.filterNotNull()?.forEach { checkedItem ->
                actionModeListener.setItemChecked(checkedItem, true)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        setHasOptionsMenu(true)
        if (binding == null)
            return
        binding?.fragment = this
        tunnelManager.getTunnels().thenAccept { binding?.tunnels = it }
        binding?.rowConfigurationHandler =
                object : ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler<TunnelListItemBinding, Tunnel> {
                    override fun onConfigureRow(binding: TunnelListItemBinding, tunnel: Tunnel, position: Int) {
                        binding.fragment = this@TunnelListFragment
                        binding.root.setOnClickListener {
                            if (actionMode == null) {
                                selectedTunnel = tunnel
                            } else {
                                actionModeListener.toggleItemChecked(position)
                            }
                        }
                        binding.root.setOnLongClickListener {
                            actionModeListener.toggleItemChecked(position)
                            true
                        }

                        (binding.root as MultiselectableRelativeLayout).apply {
                            if (actionMode != null)
                                setMultiSelected(actionModeListener.checkedItems.contains(position))
                            else
                                setSingleSelected(selectedTunnel == tunnel)
                        }
                    }
                }
    }

    private inner class ActionModeListener : ActionMode.Callback {
        val checkedItems = HashSet<Int>()

        private var resources: Resources? = null

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_action_delete -> {
                    val tunnelsToDelete = ArrayList<Tunnel>()
                    val copyCheckedItems = HashSet(checkedItems)
                    tunnelManager.getTunnels().thenAccept { tunnels ->
                        for (position in copyCheckedItems)
                            tunnelsToDelete.add(tunnels[position])
                    }
                    val ctx = requireContext()
                    val tunnelCount = tunnelsToDelete.size
                    MaterialAlertDialogBuilder(ctx)
                            .setMessage(ctx.resources.getQuantityString(R.plurals.confirm_tunnel_deletion, tunnelCount, tunnelCount))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val futures = KotlinCompanions.streamForDeletion(tunnelsToDelete)
                                CompletableFuture.allOf(*futures)
                                        .thenApply { futures.size }
                                        .whenComplete { count, throwable ->
                                            onTunnelDeletionFinished(count, throwable)
                                        }
                                binding?.createFab?.extend()
                                mode.finish()
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                mode.finish()
                            }
                            .setOnCancelListener {
                                checkedItems.clear()
                                mode.finish()
                            }
                            .show()
                    return true
                }
                R.id.menu_action_select_all -> {
                    tunnelManager.getTunnels().thenAccept { tunnels ->
                        val allChecked = checkedItems.size == tunnels.size
                        if (allChecked) {
                            mode.finish()
                        } else {
                            tunnels.indices.forEach { setItemChecked(it, true) }
                        }
                    }
                    return true
                }
                else -> return false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            resources = requireActivity().resources
            mode.menuInflater.inflate(R.menu.tunnel_list_action_mode, menu)
            binding?.tunnelList?.adapter?.notifyDataSetChanged()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            resources = null
            checkedItems.clear()
            binding?.tunnelList?.adapter?.notifyDataSetChanged()
        }

        internal fun toggleItemChecked(position: Int) {
            setItemChecked(position, !checkedItems.contains(position))
        }

        internal fun getCheckedItems(): ArrayList<Int> {
            return ArrayList(checkedItems)
        }

        internal fun setItemChecked(position: Int, checked: Boolean) {
            if (checked) {
                checkedItems.add(position)
            } else {
                checkedItems.remove(position)
            }

            val adapter = binding?.tunnelList?.adapter

            if (actionMode == null && checkedItems.isNotEmpty()) {
                (activity as AppCompatActivity).startSupportActionMode(this)
            } else if (checkedItems.isEmpty()) {
                actionMode?.finish()
            }

            adapter?.notifyItemChanged(position)

            updateTitle(actionMode)
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            updateTitle(mode)
            return false
        }

        private fun updateTitle(mode: ActionMode?) {
            if (mode == null) {
                return
            }

            val count = checkedItems.size
            mode.title = if (count == 0) "" else resources?.getQuantityString(R.plurals.delete_title, count, count)
        }
    }

    companion object {
        const val REQUEST_IMPORT = 1
    }
}
