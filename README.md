# CompactSet

A small Kotlin library implementing a set data structure called `CompactSet` that automatically specializes to certain
primitive types by dynamically generating specialized bytecode.

On the JVM, generics are usually implemented using type erasure and essentially using `Object` anywhere rather than
a specific type. For primitives, this will require [boxing](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html)
of primitive values, where the value is wrapped in an object.

This incurs significant overhead in both performance and memory usage, so this library offers dynamic bytecode generation
of instances of `CompactSet` that use the concrete types of the primitives when storing them.

<!-- TOC -->
* [CompactSet](#compactset)
  * [Usage](#usage)
  * [Project Structure](#project-structure)
  * [Building](#building)
  * [Implementation](#implementation)
    * [CompactSet Implementation](#compactset-implementation)
    * [Code Generation DSL](#code-generation-dsl)
  * [Performance Characteristics](#performance-characteristics)
    * [Saved Memory](#saved-memory)
    * [Improved Performance](#improved-performance)
    * [Benchmarks](#benchmarks)
<!-- TOC -->

## Usage

To create an instance of a `CompactSet` you may use the included function
```kotlin
newCompactSet<T>(expectedSize)
```
that will return a specialized and optimized version for the primitive types `Int`, `Long` and `Double`, or
a default implementation that is not specialized for any other type.


## Project Structure

    .
    ├──
    ├── src
    │   ├── main/kotlin         # main package including CompactSet interface
    │   │   ├── codegen         # classes used to generate specialized bytecode
    │   │   ├── impl            # implementations of the CompactSet interface
    │   ├── test/kotlin         # package containing unit tests
    └── ...

## Building

To build this project, simply run gradle either using an installed version of gradle, or by invoking the included
gradle wrapper with `./gradlew` or `./gradlew.bat` depending on your platform.

The command used to build the project is
```
    gradle build
```

This command will both build the library and run the included tests.

If you want to see more detailed output of the tests, you can invoke:
```
    gradle test
```

## Implementation

### CompactSet Implementation

The algorithm used to implement the `CompactSet` is based on hashing, that is, the set has a backing array and the
slot the value will be stored in is based on the hash code of the value.
Collision resolution is implemented using [open addressing with linear probing](https://en.wikipedia.org/wiki/Open_addressing).

This yields the most dense and memory friendly way of implementing a HashSet, which is great for effectively using
the cache, especially when considering the specialized implementations. However, this kind of implementation is
also vulnerable to clusters of values that can degrade the effectively constant performance of this set.

### Code Generation DSL
This project automatically generates bytecode for specialized version of the implementation described above.
To generate the code, a code generation DSL is used that mimics how one writes regular code and emits the 
code as bytecode instructions.

***Important:***
It should be noted, that this generation DSL was only written as far as required to generate the code for the
specialized `CompactSet` classes and is by no means complete. It might also contain several bugs and is certainly
not in any way optimized (for example, variable slots are not reused in different branches).


## Performance Characteristics

### Saved Memory

In this section we will discuss the memory saved by using a specialized implementation rather than
the default one, based on the Int primitive type.

On a 32-bit JVM, a primitive integer is (typically) represented using 4 bytes, while a boxed integer typically takes up
16 bytes (12 bytes object header + 4 bytes value).

Additional to the overhead per value, we also have to consider that primitive integers are stored as values in the
backing array. The boxed objects are all allocated separately on the heap and only the references to the objects
are stored (as values) in the array. Since on the 32 bit JVM, references are also (typically) 4 bytes, the overhead is
consists entirely of the size of the boxed values.

For $n$ boxed integers, this results in a memory overhead of $16 * n$ bytes.

### Improved Performance

Another aspect to consider is the improved memory locality when storing values as primitives in an array.
When we loop through the backing array of the set using primitives, then each value can be compared to the one
we are looking for immediately. This is especially a big factor for the linear probing algorithm used by this implemenation.

When using boxed values, we need to dereference the wrapper in the array and access the field in the object that stores the value.
This will likely incur a cache miss for every such element and requiring going to main memory, an operation several times
slower than accessing a value in the cache (note that the references will still likely hit the cache).

While both this will still yield the same theoretical time complexity, the gains will still be immense,
with the specialized implementation possibly being several times faster than the default one.


### Benchmarks

There are a few benchmarks included in the project using the [JMH](https://github.com/openjdk/jmh) framework. The benchmarks can be run by invoking
```
    gradle testBenchmark
```

Please note that these benchmarks are very simple and do not consider any edge cases such as bad hash functions
and multiple collisions. The JVM's HashSet implementation will likely perform much better under these circumstances due
to using more complicated algorithms involving overflow red-black trees.

All the following benchmarks were performed on an AMD Ryzen 5900X CPU.


Creating a new set of `10000` elements starting with a set of initial size `16`
(one operation is creating the entire set including all elements):

| Implementation        | Performance (ops/s) |
|-----------------------|---------------------|
| HashSet               | 4350                |
| DefaultCompactSet     | 8255                |
| SpecializedCompactSet | 14843               |


Inserting an element into an existing set of size `10000` at the start
each operation will increase the size of the existing set, the set getting larger over time):

| Implementation        | Performance (ops/s) |
|-----------------------|---------------------|
| HashSet               | 8913794             |
| DefaultCompactSet     | 158780814           |
| SpecializedCompactSet | 237234593           |

Checking if an element (that is contained) is contained within an existing set of size `10000`:

| Implementation        | Performance (ops/s) |
|-----------------------|---------------------|
| HashSet               | 166946664           |
| DefaultCompactSet     | 224524307           |
| SpecializedCompactSet | 326134688           |

As can be seen, both the default and specialized implementation are significantly faster at inserting elements
and creating the set, with the specialized implementation being more than 20 times faster than Java's HashSet implementation.
This is likely due to Java's HashSet implementation having complicated data structures that keep a good performance
even in the worst case, but requires a lot of constant time to set up and recreating during rehashing.

However, Java's HashSet implementation performs better for lookups in the default case. The good performance likely stems
from the advanced data structures that Java's HashSet uses, that are quite expensive to set up (hence the bad insert performance) but yield
a very good performance characteristics for lookups. However, even in this case, the specialized implementation is still significantly
faster than the default HashSet.

It should be noted that these benchmarks essentially reflect the best-case scenario for a HashSet.
A scenario where the hash function is bad will likely result in Java's HashSet winning even compared to the specialized
implementations.