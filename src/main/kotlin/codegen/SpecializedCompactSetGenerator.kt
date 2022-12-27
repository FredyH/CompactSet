package com.example.compactset.codegen

import com.example.compactset.CompactSet
import org.objectweb.asm.*

/**
 * A class responsible for generating bytecode of a specialized implementation of a [CompactSet].
 * Defines the bytecode settings for the primitive type using [primitiveSettings] and creates a class in the
 * com.example.compactset package with the name [className].
 *
 * The structure of the specialized classes follows very closely to the implementation given
 * by the default implementation, but instead it uses primitive arrays to achieve higher performance and
 * lower memory usage.
 */
internal class SpecializedCompactSetGenerator(
    val className: String,
    private val primitiveSettings: PrimitiveTypeInstructionSettings
) {
    private val setPackage: String = "com/example/compactset"

    private val fullyQualifiedClassName = "$setPackage/$className"
    private val specializedArrayTypeName = "[${primitiveSettings.specializedTypeName}"
    private val specializedTypeName = primitiveSettings.specializedTypeName

    private val insertElementDescriptor = "([${specializedTypeName}${specializedTypeName})Z"
    private val getElementIndexDescriptor = "([${specializedTypeName}${specializedTypeName})I"


    private fun generateConstructor(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, emptyArray())

        //Call object super constructor
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

        //Initialize array using size argument of the constructor
        mv.visitVarInsn(Opcodes.ALOAD, 0)

        //Math.max(initialSize, 4)
        mv.visitVarInsn(Opcodes.ILOAD, 1)
        mv.visitInsn(Opcodes.ICONST_4)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(II)I", false)

        mv.visitIntInsn(Opcodes.NEWARRAY, primitiveSettings.specializedTypeCode)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)

        mv.visitInsn(Opcodes.RETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateGetLoadFactorMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "getLoadFactor", "()F", null, emptyArray())
        mv.visitCode()

        //Get size
        val endLabel = Label()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")

        //If our array contains zero, subtract one from size (because zero does not appear in the backing array)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "containsZero", "Z")
        mv.visitJumpInsn(Opcodes.IFEQ, endLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.ISUB)

        //Convert our number of filled buckets to a float
        mv.visitLabel(endLabel)
        mv.visitInsn(Opcodes.I2F)

        //Get size of backking array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitInsn(Opcodes.I2F)

        //Return ratio between the two
        mv.visitInsn(Opcodes.FDIV)
        mv.visitInsn(Opcodes.FRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun storeArrayValue(mv: MethodVisitor, arraySlot: Int, indexSlot: Int, valueSlot: Int) {
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadArrayEntry(mv)
        primitiveSettings.storePrimitive(mv, valueSlot)
    }

    private fun generateGetElementIndex(cw: ClassWriter) {
        val arraySlot = 1
        val elementSlot = 2

        val mv = cw.visitMethod(
            Opcodes.ACC_PRIVATE,
            "getElementIndex",
            getElementIndexDescriptor,
            null,
            emptyArray()
        )
        mv.visitCode()

        primitiveSettings.loadPrimitive(mv, elementSlot)
        primitiveSettings.calculateHashCode(mv)
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "floorMod", "(II)I", false)

        val indexSlot = elementSlot + primitiveSettings.slotSize
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        val currentValueSlot = indexSlot + 1
        storeArrayValue(mv, arraySlot, indexSlot, currentValueSlot)

        val conditionLabel = Label()
        //Jump to condition check
        mv.visitJumpInsn(Opcodes.GOTO, conditionLabel)

        val loopBodyLabel = Label()
        mv.visitLabel(loopBodyLabel)

        //Check if the current value is the element we are looking for
        val elementsNotEqualLabel = Label()
        primitiveSettings.loadPrimitive(mv, currentValueSlot)
        primitiveSettings.loadPrimitive(mv, elementSlot)
        primitiveSettings.jumpNotEquals(mv, elementsNotEqualLabel)

        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitLabel(elementsNotEqualLabel)

        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitInsn(Opcodes.IREM)
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        storeArrayValue(mv, arraySlot, indexSlot, currentValueSlot)

        mv.visitLabel(conditionLabel)
        //Condition check, if not zero, run loop body
        primitiveSettings.loadPrimitive(mv, currentValueSlot)
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpNotEquals(mv, loopBodyLabel)


        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateInsertElementMethod(cw: ClassWriter) {
        val arraySlot = 1
        val elementSlot = 2
        val indexSlot = elementSlot + primitiveSettings.slotSize

        val mv = cw.visitMethod(
            Opcodes.ACC_PRIVATE,
            "insertElement",
            insertElementDescriptor,
            null,
            emptyArray()
        )
        mv.visitCode()

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        primitiveSettings.loadPrimitive(mv, elementSlot)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            fullyQualifiedClassName,
            "getElementIndex",
            getElementIndexDescriptor,
            false
        )
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        val returnFalseLabel = Label()
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadArrayEntry(mv)
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpNotEquals(mv, returnFalseLabel)

        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadPrimitive(mv, elementSlot)
        primitiveSettings.storeInArray(mv)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)


        mv.visitLabel(returnFalseLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }


    private fun generateRehashMethod(cw: ClassWriter) {
        val newArraySlot = 1
        val oldArraySlot = 2
        val oldSizeSlot = 3
        val indexSlot = 4

        val mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "rehash", "()V", null, emptyArray())
        mv.visitCode()

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)
        mv.visitInsn(Opcodes.DUP)
        mv.visitVarInsn(Opcodes.ASTORE, oldArraySlot)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitInsn(Opcodes.DUP)
        mv.visitVarInsn(Opcodes.ISTORE, oldSizeSlot)
        mv.visitInsn(Opcodes.ICONST_2)
        mv.visitInsn(Opcodes.IMUL)
        mv.visitIntInsn(Opcodes.NEWARRAY, primitiveSettings.specializedTypeCode)
        mv.visitVarInsn(Opcodes.ASTORE, newArraySlot)

        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)
        val conditionLabel = Label()
        val loopBodyLabel = Label()
        val loopIncLabel = Label()
        mv.visitJumpInsn(Opcodes.GOTO, conditionLabel)


        mv.visitLabel(loopBodyLabel)
        mv.visitVarInsn(Opcodes.ALOAD, oldArraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadArrayEntry(mv)
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpEquals(mv, loopIncLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitVarInsn(Opcodes.ALOAD, oldArraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadArrayEntry(mv)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            fullyQualifiedClassName,
            "insertElement",
            insertElementDescriptor,
            false
        )
        mv.visitInsn(Opcodes.POP)


        mv.visitLabel(loopIncLabel)
        mv.visitIincInsn(indexSlot, 1)
        mv.visitLabel(conditionLabel)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        mv.visitVarInsn(Opcodes.ILOAD, oldSizeSlot)
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, loopBodyLabel)


        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)
        mv.visitInsn(Opcodes.RETURN)


        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateGetSizeMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getSize", "()I", null, emptyArray())
        mv.visitCode()

        //Load size from object's field and return it
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateContainsMethod(cw: ClassWriter) {
        val boxedValueSlot = 1
        val valueSlot = boxedValueSlot + 1
        val arraySlot = valueSlot + primitiveSettings.slotSize
        val indexSlot = arraySlot + 1

        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "contains", "(Ljava/lang/Object;)Z", null, emptyArray())
        mv.visitCode()

        val elseCaseLabel = Label()
        mv.visitVarInsn(Opcodes.ALOAD, boxedValueSlot)
        primitiveSettings.unboxPrimitive(mv)
        primitiveSettings.storePrimitive(mv, valueSlot)

        primitiveSettings.loadPrimitive(mv, valueSlot)
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpNotEquals(mv, elseCaseLabel)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "containsZero", "Z")
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitLabel(elseCaseLabel)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)
        mv.visitVarInsn(Opcodes.ASTORE, arraySlot)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        primitiveSettings.loadPrimitive(mv, valueSlot)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            fullyQualifiedClassName,
            "getElementIndex",
            getElementIndexDescriptor,
            false
        )
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        primitiveSettings.loadArrayEntry(mv)

        val zeroLabel = Label()
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpEquals(mv, zeroLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitLabel(zeroLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateAddMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "add", "(Ljava/lang/Object;)Z", null, emptyArray())
        mv.visitCode()

        val boxedSlot = 1
        val primitiveSlot = boxedSlot + 1;

        mv.visitVarInsn(Opcodes.ALOAD, boxedSlot)
        primitiveSettings.unboxPrimitive(mv)
        primitiveSettings.storePrimitive(mv, primitiveSlot)

        val notZeroLabel = Label()
        primitiveSettings.loadPrimitive(mv, primitiveSlot)
        primitiveSettings.loadZero(mv)
        primitiveSettings.jumpNotEquals(mv, notZeroLabel)

        val doesNotContainZero = Label()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "containsZero", "Z")
        mv.visitJumpInsn(Opcodes.IFEQ, doesNotContainZero)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitLabel(doesNotContainZero)

        //containsZero = true
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "containsZero", "Z")

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)


        val noRehashLabel = Label()
        mv.visitLabel(notZeroLabel)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, fullyQualifiedClassName, "getLoadFactor", "()F", false)
        mv.visitLdcInsn(0.6f)
        mv.visitInsn(Opcodes.FCMPL)
        mv.visitJumpInsn(Opcodes.IFLE, noRehashLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, fullyQualifiedClassName, "rehash", "()V", false)

        mv.visitLabel(noRehashLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayTypeName)
        primitiveSettings.loadPrimitive(mv, primitiveSlot)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            fullyQualifiedClassName,
            "insertElement",
            insertElementDescriptor,
            false
        )
        val returnFalseLabel = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, returnFalseLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)


        mv.visitLabel(returnFalseLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    /**
     * Generates the bytecode of the specialized implementation of this instance's primitive type.
     * The returned bytecode can either be written to a file, or be loaded directly in a custom class loader.
     */
    fun generateClass(): ByteArray {
        val compactSetClassName = Type.getInternalName(CompactSet::class.java)

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
            fullyQualifiedClassName,
            "Ljava/lang/Object;L$compactSetClassName<L${primitiveSettings.boxedTypeName};>;",
            "java/lang/Object",
            arrayOf(compactSetClassName)
        )

        cw.visitField(Opcodes.ACC_PRIVATE, "containsZero", "Z", null, false)
        cw.visitField(Opcodes.ACC_PRIVATE, "backingArray", specializedArrayTypeName, null, null)
        cw.visitField(Opcodes.ACC_PRIVATE, "size", "I", null, 0)

        generateConstructor(cw)

        //Private functions
        generateGetLoadFactorMethod(cw)
        generateGetElementIndex(cw)
        generateInsertElementMethod(cw)
        generateRehashMethod(cw)

        //Public methods
        generateGetSizeMethod(cw)
        generateContainsMethod(cw)
        generateAddMethod(cw)

        cw.visitEnd()

        return cw.toByteArray()
    }
}