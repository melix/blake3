/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rctcwyvrn.blake3;

import io.github.rctcwyvrn.blake3.internal.DefaultBlake3Factory;
import io.github.rctcwyvrn.blake3.internal.VectorizedBlake3Factory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@BenchmarkMode(Mode.Throughput)
public class Blake3Benchmark {
    private final static Blake3Factory REFERENCE_IMPLEMENTATION = new DefaultBlake3Factory();
    private final static Blake3Factory VECTORIZED_IMPLEMENTATION = new VectorizedBlake3Factory();
    private final static Blake3Factory NATIVE_IMPLEMENTATION = new NativeBlake3Factory();

    @State(Scope.Benchmark)
    public static class TestData {
        final Random random = new Random(123456);
        final byte[] array = new byte[65536];

        public TestData() {
            random.nextBytes(array);
        }
    }

    @Benchmark
    public void md5(TestData state, Blackhole bh) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("md5");
        md5.update(state.array);
        bh.consume(md5.digest());
    }

    @Benchmark
    public void referenceImplementation(TestData state, Blackhole bh) {
        Blake3 blake3 = REFERENCE_IMPLEMENTATION.newInstance();
        blake3.update(state.array);
        bh.consume(blake3.digest());
    }

    @Benchmark
    public void vectorizedImplementation(TestData state, Blackhole bh) {
        Blake3 blake3 = VECTORIZED_IMPLEMENTATION.newInstance();
        blake3.update(state.array);
        bh.consume(blake3.digest());
    }

    @Benchmark
    public void nativeImplementation(TestData state, Blackhole bh) {
        Blake3 blake3 = NATIVE_IMPLEMENTATION.newInstance();
        blake3.update(state.array);
        bh.consume(blake3.digest());
    }
}
