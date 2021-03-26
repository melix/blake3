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

import io.lktk.NativeBLAKE3;
import io.lktk.NativeBLAKE3Util;

public class NativeBlake3 implements Blake3 {
    private final NativeBLAKE3 blake3;

    public NativeBlake3() {
        blake3 = new NativeBLAKE3();
        blake3.initDefault();
    }

    public NativeBlake3(byte[] key) {
        this();
        blake3.initKeyed(key);
    }

    public NativeBlake3(String context) {
        this();
        blake3.initDeriveKey(context);
    }


    @Override
    public void update(byte[] input) {
        blake3.update(input);
    }

    @Override
    public byte[] digest(int hashLen) {
        try {
            return blake3.getOutput(hashLen);
        } catch (NativeBLAKE3Util.InvalidNativeOutput invalidNativeOutput) {
            throw new RuntimeException(invalidNativeOutput);
        }
    }
}
