package io.quarkus.qson.desserializer;

import io.vertx.core.buffer.Buffer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class VertxBufferParserContext extends AbstractParserContext {
    protected BufferBuilder tokenBuffer;
    protected Buffer buffer;


    public VertxBufferParserContext(ParserState initialState) {
        super(initialState);
    }

    @Override
    public boolean isBufferEmpty() {
        return ptr >= buffer.length();
    }

    @Override
    public int consume() {
        if (ptr >= buffer.length()) {
            if (buildingToken) {
                if (tokenBuffer == null ) {
                    if (tokenStart >= 0) {
                        createTokenBuffer();
                        tokenBuffer.writeBytes(buffer.getBytes(tokenStart, ptr));
                    } else {
                        tokenStart = 0;
                    }
                } else {
                    tokenBuffer.writeBytes(buffer.getBytes());
                }
            }
            return 0;
        }
        return buffer.getByte(ptr++) & 0xFF;
    }

    @Override
    public void createTokenBuffer() {
        tokenBuffer = new BufferBuilder(1024);
    }

    @Override
    public void startTokenNextConsumed() {
        escaped = false;
        buildingToken = true;
        // if current pointer points outside of buffer, set it to start of next buffer.
        if (ptr >= buffer.length()) tokenStart = -1;
        else tokenStart = ptr;
    }

    @Override
    public void endToken() {
        buildingToken = false;
        if (tokenBuffer != null) {
            if (ptr - 1 > 0) tokenBuffer.writeBytes(buffer.getBytes(0, ptr - 1));
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
            return buffer.getByte(tokenStart + index) & 0xFF;
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
                int c = buffer.getByte(tokenStart + i + index) & 0xFF;
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
        String val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new RuntimeException("Token not started.");
            if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
            val = VertxParsePrimitives.readString(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readString(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public boolean popBooleanToken() {
        boolean val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new RuntimeException("Token not started.");
            if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
            val = VertxParsePrimitives.readBoolean(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readBoolean(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    @Override
    public int popIntToken() {
        return (int) popLongToken();
    }
    @Override
    public short popShortToken() {
        return (short) popLongToken();
    }
    @Override
    public byte popByteToken() {
        return (byte) popLongToken();
    }

    @Override
    public float popFloatToken() {
        return Float.parseFloat(popToken());
    }

    @Override
    public double popDoubleToken() {
        return Double.parseDouble(popToken());
    }

    @Override
    public long popLongToken() {
        long val;
        if (tokenBuffer == null) {
            val = VertxParsePrimitives.readLong(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readLong(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    public boolean parse(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) return false;

        this.buffer = buffer;
        this.ptr = 0;

        if (state == null || state.isEmpty()) {
            return initialState.parse(this);
        }

        while (ptr < buffer.length() || (state != null && !state.isEmpty())) {
            if (!state.peek().parse(this)) {
                return false;
            }
        }
        return state != null && state.isEmpty();
    }

    public boolean parse(String fullJson) {
        byte[] bytes = null;
        try {
            bytes = fullJson.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return parse(Buffer.buffer(bytes));
    }

}
