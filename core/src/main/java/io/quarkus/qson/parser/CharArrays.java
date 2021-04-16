package io.quarkus.qson.parser;

import java.util.Arrays;

import static io.quarkus.qson.util.IntChar.*;

class CharArrays {
    final static int[] sHexValues = new int[256];
    final static int[] TRUE_VALUE = {INT_t, INT_r, INT_u, INT_e};
    final static int[] FALSE_VALUE = {INT_f, INT_a, INT_l, INT_s, INT_e};

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


}
