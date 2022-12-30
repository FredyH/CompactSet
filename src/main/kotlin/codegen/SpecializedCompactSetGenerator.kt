package com.example.compactset.codegen

import com.example.compactset.CompactSet
import com.example.compactset.codegen.dsl.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * A class responsible for generating bytecode of a specialized implementation of a [CompactSet].
 * Requires defining methods and settings for dealing with the type at hand.
 *
 * The structure of the specialized classes follows very closely to the implementation given
 * by the default implementation, but instead it uses primitive arrays to achieve higher performance and
 * lower memory usage.
 */
internal class SpecializedCompactSetGenerator(
    val className: String,
    private val primitiveType: JVMType,
    private val primitiveArrayType: JVMArrayType,
    private val boxedType: JVMType,
    private val primitiveHashCodeFunction: MethodSignature,
    private val unboxMethod: MethodSignature,
    private val zeroConstant: Any
) {
    private val setPackage: String = "com/example/compactset"

    private val fullyQualifiedClassName = "$setPackage/$className"


    private val backingArray =
        ClassField(fullyQualifiedClassName, "backingArray", primitiveArrayType, Opcodes.ACC_PRIVATE)
    private val size = ClassField(fullyQualifiedClassName, "size", IntType, Opcodes.ACC_PRIVATE)
    private val containsZero = ClassField(fullyQualifiedClassName, "containsZero", BooleanType, Opcodes.ACC_PRIVATE)

    private val mathMax =
        MethodSignature("java/lang/Math", "max", listOf(IntType, IntType), IntType, Opcodes.ACC_STATIC)

    private val floorMod =
        MethodSignature("java/lang/Math", "floorMod", listOf(IntType, IntType), IntType, Opcodes.ACC_STATIC)

    private val superConstructor =
        MethodSignature("java/lang/Object", "<init>", listOf(), VoidType, Opcodes.ACC_PUBLIC)

    private val constructor =
        MethodSignature(fullyQualifiedClassName, "<init>", listOf(IntType), VoidType, Opcodes.ACC_PUBLIC)

    private val getLoadFactor =
        MethodSignature(fullyQualifiedClassName, "getLoadFactor", listOf(), FloatType, Opcodes.ACC_PRIVATE)

    private val getSize =
        MethodSignature(fullyQualifiedClassName, "getSize", listOf(), IntType, Opcodes.ACC_PUBLIC)


    private val getElementIndex = MethodSignature(
        owner = fullyQualifiedClassName,
        name = "getElementIndex",
        parameters = listOf(primitiveArrayType, primitiveType),
        returnType = IntType,
        flags = Opcodes.ACC_PRIVATE
    )

    private val insertElement = MethodSignature(
        owner = fullyQualifiedClassName,
        name = "insertElement",
        parameters = listOf(primitiveArrayType, primitiveType),
        returnType = BooleanType,
        flags = Opcodes.ACC_PRIVATE
    )

    private val rehash = MethodSignature(
        owner = fullyQualifiedClassName,
        name = "rehash",
        parameters = emptyList(),
        returnType = VoidType,
        flags = Opcodes.ACC_PRIVATE
    )

    private val contains = MethodSignature(
        owner = fullyQualifiedClassName,
        name = "contains",
        parameters = listOf(ObjectType("java/lang/Object")),
        returnType = BooleanType,
        flags = Opcodes.ACC_PUBLIC
    )

    private val add = MethodSignature(
        owner = fullyQualifiedClassName,
        name = "add",
        parameters = listOf(ObjectType("java/lang/Object")),
        returnType = BooleanType,
        flags = Opcodes.ACC_PUBLIC
    )

    private fun generateConstructor(cw: ClassWriter) {
        cw.generateMethod(constructor) {
            val sizeParam = declareParameter(IntType)

            `this`[superConstructor].invokeStatement()

            sizeParam `=` mathMax(sizeParam, 4.asConstant())
            `this`[backingArray] `=` newArray(primitiveType, sizeParam)
            `this`[size] `=` 0.asConstant()
        }
    }

    private fun generateGetLoadFactorMethod(cw: ClassWriter) {
        cw.generateMethod(getLoadFactor) {
            val sizeWithoutNull = declareVariable(IntType)

            `if`(`this`[containsZero]) {
                sizeWithoutNull `=` `this`[size] - 1.asConstant()
            } `else` {
                sizeWithoutNull `=` `this`[size]
            }

            `return`(sizeWithoutNull.castToFloat() / `this`[backingArray].arrayLength().castToFloat())
        }
    }

    private fun generateGetElementIndex(cw: ClassWriter) {
        cw.generateMethod(getElementIndex) {
            val arrayParam = declareParameter(primitiveArrayType)
            val elementParam = declareParameter(primitiveType)
            val indexVariable = declareVariable(IntType)
            val currentValue = declareVariable(primitiveType)

            indexVariable `=` floorMod(primitiveHashCodeFunction(elementParam), arrayParam.arrayLength())
            currentValue `=` arrayParam[indexVariable]

            `while`(currentValue.neq(zeroConstant.asConstant())) {
                `if`(currentValue.eq(elementParam)) {
                    `return`(indexVariable)
                }
                indexVariable `=` (indexVariable + 1.asConstant()) % arrayParam.arrayLength()
                currentValue `=` arrayParam[indexVariable]
            }
            `return`(indexVariable)
        }
    }

    private fun generateInsertElementMethod(cw: ClassWriter) {
        cw.generateMethod(insertElement) {
            val arrayParam = declareParameter(primitiveArrayType)
            val elementParam = declareParameter(primitiveType)
            val indexVariable = declareVariable(IntType)

            indexVariable `=` `this`[getElementIndex](arrayParam, elementParam)
            `if`(arrayParam[indexVariable].neq(zeroConstant.asConstant())) {
                `return`(false.asConstant())
            }

            arrayParam[indexVariable] = elementParam
            `return`(true.asConstant())
        }
    }


    private fun generateRehashMethod(cw: ClassWriter) {
        cw.generateMethod(rehash) {
            val newArray = declareVariable(primitiveArrayType)
            val currentElem = declareVariable(primitiveType)

            newArray `=` newArray(primitiveType, `this`[backingArray].arrayLength() * 2.asConstant())

            forEach(currentElem, `this`[backingArray]) {
                `if`(currentElem.eq(zeroConstant.asConstant())) {
                    `continue`()
                }
                `this`[insertElement].invokeStatement(newArray, currentElem)
            }

            `this`[backingArray] `=` newArray
        }
    }

    private fun generateGetSizeMethod(cw: ClassWriter) {
        cw.generateMethod(getSize) {
            `return`(`this`[size])
        }
    }

    private fun generateContainsMethod(cw: ClassWriter) {
        cw.generateMethod(contains) {
            val boxedParam = declareParameter(ObjectType("java/lang/Object"))
            val primitiveVar = declareVariable(primitiveType)

            primitiveVar `=` unboxMethod(boxedParam.objectCast(boxedType))
            `if`(primitiveVar.eq(zeroConstant.asConstant())) {
                `return`(`this`[containsZero])
            }

            val indexParam = declareVariable(IntType)
            indexParam `=` `this`[getElementIndex](`this`[backingArray], primitiveVar)

            `return`(`this`[backingArray][indexParam].neq(zeroConstant.asConstant()))
        }
    }

    private fun generateAddMethod(cw: ClassWriter) {
        cw.generateMethod(add) {
            val boxedParam = declareParameter(ObjectType("java/lang/Object"))
            val primitiveValue = declareVariable(primitiveType)

            primitiveValue `=` unboxMethod(boxedParam.objectCast(boxedType))
            `if`(primitiveValue.eq(zeroConstant.asConstant())) {
                `if`(`this`[containsZero]) {
                    `return`(false.asConstant())
                }
                `this`[containsZero] `=` true.asConstant()
                `this`[size] `=` `this`[size] + 1.asConstant()
                `return`(true.asConstant())
            }

            `if`(`this`[getLoadFactor]().gt(0.6f.asConstant())) {
                `this`[rehash].invokeStatement()
            }

            `if`(!`this`[insertElement](`this`[backingArray], primitiveValue)) {
                `return`(false.asConstant())
            }
            `this`[size] `=` `this`[size] + 1.asConstant()

            `return`(true.asConstant())
        }
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
            "Ljava/lang/Object;L$compactSetClassName<${boxedType.typeName}>;",
            "java/lang/Object",
            arrayOf(compactSetClassName)
        )

        containsZero.addToClass(cw)
        backingArray.addToClass(cw)
        size.addToClass(cw)

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