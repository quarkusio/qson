package io.quarkus.qson.util;

public class IntChar {
    public final static int INT_EOF = -1;
    public final static int INT_TAB = '\t';
    public final static int INT_LF = '\n';
    public final static int INT_CR = '\r';
    public final static int INT_SPACE = 0x0020;

    // Markup
    public final static int INT_LBRACKET = '[';
    public final static int INT_RBRACKET = ']';
    public final static int INT_LCURLY = '{';
    public final static int INT_RCURLY = '}';
    public final static int INT_QUOTE = '"';
    public final static int INT_APOS = '\'';
    public final static int INT_BACKSLASH = '\\';
    public final static int INT_SLASH = '/';
    public final static int INT_ASTERISK = '*';
    public final static int INT_COLON = ':';
    public final static int INT_COMMA = ',';
    public final static int INT_HASH = '#';

    // Number chars
    public final static int INT_0 = '0';
    public final static int INT_9 = '9';
    public final static int INT_MINUS = '-';
    public final static int INT_PLUS = '+';

    public final static int INT_PERIOD = '.';
    public final static int INT_e = 'e';
    public final static int INT_E = 'E';

    public final static int INT_t = 't';
    public final static int INT_r = 'r';
    public final static int INT_u = 'u';
    public final static int INT_f = 'f';
    public final static int INT_a = 'a';
    public final static int INT_l = 'l';
    public final static int INT_s = 's';


    public static boolean isDigit(int ch) {
        return ch >=INT_0 && ch <= INT_9;
    }

    public static boolean isWhitespace(int ch) {
        return ch == INT_SPACE
            || ch == INT_LF
                || ch == INT_CR
                  || ch == INT_TAB;
    }
}
