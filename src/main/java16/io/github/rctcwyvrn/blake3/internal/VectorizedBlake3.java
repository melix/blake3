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
package io.github.rctcwyvrn.blake3.internal;

import io.github.rctcwyvrn.blake3.Blake3;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VectorizedBlake3 implements Blake3 {
    private static final VectorSpecies<Integer> PREFERRED = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> V_128 = IntVector.SPECIES_128;

    private static final int OUT_LEN = 32;
    private static final int KEY_LEN = 32;
    private static final int BLOCK_LEN = 64;
    private static final int CHUNK_LEN = 1024;

    private static final int CHUNK_START = 1;
    private static final int CHUNK_END = 2;
    private static final int PARENT = 4;
    private static final int ROOT = 8;
    private static final int KEYED_HASH = 16;
    private static final int DERIVE_KEY_CONTEXT = 32;
    private static final int DERIVE_KEY_MATERIAL = 64;

    private static final VectorMask<Integer> FIRST_4_INTS = VectorMask.fromArray(V_128, new boolean[]{
            true, true, true, true,
            false, false, false, false,
            false, false, false, false,
            false, false, false, false
    }, 0);

    private static final int[] IV = {
            0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19
    };

    private static final int[] MSG_PERMUTATION = {
            2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8
    };

    // (x >>> len) | (x << (32 - len));
    private static IntVector rotateRight(IntVector x, int len) {
        IntVector left = x.lanewise(VectorOperators.LSHR, len);
        IntVector right = x.lanewise(VectorOperators.LSHL, 32 - len);
        return left.or(right);
    }

    private static void g(IntVector[] rows, IntVector mx, IntVector my) {
        rows[0] = wrappingAdd(wrappingAdd(rows[0], rows[1]), mx);
        rows[3] = rotateRight(xor(rows[3], rows[0]), 16);
        rows[2] = wrappingAdd(rows[2], rows[3]);
        rows[1] = rotateRight(xor(rows[1], rows[2]), 12);
        rows[0] = wrappingAdd(wrappingAdd(rows[0], rows[1]), my);
        rows[3] = rotateRight(xor(rows[3], rows[0]), 8);
        rows[2] = wrappingAdd(rows[2], rows[3]);
        rows[1] = rotateRight(xor(rows[1], rows[2]), 7);
    }

    private static IntVector xor(IntVector a, IntVector b) {
        return a.lanewise(VectorOperators.XOR, b);
    }

    private static IntVector wrappingAdd(IntVector a, IntVector b) {
        return a.add(b);
    }

    private static void roundFn(IntVector[] rows, int[] m, int[] buffer) {
        int[] mxa = {
                m[0],
                m[2],
                m[4],
                m[6],
                m[8],
                m[10],
                m[12],
                m[14]
        };
        int[] mya = {
                m[1],
                m[3],
                m[5],
                m[7],
                m[9],
                m[11],
                m[13],
                m[15]
        };
        IntVector mx = IntVector.fromArray(V_128, mxa, 0);
        IntVector my = IntVector.fromArray(V_128, mya, 0);
        g(rows, mx, my);
        mx = IntVector.fromArray(V_128, mxa, 4);
        my = IntVector.fromArray(V_128, mya, 4);
        diagnonalize(rows, buffer);
        g(rows, mx, my);
        undiagnonalize(rows, buffer);
    }

    private static void diagnonalize(IntVector[] rows, int[] buffer) {
        linearize(rows, buffer);
        rows[0] = IntVector.fromArray(V_128, new int[]{buffer[0], buffer[1], buffer[2], buffer[3]}, 0);
        rows[1] = IntVector.fromArray(V_128, new int[]{buffer[5], buffer[6], buffer[7], buffer[4]}, 0);
        rows[2] = IntVector.fromArray(V_128, new int[]{buffer[10], buffer[11], buffer[8], buffer[9]}, 0);
        rows[3] = IntVector.fromArray(V_128, new int[]{buffer[15], buffer[12], buffer[13], buffer[14]}, 0);
    }

    private static void undiagnonalize(IntVector[] rows, int[] buffer) {
        linearize(rows, buffer);
        rows[0] = IntVector.fromArray(V_128, new int[]{buffer[0], buffer[1], buffer[2], buffer[3]}, 0);
        rows[1] = IntVector.fromArray(V_128, new int[]{buffer[7], buffer[4], buffer[5], buffer[6]}, 0);
        rows[2] = IntVector.fromArray(V_128, new int[]{buffer[10], buffer[11], buffer[8], buffer[9]}, 0);
        rows[3] = IntVector.fromArray(V_128, new int[]{buffer[13], buffer[14], buffer[15], buffer[12]}, 0);
    }

    private static int[] permute(int[] m) {
        int[] permuted = new int[16];
        for (int i = 0; i < 16; i++) {
            permuted[i] = m[MSG_PERMUTATION[i]];
        }
        return permuted;
    }

    private static int[] stateMatrix(int[] chainingValue, long counter, int blockLen, int flags) {
        int counterInt = (int) (counter & 0xffffffffL);
        int counterShift = (int) ((counter >> 32) & 0xffffffffL);
        return new int[]{
                chainingValue[0],
                chainingValue[1],
                chainingValue[2],
                chainingValue[3],
                chainingValue[4],
                chainingValue[5],
                chainingValue[6],
                chainingValue[7],
                IV[0],
                IV[1],
                IV[2],
                IV[3],
                counterInt,
                counterShift,
                blockLen,
                flags
        };
    }

    private static int[] simdCompress(int[] chainingValue, int[] blockWords, long counter, int blockLen, int flags) {
        int[] buffer = stateMatrix(chainingValue, counter, blockLen, flags);
        IntVector[] rows = {
                IntVector.fromArray(V_128, buffer, 0, FIRST_4_INTS),
                IntVector.fromArray(V_128, buffer, 4, FIRST_4_INTS),
                IntVector.fromArray(V_128, buffer, 8, FIRST_4_INTS),
                IntVector.fromArray(V_128, buffer, 12, FIRST_4_INTS),
        };
        roundFn(rows, blockWords, buffer);         // Round 1
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 2
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 3
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 4
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 5
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 6
        blockWords = permute(blockWords);
        roundFn(rows, blockWords, buffer);         // Round 7

        linearize(rows, buffer);
        for (int i = 0; i < 8; i++) {
            buffer[i] ^= buffer[i + 8];
            buffer[i + 8] ^= chainingValue[i];
        }
        return buffer;
    }

    private static void linearize(IntVector[] columns, int[] buffer) {
        for (int i = 0; i < columns.length; i++) {
            columns[i].intoArray(buffer, 4 * i);
        }
    }

    private static int[] wordsFromLEBytes(byte[] bytes) {
        return wordsFromLEBytes(bytes, 0, bytes.length);
    }

    private static int[] wordsFromLEBytes(byte[] bytes, int offset, int len) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes)
                .slice(offset, len)
                .order(ByteOrder.LITTLE_ENDIAN);
        int wc = Math.max(len / 4, 16);
        int canTake = len / 4;
        int[] words = new int[wc];
        int i = 0;
        while (i < canTake) {
            words[i++] = buffer.getInt();
        }
        if (buffer.hasRemaining()) {
            ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            while (buffer.hasRemaining()) {
                buf.put(buffer.get());
            }
            words[i] = buf.getInt(0);
        }
        return words;
    }

    // Node of the Blake3 hash tree
    // Is either chained into the next node using chainingValue()
    // Or used to calculate the hash digest using rootOutputBytes()
    private static class Node {
        int[] inputChainingValue;
        int[] blockWords;
        long counter;
        int blockLen;
        int flags;

        private Node(int[] inputChainingValue, int[] blockWords, long counter, int blockLen, int flags) {
            this.inputChainingValue = inputChainingValue;
            this.blockWords = blockWords;
            this.counter = counter;
            this.blockLen = blockLen;
            this.flags = flags;
        }

        // Return the 8 int CV
        private int[] chainingValue() {
            return Arrays.copyOfRange(
                    simdCompress(inputChainingValue, blockWords, counter, blockLen, flags),
                    0, 8);
        }

        private byte[] rootOutputBytes(int outLen) {
            int outputCounter = 0;
            int outputsNeeded = Math.floorDiv(outLen, (2 * OUT_LEN)) + 1;
            byte[] hash = new byte[outLen];
            int i = 0;
            while (outputCounter < outputsNeeded) {
                int[] words = simdCompress(inputChainingValue, blockWords, outputCounter, blockLen, flags | ROOT);

                for (int word : words) {
                    for (byte b : ByteBuffer.allocate(4)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(word)
                            .array()) {
                        hash[i] = b;
                        i++;
                        if (i == outLen) {
                            return hash;
                        }
                    }
                }
                outputCounter++;
            }
            throw new IllegalStateException("Uh oh something has gone horribly wrong. Please create an issue on https://github.com/rctcwyvrn/blake3");
        }
    }

    // Helper object for creating new Nodes and chaining them
    private static class ChunkState {
        private static final byte[] ZERO = new byte[0];
        byte[] input = ZERO;
        int[] chainingValue;
        long chunkCounter;
        int offset;
        byte blockLen = 0;
        byte blocksCompressed = 0;
        int flags;

        public ChunkState(int[] key, long chunkCounter, int flags) {
            this.chainingValue = key;
            this.chunkCounter = chunkCounter;
            this.flags = flags;
        }

        public int len() {
            return BLOCK_LEN * blocksCompressed + blockLen;
        }

        private int startFlag() {
            return blocksCompressed == 0 ? CHUNK_START : 0;
        }

        private void update(byte[] input, int offset, int len) {
            this.offset = offset;
            this.input = input;
            int currPos = 0;
            while (currPos < len) {
                // Chain the next 64 byte block into this chunk/node
                if (blockLen == BLOCK_LEN) {
                    int[] blockWords = wordsFromLEBytes(input, this.offset, blockLen);
                    this.chainingValue = Arrays.copyOfRange(
                            simdCompress(this.chainingValue, blockWords, this.chunkCounter, BLOCK_LEN, this.flags | this.startFlag()),
                            0, 8);
                    blocksCompressed++;
                    this.offset += BLOCK_LEN;
                    this.blockLen = 0;
                }

                // Take bytes out of the input and update
                int want = BLOCK_LEN - this.blockLen; // How many bytes we need to fill up the current block
                int canTake = Math.min(want, len - currPos);

                blockLen += canTake;
                currPos += canTake;
            }
        }

        private Node createNode() {
            return new Node(chainingValue, wordsFromLEBytes(input, offset, blockLen), chunkCounter, blockLen, flags | startFlag() | CHUNK_END);
        }
    }

    // Hasher
    private ChunkState chunkState;
    private int[] key;
    private final int[][] cvStack = new int[54][];
    private byte cvStackLen = 0;
    private int flags;

    public VectorizedBlake3() {
        initialize(IV, 0);
    }

    public VectorizedBlake3(byte[] key) {
        if (!(key.length == KEY_LEN)) {
            throw new IllegalStateException("Invalid key length");
        }
        initialize(wordsFromLEBytes(key), KEYED_HASH);
    }


    public VectorizedBlake3(String context) {
        VectorizedBlake3 contextHasher = new VectorizedBlake3();
        contextHasher.initialize(IV, DERIVE_KEY_CONTEXT);
        contextHasher.update(context.getBytes(StandardCharsets.UTF_8));
        int[] contextKey = wordsFromLEBytes(contextHasher.digest());
        initialize(contextKey, DERIVE_KEY_MATERIAL);
    }

    private void initialize(int[] key, int flags) {
        this.chunkState = new ChunkState(key, 0, flags);
        this.key = key;
        this.flags = flags;
    }

    @Override
    public void update(byte[] input) {
        int currPos = 0;
        while (currPos < input.length) {

            // If this chunk has chained in 16 64 bytes of input, add its CV to the stack
            if (chunkState.len() == CHUNK_LEN) {
                int[] chunkCV = chunkState.createNode().chainingValue();
                long totalChunks = chunkState.chunkCounter + 1;
                addChunkChainingValue(chunkCV, totalChunks);
                chunkState = new ChunkState(key, totalChunks, flags);
            }

            int want = CHUNK_LEN - chunkState.len();
            int take = Math.min(want, input.length - currPos);
            chunkState.update(input, currPos, take);
            currPos += take;
        }
    }

    @Override
    public byte[] digest(int hashLen) {
        Node node = this.chunkState.createNode();
        /*IntVector[] vectors = prepareVectors();
        for (int i = 0; i < cvStackLen; i++) {
            int[] state = stateFor(cvStack[i], node.counter, node.blockLen, flags);
            for (int j = 0; j < vectors.length; j++) {

            }
        }*/
        int parentNodesRemaining = cvStackLen;
        while (parentNodesRemaining > 0) {
            parentNodesRemaining--;
            node = parentNode(
                    cvStack[parentNodesRemaining],
                    node.chainingValue(),
                    key,
                    flags
            );
        }
        return node.rootOutputBytes(hashLen);
    }

    IntVector[] prepareVectors() {
        IntVector[] vectors = new IntVector[16];
        for (int i = 0; i < vectors.length; i++) {
            vectors[i] = IntVector.zero(PREFERRED);
        }
        return vectors;
    }

    private void pushStack(int[] cv) {
        this.cvStack[this.cvStackLen] = cv;
        cvStackLen++;
    }

    private int[] popStack() {
        this.cvStackLen--;
        return cvStack[cvStackLen];
    }

    // Combines the chaining values of two children to create the parent node
    private static Node parentNode(int[] leftChildCV, int[] rightChildCV, int[] key, int flags) {
        int[] blockWords = new int[16];
        System.arraycopy(leftChildCV, 0, blockWords, 0, leftChildCV.length);
        System.arraycopy(rightChildCV, 0, blockWords, leftChildCV.length, rightChildCV.length);
        return new Node(key, blockWords, 0, BLOCK_LEN, PARENT | flags);
    }

    private static int[] parentCV(int[] leftChildCV, int[] rightChildCV, int[] key, int flags) {
        return parentNode(leftChildCV, rightChildCV, key, flags).chainingValue();
    }

    private void addChunkChainingValue(int[] newCV, long totalChunks) {
        while ((totalChunks & 1) == 0) {
            newCV = parentCV(popStack(), newCV, key, flags);
            totalChunks >>= 1;
        }
        pushStack(newCV);
    }
}
