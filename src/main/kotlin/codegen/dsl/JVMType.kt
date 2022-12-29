package com.example.compactset.codegen.dsl

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.IllegalArgumentException

/**
 * A class modelling types on the JVM. Contains the [typeName] used in the JVM bytecode descriptors
 * as well as the [slotSize] a value will take up on the stack or the local variable segment.
 *
 * Also contains several methods that can be used to generate instructions for the specific type of thiis class.
 */
sealed class JVMType(val typeName: String, val slotSize: Int) {
    override fun toString(): String = typeName

    /**
     * Emits the instructions for loading a value of this type from the [slot].
     */
    abstract fun emitLoadInstruction(mv: MethodVisitor, slot: Int)

    /**
     * Emits the instructions for storing a value of this type into the [slot].
     */
    abstract fun emitStoreInstruction(mv: MethodVisitor, slot: Int)

    /**
     * Emits the return instruction for returning a value of this type.
     */
    open fun emitReturnInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ARETURN)
    }

    /**
     * Emits instructions that will consume the two values of this type on the top of the stack and
     * push true on the stack if the values are equal, false otherwise.
     */
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

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is JVMType) return false
        return other.typeName == typeName
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }

    /**
     * Given this type, returns the corresponding array type.
     */
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

    /**
     * Emits instructions that will jump to the [label] if the two primitive values of this type on top of the stack
     * are equal. Consumes the primitive values on the stack.
     */
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
 *
 *
 * Note: For floating point types, Float/Double.compare() will be used to compare.
 * This means that NaN will equal itself (as it should!)
 */
sealed class NumericPrimitiveType(typeName: String, slotSize: Int, typeCode: Int) :
    PrimitiveType(typeName, slotSize, typeCode) {

    /**
     * Emits an instruction consuming and adding two values of this type from the top of the stack.
     */
    abstract fun emitAddInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction consuming and subtracting two values of this type from the top of the stack.
     */
    abstract fun emitSubInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction consuming and multiplying two values of this type from the top of the stack.
     */
    abstract fun emitMulInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction consuming and dividing two values of this type from the top of the stack.
     */
    abstract fun emitDivInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction consuming and calculating the remainder of two values of this type from the top of the stack.
     */
    abstract fun emitRemInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction consuming and comparing two values of this type from the top of the stack.
     */
    abstract fun emitCmpInstruction(mv: MethodVisitor)

    /**
     * Emits an instruction casting this value to a float.
     */
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

/**
 * Base class for types of arrays of a given [containingType].
 */
sealed class JVMArrayType(val containingType: JVMType) : JVMType("[${containingType.typeName}", 1) {

    override fun emitLoadInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ALOAD, slot)
    }

    override fun emitStoreInstruction(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ASTORE, slot)
    }

    /**
     * Emits the instruction for loading an entry from an array.
     * Consumes an array of this type and an index from the top of the stack.
     * Pushes the value of this array at the index.
     */
    abstract fun emitLoadFromArrayInstruction(mv: MethodVisitor)

    /**
     * Emits the instruction for storing a value into this array at an index.
     * Consumes an array, the index and the value from the top of the stack.
     */
    abstract fun emitStoreIntoArrayInstruction(mv: MethodVisitor)

    override val arrayType: JVMType
        get() = JVMObjectArrayType(this)
}

/**
 * A class for arrays of objects of the given [containingType].
 */
class JVMObjectArrayType(containingType: JVMType) : JVMArrayType(containingType) {
    override fun emitLoadFromArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.AALOAD)
    }

    override fun emitStoreIntoArrayInstruction(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.AASTORE)
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

/**
 * Basic JVM object types. Everything that is neither a primitive nor array will be of this type.
 * The [typeName] may not be a primitive type, but needs to be an actual object type.
 */
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
}