# CompactSet

A small Kotlin library implementing a set data structure called `CompactSet` that automatically specializes to primitive types
by dynamically generating bytecode for certain primitive types.

On the JVM, generics are usually implemented using type erasure and essentially using `Object` anywhere rather than
a specific type. For primitives, this will require [boxing](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html)
of primitive values, where the value is wrapped in an object.

This incurs significant overhead in both performance and memory usage, so this library offers dynamic bytecode generation
of instances of `CompactSet` that use the concrete types of the primitives when storing them.


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
    │   ├── main                # main package
    │   │   ├── codegen         # classes used to generate specialized bytecode
    │   │   ├── impl            # implementations of the CompactSet interface
    │   ├── test                # package containing unit tests
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