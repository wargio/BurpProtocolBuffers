/*
 * The MIT License
 *
 * Copyright 2019 Giovanni Dante Grazioli <wargio@libero.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package protobuf;

public final class Leb128 {
    public Integer s32;
    public Float f32;
    public Long s64;
    public Double f64;
    public int length;
    private Leb128(int s32, float f, int l) {
        this.s32 = s32;
        this.f32 = f;
        this.s64 = 0l;
        this.f64 = 0.0;
        this.length = l;
    }

    private Leb128(long s64, double d, int l) {
        this.s32 = 0;
        this.f32 = 0.0f;
        this.s64 = s64;
        this.f64 = d;
        this.length = l;
    }
    
    public String toS32String() {
        return this.s32.toString();
    }
    
    public String toF32String() {
        return this.f32.toString();
    }
    
    public String toU32String() {
        return Integer.toUnsignedString(this.s32);
    }
    
    public String toS64String() {
        return this.s64.toString();
    }
    
    public String toF64String() {
        return this.f64.toString();
    }
    
    public String toU64String() {
        return Long.toUnsignedString(this.s64);
    }
    
    public static Leb128 parse32(byte[] in, int start, int end) throws Exception {
        int result = 0;
        int cur = 0x80;
        int count = 0;

        for (int i = start; i < end && ((cur & 0x80) == 0x80) && count < 5; i++) {
            cur = in[i] & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        }

        if ((cur & 0x80) == 0x80) {
            throw new Exception("invalid LEB128 sequence");
        }

        return new Leb128(result, Float.intBitsToFloat(result), count);
    }
    
    public static Leb128 parse64(byte[] in, int start, int end) throws Exception {
        long result = 0;
        int cur = 0x80;
        int count = 0;

        for (int i = start; i < end && ((cur & 0x80) == 0x80) && count < 9; i++) {
            cur = in[i] & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        }

        if ((cur & 0x80) == 0x80) {
            throw new Exception("invalid LEB128 sequence");
        }

        return new Leb128(result, Double.longBitsToDouble(result), count);
    }
}