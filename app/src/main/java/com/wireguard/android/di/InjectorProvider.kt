/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.app.Service
import android.content.ContentProvider
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference

interface InjectorProvider {
    val component: AppComponent
}

val ContentProvider.injector get() = (context?.applicationContext as InjectorProvider).component
val FragmentActivity.injector get() = (application as InjectorProvider).component
val Fragment.injector get() = (requireContext().applicationContext as InjectorProvider).component
val Preference.injector get() = (context.applicationContext as InjectorProvider).component
val Service.injector get() = (applicationContext as InjectorProvider).component
fun getInjector(context: Context) = (context.applicationContext as InjectorProvider).component
