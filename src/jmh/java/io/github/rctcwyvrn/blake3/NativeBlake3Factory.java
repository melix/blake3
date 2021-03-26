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

public class NativeBlake3Factory implements Blake3Factory {
    @Override
    public Blake3 newInstance() {
        return new NativeBlake3();
    }

    @Override
    public Blake3 newKeyedHasher(byte[] key) {
        return new NativeBlake3(key);
    }

    @Override
    public Blake3 newKeyDerivationHasher(String context) {
        return new NativeBlake3(context);
    }
}
