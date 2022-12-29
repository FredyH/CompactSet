package com.example.compactset.impl

import com.example.compactset.CompactSet
import com.example.compactset.codegen.*
import com.example.compactset.codegen.CompactSetClassLoader
import com.example.compactset.codegen.dsl.*
import org.objectweb.asm.Opcodes
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal val generatorMap = mapOf<KClass<*>, SpecializedCompactSetGenerator>(
    Int::class to SpecializedCompactSetGenerator(
        className = "IntCompactSetImpl",
        primitiveType = IntType,
        primitiveArrayType = IntArrayType,
        primitiveHashCodeFunction = MethodSignature(
            owner = "java/lang/Integer",
            name = "hashCode",
            parameters = listOf(IntType),
            returnType = IntType,
            flags = Opcodes.ACC_STATIC
        ),
        boxedType = ObjectType("java/lang/Integer"),
        unboxMethod = MethodSignature(
            owner = "java/lang/Integer",
            name = "intValue",
            parameters = listOf(),
            returnType = IntType,
            flags = Opcodes.ACC_PUBLIC
        ),
        zeroConstant = 0
    ),
    Long::class to SpecializedCompactSetGenerator(
        className = "LongCompactSetImpl",
        primitiveType = LongType,
        primitiveArrayType = LongArrayType,
        primitiveHashCodeFunction = MethodSignature(
            owner = "java/lang/Long",
            name = "hashCode",
            parameters = listOf(LongType),
            returnType = IntType,
            flags = Opcodes.ACC_STATIC
        ),
        boxedType = ObjectType("java/lang/Long"),
        unboxMethod = MethodSignature(
            owner = "java/lang/Long",
            name = "longValue",
            parameters = listOf(),
            returnType = LongType,
            flags = Opcodes.ACC_PUBLIC
        ),
        zeroConstant = 0L
    ),
    Double::class to SpecializedCompactSetGenerator(
        className = "DoubleCompactSetImpl",
        primitiveType = DoubleType,
        primitiveArrayType = DoubleArrayType,
        primitiveHashCodeFunction = MethodSignature(
            owner = "java/lang/Double",
            name = "hashCode",
            parameters = listOf(DoubleType),
            returnType = IntType,
            flags = Opcodes.ACC_STATIC
        ),
        boxedType = ObjectType("java/lang/Double"),
        unboxMethod = MethodSignature(
            owner = "java/lang/Double",
            name = "doubleValue",
            parameters = listOf(),
            returnType = DoubleType,
            flags = Opcodes.ACC_PUBLIC
        ),
        zeroConstant = 0.0
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