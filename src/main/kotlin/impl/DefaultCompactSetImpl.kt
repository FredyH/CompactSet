package com.example.compactset.impl

import com.example.compactset.CompactSet

/**
 * The default implementation of [CompactSet] for non-primitive types.
 */
internal class DefaultCompactSetImpl<T>(initialSize: Int) : CompactSet<T> {
    private var containsNull: Boolean = false
    private var backingArray = Array<Any?>(initialSize.coerceAtLeast(4)) { null }

    override var size: Int = 0
        private set

    private val loadFactor: Float
        get() {
            val sizeWithoutNull = if (containsNull) size - 1 else size
            return sizeWithoutNull / backingArray.size.toFloat()
        }

    private fun hashIndex(element: Any, size: Int): Int {
        return Math.floorMod(element.hashCode(), size)
    }

    private fun getElementIndex(element: Any, array: Array<Any?>): Int {
        var index = hashIndex(element, array.size)

        var currentValue = array[index]
        while (currentValue != null) {
            if (currentValue == element) return index
            index = (index + 1) % array.size
            currentValue = array[index]
        }
        return index
    }

    private fun insertElement(element: Any, array: Array<Any?>): Boolean {
        val index = getElementIndex(element, array)
        if (array[index] != null) return false

        array[index] = element
        return true
    }

    private fun rehash() {
        val newSize = backingArray.size * 2
        val newArray = Array<Any?>(newSize) { null }

        for (a in backingArray) {
            if (a == null) continue
            insertElement(a, newArray)
        }

        backingArray = newArray
    }

    override fun contains(value: T): Boolean {
        if (value == null) return containsNull

        val index = getElementIndex(value, backingArray)
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

        if (!insertElement(element, backingArray)) return false
        size++

        return true
    }
}
