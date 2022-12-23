package com.example.compactset

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class PrimitiveTypeInstructionSettings(
    val specializedTypeName: String,
    val boxedTypeName: String,
    val specializedTypeCode: Int,
    val slotSize: Int
) {
    abstract fun loadPrimitive(mv: MethodVisitor, slot: Int)

    //Unbox primitive at top of stack
    abstract fun unboxPrimitive(mv: MethodVisitor)

    //Unbox primitive at top of stack
    abstract fun storePrimitive(mv: MethodVisitor, slot: Int)

    //Unbox primitive at top of stack
    abstract fun storeInArray(mv: MethodVisitor)

    abstract fun loadArrayEntry(mv: MethodVisitor)

    /**
     * Jumps to the [target] if the two variables on top of the stack are not equal.
     */
    abstract fun jumpNotEquals(mv: MethodVisitor, target: Label)
}


object IntInstructionSettings: PrimitiveTypeInstructionSettings(
    specializedTypeName = "I",
    specializedTypeCode = Opcodes.T_INT,
    boxedTypeName = "java/lang/Integer",
    slotSize = 1
) {
    override fun loadPrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ILOAD, slot)
    }

    override fun unboxPrimitive(mv: MethodVisitor) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, boxedTypeName)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxedTypeName, "intValue", "()I", false)
    }

    override fun storePrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.ISTORE, slot)
    }

    override fun loadArrayEntry(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IALOAD)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, target)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IASTORE)
    }
}


object LongInstructionSettings: PrimitiveTypeInstructionSettings(
    specializedTypeName = "J",
    specializedTypeCode = Opcodes.T_LONG,
    boxedTypeName = "java/lang/Long",
    slotSize = 2
) {
    override fun loadPrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.LLOAD, slot)
    }

    override fun unboxPrimitive(mv: MethodVisitor) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, boxedTypeName)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxedTypeName, "longValue", "()J", false)
    }

    override fun storePrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.LSTORE, slot)
    }

    override fun loadArrayEntry(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LALOAD)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.LCMP)
        mv.visitJumpInsn(Opcodes.IFNE, target)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LASTORE)
    }
}


object DoubleInstructionSettings: PrimitiveTypeInstructionSettings(
    specializedTypeName = "D",
    specializedTypeCode = Opcodes.T_DOUBLE,
    boxedTypeName = "java/lang/Double",
    slotSize = 2
) {
    override fun loadPrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.DLOAD, slot)
    }

    override fun unboxPrimitive(mv: MethodVisitor) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, boxedTypeName)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxedTypeName, "doubleValue", "()D", false)
    }

    override fun storePrimitive(mv: MethodVisitor, slot: Int) {
        mv.visitVarInsn(Opcodes.DSTORE, slot)
    }

    override fun loadArrayEntry(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DALOAD)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.DCMPG)
        mv.visitJumpInsn(Opcodes.IFNE, target)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DASTORE)
    }
}