package com.example.compactset.benchmark

import com.example.compactset.CompactSet
import com.example.compactset.newCompactSet
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
open class DefaultCompactSetInsertBenchmark {
    private lateinit var compactSet: CompactSet<Int?>
    private lateinit var specializedCompactSet: CompactSet<Int>
    private lateinit var hashSet: HashSet<Int>

    private lateinit var valueArray: Array<Int>

    @Setup
    open fun setup() {
        compactSet = newCompactSet(16)
        specializedCompactSet = newCompactSet(16)
        hashSet = HashSet(16)
        valueArray = Array(1000) { Random.nextInt() }
    }

    @Benchmark
    open fun benchmarkSpecializedCompactSet(blackhole: Blackhole) {
        valueArray.forEach {
            blackhole.consume(specializedCompactSet.add(it))
        }
    }

    @Benchmark
    open fun benchmarkCompactSet(blackhole: Blackhole) {
        valueArray.forEach {
            blackhole.consume(compactSet.add(it))
        }
    }

    @Benchmark
    open fun benchmarkHashSet(blackhole: Blackhole) {
        valueArray.forEach {
            blackhole.consume(hashSet.add(it))
        }
    }
}