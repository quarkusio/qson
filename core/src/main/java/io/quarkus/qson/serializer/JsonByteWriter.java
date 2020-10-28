package io.quarkus.qson.serializer;

import io.quarkus.qson.IntChar;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonByteWriter implements JsonWriter {
    final ByteWriter writer;

    static final byte[] TRUE = {'t', 'r', 'u', 'e'};
    static final byte[] FALSE = {'f', 'a', 'l', 's', 'e'};

    public static Charset UTF8 = Charset.forName("UTF-8");

    public JsonByteWriter(ByteWriter writer) {
        this.writer = writer;
    }

    @Override
    public void writeLCurley() {
        this.writer.write(IntChar.INT_LCURLY);

    }

    @Override
    public void writeRCurley() {
        this.writer.write(IntChar.INT_RCURLY);
    }

    @Override
    public void write(short val) {
        write((long)val);
    }

    @Override
    public void write(int val) {
        write((long)val);
    }

    @Override
    public void write(long x) {
        int places = 1;
        long n = x / 10;
        for (; n != 0; n /= 10) places *= 10;

        StringBuilder builder = new StringBuilder();
        if (x < 0) {
            builder.append('-');
            for (int place = places; place >=1; place /= 10) {
                int i = (int)(x / place);
                writer.write('0' - i);
                x -= i * place;
            }
        } else {
            for (int place = places; place >=1; place /= 10) {
                int i = (int)(x / place);
                writer.write('0' + i);
                x -= i * place;
            }
        }
    }

    @Override
    public void write(boolean val) {
        if (val) writer.write(TRUE);
        else writer.write(FALSE);
    }

    @Override
    public void write(byte val) {
        write((long)val);
    }

    @Override
    public void write(float val) {
        writer.write(Float.toString(val).getBytes(UTF8));
    }

    @Override
    public void write(double val) {
        writer.write(Double.toString(val).getBytes(UTF8));
    }

    @Override
    public void write(char val) {
        write(Character.toString(val));
    }

    @Override
    public void write(Character val) {
        write(val.charValue());
    }

    @Override
    public void write(Short val) {
        write(val.longValue());

    }

    @Override
    public void write(Integer val) {
        write(val.longValue());

    }

    @Override
    public void write(Long val) {
        write(val.longValue());

    }

    @Override
    public void write(Boolean val) {
        write(val.booleanValue());
    }

    @Override
    public void write(Byte val) {
        write(val.longValue());
    }

    @Override
    public void write(Float val) {
        write(val.floatValue());

    }

    @Override
    public void write(Double val) {
        write(val.doubleValue());

    }

    @Override
    public void write(String val) {
        writer.write(IntChar.INT_QUOTE);
        final int[] escCodes = sOutputEscapes128;
        for (int i = 0; i < val.length(); i++) {
            int ch = val.charAt(i);
            if (ch <= 0x7F) {
                int escape = escCodes[ch];
                if (escape == 0) {
                    writer.write(ch);
                } else if (escape > 0) { // 2-char escape
                    writer.write(IntChar.INT_BACKSLASH);
                    writer.write(escape);
                } else {
                    // ctrl-char, 6-byte escape...
                   writeGenericEscape(ch);
                }
            } else if (ch <= 0x7FF) { // fine, just needs 2 byte output
                writer.write(0xc0 | (ch >> 6));
                writer.write(0x80 | (ch & 0x3f));
            } else {
                outputMultiByteChar(ch);
            }
        }
        writer.write(IntChar.INT_QUOTE);
    }

    private final static int[] HEX_CHARS;
    static {
        String hex = "0123456789ABCDEF";
        int len = hex.length();
        HEX_CHARS = new int[len];
        for (int i = 0; i < len; ++i) {
            HEX_CHARS[i] = hex.charAt(i);
        }
    }


    private void writeGenericEscape(int charToEscape)
    {
        writer.write(IntChar.INT_BACKSLASH);
        writer.write(IntChar.INT_u);
        if (charToEscape > 0xFF) {
            int hi = (charToEscape >> 8) & 0xFF;
            writer.write(HEX_CHARS[hi >> 4]);
            writer.write(HEX_CHARS[hi & 0xF]);
            charToEscape &= 0xFF;
        } else {
            writer.write(IntChar.INT_0);
            writer.write(IntChar.INT_0);
        }
        // We know it's a control char, so only the last 2 chars are non-0
        writer.write(HEX_CHARS[charToEscape >> 4]);
        writer.write(HEX_CHARS[charToEscape & 0xF]);
    }

    public final static int SURR1_FIRST = 0xD800;
    public final static int SURR1_LAST = 0xDBFF;
    public final static int SURR2_FIRST = 0xDC00;
    public final static int SURR2_LAST = 0xDFFF;


    private final void outputMultiByteChar(int ch)
    {
        if (ch >= SURR1_FIRST && ch <= SURR2_LAST) { // yes, outside of BMP; add an escape
            writer.write(IntChar.INT_BACKSLASH);
            writer.write(IntChar.INT_u);

            writer.write(HEX_CHARS[(ch >> 12) & 0xF]);
            writer.write(HEX_CHARS[(ch >> 8) & 0xF]);
            writer.write(HEX_CHARS[(ch >> 4) & 0xF]);
            writer.write(HEX_CHARS[ch & 0xF]);
        } else {
            writer.write(0xe0 | (ch >> 12));
            writer.write(0x80 | ((ch >> 6) & 0x3f));
            writer.write(0x80 | (ch & 0x3f));
        }
    }



    /**
     * Lookup table used for determining which output characters in
     * 7-bit ASCII range need to be quoted.
     */
    private final static int[] sOutputEscapes128;
    static {
        int[] table = new int[128];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            table[i] = -1;
        }
        // Others (and some within that range too) have explicit shorter sequences
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        sOutputEscapes128 = table;
    }

    @Override
    public void writeObject(Object obj) {
        if (obj instanceof Map) {
            write((Map)obj);
        } else if (obj instanceof List || obj instanceof Set) {
            write((Collection)obj);
        } else if (obj instanceof String) {
            write((String)obj);
        } else if (obj instanceof Short) {
            write((Short)obj);
        } else if (obj instanceof Byte) {
            write((Byte)obj);
        } else if (obj instanceof Integer) {
            write((Integer) obj);
        } else if (obj instanceof Long) {
            write((Long)obj);
        } else if (obj instanceof Float) {
            write((Float)obj);
        } else if (obj instanceof Double) {
            write((Double)obj);
        } else if (obj instanceof Boolean) {
            write((Boolean)obj);
        } else if (obj instanceof Character) {
            write((Character)obj);
        } else {
            throw new RuntimeException("Unable to determine type to write: " + obj.getClass().getName());
        }
    }

    @Override
    public void write(Map val) {
        writer.write(IntChar.INT_LCURLY);
        Set<Map.Entry<Object, Object>> set = val.entrySet();
        boolean first = true;
        for (Map.Entry<Object, Object> entry : set) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            writePropertyName(entry.getKey());
            writer.write(IntChar.INT_COLON);
            writeObject(entry.getValue());
        }
        writer.write(IntChar.INT_RCURLY);
    }

    @Override
    public void write(Map val, ObjectWriter valueWriter) {
        writer.write(IntChar.INT_LCURLY);
        Set<Map.Entry<Object, Object>> set = val.entrySet();
        boolean first = true;
        for (Map.Entry<Object, Object> entry : set) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            writePropertyName(entry.getKey());
            writer.write(IntChar.INT_COLON);
            valueWriter.write(this, entry.getValue());
        }
        writer.write(IntChar.INT_RCURLY);
    }

    @Override
    public void write(Collection val) {
        writer.write(IntChar.INT_LBRACKET);
        boolean first = true;
        for (Object item : val) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            writeObject(item);
        }
        writer.write(IntChar.INT_RBRACKET);
    }

    @Override
    public void write(Collection val, ObjectWriter elementWriter) {
        writer.write(IntChar.INT_LBRACKET);
        boolean first = true;
        for (Object item : val) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            elementWriter.write(this, item);
        }
        writer.write(IntChar.INT_RBRACKET);
    }



    @Override
    public void writeProperty(String name, char val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, short val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, int val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, long val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, boolean val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, byte val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, float val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public void writeProperty(String name, double val, boolean comma) {
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
    }

    @Override
    public boolean writeProperty(String name, Character val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Short val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Integer val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Long val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Boolean val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Byte val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Float val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Double val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, String val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    @Override
    public boolean writeObjectProperty(String name, Object val, ObjectWriter writer, boolean comma) {
        if (val == null) return comma;
        if (comma) this.writer.write(IntChar.INT_COMMA);
        write(name);
        this.writer.write(IntChar.INT_COLON);
        writer.write(this, val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Map val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        write(val);
        return true;
    }

    private void writePropertyName(Object obj) {
        if (obj instanceof String) {
            write((String)obj);
            return;
        }
        if (obj instanceof Character) {
            write((Character)obj);
            return;
        }
        if (obj instanceof Short) {
            writer.write(IntChar.INT_QUOTE);
            write((Short)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Integer) {
            writer.write(IntChar.INT_QUOTE);
            write((Integer)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Long) {
            writer.write(IntChar.INT_QUOTE);
            write((Long)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Byte) {
            writer.write(IntChar.INT_QUOTE);
            write((Byte)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Boolean) {
            writer.write(IntChar.INT_QUOTE);
            write((Boolean)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Float) {
            writer.write(IntChar.INT_QUOTE);
            write((Short)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        if (obj instanceof Double) {
            writer.write(IntChar.INT_QUOTE);
            write((Short)obj);
            writer.write(IntChar.INT_QUOTE);
            return;
        }
        write(obj.toString());
    }

    @Override
    public boolean writeObjectProperty(String name, Object val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        writeObject(val);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Collection val, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        try {
            write(val);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to write collection property: " + name, e);
        }
        return true;
    }

    @Override
    public boolean writeProperty(String name, Map val, ObjectWriter objectWriter, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        writer.write(IntChar.INT_LCURLY);
        Set<Map.Entry<Object, Object>> set = val.entrySet();
        boolean first = true;
        for (Map.Entry<Object, Object> entry : set) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            writePropertyName(entry.getKey());
            writer.write(IntChar.INT_COLON);
            try {
                objectWriter.write(this, entry.getValue());
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to write map property: " + name, e);
            }
        }
        writer.write(IntChar.INT_RCURLY);
        return true;
    }

    @Override
    public boolean writeProperty(String name, Collection val, ObjectWriter objectWriter, boolean comma) {
        if (val == null) return comma;
        if (comma) writer.write(IntChar.INT_COMMA);
        write(name);
        writer.write(IntChar.INT_COLON);
        writer.write(IntChar.INT_LBRACKET);
        boolean first = true;
        for (Object item : val) {
            if (first) first = false;
            else writer.write(IntChar.INT_COMMA);
            objectWriter.write(this, item);
        }
        writer.write(IntChar.INT_RBRACKET);
        return true;
    }
}
