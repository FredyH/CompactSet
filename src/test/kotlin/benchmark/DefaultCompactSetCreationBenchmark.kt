package com.example.compactset.benchmark

import com.example.compactset.CompactSet
import com.example.compactset.newCompactSet
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
open class DefaultCompactSetCreationBenchmark {

    private lateinit var compactSet: CompactSet<String?>
    private lateinit var hashSet: HashSet<String?>

    private lateinit var valueArray: Array<String>

    @Setup
    open fun setup() {
        compactSet = newCompactSet(16)
        hashSet = HashSet(16)
        valueArray = Array(10000) { Random.nextInt().toString() }
    }

    @Benchmark
    open fun benchmarkCompactSet() {
        valueArray.forEach {
            compactSet.add(it)
        }
    }

    @Benchmark
    open fun benchmarkHashSet() {
        valueArray.forEach {
            hashSet.add(it)
        }
    }
}