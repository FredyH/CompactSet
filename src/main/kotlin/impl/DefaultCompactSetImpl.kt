package com.example.compactset.impl

import com.example.compactset.CompactSet

/**
 * The default implementation of [CompactSet] for non-primitive types.
 */
internal class DefaultCompactSetImpl<T>(expectedSize: Int) : CompactSet<T> {
    private var backingArray = Array<Any?>(expectedSize) { null }

    override var size: Int = 0
        private set

    override fun contains(value: T): Boolean {
        //It is important to limit ourselves to size here, since only size elements
        //are actually in the set, while the rest of the value are "uninitialized" and null.
        for (i in 0 until size) {
            if (backingArray[i] == value) {
                return true
            }
        }
        return false
    }

    override fun add(element: T): Boolean {
        if (contains(element)) return false

        if (backingArray.size <= size) {
            val newArray = Array<Any?>(backingArray.size * 2) { null }
            System.arraycopy(backingArray, 0, newArray, 0, size)
            backingArray = newArray
        }
        backingArray[size++] = element

        return true
    }
}
