package com.example.compactset.codegen.dsl

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * The scope of generating a method, contains all of its variables and statements
 * and offers ways of constructing statements/expressions.
 */
class MethodGeneratorScope internal constructor(methodSignature: MethodSignature) : StatementScope(methodSignature) {
    private var currentSlot = if (methodSignature.hasThis) 1 else 0
    private val registeredParameters = mutableListOf<LocalVariable>()
    private val registeredVariables = mutableListOf<LocalVariable>()

    /**
     * The list that new statements will be appended to.
     * This is changed in sub-scopes of if/while blocks.
     */
    internal var statementTarget: MutableList<Statement> = statements


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
     * Declares that this method has a `this` parameter and returns a variable that can be used to load `this`.
     */
    fun declareThis(): LocalVariable {
        if (!methodSignature.hasThis) {
            throw IllegalStateException("Cannot use `this` in non-object method")
        }

        return LocalVariable(ObjectType("L${methodSignature.owner};"), 0)
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
        if (methodSignature.returnType == VoidType && statementTarget.lastOrNull() !is ReturnVoidStatement) {
            mv.visitInsn(Opcodes.RETURN)
        }

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    /**
     * A function that overrides the current [statementTarget] with the one of the [statementScope]
     * and runs the [body] of the scope, restoring the original [statementTarget] afterwards.
     *
     * This is useful for inner scopes that should contain their own statements rather than this method scope.
     */
    fun <T : StatementScope> runWithNewStatementScope(statementScope: T, body: T.() -> Unit) {
        val old = statementTarget
        statementTarget = statementScope.statements
        statementScope.body()
        statementTarget = old
    }

    override val topScope: MethodGeneratorScope = this


    //region The rest of this class contains the DSL for generating statements/expressions

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
        if (methodSignature.returnType !is VoidType) {
            throw IllegalStateException("Cannot return void from non-void method")
        }
        statementTarget.add(ReturnVoidStatement())
    }

    fun returnValue(value: Expression) {
        if (methodSignature.returnType != value.type) {
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

    operator fun MethodSignature.invoke(vararg expressions: Expression): Expression {
        if (this.returnType is VoidType) {
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
        if (methodSignature.parameters.size != registeredParameters.size) {
            throw IllegalStateException("Attempted to declare variable before all parameters were registered.")
        }
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

    fun Expression.castToFloat(): Expression {
        return CastToFloatExpression(this)
    }

    //endregion
}

/**
 * Generates code for the [methodSignature] in the [ClassWriter] using the DSL defined in [MethodGeneratorScope].
 */
fun ClassWriter.generateMethod(
    methodSignature: MethodSignature,
    body: MethodGeneratorScope.() -> Unit
) {
    val scope = MethodGeneratorScope(methodSignature)
    scope.body()
    scope.writeMethod(this)
}