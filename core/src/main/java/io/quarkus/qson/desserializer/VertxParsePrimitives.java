package io.quarkus.qson.desserializer;

import io.vertx.core.buffer.Buffer;

import static io.quarkus.qson.IntChar.*;

public class VertxParsePrimitives {
    public static String readString(Buffer buffer, int tokenStart, int tokenEnd) {
        char[] charbuf = new char[tokenEnd - tokenStart];
        int count = 0;
        int ptr = tokenStart;
        while (ptr < tokenEnd) {
            int c = buffer.getByte(ptr++) & 0xFF;
            if (c == '\\') {
                c = buffer.getByte(ptr++) & 0xFF;
                boolean encoded = false;
                switch (c) {
                    // First, ones that are mapped
                    case 'b':
                        charbuf[count++] = '\b';
                        break;
                    case 't':
                        charbuf[count++] = '\t';
                        break;
                    case 'n':
                        charbuf[count++] = '\n';
                        break;
                    case 'f':
                        charbuf[count++] = '\f';
                        break;
                    case 'r':
                        charbuf[count++] = '\r';
                        break;

                    // And these are to be returned as they are
                    case '"':
                    case '/':
                    case '\\':
                        charbuf[count++] = (char) c;
                        break;

                    case 'u': // and finally hex-escaped
                        encoded = true;
                        break;

                    default:
                        throw new RuntimeException("Unknown character format in string");
                }

                if (encoded) {
                    // Ok, a hex escape. Need 4 characters
                    int value = 0;
                    for (int i = 0; i < 4; ++i) {
                        int ch = buffer.getByte(ptr++) & 0xFF;
                        int digit = CharArrays.sHexValues[ch & 0xFF];
                        if (digit < 0) {
                            throw new RuntimeException("expected a hex-digit for character escape sequence");
                        }
                        value = (value << 4) | digit;
                    }
                    charbuf[count++] = (char) value;
                }
            } else {
                int tmp = c & 0xF0; // mask out top 4 bits to test for multibyte
                String hex = Integer.toHexString(c);
                String tmphex = Integer.toHexString(tmp);
                if (tmp == 0xC0 || tmp == 0xD0) {
                    // 2 byte
                    int d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 2 byte encoding");
                    }
                    c = ((c & 0x1F) << 6) | (d & 0x3F);
                } else if (tmp == 0xE0) {
                    // 3 byte
                    c &= 0x0F;
                    int d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 3 byte encoding");
                    }
                    c = (c << 6) | (d & 0x3F);
                    d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 3 byte encoding");
                    }
                    c = (c << 6) | (d & 0x3F);
                } else if (tmp == 0xF0) {
                    // 4 byte
                    int d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 4 byte encoding");
                    }
                    c = ((c & 0x07) << 6) | (d & 0x3F);
                    d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 4 byte encoding");
                    }
                    c = (c << 6) | (d & 0x3F);
                    d = (int) buffer.getByte(ptr++);
                    if ((d & 0xC0) != 0x080) {
                        throw new RuntimeException("Invalid UTF8 4 byte encoding");
                    }
                    c =  ((c << 6) | (d & 0x3F)) - 0x10000;
                    charbuf[count++] = (char) (0xD800 | (c >> 10));
                    c = 0xDC00 | (c & 0x3FF);
                }
                charbuf[count++] = (char) c;
            }

        }
        return new String(charbuf, 0, count);
    }


     public static boolean readBoolean(Buffer buffer, int tokenStart, int tokenEnd) {
        if (tokenStart < 0) throw new RuntimeException("Token not started.");
        if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
        int len = tokenEnd - tokenStart;
        if (len == 4) {
            for (int i = 0; i < 4; i++) {
                if (CharArrays.TRUE_VALUE[i] != ((int)buffer.getByte(tokenStart + i) & 0xFF)) {
                    break;
                }
            }
            return true;
        } else if (len == 5) {
            for (int i = 0; i < 5; i++) {
                if (CharArrays.FALSE_VALUE[i] != ((int)buffer.getByte(tokenStart + i) & 0xFF)) {
                    break;
                }
            }
            return false;

        }
        throw new RuntimeException("Illegal boolean true value syntax");
    }

    public static long readLong(Buffer buffer, int tokenStart, int tokenEnd) {
        boolean negative = false;
        int i = 0;
        int len = tokenEnd - tokenStart;
        long limit = -9223372036854775807L;
        if (len <= 0) {
            return 0;
        } else {
            int firstChar = buffer.getByte(tokenStart) & 0xFF;
            if (firstChar < INT_0) {
                if (firstChar == INT_MINUS) {
                    negative = true;
                    limit = -9223372036854775808L;
                } else if (firstChar != INT_PLUS) {
                    throw new RuntimeException("Illegal number format");
                }

                if (len == 1) {
                    throw new RuntimeException("Illegal number format");
                }

                ++i;
            }

            long multmin = limit / (long)10;

            long result;
            int digit;
            for(result = 0L; i < len; result -= (long)digit) {
                digit = (buffer.getByte(i++ + tokenStart) & 0xFF) - INT_0;
                if (digit < 0 || result < multmin) {
                    throw new RuntimeException("Illegal number format");
                }

                result *= (long)10;
                if (result < limit + (long)digit) {
                    throw new RuntimeException("Illegal number format");
                }
            }

            return negative ? result : -result;
        }
    }


}
