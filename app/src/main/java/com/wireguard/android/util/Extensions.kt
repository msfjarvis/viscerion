package com.wireguard.android.util

import com.wireguard.config.Attribute

fun <T> ArrayList<T>.addExclusive(otherArray: ArrayList<T>): ArrayList<T> {
    for(item: T in otherArray)
        if (item !in this)
            this.add(item)
    return this
}

fun <T> ArrayList<T>.addExclusive(otherArray: Array<T>): ArrayList<T> {
    for(item: T in otherArray)
        if (item !in this)
            this.add(item)
    return this
}

fun String?.addExclusive(otherArray: ArrayList<String>): String {
    val stringCopy = Attribute.stringToList(this).toCollection(ArrayList())
    for (item: String in otherArray)
        if (item !in stringCopy)
            stringCopy.add(item)
    return Attribute.iterableToString(stringCopy)
}