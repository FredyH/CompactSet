package com.example.compactset.codegen.dsl

import org.objectweb.asm.MethodVisitor

class LocalVariable internal constructor(
    override val type: JVMType,
    internal val slot: Int
) : Expression() {
    override fun emitCode(mv: MethodVisitor) {
        type.emitLoadInstruction(mv, slot)
    }

}