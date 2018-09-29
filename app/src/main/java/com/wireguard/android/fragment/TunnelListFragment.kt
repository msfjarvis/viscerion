package com.wireguard.android.fragment

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.activity.TunnelCreatorActivity
import com.wireguard.android.databinding.ObservableKeyedRecyclerViewAdapter
import com.wireguard.android.databinding.TunnelListFragmentBinding
import com.wireguard.android.databinding.TunnelListItemBinding
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.KotlinCompanions
import com.wireguard.android.widget.MultiselectableRelativeLayout
import com.wireguard.android.widget.fab.FloatingActionButtonRecyclerViewScrollListener
import com.wireguard.config.Attribute
import com.wireguard.config.Config
import java9.util.concurrent.CompletableFuture
import kotlinx.android.synthetic.main.add_tunnels_bottom_sheet.*
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TunnelListFragment : BaseFragment() {

    private val actionModeListener = ActionModeListener()
    private var actionMode: ActionMode? = null
    private var binding: TunnelListFragmentBinding? = null

    private fun importTunnel(configText: String) {
        try {
            // Ensure the config text is parseable before proceeding…
            Config.from(configText)

            // Config text is valid, now create the tunnel…
            val fragmentManager = fragmentManager
            if (fragmentManager != null)
                ConfigNamingDialogFragment.newInstance(configText).show(fragmentManager, null)
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
        Application.asyncWorker.supplyAsync {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
            var name: String? = null
            contentResolver.query(uri, columns, null, null, null)!!.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0))
                    name = cursor.getString(0)
            }
            if (name == null)
                name = Uri.decode(uri.lastPathSegment)
            var idx = name!!.lastIndexOf('/')
            if (idx >= 0) {
                if (idx >= name!!.length - 1)
                    throw IllegalArgumentException("Illegal file name: " + name!!)
                name = name!!.substring(idx + 1)
            }
            val isZip = name!!.toLowerCase().endsWith(".zip")
            if (name!!.toLowerCase().endsWith(".conf"))
                name = name!!.substring(0, name!!.length - ".conf".length)
            else if (!isZip)
                throw IllegalArgumentException("File must be .conf or .zip")

            if (isZip) {
                ZipInputStream(contentResolver.openInputStream(uri)).use { zip ->
                    val reader = BufferedReader(InputStreamReader(zip, StandardCharsets.UTF_8))
                    var entry: ZipEntry
                    while (true) {
                        entry = zip.nextEntry
                        if (entry == null) continue
                        name = entry.name
                        idx = name!!.lastIndexOf('/')
                        if (idx >= 0) {
                            if (idx >= name!!.length - 1)
                                continue
                            name = name!!.substring(name!!.lastIndexOf('/') + 1)
                        }
                        if (name!!.toLowerCase().endsWith(".conf"))
                            name = name!!.substring(0, name!!.length - ".conf".length)
                        else
                            continue
                        var config: Config? = null
                        try {
                            config = Config.from(reader)
                        } catch (e: Exception) {
                            throwables.add(e)
                        }

                        if (config != null)
                            futureTunnels.add(Application.tunnelManager.create(name!!, config).toCompletableFuture())
                    }
                }
            } else {
                futureTunnels.add(
                    Application.tunnelManager.create(
                        name!!,
                        Config.from(contentResolver.openInputStream(uri))
                    ).toCompletableFuture()
                )
            }

            if (futureTunnels.isEmpty()) {
                if (throwables.size == 1)
                    throw throwables[0]
                else if (throwables.isEmpty())
                    throw IllegalArgumentException("No configurations found")
            }

            CompletableFuture.allOf(*futureTunnels.toTypedArray())
        }.whenComplete { future, exception ->
            if (exception != null) {
                onTunnelImportFinished(emptyList(), listOf(exception))
            } else {
                future.whenComplete { _, _ ->
                    val tunnels = ArrayList<Tunnel>(futureTunnels.size)
                    for (futureTunnel in futureTunnels) {
                        var tunnel: Tunnel? = null
                        try {
                            tunnel = futureTunnel.getNow(null)
                        } catch (e: Exception) {
                            throwables.add(e)
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
                if (resultCode == Activity.RESULT_OK && data != null)
                    importTunnel(data.data)
                return
            }
            IntentIntegrator.REQUEST_CODE -> {
                val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                if (result != null && result.contents != null) {
                    importTunnel(result.contents)
                }
                return
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.tag(TAG)

        val dialog = BottomSheetDialog(context!!, R.style.BottomSheetDialogTheme)
        dialog.setContentView(R.layout.add_tunnels_bottom_sheet)
        dialog.findViewById<MaterialButton>(R.id.create_empty)?.setOnClickListener {
            dialog.dismiss()
            onRequestCreateConfig()
        }
        dialog.findViewById<MaterialButton>(R.id.create_from_file)?.setOnClickListener {
            dialog.dismiss()
            onRequestImportConfig()
        }
        dialog.findViewById<MaterialButton>(R.id.scan_qr_code)?.setOnClickListener {
            dialog.dismiss()
            onRequestScanQRCode()
        }

        binding = TunnelListFragmentBinding.inflate(inflater, container, false)
        binding!!.createFab.setOnClickListener { dialog.show() }
        binding!!.tunnelList.setOnScrollListener(FloatingActionButtonRecyclerViewScrollListener(binding!!.createFab))
        binding!!.executePendingBindings()
        return binding!!.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun onRequestCreateConfig() {
        startActivity(Intent(activity, TunnelCreatorActivity::class.java))
    }

    private fun onRequestImportConfig() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    private fun onRequestScanQRCode() {
        val intentIntegrator = IntentIntegrator.forSupportFragment(this)
        intentIntegrator.setOrientationLocked(false)
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setPrompt(getString(R.string.qrcode_hint))
        intentIntegrator.initiateScan(listOf(IntentIntegrator.QR_CODE))
    }

    private fun viewForTunnel(tunnel: Tunnel?, tunnels: List<*>): MultiselectableRelativeLayout? {
        return if (binding != null && binding!!.tunnelList.findViewHolderForAdapterPosition(tunnels.indexOf(tunnel)) != null)
            binding!!.tunnelList.findViewHolderForAdapterPosition(tunnels.indexOf(tunnel))!!.itemView as MultiselectableRelativeLayout
        else
            null
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        if (binding == null)
            return
        Application.tunnelManager.getTunnels().thenAccept { tunnels ->
            if (newTunnel != null)
                viewForTunnel(newTunnel, tunnels)!!.setSingleSelected(true)
            if (oldTunnel != null)
                viewForTunnel(oldTunnel, tunnels)!!.setSingleSelected(false)
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
        var message: String? = null

        for (throwable in throwables) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.import_error, error)
            Timber.e(throwable)
        }

        if (tunnels.size == 1 && throwables.isEmpty())
            message = getString(R.string.import_success, tunnels[0].getName())
        else if (tunnels.isEmpty() && throwables.size == 1)
        else if (throwables.isEmpty())
            message = resources.getQuantityString(
                R.plurals.import_total_success,
                tunnels.size, tunnels.size
            )
        else if (!throwables.isEmpty())
            message = resources.getQuantityString(
                R.plurals.import_partial_success,
                tunnels.size + throwables.size,
                tunnels.size, tunnels.size + throwables.size
            )/* Use the exception message from above. */

        Application.tunnelManager.completableTunnels.thenAccept { allTunnels ->
            for (tunnel in allTunnels) {
                val oldConfig = tunnel.getConfig()
                if (oldConfig != null) {
                    oldConfig.getInterface()
                        .addExcludedApplications(Attribute.stringToList(ApplicationPreferences.exclusions))
                    tunnel.setConfig(oldConfig)
                    if (tunnel.getState() === Tunnel.State.UP)
                        tunnel.setState(Tunnel.State.DOWN).whenComplete { _, _ -> tunnel.setState(Tunnel.State.UP) }
                }
            }
        }

        if (binding != null)
            Snackbar.make(binding!!.mainContainer, message!!, Snackbar.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntegerArrayList("CHECKED_ITEMS", actionModeListener.getCheckedItems())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            val checkedItems = savedInstanceState.getIntegerArrayList("CHECKED_ITEMS")
            if (checkedItems != null) {
                for (i in checkedItems)
                    actionModeListener.setItemChecked(i!!, true)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (binding == null) {
            return
        }
        binding!!.fragment = this
        Application.tunnelManager.getTunnels().thenAccept { binding!!.tunnels = it }
        binding!!.rowConfigurationHandler = ObservableKeyedRecyclerViewAdapter.RowConfigurationHandler { binding: TunnelListItemBinding, tunnel: Tunnel, position ->
            binding.fragment = this
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

            if (actionMode != null)
                (binding.root as MultiselectableRelativeLayout).setMultiSelected(
                    actionModeListener.checkedItems.contains(position)
                )
            else
                (binding.root as MultiselectableRelativeLayout).setSingleSelected(selectedTunnel == tunnel)
        }
    }

    private inner class ActionModeListener : ActionMode.Callback {
        val checkedItems = HashSet<Int>()

        private var resources: Resources? = null

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_action_delete -> {
                    val copyCheckedItems = HashSet(checkedItems)
                    Application.tunnelManager.getTunnels().thenAccept { tunnels ->
                        val tunnelsToDelete = ArrayList<Tunnel>()
                        for (position in copyCheckedItems)
                            tunnelsToDelete.add(tunnels[position!!])

                        val futures = KotlinCompanions.streamForDeletion(tunnels)
                        CompletableFuture.allOf(*futures)
                            .thenApply { futures.size }
                            .whenComplete { count, throwable ->
                                onTunnelDeletionFinished(count, throwable)
                            }
                    }
                    if (binding != null)
                        if (binding!!.createFab.isOrWillBeHidden)
                            binding!!.createFab.show()
                    checkedItems.clear()
                    mode.finish()
                    return true
                }
                R.id.menu_action_select_all -> {
                    Application.tunnelManager.getTunnels().thenAccept { tunnels ->
                        for (i in tunnels.indices) {
                            setItemChecked(i, true)
                        }
                    }
                    return true
                }
                else -> return false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            if (activity != null) {
                resources = activity!!.resources
            }
            mode.menuInflater.inflate(R.menu.tunnel_list_action_mode, menu)
            if (binding != null)
                binding!!.tunnelList.adapter!!.notifyDataSetChanged()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            resources = null
            checkedItems.clear()
            if (binding != null)
                binding!!.tunnelList.adapter!!.notifyDataSetChanged()
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

            val adapter = if (binding == null) null else binding!!.tunnelList.adapter

            if (actionMode == null && !checkedItems.isEmpty() && activity != null) {
                (activity as AppCompatActivity).startSupportActionMode(this)
            } else if (actionMode != null && checkedItems.isEmpty()) {
                actionMode!!.finish()
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
            if (count == 0) {
                mode.title = ""
            } else {
                mode.title = resources!!.getQuantityString(R.plurals.delete_title, count, count)
            }
        }
    }

    companion object {
        private const val REQUEST_IMPORT = 1
        private val TAG = "WireGuard/" + TunnelListFragment::class.java.simpleName
    }
}