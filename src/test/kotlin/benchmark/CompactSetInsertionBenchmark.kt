package com.example.compactset.benchmark

import com.example.compactset.CompactSet
import com.example.compactset.newCompactSet
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
open class CompactSetInsertionBenchmark {
    private lateinit var compactSet: CompactSet<Int?>
    private lateinit var specializedCompactSet: CompactSet<Int>
    private lateinit var hashSet: HashSet<Int>
    private var counter = 0

    private lateinit var valueArray: Array<Int>

    @Setup
    open fun setup() {
        compactSet = newCompactSet(16)
        specializedCompactSet = newCompactSet(16)
        hashSet = HashSet(16)

        for (i in 1..10000) {
            val v = i * 3167
            compactSet.add(v)
            specializedCompactSet.add(v)
            hashSet.add(v)
        }

        valueArray = Array(1000000) { it * 3727 }
    }

    @Benchmark
    open fun benchmarkSpecializedCompactSet(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(specializedCompactSet.add(valueArray[counter]))
    }

    @Benchmark
    open fun benchmarkCompactSet(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(compactSet.add(valueArray[counter]))
    }

    @Benchmark
    open fun benchmarkHashSet(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(hashSet.add(valueArray[counter]))
    }
}