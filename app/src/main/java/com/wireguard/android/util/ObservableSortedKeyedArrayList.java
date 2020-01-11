/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util;

import androidx.annotation.NonNull;
import com.wireguard.util.Keyed;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;

/**
 * KeyedArrayList that enforces uniqueness and sorted order across the set of keys. This class uses
 * binary search to improve lookup and replacement times to O(log(n)). However, due to the
 * array-based nature of this class, insertion and removal of elements with anything but the largest
 * key still require O(n) time.
 */
public class ObservableSortedKeyedArrayList<K, E extends Keyed<? extends K>>
        extends ObservableKeyedArrayList<K, E> implements ObservableSortedKeyedList<K, E> {
    @NonNull private final Comparator<? super K> comparator;
    private final transient KeyList<K, E> keyList = new KeyList<>(this);

    public ObservableSortedKeyedArrayList(@NonNull final Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    @Override
    public boolean add(final E element) {
        final int insertionPoint = getInsertionPoint(element);
        if (insertionPoint < 0) {
            // Skipping insertion is non-destructive if the new and existing objects are the same.
            if (element == get(-insertionPoint - 1)) return false;
            throw new IllegalArgumentException("Element with same key already exists in list");
        }
        super.add(insertionPoint, element);
        return true;
    }

    @Override
    public void add(final int index, final E element) {
        final int insertionPoint = getInsertionPoint(element);
        if (insertionPoint < 0)
            throw new IllegalArgumentException("Element with same key already exists in list");
        if (insertionPoint != index)
            throw new IndexOutOfBoundsException("Wrong index given for element");
        super.add(index, element);
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends E> elements) {
        boolean didChange = false;
        for (final E e : elements) if (add(e)) didChange = true;
        return didChange;
    }

    @Override
    public boolean addAll(int index, @NonNull final Collection<? extends E> elements) {
        for (final E e : elements) add(index++, e);
        return true;
    }

    @NonNull
    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    @Override
    public K firstKey() {
        if (isEmpty())
            // The parameter in the exception is only to shut
            // lint up, we never care for the exception message.
            throw new NoSuchElementException("Empty set");
        return get(0).getKey();
    }

    private int getInsertionPoint(final E e) {
        return -Collections.binarySearch(keyList, e.getKey(), comparator) - 1;
    }

    @Override
    public int indexOfKey(final K key) {
        final int index;
        index = Collections.binarySearch(keyList, key, comparator);
        return index >= 0 ? index : -1;
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return keyList;
    }

    @Override
    public int lastIndexOfKey(final K key) {
        // There can never be more than one element with the same key in the list.
        return indexOfKey(key);
    }

    @Override
    public K lastKey() {
        if (isEmpty())
            // The parameter in the exception is only to shut
            // lint up, we never care for the exception message.
            throw new NoSuchElementException("Empty set");
        return get(size() - 1).getKey();
    }

    @Override
    public E set(final int index, final E e) {
        final int order;
        order = comparator.compare(e.getKey(), get(index).getKey());
        if (order != 0) {
            // Allow replacement if the new key would be inserted adjacent to the replaced element.
            final int insertionPoint = getInsertionPoint(e);
            if (insertionPoint < index || insertionPoint > index + 1)
                throw new IndexOutOfBoundsException("Wrong index given for element");
        }
        return super.set(index, e);
    }

    @NonNull
    @Override
    public Collection<E> values() {
        return this;
    }

    private static final class KeyList<K, E extends Keyed<? extends K>> extends AbstractList<K>
            implements Set<K> {
        private final ObservableSortedKeyedArrayList<K, E> list;

        private KeyList(final ObservableSortedKeyedArrayList<K, E> list) {
            this.list = list;
        }

        @Override
        public K get(final int index) {
            return list.get(index).getKey();
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        @SuppressWarnings("EmptyMethod")
        public Spliterator<K> spliterator() {
            return super.spliterator();
        }
    }
}
