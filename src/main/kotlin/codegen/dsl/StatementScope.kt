package com.example.compactset.codegen.dsl

import org.objectweb.asm.MethodVisitor


/**
 * A scope of statements within a method.
 */
abstract class StatementScope(protected val methodSignature: MethodSignature) {

    /**
     * The statements contained in this scope.
     */
    internal val statements = mutableListOf<Statement>()

    /**
     * The scope of the method.
     */
    protected abstract val topScope: MethodGeneratorScope

    /**
     * Emits the code of all statements of this scope.
     */
    fun emitCode(mv: MethodVisitor) {
        statements.forEach { it.emitCode(mv) }
    }
}