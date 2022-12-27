# CompactSet

A small Kotlin library implementing a set data structure called `CompactSet` that automatically specializes to certain
primitive types by dynamically generating specialized bytecode.

On the JVM, generics are usually implemented using type erasure and essentially using `Object` anywhere rather than
a specific type. For primitives, this will require [boxing](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html)
of primitive values, where the value is wrapped in an object.

This incurs significant overhead in both performance and memory usage, so this library offers dynamic bytecode generation
of instances of `CompactSet` that use the concrete types of the primitives when storing them.

Currently, both operations of the `CompactSet` are implemented the "naive" way and, in the worst case,
will take time linear in the amount of elements in the set to complete. 

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

## Performance Characteristics

### Saved Memory

In this section we will discuss the memory saved by using a specialized implementation based on the Int primitive type.

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
we are looking for immediately.

When using boxed values, we need to dereference the wrapper in the array and access the field in the object that stores the value.
This will likely incur a cache miss for every such element and requiring going to main memory, an operation several times
slower than accessing a value in the cache (note that the references will still likely hit the cache).

While both this will still yield the same theoretical time complexity, the gains will still be immense,
with the specialized implementation possibly being several times faster than the default one.