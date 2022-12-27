package com.example.compactset.impl

import com.example.compactset.CompactSet
import com.example.compactset.codegen.*
import com.example.compactset.codegen.CompactSetClassLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType

private val generatorMap = mapOf<KClass<*>, SpecializedCompactSetGenerator>(
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

private val generatedClassMap = ConcurrentHashMap<SpecializedCompactSetGenerator, Class<*>>()

@PublishedApi
internal fun createCompactSet(initialSize: Int, type: KType): CompactSet<Any?> {
    val generator = generatorMap[type.classifier]
    //Important to check for nullable here. Primitives cannot be null, so they have to be stored
    //in boxed form to achieve nullability.
    return if (!type.isMarkedNullable && generator != null) {
        val loadedClass = generatedClassMap.computeIfAbsent(generator) {
            val generatedClass = generator.generateClass()
            CompactSetClassLoader.defineClass("com.example.compactset.${generator.className}", generatedClass)
        }
        val constructor = loadedClass.getConstructor(Int::class.java)
        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(initialSize) as CompactSet<Any?>
    } else {
        DefaultCompactSetImpl(initialSize)
    }
}