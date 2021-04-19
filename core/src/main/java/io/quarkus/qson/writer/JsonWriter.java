package io.quarkus.qson.writer;

import java.util.Collection;
import java.util.Map;

/**
 * Abstraction for writing json to a stream, buffer, etc.
 *
 */
public interface JsonWriter {
    void writeBytes(byte[] bytes);
    void writeByte(int b);

    void writeLCurley();
    void writeRCurley();

    void writeLBracket();

    void writeRBracket();

    void writeSeparator();

    void writeComma();

    void writeQuote();

    void write(short val);
    void write(int val);
    void write(long val);
    void write(boolean val);
    void write(byte val);
    void write(float val);
    void write(double val);
    /**
     * Writes a quoted character as string with character escape codes where approriate
     * @param val
     */
    void write(char val);
    /**
     * Writes a quoted character as string with character escape codes where approriate
     * @param val
     */
    void write(Character val);
    void write(Short val);
    void write(Integer val);
    void write(Long val);
    void write(Boolean val);
    void write(Byte val);
    void write(Float val);
    void write(Double val);

    /**
     * Writes a quoted string with character escape codes where approriate
     * @param val
     */
    void write(String val);

    void write(Enum e);

    void writeObject(Object obj);
    void write(Map val);

    boolean writeAny(Map val, boolean comma);

    void write(Map val, QsonObjectWriter valueWriter);
    void write(Collection val);
    void write(Collection val, QsonObjectWriter elementWriter);

    void writeProperty(String name, char val, boolean comma);
    void writeProperty(String name, short val, boolean comma);
    void writeProperty(String name, int val, boolean comma);
    void writeProperty(String name, long val, boolean comma);
    void writeProperty(String name, boolean val, boolean comma);
    void writeProperty(String name, byte val, boolean comma);
    void writeProperty(String name, float val, boolean comma);
    void writeProperty(String name, double val, boolean comma);
    boolean writeProperty(String name, Character val, boolean comma);
    boolean writeProperty(String name, Short val, boolean comma);
    boolean writeProperty(String name, Integer val, boolean comma);
    boolean writeProperty(String name, Long val, boolean comma);
    boolean writeProperty(String name, Boolean val, boolean comma);
    boolean writeProperty(String name, Byte val, boolean comma);
    boolean writeProperty(String name, Float val, boolean comma);
    boolean writeProperty(String name, Double val, boolean comma);
    boolean writeProperty(String name, String val, boolean comma);

    boolean writeProperty(String name, Enum val, boolean comma);

    boolean writeObjectProperty(String name, Object obj, QsonObjectWriter writer, boolean comma);
    boolean writeObjectProperty(String name, Object obj, boolean comma);


    boolean writeProperty(String name, Map map, boolean comma);
    boolean writeProperty(String name, Collection list, boolean comma);
    boolean writeProperty(String name, Map map, QsonObjectWriter writer, boolean comma);
    boolean writeProperty(String name, Collection list, QsonObjectWriter writer, boolean comma);
}
