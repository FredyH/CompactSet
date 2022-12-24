package com.example.compactset

import com.example.compactset.impl.DefaultCompactSetImpl
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NewCompactSetTest : WordSpec({
    "The newCompactSet function" should {
        "return the default implementation for an object type" {
            newCompactSet<String>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
        }

        "return the default implementation for a nullable object type" {
            newCompactSet<String?>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
        }

        "return the default implementation for a unsupported primitive types" {
            newCompactSet<Char>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Byte>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Float>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Short>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Boolean>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
        }

        "return the default implementation for a nullable primitive type" {
            newCompactSet<Int?>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Long?>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Double?>().javaClass.shouldBe(DefaultCompactSetImpl::class.java)
        }

        "return specialized implementations for supported primitive types" {
            newCompactSet<Int>().javaClass.shouldNotBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Long>().javaClass.shouldNotBe(DefaultCompactSetImpl::class.java)
            newCompactSet<Double>().javaClass.shouldNotBe(DefaultCompactSetImpl::class.java)
        }

        "work for different sizes" {
            shouldNotThrowAny {
                for (i in 1..100) {
                    newCompactSet<Int>(i)
                    newCompactSet<Int?>(i)
                }
            }
        }
    }
})