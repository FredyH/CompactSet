package com.example.compactset

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * An interface modelling a data structure that elements can be added to and that can be queried if
 * elements are already contained in it.
 *
 * Note: Implementations of this interface are not expected to be thread-safe
 */
interface CompactSet<T> {
    /**
     * The current size of the set
     */
    val size: Int

    /**
     * Returns true iff the [value] is contained within this set
     */
    operator fun contains(value: T): Boolean

    /**
     * Adds an [element] to this set.
     * If the [element] is already in this set, false is returned, and it is not added again.
     * Otherwise, the [element] is added and true is returned.
     */
    fun add(element: T): Boolean
}

@PublishedApi
internal val generatorMap = mutableMapOf<KClass<*>, SpecializedCompactSetGenerator>(
    Int::class to SpecializedCompactSetGenerator(
        className = "IntCompactSetImpl",
        primitiveSettings = IntInstructionSettings
    ),
    Long::class to SpecializedCompactSetGenerator(
        className = "LongCompactSetImpl",
        primitiveSettings = LongInstructionSettings
    ),
    Double::class to SpecializedCompactSetGenerator(
        className = "DoubleCompactSetImpl",
        primitiveSettings = DoubleInstructionSettings
    )
)

@PublishedApi
internal val generatedClassMap = ConcurrentHashMap<SpecializedCompactSetGenerator, Class<*>>()

@PublishedApi
internal val compactSetClassLoader = CompactSetClassLoader()

inline fun <reified T> newCompactSet(expectedSize: Int = 16): CompactSet<T> {
    val generator = generatorMap[T::class]
    return if (generator != null) {
        val loadedClass = generatedClassMap.computeIfAbsent(generator) {
            val generatedClass = generator.generateClass()
            compactSetClassLoader.defineClass("com.example.compactset.${generator.className}", generatedClass)
        }
        val constructor = loadedClass.getConstructor(Int::class.java)
        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(expectedSize) as CompactSet<T>
    } else {
        DefaultCompactSetImpl(expectedSize)
    }
}