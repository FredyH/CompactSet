package com.example.compactset

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class FloatingPointWeirdnessTest: WordSpec({
    "A compactSet" should {
        "work correctly with NaN in the default implementation" {
            val set = newCompactSet<Double?>()
            set.contains(Double.NaN).shouldBeFalse()
            set.add(Double.NaN).shouldBeTrue()
            set.contains(Double.NaN).shouldBeTrue()
        }

        "work correctly with NaN in the specialized implementation" {
            val set = newCompactSet<Double>()
            set.contains(Double.NaN).shouldBeFalse()
            set.add(Double.NaN).shouldBeTrue()
            set.contains(Double.NaN).shouldBeTrue()
        }
    }
})