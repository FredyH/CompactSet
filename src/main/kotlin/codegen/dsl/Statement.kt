package com.example.compactset.codegen.dsl

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * A class representing a statement in the code.
 * A statement should be self-contained w.r.t. the JVM-stack. That is, the JVM-stack
 * should look the same before and after this statement is done, to avoid stack overflow/underflow.
 */
abstract class Statement {

    /**
     * Emits the code of this statement.
     */
    abstract fun emitCode(mv: MethodVisitor)
}

class ReturnVoidStatement : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.RETURN)
    }
}

class ReturnValueStatement(private val valueExpression: Expression) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        valueExpression.emitCode(mv)
        valueExpression.type.emitReturnInstruction(mv)
    }
}

class FieldAssignment(
    private val variable: ClassField,
    private val objectInstance: Expression,
    private val expression: Expression
) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        objectInstance.emitCode(mv)
        expression.emitCode(mv)
        mv.visitFieldInsn(Opcodes.PUTFIELD, variable.owner, variable.name, variable.type.typeName)
    }
}

class VariableAssignment(
    private val variable: LocalVariable,
    private val expression: Expression
) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        expression.emitCode(mv)
        variable.type.emitStoreInstruction(mv, variable.slot)
    }
}

class StoreInArrayStatement(
    private val arrayExpression: Expression,
    private val indexExpression: Expression,
    private val valueExpression: Expression,
) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        arrayExpression.emitCode(mv)
        indexExpression.emitCode(mv)
        valueExpression.emitCode(mv)
        (arrayExpression.type as JVMArrayType).emitStoreIntoArrayInstruction(mv)
    }
}

class FunctionCallStatement(private val functionCallExpression: FunctionCallExpression) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        functionCallExpression.emitCode(mv)
        val returnType = functionCallExpression.methodSignature.returnType

        if (returnType.slotSize == 2) {
            mv.visitInsn(Opcodes.POP2)
        } else if (returnType.slotSize == 1) {
            mv.visitInsn(Opcodes.POP)
        }
    }
}

class IfScope(parentScope: StatementScope) : SubScope(parentScope)

class IfStatement(
    parentScope: StatementScope,
    private val condition: Expression,
    ifBody: IfScope.() -> Unit
) : Statement() {

    private val ifScope: IfScope = IfScope(parentScope)
    private val elseScope: IfScope = IfScope(parentScope)

    internal fun setElseBody(elseBody: IfScope.() -> Unit) {
        elseScope.elseBody()
    }

    init {
        if (condition.type != BooleanType) {
            throw IllegalStateException("Condition of if statement needs to be of type boolean")
        }
        ifScope.ifBody()
    }

    override fun emitCode(mv: MethodVisitor) {
        val elseBodyLabel = Label()
        val endLabel = Label()

        condition.emitCode(mv)
        mv.visitJumpInsn(Opcodes.IFEQ, elseBodyLabel)
        ifScope.emitCode(mv)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(elseBodyLabel)
        elseScope.emitCode(mv)
        mv.visitLabel(endLabel)
    }
}

class ContinueStatement(private val continueLabel: Label): Statement() {
    override fun emitCode(mv: MethodVisitor) {
        mv.visitJumpInsn(Opcodes.GOTO, continueLabel)
    }

}

class BreakStatement(private val endLabel: Label): Statement() {
    override fun emitCode(mv: MethodVisitor) {
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
    }
}

class LoopScope(
    parentScope: StatementScope,
    internal val conditionLabel: Label,
    internal val endLabel: Label
) : SubScope(parentScope) {
    override val currentClosestLoop: LoopScope
        get() = this
}

class WhileStatement(
    parentScope: StatementScope,
    private val condition: Expression,
    whileBody: LoopScope.() -> Unit
) : Statement() {

    private val conditionLabel = Label()
    private val endLabel = Label()

    private val whileScope = LoopScope(parentScope, conditionLabel, endLabel)
    init {
        if (condition.type != BooleanType) {
            throw IllegalStateException("Condition of if statement needs to be of type boolean")
        }
        whileScope.whileBody()
    }

    override fun emitCode(mv: MethodVisitor) {
        mv.visitLabel(conditionLabel)
        condition.emitCode(mv)
        mv.visitJumpInsn(Opcodes.IFEQ, endLabel)
        whileScope.emitCode(mv)
        mv.visitJumpInsn(Opcodes.GOTO, conditionLabel)
        mv.visitLabel(endLabel)
    }
}