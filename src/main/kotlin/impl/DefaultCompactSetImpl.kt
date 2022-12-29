package com.example.compactset.impl

import com.example.compactset.CompactSet

/**
 * The default implementation of [CompactSet] for non-primitive types.
 *
 * Uses open addressing hashing with linear probing.
 */
internal class DefaultCompactSetImpl<T>(initialSize: Int) : CompactSet<T> {

    /**
     * We want to use the null value as a special placeholder that indicates an empty slot, so we cannot
     * allow for it to be added to the array, as it would be indistinguishable from the empty slot.
     * Instead of adding null to the array, we use this boolean to indicate whether or not null is in this set.
     */
    private var containsNull: Boolean = false


    private var backingArray = Array<Any?>(initialSize.coerceAtLeast(4)) { null }

    override var size: Int = 0
        private set

    private val loadFactor: Float
        get() {
            val sizeWithoutNull = if (containsNull) size - 1 else size
            return sizeWithoutNull / backingArray.size.toFloat()
        }

    private fun hashIndex(size: Int, element: Any): Int {
        return Math.floorMod(element.hashCode(), size)
    }

    private fun getElementIndex(array: Array<Any?>, element: Any): Int {
        var index = hashIndex(array.size, element)

        var currentValue = array[index]
        while (currentValue != null) {
            if (currentValue == element) return index
            index = (index + 1) % array.size
            currentValue = array[index]
        }
        return index
    }

    private fun insertElement(array: Array<Any?>, element: Any): Boolean {
        val index = getElementIndex(array, element)
        if (array[index] != null) return false

        array[index] = element
        return true
    }

    private fun rehash() {
        val newSize = backingArray.size * 2
        val newArray = Array<Any?>(newSize) { null }

        for (a in backingArray) {
            if (a == null) continue
            insertElement(newArray, a)
        }

        backingArray = newArray
    }

    override fun contains(value: T): Boolean {
        if (value == null) return containsNull

        val index = getElementIndex(backingArray, value)
        return backingArray[index] != null
    }

    override fun add(element: T): Boolean {
        if (element == null) {
            if (containsNull) return false
            containsNull = true
            size++
            return true
        }

        if (loadFactor > 0.6) {
            rehash()
        }

        if (!insertElement(backingArray, element)) return false
        size++

        return true
    }
}
