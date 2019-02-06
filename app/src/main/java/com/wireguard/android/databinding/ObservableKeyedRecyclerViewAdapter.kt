/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.databinding

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.wireguard.android.BR
import com.wireguard.android.util.ObservableKeyedList
import com.wireguard.util.Keyed

import java.lang.ref.WeakReference

/**
 * A generic `RecyclerView.Adapter` backed by a `ObservableKeyedList`.
 */

class ObservableKeyedRecyclerViewAdapter<K, E : Keyed<out K>> internal constructor(
    context: Context,
    private val layoutId: Int,
    list: ObservableKeyedList<K, E>
) : Adapter<ObservableKeyedRecyclerViewAdapter.ViewHolder>() {

    private val callback = OnListChangedCallback(this)
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var list: ObservableKeyedList<K, E>? = null
    private var rowConfigurationHandler: RowConfigurationHandler<ViewDataBinding, E>? = null

    init {
        setList(list)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    private fun getItem(position: Int): E? {
        return list?.let {
            if (position < 0 || position >= it.size) null else it[position]
        }
    }

    override fun getItemId(position: Int): Long {
        val key = getKey(position)
        return (key?.hashCode() ?: -1).toLong()
    }

    private fun getKey(position: Int): K? {
        val item = getItem(position)
        return item?.key
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(layoutInflater, layoutId, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.setVariable(BR.collection, list)
        holder.binding.setVariable(BR.key, getKey(position))
        holder.binding.setVariable(BR.item, getItem(position))
        holder.binding.executePendingBindings()

        getItem(position)?.let {
            rowConfigurationHandler?.onConfigureRow(holder.binding, it, position)
        }
    }

    internal fun setList(newList: ObservableKeyedList<K, E>?) {
        list?.removeOnListChangedCallback(callback)
        list = newList
        list?.addOnListChangedCallback(callback)
        notifyDataSetChanged()
    }

    internal fun setRowConfigurationHandler(rowConfigurationHandler: RowConfigurationHandler<ViewDataBinding, E>?) {
        this.rowConfigurationHandler = rowConfigurationHandler
    }

    interface RowConfigurationHandler<B : ViewDataBinding, T> {
        fun onConfigureRow(binding: B, tunnel: T, position: Int)
    }

    private class OnListChangedCallback<E : Keyed<*>> constructor(adapter: ObservableKeyedRecyclerViewAdapter<*, E>) :
        ObservableList.OnListChangedCallback<ObservableList<E>>() {

        private val weakAdapter: WeakReference<ObservableKeyedRecyclerViewAdapter<*, E>> = WeakReference(adapter)

        override fun onChanged(sender: ObservableList<E>) {
            val adapter = weakAdapter.get()
            if (adapter != null)
                adapter.notifyDataSetChanged()
            else
                sender.removeOnListChangedCallback(this)
        }

        override fun onItemRangeChanged(
            sender: ObservableList<E>,
            positionStart: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }

        override fun onItemRangeInserted(
            sender: ObservableList<E>,
            positionStart: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }

        override fun onItemRangeMoved(
            sender: ObservableList<E>,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }

        override fun onItemRangeRemoved(
            sender: ObservableList<E>,
            positionStart: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }
    }

    class ViewHolder(internal val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)
}
