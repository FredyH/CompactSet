package com.example.compactset.codegen.dsl

import org.objectweb.asm.MethodVisitor

/**
 * A local variable of a method of [type] and in the [slot].
 * Can be used as an expression to load the value of this variable.
 */
class LocalVariable internal constructor(
    override val type: JVMType,
    internal val slot: Int
) : Expression() {
    override fun emitCode(mv: MethodVisitor) {
        type.emitLoadInstruction(mv, slot)
    }

}