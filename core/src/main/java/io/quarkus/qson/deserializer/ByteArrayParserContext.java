package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class ByteArrayParserContext extends AbstractParserContext {
    protected BufferBuilder tokenBuffer;
    protected byte[] buffer;
    protected int len;


    public ByteArrayParserContext(QsonParser parser, ParserState initialState) {
        super(parser, initialState);
    }

    public ByteArrayParserContext(QsonParser parser) {
        super(parser, parser.startState());
    }

    @Override
    public boolean isBufferEmpty() {
        return ptr >= len;
    }

    @Override
    public int consume() {
        if (eof) return -1;
        if (ptr >= len) {
            if (buildingToken) {
                if (tokenBuffer == null ) {
                    if (tokenStart >= 0) {
                        tokenBuffer = new BufferBuilder(ptr - tokenStart);
                        tokenBuffer.write(buffer, tokenStart, ptr - tokenStart);
                    } else {
                        tokenStart = 0;
                    }
                } else {
                    try {
                        tokenBuffer.write(buffer);
                    } catch (IOException e) {
                        // should be unreachable
                        throw new QsonException(e);
                    }

                }
            }
            return 0;
        }
        return buffer[ptr++] & 0xFF;
    }

    @Override
    public void startTokenNextConsumed() {
        escaped = false;
        buildingToken = true;
        // if current pointer points outside of buffer, set it to start of next buffer.
        if (ptr >= len) tokenStart = -1;
        else tokenStart = ptr;
    }

    @Override
    public void endToken() {
        buildingToken = false;
        if (tokenBuffer != null) {
            if (!eof && ptr - 1 > 0) tokenBuffer.write(buffer, 0, ptr - 1);
        } else {
            if (tokenStart < 0) tokenStart = 0;  // when asked to start token at next buffer tokenStart will be -1
            tokenEnd = ptr - 1;
        }
    }

    @Override
    public void clearToken() {
        buildingToken = false;
        tokenBuffer = null;
        tokenStart = -1;
        tokenEnd = -1;
    }


    @Override
    public int tokenCharAt(int index) {
        if (tokenBuffer == null) {
            return buffer[tokenStart + index] & 0xFF;
        } else {
            return tokenBuffer.getBuffer()[index] & 0xFF;
        }
    }

    @Override
    public boolean compareToken(int index, String str) {
        if (tokenBuffer == null) {
            int size = tokenEnd - tokenStart - index;
            if (size != str.length()) return false;
            for (int i = 0; i < size; i++) {
                int c = buffer[tokenStart + i + index] & 0xFF;
                if (c != str.charAt(i)) return false;
            }
            clearToken();
            return true;
        } else {
            int size = tokenBuffer.size() - index;
            if (size != str.length()) return false;
            byte[] buf = tokenBuffer.getBuffer();
            for (int i = 0; i < size; i++) {
                int c = buf[i + index] & 0xFF;
                if (c != str.charAt(i)) return false;
            }
            clearToken();
            return true;
        }
    }

    @Override
    public String popToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        String val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new QsonException("Token not started.");
            val = ParsePrimitives.readString(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readString(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public boolean popBooleanToken() {
        if (nullToken) {
            nullToken = false;
            return false;
        }
        boolean val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new QsonException("Token not started.");
            val = ParsePrimitives.readBoolean(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readBoolean(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public Boolean popBooleanObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        Boolean val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new QsonException("Token not started.");
            val = ParsePrimitives.readBooleanObject(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readBooleanObject(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public int popIntToken() {
        return (int) popLongToken();
    }

    @Override
    public Integer popIntObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return (int)popLongToken();
    }
    @Override
    public short popShortToken() {
        return (short) popLongToken();
    }

    @Override
    public Short popShortObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return (short) popLongToken();
    }

    @Override
    public byte popByteToken() {
        return (byte) popLongToken();
    }

    @Override
    public Byte popByteObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return (byte) popLongToken();
    }

    @Override
    public float popFloatToken() {
        if (nullToken) {
            nullToken = false;
            return 0.0f;
        }
        return Float.parseFloat(popToken());
    }

    @Override
    public Float popFloatObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return Float.valueOf(popToken());
    }

    @Override
    public double popDoubleToken() {
        if (nullToken) {
            nullToken = false;
            return 0.0;
        }
        return Double.parseDouble(popToken());
    }

    @Override
    public Double popDoubleObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return Double.valueOf(popToken());
    }

    @Override
    public long popLongToken() {
        if (nullToken) {
            nullToken = false;
            return 0;
        }
        long val;
        if (tokenBuffer == null) {
            val = ParsePrimitives.readLong(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readLong(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public Long popLongObjectToken() {
        if (nullToken) {
            nullToken = false;
            return null;
        }
        return popLongToken();
    }

    public boolean parse(byte[] buffer, int len) {
        if (parserComplete) throw new QsonException("Parser is complete, extra bytes invalid");
        if (buffer == null || len == 0) return parserComplete;
        this.len = len;
        this.buffer = buffer;
        this.ptr = 0;

        if (state == null || state.isEmpty()) {
            return initialState.parse(this);
        }

        while (ptr < len || (state != null && !state.isEmpty())) {
            if (!state.peek().parse(this)) {
                return false;
            }
        }
        parserComplete = state == null || state.isEmpty();
        return parserComplete;
    }

    public boolean parse(byte[] buffer) {
        return parse(buffer, buffer.length);
    }

    public boolean parse(String fullJson) {
        byte[] bytes = null;
        try {
            bytes = fullJson.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new QsonException(e);
        }
        return parse(bytes);
    }

    public boolean parse(InputStream is, int bufferSize) throws IOException {
        final byte[] bytes = new byte[bufferSize];
        int read ;
        boolean success;
        do {
            read = is.read(bytes);
            success = parse(bytes, read);
        } while (read >= 0 && !success);
        return success;
    }

    public boolean parse(InputStream is) throws IOException {
        return parse(is, 8192);
    }

    /**
     * Parse the given input stream.
     *
     * @param is
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T> T finish(InputStream is) throws IOException {
        parse(is);
        return finish();
    }

    /**
     * Finish this parse with the last string buffer and return the parsed object.
     *
     * @param str
     * @param <T>
     * @return
     */
    public <T> T finish(String str) {
        parse(str);
        return finish();
    }

    /**
     * Finish this parse with the last buffer and return the parsed object.
     *
     * @param bytes
     * @param <T>
     * @return
     */
    public <T> T finish(byte[] bytes) {
        parse(bytes);
        return finish();
    }

}
