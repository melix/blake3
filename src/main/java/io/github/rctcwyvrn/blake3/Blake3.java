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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static io.github.rctcwyvrn.blake3.internal.DigestUtils.bytesToHex;

public interface Blake3 {
    int DEFAULT_HASH_LEN = 32;

    /**
     * Append the byte contents of the file to the hash tree
     * @param file File to be added
     * @throws IOException If the file does not exist
     */
    default void update(File file) throws IOException {
        // Update the hasher 4kb at a time to avoid memory issues when hashing large files
        try(InputStream ios = new FileInputStream(file)){
            byte[] buffer = new byte[4096];
            int read = 0;
            while((read = ios.read(buffer)) != -1){
                if(read == buffer.length) {
                    update(buffer);
                } else {
                    update(Arrays.copyOfRange(buffer, 0, read));
                }
            }
        }
    }

    /**
     * Appends new data to the hash tree
     * @param input Data to be added
     */
    void update(byte[] input);

    /**
     * Generate the blake3 hash for the current tree with the given byte length
     * @param hashLen The number of bytes of hash to return
     * @return The byte array representing the hash
     */
    byte[] digest(int hashLen);

    /**
     * Generate the blake3 hash for the current tree with the default byte length of 32
     * @return The byte array representing the hash
     */
    default byte[] digest(){
        return digest(DEFAULT_HASH_LEN);
    }

    /**
     * Generate the blake3 hash for the current tree with the given byte length
     * @param hashLen The number of bytes of hash to return
     * @return The hex string representing the hash
     */
    default String hexdigest(int hashLen){
        return bytesToHex(digest(hashLen));
    }

    /**
     * Generate the blake3 hash for the current tree with the default byte length of 32
     * @return The hex string representing the hash
     */
    default String hexdigest(){
        return hexdigest(DEFAULT_HASH_LEN);
    }
}
