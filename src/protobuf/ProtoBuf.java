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

public class ProtoBuf {

    static private final byte WIRE_VARINT /*   */ = 0; // int32, int64, uint32, uint64, sint32, sint64, bool, enum
    static private final byte WIRE_64_BIT /*   */ = 1; // fixed64, sfixed64, double
    static private final byte WIRE_LEN_DELIM /**/ = 2; // string, bytes, embedded messages, packed repeated fields
    static private final byte WIRE_START_GRP /**/ = 3; // groups (deprecated)
    static private final byte WIRE_END_GRP /*  */ = 4; // groups (deprecated)
    static private final byte WIRE_32_BIT /*   */ = 5; // fixed32, sfixed32, float

    private static class Head {

        public byte u8;
        public byte wire;
        public byte number;

        public void parse(final byte b) {
            this.u8 = b;
            this.wire = ((byte) (b & 0x3));
            this.number = (byte) ((b & 0xFF) >> 3);
        }

        @Override
        public String toString() {
            return "[" + Integer.toHexString(u8 & 0xFF) + "]";
        }

        public String wireName() {
            switch (wire) {
                case WIRE_VARINT:
                    return "[VARINT]";
                case WIRE_64_BIT:
                    return "[64_BIT]";
                case WIRE_LEN_DELIM:
                    return "[LEN_DELIM]";
                case WIRE_START_GRP:
                    return "[START_GROUP]";
                case WIRE_END_GRP:
                    return "[END_GROUP]";
                case WIRE_32_BIT:
                    return "[32_BIT]";
                default:
                    return "[UNKN]";
            }
        }
    }

    private static String pad(int size) {
        return new String(new char[size]).replace("\0", " ");
    }

    private static String parseString(byte[] data, int start, int end) throws Exception {
        String s = "";
        for (int i = start; i < data.length && i < end; i++) {
            byte b = data[i];
            if (b < 0x20 || b > 0x7E) {
                throw new Exception("Not a string: " + Integer.toHexString(b & 0xFF) + " at " + i);
            }
            s += (char) b;
        }
        return s;
    }

    private static String decodeArray(byte[] data, int start, int end) {
        String s = "";
        for (int i = start; i < end; i++) {
            String hex = Integer.toHexString(((int) data[i]) & 0xff);
            if (hex.length() < 2) {
                s += "0" + hex + " ";
            } else {
                s += hex + " ";
            }
        }
        return s.trim();
    }

    private static String decodeBuffer(byte[] data, int start, int end, int padcnt) throws Exception {
        String decoded = "";
        Head h = new Head();
        int bytes_read = 0;
        for (int i = start; i < end;) {
            h.parse(data[i]);
            i++;
            if (h.wire > WIRE_32_BIT /*|| i >= end*/) {
                throw new Exception("Invalid wire code: " + Byte.toString(h.wire));
            }
            if (h.wire != WIRE_END_GRP) {
                decoded += pad(padcnt) + h.number;
            }
            switch (h.wire) {
                case ProtoBuf.WIRE_VARINT: {
                    Leb128 l = Leb128.parse64(data, i, data.length);
                    bytes_read = l.length;
                    decoded += ": " + l.toS64String() + " | " + l.toU64String() + "\n";
                    break;
                }
                case ProtoBuf.WIRE_64_BIT: {
                    Leb128 l = Leb128.parse64(data, i, data.length);
                    bytes_read = l.length;
                    decoded += ": " + l.toS64String() + " | " + l.toU64String() + " | " + l.toF64String() + "\n";
                    break;
                }
                case ProtoBuf.WIRE_LEN_DELIM: {
                    Leb128 l = Leb128.parse32(data, i, data.length);
                    int ps = i + l.length;
                    int pe = ps + l.s32;
                    if (ps > i && pe <= end) {
                        try {
                            decoded += ": \"" + parseString(data, ps, pe) + "\"\n";
                        } catch (Exception e) {
                            decoded += " {\n";
                            decoded += decodeBuffer(data, ps, pe, padcnt + 1);
                            decoded += pad(padcnt) + "}\n";
                        }
                        bytes_read = l.length + l.s32;
                    } else {
                        throw new Exception("Invalid length: " + l.toString() + " ps:" + ps + " pe:" + pe + " i:" + i + " end:" + end);
                    }
                    break;
                }
                case ProtoBuf.WIRE_START_GRP:
                    decoded += " {\n";
                    padcnt++;
                    break;
                case ProtoBuf.WIRE_END_GRP:
                    if (padcnt > 1) {
                        padcnt--;
                    }
                    decoded += pad(padcnt) + "}\n";
                    break;
                case ProtoBuf.WIRE_32_BIT: {
                    Leb128 l = Leb128.parse32(data, i, data.length);
                    bytes_read = l.length;
                    decoded += ": " + l.toS32String() + " | " + l.toU32String() + " | " + l.toF32String() + "\n";
                    break;
                }
                default:
                    decoded += decodeArray(data, i, data.length);
                    return decoded;
            }
            i += bytes_read;
        }
        return decoded;
    }

    public static String decode(byte[] data, int offset) throws Exception {
        return decodeBuffer(data, offset, data.length, 0).trim();
    }
}
