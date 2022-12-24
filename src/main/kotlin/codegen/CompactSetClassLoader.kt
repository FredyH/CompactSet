package com.example.compactset.codegen

/**
 * A classloader that simply allows us to load classes from bytecode at runtime.
 */
internal object CompactSetClassLoader: ClassLoader() {
    fun defineClass(name: String, bytes: ByteArray): Class<*> {
        return defineClass(name, bytes, 0, bytes.size)
    }
}