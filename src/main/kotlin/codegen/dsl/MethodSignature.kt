package com.example.compactset.codegen.dsl

import org.objectweb.asm.Opcodes

data class MethodSignature(
    val owner: String,
    val name: String,
    val parameters: List<JVMType>,
    val returnJVMType: JVMType,
    val flags: Int,
    val isInterface: Boolean = false
) {
    val hasThis: Boolean = flags and Opcodes.ACC_STATIC == 0

    val descriptor: String
        get() {
            val paramStr = parameters.joinToString("") { it.typeName }
            val retStr = returnJVMType.typeName
            return "($paramStr)$retStr"
        }
}