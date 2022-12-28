package com.example.compactset.codegen.dsl

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class Expression {
    abstract val type: JVMType

    abstract fun emitCode(mv: MethodVisitor)
}

sealed class BinaryExpression(val left: Expression, val right: Expression) : Expression() {
    init {
        if (left.type != right.type) {
            throw IllegalArgumentException("Attempting to compare two types that are not equal")
        }
    }
}

class CastObjectExpression(private val sub: Expression, override val type: JVMType) : Expression() {
    init {
        if (sub.type is PrimitiveType || type is PrimitiveType) {
            throw IllegalArgumentException("Cannot do runtime cast of primitive types")
        }
    }

    override fun emitCode(mv: MethodVisitor) {
        sub.emitCode(mv)
        val name = if (type.typeName.startsWith("L")) type.typeName.drop(1).dropLast(1) else type.typeName
        mv.visitTypeInsn(Opcodes.CHECKCAST, name)
    }
}

sealed class BinaryArithmeticExpression(left: Expression, right: Expression) : BinaryExpression(left, right) {
    init {
        if (left.type != right.type) {
            throw IllegalArgumentException("Incompatible types for binary expression: ${left.type}, ${right.type}")
        }
    }

    override val type: JVMType = left.type

    abstract fun emitOperationCode(mv: MethodVisitor)

    override fun emitCode(mv: MethodVisitor) {
        left.emitCode(mv)
        right.emitCode(mv)
        emitOperationCode(mv)
    }
}

class BooleanNegation(private val sub: Expression) : Expression() {
    init {
        if (sub.type != BooleanType) {
            throw IllegalArgumentException("Attempted to negate non-boolean")
        }
    }

    override val type: JVMType = BooleanType

    override fun emitCode(mv: MethodVisitor) {
        sub.emitCode(mv)
        val trueLabel = Label()
        val endLabel = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, trueLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLabel(endLabel)
    }
}

sealed class BinaryComparisonExpression(left: Expression, right: Expression) : BinaryExpression(left, right) {
    override val type: JVMType = BooleanType
}

class EqualsComparison(left: Expression, right: Expression) : BinaryComparisonExpression(left, right) {
    override fun emitCode(mv: MethodVisitor) {
        left.emitCode(mv)
        right.emitCode(mv)
        left.type.emitEqualsInstructions(mv)
    }
}

sealed class NumericBinaryComparisonExpression(left: Expression, right: Expression, private val jumpCode: Int) :
    BinaryComparisonExpression(left, right) {
    init {
        if (left.type != right.type) {
            throw IllegalArgumentException("Attempting to compare two types that are not equal")
        }
        if (left.type !is NumericPrimitiveType) {
            throw IllegalArgumentException("Addition only works with primitive types")
        }
    }

    override fun emitCode(mv: MethodVisitor) {
        left.emitCode(mv)
        right.emitCode(mv)

        val trueLabel = Label()
        val endLabel = Label()
        (left.type as NumericPrimitiveType).emitCmpInstruction(mv)
        mv.visitJumpInsn(jumpCode, trueLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLabel(endLabel)

    }
}

class LessThanComparison(left: Expression, right: Expression) :
    NumericBinaryComparisonExpression(left, right, Opcodes.IFLT)

class LessEqualsComparison(left: Expression, right: Expression) :
    NumericBinaryComparisonExpression(left, right, Opcodes.IFLE)

class GreaterThanComparison(left: Expression, right: Expression) :
    NumericBinaryComparisonExpression(left, right, Opcodes.IFGT)

class GreaterEqualsComparison(left: Expression, right: Expression) :
    NumericBinaryComparisonExpression(left, right, Opcodes.IFGE)

sealed class NumericBinaryExpression(left: Expression, right: Expression) : BinaryArithmeticExpression(left, right) {
    init {
        if (left.type !is NumericPrimitiveType) {
            throw IllegalArgumentException("Addition only works with primitive types")
        }
    }

    override val type: NumericPrimitiveType = left.type as NumericPrimitiveType
}

class AdditionExpression(left: Expression, right: Expression) : NumericBinaryExpression(left, right) {
    override fun emitOperationCode(mv: MethodVisitor) {
        type.emitAddInstruction(mv)
    }
}

class SubtractionExpression(left: Expression, right: Expression) : NumericBinaryExpression(left, right) {
    override fun emitOperationCode(mv: MethodVisitor) {
        type.emitSubInstruction(mv)
    }
}

class MultiplicationExpression(left: Expression, right: Expression) : NumericBinaryExpression(left, right) {
    override fun emitOperationCode(mv: MethodVisitor) {
        type.emitMulInstruction(mv)
    }
}

class DivisionExpression(left: Expression, right: Expression) : NumericBinaryExpression(left, right) {
    override fun emitOperationCode(mv: MethodVisitor) {
        type.emitDivInstruction(mv)
    }
}

class RemainderExpression(left: Expression, right: Expression) : NumericBinaryExpression(left, right) {
    override fun emitOperationCode(mv: MethodVisitor) {
        type.emitRemInstruction(mv)
    }
}

class ThisExpression(override val type: JVMType) : Expression() {
    override fun emitCode(mv: MethodVisitor) {
        mv.visitVarInsn(Opcodes.ALOAD, 0)
    }
}

class ArrayLengthExpression(private val expression: Expression) : Expression() {
    init {
        if (expression.type !is JVMArrayType) {
            throw IllegalArgumentException("Addition only works with primitive types")
        }
    }

    override val type: JVMType = IntType

    override fun emitCode(mv: MethodVisitor) {
        expression.emitCode(mv)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
    }
}

class CastToFloatExpression(private val expression: Expression) : Expression() {
    init {
        if (expression.type !is NumericPrimitiveType) {
            throw IllegalArgumentException("Addition only works with primitive types")
        }
    }

    override val type: NumericPrimitiveType = FloatType

    override fun emitCode(mv: MethodVisitor) {
        expression.emitCode(mv)
        (expression.type as NumericPrimitiveType).emitCastToFloatInstruction(mv)
    }
}

class LoadConstantExpression(
    private val value: Any
) : Expression() {
    private val constantOpCodes = mapOf(
        false to Opcodes.ICONST_0,
        true to Opcodes.ICONST_1,
        0 to Opcodes.ICONST_0,
        1 to Opcodes.ICONST_1,
        0L to Opcodes.LCONST_0,
        1L to Opcodes.LCONST_1,
        0.0f to Opcodes.FCONST_0,
        1.0f to Opcodes.FCONST_1,
        0.0 to Opcodes.DCONST_0,
        1.0 to Opcodes.DCONST_1
    )

    override val type: JVMType
        get() = when (value) {
            is Int -> IntType
            is Long -> LongType
            is Float -> FloatType
            is Double -> DoubleType
            is Boolean -> BooleanType
            else -> throw IllegalArgumentException("Could not determine type of $value")
        }

    override fun emitCode(mv: MethodVisitor) {
        val code = constantOpCodes[value]
        if (code != null) {
            mv.visitInsn(code)
        } else {
            mv.visitLdcInsn(value)
        }
    }
}

class LoadFieldExpression(
    private val objectInstance: Expression,
    private val field: ClassField
) : Expression() {
    override val type: JVMType = field.type

    override fun emitCode(mv: MethodVisitor) {
        objectInstance.emitCode(mv)
        mv.visitFieldInsn(Opcodes.GETFIELD, field.owner, field.name, field.type.typeName)
    }
}

class IndexArrayExpression(
    internal val arrayExpression: Expression,
    internal val indexExpression: Expression
) : Expression() {
    override val type: JVMType
        get() {
            val arrayType = arrayExpression.type
            if (arrayType !is JVMArrayType) {
                throw IllegalArgumentException("Indexing array instruction argument not array")
            }
            return arrayType.containingType
        }

    override fun emitCode(mv: MethodVisitor) {
        arrayExpression.emitCode(mv)
        indexExpression.emitCode(mv)
        (arrayExpression.type as JVMArrayType).emitLoadFromArrayInstruction(mv)
    }
}

class NewArrayExpression(
    private val sizeExpression: Expression,
    internal val elementType: JVMType,
) : Expression() {
    init {
        if (sizeExpression.type != IntType) {
            throw IllegalArgumentException("Array size expression needs to be of type Int")
        }
    }

    override val type: JVMType = elementType.arrayType

    override fun emitCode(mv: MethodVisitor) {
        sizeExpression.emitCode(mv)
        if (elementType is PrimitiveType) {
            mv.visitIntInsn(Opcodes.NEWARRAY, elementType.typeCode)
        } else {
            mv.visitTypeInsn(Opcodes.ANEWARRAY, type.typeName)
        }
    }
}


class FunctionCallExpression(
    internal val methodSignature: MethodSignature,
    internal val parameters: List<Expression>
) : Expression() {
    override val type: JVMType = methodSignature.returnJVMType

    override fun emitCode(mv: MethodVisitor) {
        for (p in parameters) {
            p.emitCode(mv)
        }

        val opcode = if (methodSignature.flags and Opcodes.ACC_STATIC > 0) {
            Opcodes.INVOKESTATIC
        } else if (methodSignature.flags and Opcodes.ACC_PRIVATE > 0) {
            Opcodes.INVOKESPECIAL
        } else {
            Opcodes.INVOKEVIRTUAL
        }
        mv.visitMethodInsn(
            opcode,
            methodSignature.owner,
            methodSignature.name,
            methodSignature.descriptor,
            methodSignature.isInterface
        )
        methodSignature
    }
}