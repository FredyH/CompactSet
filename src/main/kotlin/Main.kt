package com.example.compactset

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter


//Just a file containing debug functions
fun generateClass() {
    val generators = listOf(
        SpecializedCompactSetGenerator(
            className = "IntCompactSetImpl",
            primitiveSettings = IntInstructionSettings
        ),
        SpecializedCompactSetGenerator(
            className = "LongCompactSetImpl",
            primitiveSettings = LongInstructionSettings
        ),
        SpecializedCompactSetGenerator(
            className = "DoubleCompactSetImpl",
            primitiveSettings = DoubleInstructionSettings
        ),
    )

    for (generator in generators) {
        val arr = generator.generateClass()
        File("${generator.className}.class").writeBytes(arr)
        val cr = ClassReader(arr)
        val sw = StringWriter()
        cr.accept(TraceClassVisitor(PrintWriter(sw)), 0)

        println(sw.toString())
    }
}

fun main() {
    generateClass()
}