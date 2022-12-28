package com.example.compactset.codegen.dsl

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

class MethodGeneratorScope internal constructor(methodSignature: MethodSignature) : StatementScope(methodSignature) {
    private var currentSlot = 0
    private val registeredParameters = mutableListOf<LocalVariable>()
    protected val registeredVariables = mutableListOf<LocalVariable>()

    internal var statementTarget: MutableList<Statement> = statements


    fun addVariableSlot(type: JVMType): LocalVariable {
        val variable = LocalVariable(type, currentSlot)
        currentSlot += type.slotSize
        return variable
    }

    fun declareThis(): LocalVariable {
        if (!methodSignature.hasThis) {
            throw IllegalStateException("Cannot call this in non-object method")
        }
        return declareParameter(ObjectType(methodSignature.owner))
    }

    fun declareParameter(type: JVMType): LocalVariable {
        if (registeredVariables.isNotEmpty()) {
            throw IllegalStateException("Cannot declare a parameter after a variable has been declared")
        }
        val variable = addVariableSlot(type)
        registeredParameters.add(variable)
        return variable
    }

    fun ClassField.load(objectInstance: Expression): Expression {
        return LoadFieldExpression(objectInstance, this)
    }

    fun newArray(type: JVMType, size: Expression): Expression {
        return NewArrayExpression(size, type)
    }

    //Array set
    operator fun Expression.set(indexExpr: Expression, valueExpression: Expression) {
        statementTarget.add(StoreInArrayStatement(this, indexExpr, valueExpression))

    }

    //Array index
    operator fun Expression.get(expr: Expression): Expression {
        return IndexArrayExpression(this, expr)
    }

    fun Any.asConstant(): Expression {
        return LoadConstantExpression(this)
    }

    fun LocalVariable.assign(expr: Expression) {
        statementTarget.add(VariableAssignment(this, expr))
    }

    fun ClassField.assign(objectInstance: Expression, expr: Expression) {
        statementTarget.add(FieldAssignment(this, objectInstance, expr))
    }

    fun returnVoid() {
        if (methodSignature.returnJVMType !is VoidType) {
            throw IllegalStateException("Cannot return void from non-void method")
        }
        statementTarget.add(ReturnVoidStatement())
    }

    fun returnValue(value: Expression) {
        if (methodSignature.returnJVMType != value.type) {
            throw IllegalStateException("Attempting to return incorrect type")
        }
        statementTarget.add(ReturnValueStatement(value))
    }

    //A bit of mutable magic
    class OptionalElseScope(private val ifStatement: IfStatement) {
        infix fun elseStatement(elseBody: IfScope.() -> Unit) {
            ifStatement.setElseBody(elseBody)
        }
    }

    fun ifStatement(condition: Expression, ifBody: IfScope.() -> Unit): OptionalElseScope {
        val ifStatement = IfStatement(this, methodSignature, condition, ifBody)
        statementTarget.add(ifStatement)
        return OptionalElseScope(ifStatement)
    }

    fun whileLoop(condition: Expression, whileBody: LoopScope.() -> Unit) {
        val whileStatement = WhileStatement(this, methodSignature, condition, whileBody)
        statementTarget.add(whileStatement)
    }

    fun forEachLoop(elemVariable: LocalVariable, arrayExpression: Expression, body: LoopScope.() -> Unit) {
        val arrayType = arrayExpression.type
        if (arrayType !is JVMArrayType) {
            throw IllegalStateException("Can currently only iterate over array values")
        }
        if (elemVariable.type != arrayType.containingType) {
            throw IllegalStateException("For loop variable type is not equal to array type!")
        }
        val index = declareVariable(IntType)
        index.assign(0.asConstant())
        whileLoop(index.lt(arrayExpression.arrayLength())) {
            elemVariable.assign(arrayExpression[index])
            index.assign(index + 1.asConstant())
            body()
        }
    }

    fun MethodSignature.call(vararg expressions: Expression): Expression {
        if (this.returnJVMType is VoidType) {
            throw IllegalStateException("Cannot use void function as expression")
        }
        val requiredCount = if (hasThis) parameters.size + 1 else parameters.size
        if (expressions.size != requiredCount) {
            throw IllegalStateException("Attempted to invoke method with incorrect argument count")
        }
        return FunctionCallExpression(this, expressions.asList())
    }

    fun MethodSignature.invokeStatement(vararg expressions: Expression) {
        val requiredCount = if (hasThis) parameters.size + 1 else parameters.size
        if (expressions.size != requiredCount) {
            throw IllegalStateException("Attempted to invoke method with incorrect argument count")
        }
        statementTarget.add(FunctionCallStatement(FunctionCallExpression(this, expressions.asList())))
    }

    fun declareVariable(type: JVMType): LocalVariable {
        val variable = addVariableSlot(type)
        registeredVariables.add(variable)
        return variable
    }

    fun Expression.objectCast(type: JVMType): Expression {
        return CastObjectExpression(this, type)
    }

    fun Expression.eq(other: Expression): Expression {
        return EqualsComparison(this, other)
    }

    fun Expression.lt(other: Expression): Expression {
        return LessThanComparison(this, other)
    }

    fun Expression.le(other: Expression): Expression {
        return LessEqualsComparison(this, other)
    }

    fun Expression.gt(other: Expression): Expression {
        return GreaterThanComparison(this, other)
    }

    fun Expression.ge(other: Expression): Expression {
        return GreaterEqualsComparison(this, other)
    }

    fun Expression.neq(other: Expression): Expression {
        return !this.eq(other)
    }

    operator fun Expression.not(): Expression {
        return BooleanNegation(this)
    }


    operator fun Expression.minus(other: Expression): Expression {
        return SubtractionExpression(this, other)
    }

    operator fun Expression.plus(other: Expression): Expression {
        return AdditionExpression(this, other)
    }

    operator fun Expression.div(other: Expression): Expression {
        return DivisionExpression(this, other)
    }

    operator fun Expression.times(other: Expression): Expression {
        return MultiplicationExpression(this, other)
    }

    operator fun Expression.rem(other: Expression): Expression {
        return RemainderExpression(this, other)
    }

    fun Expression.arrayLength(): Expression {
        return ArrayLengthExpression(this)
    }


    fun writeMethod(cw: ClassWriter) {
        val expectedParameters =
            if (methodSignature.hasThis) methodSignature.parameters.size + 1 else methodSignature.parameters.size

        if (registeredParameters.size != expectedParameters) {
            throw IllegalStateException("Method signature does not have same amount of parameters as registered in method")
        }
        val parametersWithoutThis = if (methodSignature.hasThis) registeredParameters.drop(1) else registeredParameters
        parametersWithoutThis.zip(methodSignature.parameters) { param, type ->
            if (param.type != type) {
                throw IllegalStateException("Registered parameter has different type than signature (${param.type} vs $type)")
            }
        }
        val mv =
            cw.visitMethod(methodSignature.flags, methodSignature.name, methodSignature.descriptor, null, emptyArray())
        mv.visitCode()

        emitCode(mv)

        if (methodSignature.returnJVMType == VoidType && statementTarget.lastOrNull() !is ReturnVoidStatement) {
            mv.visitInsn(Opcodes.RETURN)
        }

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    fun <T : StatementScope> runWithNewStatementScope(statementScope: T, body: T.() -> Unit) {
        val old = statementTarget
        statementTarget = statementScope.statements
        statementScope.body()
        statementTarget = old
    }

    fun Expression.castToFloat(): Expression {
        return CastToFloatExpression(this)
    }

    override val topScope: MethodGeneratorScope = this
}

fun ClassWriter.generateMethod(
    methodSignature: MethodSignature,
    body: MethodGeneratorScope.() -> Unit
) {
    val scope = MethodGeneratorScope(methodSignature)
    scope.body()
    scope.writeMethod(this)
}