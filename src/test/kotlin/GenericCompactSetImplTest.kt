package com.example.compactset

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * A generic test implementation.
 * The reason this is generic is that the tests for all the type arguments are exactly the same,
 * they are just different types of values that are stored.
 */
internal abstract class GenericCompactSetImplTest<T> : WordSpec() {
    abstract fun createSet(initialSize: Int = 16): CompactSet<T>

    abstract fun generateRandomValue(): T

    private val seenValues = mutableSetOf<T>()
    private fun getNextDistinctValue(): T {
        val v = generateRandomValue()
        if (seenValues.contains(v)) {
            return getNextDistinctValue()
        }
        seenValues.add(v)
        return v
    }

    abstract val defaultArrayValue: T


    init {
        "The contains method" should {
            "return false if the set is empty" {
                val set = createSet()
                val nonContainedValue = getNextDistinctValue()
                set.contains(nonContainedValue).shouldBeFalse()
            }

            "return true if the set contains a value that was added previously" {
                val set = createSet()
                val value = getNextDistinctValue()

                set.contains(value).shouldBeFalse()
                set.add(value)
                set.contains(value).shouldBeTrue()
            }

            "return false for an empty set's backing array's default value" {
                val set = createSet()
                set.contains(defaultArrayValue).shouldBeFalse()
            }

            "return false for the backing array's default value after adding any amount of elements" {
                val set = createSet()
                for (i in 1..100) {
                    val nextValue = getNextDistinctValue()
                    if (nextValue == defaultArrayValue) continue

                    set.add(nextValue)
                    set.contains(defaultArrayValue).shouldBeFalse()
                    set.contains(nextValue).shouldBeTrue()
                }
            }
        }

        "The add method" should {
            "return false if the element was already in the set and still contain element after" {
                val set = createSet()
                val value = getNextDistinctValue()

                set.contains(value).shouldBeFalse()
                set.add(value).shouldBeTrue()
                set.contains(value).shouldBeTrue()
                set.add(value).shouldBeFalse()
                set.contains(value).shouldBeTrue()
            }

            "not break with 0 initial size" {
                val set = createSet(0)
                for (i in 1..100) {
                    val value = getNextDistinctValue()
                    set.contains(value).shouldBeFalse()
                    set.add(value).shouldBeTrue()
                    set.contains(value).shouldBeTrue()
                }
            }

            "return the correct size after adding elements" {
                val set = createSet()
                val allAddedValues = mutableListOf<T>()
                for (i in 1..10000) {
                    val nextValue = getNextDistinctValue()

                    set.contains(nextValue).shouldBeFalse()
                    set.add(nextValue)
                    set.contains(nextValue).shouldBeTrue()
                    set.size.shouldBe(i)
                    allAddedValues.add(nextValue)
                }

                set.contains(defaultArrayValue).shouldBeFalse()
                for (v in allAddedValues) {
                    set.contains(v).shouldBeTrue()
                }
            }

            "return the correct size after adding elements when containing default value" {
                val set = createSet()
                val allAddedValues = mutableListOf<T>()
                set.add(defaultArrayValue)
                set.size.shouldBe(1)
                for (i in 1..10000) {
                    val nextValue = getNextDistinctValue()

                    set.contains(nextValue).shouldBeFalse()
                    set.add(nextValue)
                    set.contains(nextValue).shouldBeTrue()
                    set.size.shouldBe(i + 1)
                    allAddedValues.add(nextValue)
                }

                set.contains(defaultArrayValue).shouldBeTrue()
                for (v in allAddedValues) {
                    set.contains(v).shouldBeTrue()
                }
            }

            "not increase the size property if trying to add an element that already exists" {
                val set = createSet()
                val nextValue = getNextDistinctValue()
                set.add(nextValue)
                set.size.shouldBe(1)
                set.add(nextValue)
                set.size.shouldBe(1)
            }

            "add the backing array's default value correctly" {
                val set = createSet()
                set.add(defaultArrayValue)
                set.size.shouldBe(1)
                set.contains(defaultArrayValue).shouldBeTrue()
            }

            "return false upon trying to add the default element twice" {
                val set = createSet()
                set.add(defaultArrayValue).shouldBeTrue()
                set.add(defaultArrayValue).shouldBeFalse()
            }
        }

        "The size property" should {
            "return 0 for an empty set" {
                createSet().size.shouldBeZero()
            }
        }
    }
}

internal class DefaultCompactSetImplTest : GenericCompactSetImplTest<String?>() {
    override fun createSet(initialSize: Int): CompactSet<String?> = newCompactSet(initialSize)
    override val defaultArrayValue: String? = null
    override fun generateRandomValue(): String = Random.nextInt().toString()
}

internal class BoxedCompactSetImplTest : GenericCompactSetImplTest<Double?>() {
    override fun createSet(initialSize: Int): CompactSet<Double?> = newCompactSet(initialSize)
    override val defaultArrayValue: Double? = null
    override fun generateRandomValue(): Double = Random.nextDouble()
}


internal class IntCompactSetImplTest : GenericCompactSetImplTest<Int>() {
    override fun createSet(initialSize: Int): CompactSet<Int> = newCompactSet(initialSize)
    override val defaultArrayValue: Int = 0
    override fun generateRandomValue(): Int = Random.nextInt()
}

internal class LongCompactSetImplTest : GenericCompactSetImplTest<Long>() {
    override fun createSet(initialSize: Int): CompactSet<Long> = newCompactSet(initialSize)
    override val defaultArrayValue: Long = 0L
    override fun generateRandomValue(): Long = Random.nextLong()
}

internal class DoubleCompactSetImplTest : GenericCompactSetImplTest<Double>() {
    override fun createSet(initialSize: Int): CompactSet<Double> = newCompactSet(initialSize)
    override val defaultArrayValue: Double = 0.0
    override fun generateRandomValue(): Double = Random.nextDouble()
}