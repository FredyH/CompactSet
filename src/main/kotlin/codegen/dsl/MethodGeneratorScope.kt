package com.example.compactset.codegen.dsl

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.lang.IllegalArgumentException

/**
 * The scope of generating a method, contains all of its variables and statements
 * and offers ways of constructing statements/expressions.
 */
class MethodGeneratorScope internal constructor(methodSignature: MethodSignature) : StatementScope(methodSignature) {
    private var currentSlot = if (methodSignature.hasThis) 1 else 0
    private val registeredParameters = mutableListOf<LocalVariable>()
    private val registeredVariables = mutableListOf<LocalVariable>()


    /**
     * Declares a variable of the [type] in this method, reserving it a slot and returning it.
     */
    private fun addVariableSlot(type: JVMType): LocalVariable {
        val variable = LocalVariable(type, currentSlot)
        currentSlot += type.slotSize
        return variable
    }

    private fun createLocalVariable(type: JVMType): LocalVariable {
        if (registeredVariables.isNotEmpty()) {
            throw IllegalStateException("Cannot declare a parameter after a variable has been declared")
        }
        return addVariableSlot(type)
    }

    /**
     * Declares a variable of the [type] in this method, reserving it a slot and returning it.
     */
    override fun declareVariable(type: JVMType): LocalVariable {
        if (methodSignature.parameters.size != registeredParameters.size) {
            throw IllegalStateException("Attempted to declare variable before all parameters were registered.")
        }
        val variable = addVariableSlot(type)
        registeredVariables.add(variable)
        return variable
    }

    /**
     * Declares a parameter of the function of [type] and returns a variable that can be used to access the parameter.
     */
    fun declareParameter(type: JVMType): LocalVariable {
        if (registeredParameters.size >= methodSignature.parameters.size) {
            throw IllegalStateException("Attempted to declare parameter that does not exist in method signature")
        }
        if (methodSignature.parameters[registeredParameters.size] != type) {
            throw IllegalStateException("Attempted to declare parameter that does not match type in method signature")
        }

        val variable = createLocalVariable(type)
        registeredParameters.add(variable)
        return variable
    }

    /**
     * Declares this method in the [cw] and emits its code.
     */
    fun writeMethod(cw: ClassWriter) {
        if (registeredParameters.size != methodSignature.parameters.size) {
            throw IllegalStateException("Method signature does not have same amount of parameters as registered in method")
        }
        val mv =
            cw.visitMethod(methodSignature.flags, methodSignature.name, methodSignature.descriptor, null, emptyArray())
        mv.visitCode()

        emitCode(mv)

        //Automatically add return as last statement in a void function, if it does end in one already.
        if (methodSignature.returnType == VoidType && statements.lastOrNull() !is ReturnVoidStatement) {
            mv.visitInsn(Opcodes.RETURN)
        }

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }
}

/**
 * Generates code for the [methodSignature] in the [ClassWriter] using the DSL defined in [MethodGeneratorScope].
 */
@StatementScopeMarker
fun ClassWriter.generateMethod(
    methodSignature: MethodSignature,
    body: MethodGeneratorScope.() -> Unit
) {
    val scope = MethodGeneratorScope(methodSignature)
    scope.body()
    scope.writeMethod(this)
}