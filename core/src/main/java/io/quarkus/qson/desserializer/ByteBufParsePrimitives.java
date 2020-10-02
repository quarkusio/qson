package io.quarkus.qson.desserializer;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;

import static io.quarkus.qson.IntChar.*;

public class ByteBufParsePrimitives {
    static int[] TRUE_VALUE = {INT_t, INT_r, INT_u, INT_e};
    static int[] FALSE_VALUE = {INT_f, INT_a, INT_l, INT_s, INT_e};

    public static String readString(ByteBuf buffer, int tokenStart, int tokenEnd) {
        char[] charbuf = new char[tokenEnd - tokenStart];
        for (int i = 0; i < tokenEnd - tokenStart; i++) charbuf[i] = (char)(buffer.getByte(tokenStart + i) & 0xFF);
        return new String(charbuf);
    }

    public static boolean readBoolean(ByteBuf buffer, int tokenStart, int tokenEnd) {
        if (tokenStart < 0) throw new RuntimeException("Token not started.");
        if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
        int len = tokenEnd - tokenStart;
        if (len == 4) {
            for (int i = 0; i < 4; i++) {
                if (TRUE_VALUE[i] != ((int)buffer.getByte(tokenStart + i) & 0xFF)) {
                    break;
                }
            }
            return true;
        } else if (len == 5) {
            for (int i = 0; i < 5; i++) {
                if (FALSE_VALUE[i] != ((int)buffer.getByte(tokenStart + i) & 0xFF)) {
                    break;
                }
            }
            return false;

        }
        throw new RuntimeException("Illegal boolean true value syntax");
    }

    public static long readLong(ByteBuf buffer, int tokenStart, int tokenEnd) {
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
