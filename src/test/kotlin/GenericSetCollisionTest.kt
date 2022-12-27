package com.example.compactset

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class GenericSetCollisionTest : WordSpec({

    class CollisionTestClass {
        override fun hashCode(): Int = 5
        override fun equals(other: Any?): Boolean {
            return this === other
        }
    }

    "Colliding hashCodes" should {
        "work correctly with the default implementation" {
            val set = newCompactSet<CollisionTestClass>()
            val a = CollisionTestClass()
            val b = CollisionTestClass()
            set.add(a).shouldBeTrue()
            set.contains(a).shouldBeTrue()
            set.contains(b).shouldBeFalse()
            set.add(b).shouldBeTrue()
            set.contains(a).shouldBeTrue()
            set.contains(b).shouldBeTrue()
        }

        //All the specialized implementations work the same, so we use Long as an example here
        "work correctly with the Long implementation" {
            //a and b have the same hashCode
            val a = 1L
            val b = Long.MIN_VALUE + Int.MAX_VALUE + 2
            a.hashCode().shouldBe(b.hashCode())
            val set = newCompactSet<Long>()
            set.add(a).shouldBeTrue()
            set.contains(a).shouldBeTrue()
            set.contains(b).shouldBeFalse()
            set.add(b).shouldBeTrue()
            set.contains(a).shouldBeTrue()
            set.contains(b).shouldBeTrue()
        }
    }
})