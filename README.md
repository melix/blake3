BLAKE3 in Java
---
An unoptimized translation of the blake3 reference implementation from rust to java.

In addition, it contains an unoptimized vectorized version of the blake3 implementation for Java 16.

### Maven
```xml
<dependency>
  <groupId>io.github.rctcwyvrn</groupId>
  <artifactId>blake3</artifactId>
  <version>1.3</version>
</dependency>
```
### Examples
```java
        // Hashing bytes
        Blake3 hasher = new DefaultBlake3Factory().newInstance();
        hasher.update("This is a string".getBytes());
        String hexhash = hasher.hexdigest();
```
```java
        // Hashing files
        Blake3 hasher = new DefaultBlake3Factory().newInstance();
        hasher.update(new File(filename));
        String filehash = hasher.hexdigest();
```

If what you want are java bindings for the fully optimized blake3, try: https://github.com/sken77/BLAKE3jni

### Building

To test the standard implementation, run

`./gradlew test`

To test the vectorized implementation, run:

`./gradlew java16Test`

To test all versions, run:

`./gradlew check`

### Benchmark

A benchmark compares 4 different hashers:

- the base Java MD5 hasher
- the reference Java implementation of blake3 provided by this project
- the vectorized Java implementation of blake3 provided by this project
- the fully optimized blake3 implementation with native bindings

To run the benchmark, run:

`./gradlew jmh`

Example results:

```
Benchmark                                  Mode  Cnt       Score       Error  Units
Blake3Benchmark.md5                       thrpt   15   95875.769 ±   564.301  ops/s
Blake3Benchmark.nativeImplementation      thrpt   15  553494.416 ± 18600.092  ops/s
Blake3Benchmark.referenceImplementation   thrpt   15   40618.199 ±   726.869  ops/s
Blake3Benchmark.vectorizedImplementation  thrpt   15    6206.087 ±    73.795  ops/s
```
