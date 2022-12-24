package com.example.compactset

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * A generic test implementation.
 * The reason this is generic is that the tests for all the type arguments are exactly the same,
 * they are just different types of values that are stored.
 */
internal abstract class GenericCompactSetImplTest<T> : WordSpec() {
    abstract fun createSet(): CompactSet<T>

    abstract fun generateNextDistinctValue(): T

    abstract val defaultArrayValue: T


    init {
        "The contains method" should {
            "return false if the set is empty" {
                val set = createSet()
                val nonContainedValue = generateNextDistinctValue()
                set.contains(nonContainedValue).shouldBeFalse()
            }

            "return true if the set contains a value that was added previously" {
                val set = createSet()
                val value = generateNextDistinctValue()

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
                    val nextValue = generateNextDistinctValue()
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
                val value = generateNextDistinctValue()

                set.contains(value).shouldBeFalse()
                set.add(value).shouldBeTrue()
                set.contains(value).shouldBeTrue()
                set.add(value).shouldBeFalse()
                set.contains(value).shouldBeTrue()
            }

            "return the correct size after adding elements" {
                val set = createSet()
                for (i in 1..10000) {
                    val nextValue = generateNextDistinctValue()

                    set.add(nextValue)
                    set.size.shouldBe(i)
                }
            }

            "not increase the size property if trying to add an element that already exists" {
                val set = createSet()
                val nextValue = generateNextDistinctValue()
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
        }

        "The size property" should {
            "return 0 for an empty set" {
                createSet().size.shouldBeZero()
            }
        }
    }
}

internal class DefaultCompactSetImplTest : GenericCompactSetImplTest<String?>() {
    override fun createSet(): CompactSet<String?> = newCompactSet()
    override val defaultArrayValue: String? = null

    private val counter = AtomicInteger(0)
    override fun generateNextDistinctValue(): String = counter.incrementAndGet().toString()
}

internal class BoxedCompactSetImplTest : GenericCompactSetImplTest<Double?>() {
    override fun createSet(): CompactSet<Double?> = newCompactSet()
    override val defaultArrayValue: Double? = null

    private val counter = AtomicInteger(0)
    override fun generateNextDistinctValue(): Double = counter.incrementAndGet().toDouble()
}


internal class IntCompactSetImplTest : GenericCompactSetImplTest<Int>() {
    override fun createSet(): CompactSet<Int> = newCompactSet()
    override val defaultArrayValue: Int = 0

    private val counter = AtomicInteger(1)
    override fun generateNextDistinctValue(): Int = counter.incrementAndGet()
}

internal class LongCompactSetImplTest : GenericCompactSetImplTest<Long>() {
    override fun createSet(): CompactSet<Long> = newCompactSet()
    override val defaultArrayValue: Long = 0L

    private val counter = AtomicLong(1)
    override fun generateNextDistinctValue(): Long = counter.incrementAndGet()
}

internal class DoubleCompactSetImplTest : GenericCompactSetImplTest<Double>() {
    override fun createSet(): CompactSet<Double> = newCompactSet()
    override val defaultArrayValue: Double = 0.0

    private val counter = AtomicLong(1)
    override fun generateNextDistinctValue(): Double = counter.incrementAndGet().toDouble()
}