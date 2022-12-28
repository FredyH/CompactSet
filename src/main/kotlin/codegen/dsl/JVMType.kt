package com.example.compactset.codegen.dsl

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.IllegalArgumentException

sealed class JVMType(val typeName: String, val slotSize: Int) {
    override fun toString(): String = typeName

    abstract fun emitLoadInstruction(mv: MethodVisitor, slot: Int)
    abstract fun emitStoreInstruction(mv: MethodVisitor, slot: Int)
    open fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ARETURN)
    }

    open fun emitEqualsInstructions(mv: MethodVisitor) {
        val trueLabel = Label()
        val endLabel = Label()
        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, trueLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLabel(endLabel)
    }

    abstract val arrayType: JVMType
}

object VoidType : JVMType("V", 0) {
    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        throw IllegalArgumentException("Cannot load void")
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        throw IllegalArgumentException("Cannot store void")
    }

    override val arrayType
        get() = throw IllegalStateException("Void type does not have an array type")
}

sealed class PrimitiveType(typeName: String, slotSize: Int, val typeCode: Int) : JVMType(typeName, slotSize) {

    protected abstract fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label)

    override fun emitEqualsInstructions(mv: MethodVisitor) {
        val trueLabel = Label()
        val endLabel = Label()

        emitJumpIfEqualInstructions(mv, trueLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLabel(endLabel)
    }
}

object BooleanType : PrimitiveType("Z", 1, Opcodes.T_BOOLEAN) {
    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ILOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ISTORE, slot)
    }

    override fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IRETURN)
    }

    override fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label)
    }

    override val arrayType: JVMType = BooleanArrayType
}

/**
 * Note: For floating point types, Float/Double.compare() will be used to compare.
 * This means that NaN will equal itself (as it should!)
 */
sealed class NumericPrimitiveType(typeName: String, slotSize: Int, typeCode: Int) :
    PrimitiveType(typeName, slotSize, typeCode) {
    abstract fun emitAddInstruction(mv: MethodVisitor)
    abstract fun emitSubInstruction(mv: MethodVisitor)
    abstract fun emitMulInstruction(mv: MethodVisitor)
    abstract fun emitDivInstruction(mv: MethodVisitor)
    abstract fun emitRemInstruction(mv: MethodVisitor)
    abstract fun emitCmpInstruction(mv: MethodVisitor)
    abstract fun emitCastToFloatInstruction(mv: MethodVisitor)
}

object IntType : NumericPrimitiveType("I", 1, Opcodes.T_INT) {
    override fun emitAddInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IADD)
    }

    override fun emitSubInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ISUB)
    }

    override fun emitMulInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IMUL)
    }

    override fun emitDivInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IDIV)
    }

    override fun emitRemInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IREM)
    }

    override fun emitCmpInstruction(mv: MethodVisitor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "compare", "(II)I", false)
    }

    override fun emitCastToFloatInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.I2F)
    }

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ILOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ISTORE, slot)
    }

    override fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IRETURN)
    }

    override fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label)
    }

    override val arrayType: JVMType = IntArrayType
}

object LongType : NumericPrimitiveType("J", 2, Opcodes.T_LONG) {
    override fun emitAddInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LADD)
    }

    override fun emitCastToFloatInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.L2F)
    }

    override fun emitSubInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LSUB)
    }

    override fun emitMulInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LMUL)
    }

    override fun emitDivInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LDIV)
    }

    override fun emitRemInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LREM)
    }

    override fun emitCmpInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LCMP)
    }

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.LLOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.LSTORE, slot)
    }

    override fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LRETURN)
    }

    override fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label) {
        mv.visitInsn(Opcodes.LCMP)
        mv.visitJumpInsn(Opcodes.IFEQ, label)
    }

    override val arrayType: JVMType = LongArrayType
}

object FloatType : NumericPrimitiveType("F", 1, Opcodes.T_FLOAT) {
    override fun emitAddInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FADD)
    }

    override fun emitCastToFloatInstruction(mv: MethodVisitor) {
    }

    override fun emitSubInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FSUB)
    }

    override fun emitMulInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FMUL)
    }

    override fun emitDivInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FDIV)
    }

    override fun emitRemInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FREM)
    }

    override fun emitCmpInstruction(mv: MethodVisitor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false)
    }

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.FLOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.FSTORE, slot)
    }

    override fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FRETURN)
    }

    override fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false)
        mv.visitJumpInsn(Opcodes.IFEQ, label)
    }

    override val arrayType: JVMType = FloatArrayType
}

object DoubleType : NumericPrimitiveType("D", 2, Opcodes.T_DOUBLE) {
    override fun emitAddInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DADD)
    }

    override fun emitCastToFloatInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.D2F)
    }

    override fun emitSubInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DSUB)
    }

    override fun emitMulInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DMUL)
    }

    override fun emitDivInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DDIV)
    }

    //Use Double.compare() to avoid problems with NaN
    override fun emitCmpInstruction(mv: MethodVisitor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false)
    }

    override fun emitRemInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DREM)
    }

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.DLOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.DSTORE, slot)
    }

    override fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DRETURN)
    }

    override fun emitJumpIfEqualInstructions(mv: MethodVisitor, label: Label) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false)
        mv.visitJumpInsn(Opcodes.IFEQ, label)
    }

    override val arrayType: JVMType = DoubleArrayType
}

sealed class JVMArrayType(val containingType: JVMType) : JVMType("[${containingType.typeName}", 1) {

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ALOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ASTORE, slot)
    }

    abstract fun emitLoadFromArrayInstruction(mv: MethodVisitor)

    abstract fun emitStoreIntoArrayInstruction(mv: MethodVisitor)

    override val arrayType: JVMType
        get() = JVMObjectArrayType(this)
}

class JVMObjectArrayType(containingType: JVMType) : JVMArrayType(containingType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.AALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.AASTORE)
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is JVMObjectArrayType) return false
        return other.containingType == containingType
    }
}

object BooleanArrayType : JVMArrayType(BooleanType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.BALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.BASTORE)
    }
}

object IntArrayType : JVMArrayType(IntType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IASTORE)
    }
}

object LongArrayType : JVMArrayType(LongType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LASTORE)
    }
}

object DoubleArrayType : JVMArrayType(DoubleType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DASTORE)
    }
}

object FloatArrayType : JVMArrayType(DoubleType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.FASTORE)
    }
}

class ObjectType(typeName: String) : JVMType("L${typeName};", 1) {
    init {
        require(typeName.length > 2) { "Object type needs to be an actual object type, but was $typeName" }
    }

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ALOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ALOAD, slot)
    }

    override val arrayType: JVMType = JVMObjectArrayType(this)

    override fun hashCode(): Int {
        return typeName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ObjectType) return false
        return other.typeName == typeName
    }
}