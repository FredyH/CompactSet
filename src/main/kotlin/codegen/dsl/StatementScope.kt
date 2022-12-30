package com.example.compactset.codegen.dsl

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.IllegalArgumentException
import kotlin.reflect.KProperty


@DslMarker
annotation class StatementScopeMarker

/**
 * A scope of statements within a method.
 * Contains all the DSL methods for expressions and statements.
 */
@StatementScopeMarker
abstract class StatementScope(internal val methodSignature: MethodSignature) {

    /**
     * The statements contained in this scope.
     */
    internal val statements = mutableListOf<Statement>()

    /**
     * Declares a variable of the [type] in this method, reserving it a slot and returning it.
     */
    @StatementScopeMarker
    internal abstract fun declareVariable(type: JVMType): LocalVariable

    internal open val currentClosestLoop: LoopScope? = null

    /**
     * Emits the code of all statements of this scope.
     */
    fun emitCode(mv: MethodVisitor) {
        statements.forEach { it.emitCode(mv) }
    }


    //region The rest of this class contains the DSL for generating statements/expressions


    /**
     * Returns the implicit `this` parameter of a method.
     * Throws an exception if this method does not have an implicit `this` parameter.
     */
    val `this`: LocalVariable
        get() {
            if (!methodSignature.hasThis) {
                throw IllegalStateException("Cannot use `this` in non-object method")
            }
            return LocalVariable(ObjectType("L${methodSignature.owner};"), 0)
        }

    fun newArray(type: JVMType, size: Expression): Expression {
        return NewArrayExpression(size, type)
    }

    //Array set
    operator fun Expression.set(indexExpr: Expression, valueExpression: Expression) {
        statements.add(StoreInArrayStatement(this, indexExpr, valueExpression))

    }

    //Array index
    operator fun Expression.get(expr: Expression): Expression {
        return IndexArrayExpression(this, expr)
    }

    fun Any.asConstant(): Expression {
        return LoadConstantExpression(this)
    }

    infix fun LocalVariable.`=`(expr: Expression) {
        statements.add(VariableAssignment(this, expr))
    }

    class ClassAssignmentScope(
        private val field: ClassField,
        private val objectInstance: Expression,
        private val generatorScope: StatementScope
    ) : Expression() {
        infix fun `=`(expr: Expression) {
            generatorScope.statements.add(FieldAssignment(field, objectInstance, expr))
        }

        override val type: JVMType = field.type

        override fun emitCode(mv: MethodVisitor) {
            objectInstance.emitCode(mv)
            mv.visitFieldInsn(Opcodes.GETFIELD, field.owner, field.name, field.type.typeName)
        }
    }

    operator fun Expression.get(classField: ClassField): ClassAssignmentScope {
        if (this.type !is ObjectType) {
            throw IllegalArgumentException("Cannot use object indexing on non object parameter")
        }
        return ClassAssignmentScope(classField, this, this@StatementScope)
    }

    private val cachedInitializedVariables = mutableMapOf<KProperty<*>, LocalVariable>()

    fun initializeVar(expr: Expression): LocalVariable {
        val variable = declareVariable(expr.type)
        variable `=` expr
        return variable
    }

    class ClassMethodInvocationScope(
        private val methodSignature: MethodSignature,
        private val objectInstance: Expression,
        private val generatorScope: StatementScope
    ) {
        operator fun invoke(vararg arguments: Expression): Expression {
            generatorScope.apply {
                return this@ClassMethodInvocationScope.methodSignature(objectInstance, *arguments)
            }
        }

        fun invokeStatement(vararg arguments: Expression) {
            generatorScope.apply {
                this@ClassMethodInvocationScope.methodSignature.invokeStatement(objectInstance, *arguments)
            }
        }
    }

    operator fun Expression.get(methodSignature: MethodSignature): ClassMethodInvocationScope {
        if (this.type !is ObjectType) {
            throw IllegalArgumentException("Cannot use object method invocation on non object parameter")
        }
        return ClassMethodInvocationScope(methodSignature, this, this@StatementScope)
    }

    fun `return`() {
        if (methodSignature.returnType !is VoidType) {
            throw IllegalStateException("Cannot return void from non-void method")
        }
        statements.add(ReturnVoidStatement())
    }

    fun `return`(value: Expression) {
        if (methodSignature.returnType != value.type) {
            throw IllegalStateException("Attempting to return incorrect type")
        }
        statements.add(ReturnValueStatement(value))
    }

    //A bit of mutable magic
   class OptionalElseScope(private val ifStatement: IfStatement) {
        infix fun `else`(elseBody: IfScope.() -> Unit) {
            ifStatement.setElseBody(elseBody)
        }
    }

    @StatementScopeMarker
    fun `if`(condition: Expression, ifBody: IfScope.() -> Unit): OptionalElseScope {
        val ifStatement = IfStatement(this, condition, ifBody)
        statements.add(ifStatement)
        return OptionalElseScope(ifStatement)
    }

    @StatementScopeMarker
    fun `while`(condition: Expression, whileBody: LoopScope.() -> Unit) {
        val whileStatement = WhileStatement(this, condition, whileBody)
        statements.add(whileStatement)
    }

    @StatementScopeMarker
    fun forEach(elemVariable: LocalVariable, arrayExpression: Expression, body: LoopScope.() -> Unit) {
        val arrayType = arrayExpression.type
        if (arrayType !is JVMArrayType) {
            throw IllegalStateException("Can currently only iterate over array values")
        }
        if (elemVariable.type != arrayType.containingType) {
            throw IllegalStateException("For loop variable type is not equal to array type!")
        }
        val index = declareVariable(IntType)
        index `=` 0.asConstant()
        `while`(index.lt(arrayExpression.arrayLength())) {
            elemVariable `=` arrayExpression[index]
            index `=` index + 1.asConstant()
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
        statements.add(FunctionCallStatement(FunctionCallExpression(this, expressions.asList())))
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


abstract class SubScope(
    private val parentScope: StatementScope,
) : StatementScope(parentScope.methodSignature) {
    override fun declareVariable(type: JVMType): LocalVariable = parentScope.declareVariable(type)

    override val currentClosestLoop: LoopScope? = parentScope.currentClosestLoop

    fun `continue`() {
        val loop = currentClosestLoop ?: throw IllegalStateException("Attempted to continue outside of a loop scope")
        statements.add(ContinueStatement(loop.conditionLabel))
    }

    fun `break`() {
        val loop = currentClosestLoop ?: throw IllegalStateException("Attempted to break outside of a loop scope")
        statements.add(BreakStatement(loop.endLabel))
    }
}