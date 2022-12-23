package com.example.compactset

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class SpecializedCompactSetGenerator(
    val className: String,
    private val primitiveSettings: PrimitiveTypeInstructionSettings
) {
    private val setPackage: String = "com/example/compactset"

    val fullyQualifiedClassName = "$setPackage/$className"
    private val specializedArrayName = "[${primitiveSettings.specializedTypeName}"

    private fun generateConstructor(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, emptyArray())

        //Call object super constructor
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

        //Initialize array using argument of method
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ILOAD, 1)
        mv.visitIntInsn(Opcodes.NEWARRAY, primitiveSettings.specializedTypeCode)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)

        mv.visitInsn(Opcodes.RETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateGetSizeMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getSize", "()I", null, emptyArray())
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateContainsMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "contains", "(Ljava/lang/Object;)Z", null, emptyArray())
        mv.visitCode()

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        var currentLocalIndex = 2; //this + boxed parameter

        mv.visitVarInsn(Opcodes.ALOAD, 1)
        primitiveSettings.unboxPrimitive(mv)

        val searchValueSlot = currentLocalIndex
        currentLocalIndex += primitiveSettings.slotSize
        primitiveSettings.storePrimitive(mv, searchValueSlot)


        val indexSlot = currentLocalIndex++
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        val arraySlot = currentLocalIndex++
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitVarInsn(Opcodes.ASTORE, arraySlot)

        //For loop
        val loopConditionLabel = Label()
        val loopEndLabel = Label()
        mv.visitLabel(loopConditionLabel)

        //Load index
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        //Load size of array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        //Compare index to size
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEndLabel)

        //Loop body

        //Load array entry and current entry
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        //Increment index
        mv.visitIincInsn(indexSlot, 1)

        primitiveSettings.loadArrayEntry(mv)
        primitiveSettings.loadPrimitive(mv, searchValueSlot)

        primitiveSettings.jumpNotEquals(mv, loopConditionLabel)

        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)



        //After loop, return false

        mv.visitLabel(loopEndLabel)

        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    private fun generateAddMethod(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "add", "(Ljava/lang/Object;)Z", null, emptyArray())
        mv.visitCode()

        var currentLocalIndex = 2; //this + boxed parameter

        val nonContainedLabel = Label()
        //Check if contained, if so return
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, fullyQualifiedClassName, "contains", "(Ljava/lang/Object;)Z", false)

        mv.visitJumpInsn(Opcodes.IFEQ, nonContainedLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitLabel(nonContainedLabel)

        val sizeSlot = currentLocalIndex++
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitVarInsn(Opcodes.ISTORE, sizeSlot)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)

        val fillArrayLabel = Label()
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, fillArrayLabel)

        //Reallocate array

        //Calculate new size
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitInsn(Opcodes.ICONST_2)
        mv.visitInsn(Opcodes.IMUL)

        mv.visitIntInsn(Opcodes.NEWARRAY, primitiveSettings.specializedTypeCode)
        val newArraySlot = currentLocalIndex++
        mv.visitVarInsn(Opcodes.ASTORE, newArraySlot)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)


        //Add into array
        mv.visitLabel(fillArrayLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "size", "I")

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)
        mv.visitVarInsn(Opcodes.ALOAD, 1) //Load primitive
        primitiveSettings.unboxPrimitive(mv)
        primitiveSettings.storeInArray(mv)


        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

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
        cw.visitField(Opcodes.ACC_PRIVATE, "backingArray", specializedArrayName, null, null)
        cw.visitField(Opcodes.ACC_PRIVATE, "size", "I", null, 0)

        generateConstructor(cw)
        generateGetSizeMethod(cw)
        generateContainsMethod(cw)
        generateAddMethod(cw)

        cw.visitEnd()

        return cw.toByteArray()
    }
}