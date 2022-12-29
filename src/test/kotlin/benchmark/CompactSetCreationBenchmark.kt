package com.example.compactset.benchmark

import com.example.compactset.newCompactSet
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
open class CompactSetCreationBenchmark {
    private lateinit var valueArray: Array<Int>

    @Setup
    open fun setup() {
        //Generate dynamically generated implementations
        newCompactSet<Int>(16)
        newCompactSet<Int?>(16)
        valueArray = Array(10000) { it * 3167 }
    }

    @Benchmark
    open fun benchmarkSpecializedCompactSet(blackhole: Blackhole) {
        val specializedCompactSet = newCompactSet<Int>(16)
        valueArray.forEach {
            blackhole.consume(specializedCompactSet.add(it))
        }
        blackhole.consume(specializedCompactSet)
    }

    @Benchmark
    open fun benchmarkCompactSet(blackhole: Blackhole) {
        val compactSet = newCompactSet<Int?>(16)
        valueArray.forEach {
            blackhole.consume(compactSet.add(it))
        }
        blackhole.consume(compactSet)
    }

    @Benchmark
    open fun benchmarkHashSet(blackhole: Blackhole) {
        val hashSet = HashSet<Int?>(16)
        valueArray.forEach {
            blackhole.consume(hashSet.add(it))
        }
        blackhole.consume(hashSet)
    }
}