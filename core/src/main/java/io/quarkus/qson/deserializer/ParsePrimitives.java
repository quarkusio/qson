package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonException;

import static io.quarkus.qson.util.IntChar.*;

public class ParsePrimitives {

    public static String readString(byte[] buffer, int tokenStart, int tokenEnd) {
        char[] charbuf = new char[tokenEnd - tokenStart];
        int count = 0;
        int ptr = tokenStart;
        while (ptr < tokenEnd) {
            int c = buffer[ptr++] & 0xFF;
            if (c == '\\') {
                c = buffer[ptr++] & 0xFF;
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
                        throw new QsonException("Unknown character format in string");
                }

                if (encoded) {
                    // Ok, a hex escape. Need 4 characters
                    int value = 0;
                    for (int i = 0; i < 4; ++i) {
                        int ch = buffer[ptr++] & 0xFF;
                        int digit = CharArrays.sHexValues[ch & 0xFF];
                        if (digit < 0) {
                            throw new QsonException("expected a hex-digit for character escape sequence");
                        }
                        value = (value << 4) | digit;
                    }
                    charbuf[count++] = (char) value;
                }
            } else {
                if (c < 128) { // Assume 1 byte chars are most common.
                    charbuf[count++] = (char) c;
                } else {
                    int tmp = c & 0xF0; // mask out top 4 bits to test for multibyte
                    if (tmp == 0xC0 || tmp == 0xD0) {
                        // 2 byte
                        int d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 2 byte encoding");
                        }
                        c = ((c & 0x1F) << 6) | (d & 0x3F);
                    } else if (tmp == 0xE0) {
                        // 3 byte
                        c &= 0x0F;
                        int d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 3 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                    } else if (tmp == 0xF0) {
                        // 4 byte
                        int d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c & 0x07) << 6) | (d & 0x3F);
                        d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = (c << 6) | (d & 0x3F);
                        d = (int) buffer[ptr++];
                        if ((d & 0xC0) != 0x080) {
                            throw new QsonException("Invalid UTF8 4 byte encoding");
                        }
                        c = ((c << 6) | (d & 0x3F)) - 0x10000;
                        charbuf[count++] = (char) (0xD800 | (c >> 10));
                        c = 0xDC00 | (c & 0x3FF);
                    }
                    charbuf[count++] = (char) c;
                }
            }

        }
        return new String(charbuf, 0, count);
    }

    public static boolean readBoolean(byte[] buffer, int tokenStart, int tokenEnd) {
        int len = tokenEnd - tokenStart;
        if (len == 4) {
            for (int i = 0; i < 4; i++) {
                if (CharArrays.TRUE_VALUE[i] != ((int) buffer[tokenStart + i] & 0xFF)) {
                    throw new QsonException("Illegal boolean syntax");
                }
            }
            return true;
        } else if (len == 5) {
            for (int i = 0; i < 5; i++) {
                if (CharArrays.FALSE_VALUE[i] != ((int) buffer[tokenStart + i] & 0xFF)) {
                    throw new QsonException("Illegal boolean syntax");
                }
            }
            return false;

        }
        throw new QsonException("Illegal boolean syntax");
    }

    public static long readLong(byte[] buffer, int tokenStart, int tokenEnd) {
        boolean negative = false;
        int i = 0;
        int len = tokenEnd - tokenStart;
        long limit = -9223372036854775807L;
        if (len <= 0) {
            return 0;
        } else {
            int firstChar = buffer[tokenStart] & 0xFF;
            if (firstChar < INT_0) {
                if (firstChar == INT_MINUS) {
                    negative = true;
                    limit = -9223372036854775808L;
                } else if (firstChar != INT_PLUS) {
                    throw new QsonException("Illegal number format");
                }

                if (len == 1) {
                    throw new QsonException("Illegal number format");
                }

                ++i;
            }

            long multmin = limit / (long) 10;

            long result;
            int digit;
            for (result = 0L; i < len; result -= (long) digit) {
                digit = (buffer[i++ + tokenStart] & 0xFF) - INT_0;
                if (digit < 0 || result < multmin) {
                    throw new QsonException("Illegal number format");
                }

                result *= (long) 10;
                if (result < limit + (long) digit) {
                    throw new QsonException("Illegal number format");
                }
            }

            return negative ? result : -result;
        }
    }


}
