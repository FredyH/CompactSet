package com.example.compactset

import com.example.compactset.impl.createCompactSet
import kotlin.reflect.typeOf

/**
 * An interface modelling a data structure that elements can be added to and that can be queried if
 * elements are already contained in it.
 *
 * Note: Implementations of this interface are not expected to be thread-safe.
 */
interface CompactSet<T> {
    /**
     * The current size of the set
     */
    val size: Int

    /**
     * Returns true if and only if the [value] is contained within this set.
     */
    operator fun contains(value: T): Boolean

    /**
     * Adds an [element] to this set.
     * If the [element] is already in this set, it is not added again and false is returned.
     * Otherwise, the [element] is added and true is returned.
     */
    fun add(element: T): Boolean
}

/**
 * Creates a new [CompactSet] depending on the type [T].
 * For any object types, nullable primitives or primitives not mentioned below,
 * a default set implementation will be used that stores elements as objects in a backing array.
 *
 * For the primitives Int, Long and Double, a specialized version of the class will be generated that stores
 * elements in a primitive backing array instead. This is more efficient, since storing primitives in boxed form
 * incurs significant overhead in both performance and memory usage.
 */
inline fun <reified T> newCompactSet(initialSize: Int = 16): CompactSet<T> {
    @Suppress("UNCHECKED_CAST")
    return createCompactSet(initialSize, typeOf<T>()) as CompactSet<T>
}