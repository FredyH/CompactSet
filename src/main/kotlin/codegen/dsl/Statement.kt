package com.example.compactset.codegen.dsl

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class Statement {
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
    internal val variable: ClassField,
    internal val objectInstance: Expression,
    internal val expression: Expression
) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        objectInstance.emitCode(mv)
        expression.emitCode(mv)
        mv.visitFieldInsn(Opcodes.PUTFIELD, variable.owner, variable.name, variable.type.typeName)
    }
}

class VariableAssignment(
    internal val variable: LocalVariable,
    internal val expression: Expression
) : Statement() {
    override fun emitCode(mv: MethodVisitor) {
        expression.emitCode(mv)
        variable.type.emitStoreInstruction(mv, variable.slot)
    }
}

class StoreInArrayStatement(
    internal val arrayExpression: Expression,
    internal val indexExpression: Expression,
    internal val valueExpression: Expression,
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
        val returnType = functionCallExpression.methodSignature.returnJVMType

        if (returnType.slotSize == 2) {
            mv.visitInsn(Opcodes.POP2)
        } else if (returnType.slotSize == 1) {
            mv.visitInsn(Opcodes.POP)
        }
    }
}

class IfScope(
    override val topScope: MethodGeneratorScope,
    methodSignature: MethodSignature
) : StatementScope(methodSignature) {

}

class IfStatement(
    private val parentScope: MethodGeneratorScope,
    methodSignature: MethodSignature,
    private val condition: Expression,
    ifBody: IfScope.() -> Unit
) : Statement() {

    private val ifScope: IfScope = IfScope(parentScope, methodSignature)
    private val elseScope: IfScope = IfScope(parentScope, methodSignature)

    internal fun setElseBody(elseBody: IfScope.() -> Unit) {
        parentScope.runWithNewStatementScope(elseScope, elseBody)
    }

    init {
        if (condition.type != BooleanType) {
            throw IllegalStateException("Condition of if statement needs to be of type boolean")
        }
        parentScope.runWithNewStatementScope(ifScope, ifBody)
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
    override val topScope: MethodGeneratorScope,
    private val conditionLabel: Label,
    private val endLabel: Label,
    methodSignature: MethodSignature
) : StatementScope(methodSignature) {

    fun continueStatement() {
        topScope.statementTarget.add(ContinueStatement(conditionLabel))
    }

    fun breakStatement() {
        topScope.statementTarget.add(ContinueStatement(endLabel))
    }

}

class WhileStatement(
    parentScope: MethodGeneratorScope,
    methodSignature: MethodSignature,
    private val condition: Expression,
    whileBody: LoopScope.() -> Unit
) : Statement() {

    private val conditionLabel = Label()
    private val endLabel = Label()

    private val whileScope = LoopScope(parentScope, conditionLabel, endLabel, methodSignature)
    init {
        if (condition.type != BooleanType) {
            throw IllegalStateException("Condition of if statement needs to be of type boolean")
        }
        parentScope.runWithNewStatementScope(whileScope, whileBody)
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