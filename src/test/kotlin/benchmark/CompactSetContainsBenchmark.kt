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
open class CompactSetContainsBenchmark {

    private lateinit var compactSet: CompactSet<Int?>
    private lateinit var specializedCompactSet: CompactSet<Int>
    private lateinit var hashSet: HashSet<Int>

    private lateinit var valueArray: Array<Int>
    private lateinit var nonContainedArray: Array<Int>
    private var counter = 0

    @Setup
    open fun setup() {
        compactSet = newCompactSet(16)
        specializedCompactSet = newCompactSet(16)
        hashSet = HashSet(16)

        valueArray = Array(10000) { it * 3167 }
        nonContainedArray = Array(10000) { it * 7919 }

        valueArray.forEach {
            compactSet.add(it)
            specializedCompactSet.add(it)
            hashSet.add(it)
        }

    }

    @Benchmark
    open fun benchmarkSpecializedCompactSetContained(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(specializedCompactSet.contains(valueArray[counter]))
    }

    @Benchmark
    open fun benchmarkSpecializedCompactSetNotContained(blackhole: Blackhole) {
        counter = (counter + 1) % nonContainedArray.size
        blackhole.consume(specializedCompactSet.contains(nonContainedArray[counter]))
    }

    @Benchmark
    open fun benchmarkCompactSetContained(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(compactSet.contains(valueArray[counter]))
    }

    @Benchmark
    open fun benchmarkCompactSetNotContained(blackhole: Blackhole) {
        counter = (counter + 1) % nonContainedArray.size
        blackhole.consume(compactSet.contains(nonContainedArray[counter]))
    }

    @Benchmark
    open fun benchmarkHashSetContained(blackhole: Blackhole) {
        counter = (counter + 1) % valueArray.size
        blackhole.consume(hashSet.contains(valueArray[counter]))
    }

    @Benchmark
    open fun benchmarkHashSetNotContained(blackhole: Blackhole) {
        counter = (counter + 1) % nonContainedArray.size
        blackhole.consume(hashSet.contains(nonContainedArray[counter]))
    }
}