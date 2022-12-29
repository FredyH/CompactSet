package com.example.compactset.codegen.dsl

import org.objectweb.asm.ClassVisitor

/**
 * A field in the class [owner] with [name] of [type] and with the given [access] flags.
 */
class ClassField constructor(
    internal val owner: String,
    internal val name: String,
    internal val type: JVMType,
    internal val access: Int
) {

    fun addToClass(cv: ClassVisitor) {
        cv.visitField(access, name, type.typeName, null, false)
    }
}