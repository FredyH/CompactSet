package com.example.compactset.codegen.dsl

import org.objectweb.asm.Opcodes

/**
 * A class defining the signature of a method of a method in the [owner] class with the [name].
 * The method has to be called with the [parameters] and returns a value of [returnType].
 * The method has the [flags] as access rights, which might make it a private or static function, depending
 * on which it might have an implicit `this` parameter.
 */
data class MethodSignature(
    val owner: String,
    val name: String,
    val parameters: List<JVMType>,
    val returnType: JVMType,
    val flags: Int,
    val isInterface: Boolean = false
) {
    /**
     * Returns true if and only if this method has an implicit this parameter.
     */
    val hasThis: Boolean = flags and Opcodes.ACC_STATIC == 0

    /**
     * Generates the JVM-descriptor of this method.
     */
    val descriptor: String
        get() {
            val paramStr = parameters.joinToString("") { it.typeName }
            val retStr = returnType.typeName
            return "($paramStr)$retStr"
        }
}