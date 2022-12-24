package com.example.compactset

import com.example.compactset.codegen.DoubleInstructionSettings
import com.example.compactset.codegen.IntInstructionSettings
import com.example.compactset.codegen.LongInstructionSettings
import com.example.compactset.codegen.SpecializedCompactSetGenerator
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter


//Just a file containing debug functions to write the specialized classes to a file.
//This allows us to look at the decompiled version in IntelliJ to verify that the code looks correctly.
private fun generateClass() {
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