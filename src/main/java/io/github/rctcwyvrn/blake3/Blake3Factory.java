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

import io.github.rctcwyvrn.blake3.internal.DefaultBlake3;

public interface Blake3Factory {
    /**
     * Construct a BLAKE3 blake3 hasher
     */
    default Blake3 newInstance() {
        return new DefaultBlake3();
    }

    /**
     * Construct a new BLAKE3 keyed mode hasher
     *
     * @param key The 32 byte key
     * @throws IllegalStateException If the key is not 32 bytes
     */
    default Blake3 newKeyedHasher(byte[] key) {
        return new DefaultBlake3(key);
    }

    /**
     * Construct a new BLAKE3 key derivation mode hasher
     * The context string should be hardcoded, globally unique, and application-specific. <br><br>
     * A good default format is <i>"[application] [commit timestamp] [purpose]"</i>, <br>
     * eg "example.com 2019-12-25 16:18:03 session tokens v1"
     *
     * @param context Context string used to derive keys.
     */
    default Blake3 newKeyDerivationHasher(String context) {
        return new DefaultBlake3(context);
    }
}
