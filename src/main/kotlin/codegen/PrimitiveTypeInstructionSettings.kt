package com.example.compactset.codegen

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Method

/**
 * The JVM has different instructions depending on the type of element being worked on.
 * Even worse, variables can take different amount of slots in the local variable section (and stack) of the JVM,
 * depending on the size (Double, Long 2 slots, everything else 1 slot).
 *
 * This class defines the settings for a primitive type on the JVM, including the
 * [specializedTypeName], the corresponding [boxedTypeName] and the [specializedTypeCode] used to create arrays
 * as well as the [slotSize] a primitive value will take up in the local variables and stack.
 *
 * Further, subclasses of this class define how code for certain instructions is emitted.
 */
internal abstract class PrimitiveTypeInstructionSettings(
    val specializedTypeName: String,
    val boxedTypeName: String,
    val specializedTypeCode: Int,
    val slotSize: Int
) {
    /**
     * Loads the primitive defined by this class onto the JVM stack from the local variable in the [slot].
     */
    abstract fun loadPrimitive(mv: MethodVisitor, slot: Int)

    /**
     * Unboxes the primitive defined by this class out of a boxed object at the top of the stack.
     * Removes the boxed object and pushes the value of the primitive onto the stack.
     */
    abstract fun unboxPrimitive(mv: MethodVisitor)

    /**
     * Stores the primitive defined by this class at the top of the JVM stack into the local variable at the [slot].
     */
    abstract fun storePrimitive(mv: MethodVisitor, slot: Int)

    /**
     * Given the following stack layout (top to bottom)
     *
     * Primitive value to store
     * Int Index to store at
     * Reference to array
     * ...
     *
     * consumes the top 3 elements of the stack and stores the primitive value defined by this class
     * into the referenced array at the given index.
     */
    abstract fun storeInArray(mv: MethodVisitor)

    /**
     * Given the following stack layout (top to bottom)
     *
     *  Int Index to load from
     *  Reference to array
     *  ...
     *
     *  consumes the top 2 elements of the stack and pushes the primitive value in the array at the given index
     *  onto the stack.
     */
    abstract fun loadArrayEntry(mv: MethodVisitor)

    /**
     * Jumps to the [target] if the two primitive values defined by this class on top of the stack are not equal.
     */
    abstract fun jumpNotEquals(mv: MethodVisitor, target: Label)

    /**
     * Jumps to the [target] if the two primitive values defined by this class on top of the stack are equal.
     */
    abstract fun jumpEquals(mv: MethodVisitor, target: Label)

    /**
     * Given a primitive value at the top of the stack, consumes that value and pushes its hash code.
     */
    abstract fun calculateHashCode(mv: MethodVisitor)

    /**
     * Load the zero value of this primitive
     */
    abstract fun loadZero(mv: MethodVisitor)
}


internal object IntInstructionSettings: PrimitiveTypeInstructionSettings(
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

    override fun jumpEquals(mv: MethodVisitor, target: Label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, target)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitJumpInsn(Opcodes.IF_ICMPNE, target)
    }

    override fun calculateHashCode(mv: MethodVisitor) {
        //Technically, equivalent to a no-op
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "hashCode", "(I)I", false)

    }

    override fun loadZero(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ICONST_0)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.IASTORE)
    }
}


internal object LongInstructionSettings: PrimitiveTypeInstructionSettings(
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

    override fun jumpEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.LCMP)
        mv.visitJumpInsn(Opcodes.IFEQ, target)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.LCMP)
        mv.visitJumpInsn(Opcodes.IFNE, target)
    }

    override fun loadZero(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LCONST_0)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.LASTORE)
    }

    override fun calculateHashCode(mv: MethodVisitor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "hashCode", "(J)I", false)
    }
}


internal object DoubleInstructionSettings: PrimitiveTypeInstructionSettings(
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

    override fun jumpEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.DCMPG)
        mv.visitJumpInsn(Opcodes.IFEQ, target)
    }

    override fun jumpNotEquals(mv: MethodVisitor, target: Label) {
        mv.visitInsn(Opcodes.DCMPG)
        mv.visitJumpInsn(Opcodes.IFNE, target)
    }

    override fun storeInArray(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DASTORE)
    }

    override fun loadZero(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.DCONST_0)
    }

    override fun calculateHashCode(mv: MethodVisitor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "hashCode", "(D)I", false)
    }
}