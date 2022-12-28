package com.example.compactset.codegen.dsl

import org.objectweb.asm.MethodVisitor

abstract class StatementScope(protected val methodSignature: MethodSignature) {

    internal val statements = mutableListOf<Statement>()

    protected abstract val topScope: MethodGeneratorScope

    fun emitCode(mv: MethodVisitor) {
        statements.forEach { it.emitCode(mv) }
    }
}