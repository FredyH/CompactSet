package com.example.compactset.codegen.dsl

import org.objectweb.asm.ClassVisitor

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