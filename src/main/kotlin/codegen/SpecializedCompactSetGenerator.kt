package com.example.compactset.codegen

import com.example.compactset.CompactSet
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

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
    private val specializedArrayName = "[${primitiveSettings.specializedTypeName}"

    private fun generateConstructor(cw: ClassWriter) {
        val mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, emptyArray())

        //Call object super constructor
        mv.visitCode()
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

        //Initialize array using size argument of the constructor
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

        //Load size from object's field and return it
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

        //Unbox boxed parameter and store the primitive value as a local variable
        val searchValueSlot = currentLocalIndex
        currentLocalIndex += primitiveSettings.slotSize
        primitiveSettings.storePrimitive(mv, searchValueSlot)

        //Create local variable for index of for loop, initialized at 0
        val indexSlot = currentLocalIndex++
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ISTORE, indexSlot)

        //Suppress in case more code gets added to not forget the incremented index
        @Suppress("UNUSED_CHANGED_VALUE")
        val arraySlot = currentLocalIndex++
        //Store the array in a local variable
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitVarInsn(Opcodes.ASTORE, arraySlot)

        //Start of for loop
        val loopConditionLabel = Label()
        val loopEndLabel = Label()
        mv.visitLabel(loopConditionLabel)

        //Load index
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        //Load size of array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        //Compare index to size, if it is greater or equal we are done
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEndLabel)

        //Loop body

        //Load array entry and current index onto stack
        mv.visitVarInsn(Opcodes.ALOAD, arraySlot)
        mv.visitVarInsn(Opcodes.ILOAD, indexSlot)
        //Also increment index after we have copied the current value to the stack
        mv.visitIincInsn(indexSlot, 1)

        //Load value of array at the current index and the value we are looking for
        primitiveSettings.loadArrayEntry(mv)
        primitiveSettings.loadPrimitive(mv, searchValueSlot)

        //If they are not equal, go back to the start of the foor loop
        primitiveSettings.jumpNotEquals(mv, loopConditionLabel)

        //Elements are equal -> value is contained in set
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IRETURN)


        //Loop fully completed -> value not in set
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

        //Check if contained, if so return false
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, fullyQualifiedClassName, "contains", "(Ljava/lang/Object;)Z", false)

        val nonContainedLabel = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, nonContainedLabel)

        //Element contained -> return false
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitInsn(Opcodes.IRETURN)

        //Element not contained -> add it into the array
        mv.visitLabel(nonContainedLabel)

        //Store current size of the set in a local variable
        val sizeSlot = currentLocalIndex++
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "size", "I")
        mv.visitVarInsn(Opcodes.ISTORE, sizeSlot)

        //Load size of array and current size of set
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)

        //If there is still space in the array, jump to the part where we add the element to the array
        val fillArrayLabel = Label()
        mv.visitJumpInsn(Opcodes.IF_ICMPGT, fillArrayLabel)

        //Array is full -> reallocate array and double its size
        //Calculate new array size (double the current size) and create array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ARRAYLENGTH)
        mv.visitInsn(Opcodes.ICONST_2)
        mv.visitInsn(Opcodes.IMUL)
        mv.visitIntInsn(Opcodes.NEWARRAY, primitiveSettings.specializedTypeCode)

        //Suppress in case more code gets added to not forget the incremented index
        @Suppress("UNUSED_CHANGED_VALUE")
        val newArraySlot = currentLocalIndex++
        //Store new array in a temporary variable
        mv.visitVarInsn(Opcodes.ASTORE, newArraySlot)

        //Load current array and invoke System.arraycopy to copy the old elements to the new array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/System",
            "arraycopy",
            "(Ljava/lang/Object;ILjava/lang/Object;II)V",
            false
        )

        //Store new array into the field of the object
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ALOAD, newArraySlot)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)


        //Add into array
        mv.visitLabel(fillArrayLabel)

        //Add one to the size stored in the object (but size stays the same in the local variable!)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitFieldInsn(Opcodes.PUTFIELD, fullyQualifiedClassName, "size", "I")

        //Store the value at the end of the array
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, fullyQualifiedClassName, "backingArray", specializedArrayName)
        mv.visitVarInsn(Opcodes.ILOAD, sizeSlot)
        mv.visitVarInsn(Opcodes.ALOAD, 1) //Load primitive
        primitiveSettings.unboxPrimitive(mv)
        primitiveSettings.storeInArray(mv)

        //Return true
        mv.visitInsn(Opcodes.ICONST_1)
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