package io.quarkus.qson.desserializer;

import java.util.Arrays;

import static io.quarkus.qson.IntChar.*;

public class ParsePrimitives {
    static int[] TRUE_VALUE = {INT_t, INT_r, INT_u, INT_e};
    static int[] FALSE_VALUE = {INT_f, INT_a, INT_l, INT_s, INT_e};

    /*
    public static String readString(byte[] buffer, int tokenStart, int tokenEnd) {
        char[] charbuf = new char[tokenEnd - tokenStart];
        for (int i = 0; i < tokenEnd - tokenStart; i++) charbuf[i] = (char)(buffer[tokenStart + i] & 0xFF);
        return new String(charbuf);
    }
    */

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
                        throw new RuntimeException("Unknown character format in string");
                }

                if (encoded) {
                    // Ok, a hex escape. Need 4 characters
                    int value = 0;
                    for (int i = 0; i < 4; ++i) {
                        int ch = buffer[ptr++] & 0xFF;
                        int digit = sHexValues[ch & 0xFF];
                        if (digit < 0) {
                            throw new RuntimeException("expected a hex-digit for character escape sequence");
                        }
                        value = (value << 4) | digit;
                    }
                    charbuf[count++] = (char) value;
                }
            } else {
                int tmp = c & 0xF0; // mask out top 4 bits to test for multibyte
                if (tmp == 0xC0) {
                    // 2 byte
                } else if (tmp == 0xE0) {
                    // 3 byte
                } else if (tmp == 0xF0) {
                    // 4 byte
                } else {
                    charbuf[count++] = (char) c;
                }
            }

        }
        return new String(charbuf, 0, count);
    }

    private final static int[] sHexValues = new int[256];

    static {
        Arrays.fill(sHexValues, -1);
        for (int i = 0; i < 10; ++i) {
            sHexValues['0' + i] = i;
        }
        for (int i = 0; i < 6; ++i) {
            sHexValues['a' + i] = 10 + i;
            sHexValues['A' + i] = 10 + i;
        }
    }


    public static boolean readBoolean(byte[] buffer, int tokenStart, int tokenEnd) {
        if (tokenStart < 0) throw new RuntimeException("Token not started.");
        if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
        int len = tokenEnd - tokenStart;
        if (len == 4) {
            for (int i = 0; i < 4; i++) {
                if (TRUE_VALUE[i] != ((int) buffer[tokenStart + i] & 0xFF)) {
                    break;
                }
            }
            return true;
        } else if (len == 5) {
            for (int i = 0; i < 5; i++) {
                if (FALSE_VALUE[i] != ((int) buffer[tokenStart + i] & 0xFF)) {
                    break;
                }
            }
            return false;

        }
        throw new RuntimeException("Illegal boolean true value syntax");
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
                    throw new RuntimeException("Illegal number format");
                }

                if (len == 1) {
                    throw new RuntimeException("Illegal number format");
                }

                ++i;
            }

            long multmin = limit / (long) 10;

            long result;
            int digit;
            for (result = 0L; i < len; result -= (long) digit) {
                digit = (buffer[i++ + tokenStart] & 0xFF) - INT_0;
                if (digit < 0 || result < multmin) {
                    throw new RuntimeException("Illegal number format");
                }

                result *= (long) 10;
                if (result < limit + (long) digit) {
                    throw new RuntimeException("Illegal number format");
                }
            }

            return negative ? result : -result;
        }
    }


}
