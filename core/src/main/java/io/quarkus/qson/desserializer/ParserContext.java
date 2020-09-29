package io.quarkus.qson.desserializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.LinkedList;

import static io.quarkus.qson.IntChar.*;

public class ParserContext {
    protected LinkedList<ParserState> state;
    protected ArrayDeque<Object> target = new ArrayDeque<>();
    protected BufferBuilder tokenBuffer;
    protected byte[] buffer;
    protected int ptr;
    protected ParserState initialState;


    protected boolean buildingToken;
    protected int tokenStart = -1;
    protected int tokenEnd = -1;

    public ParserContext(ParserState initialState) {
        this.initialState = initialState;
    }

    public void pushState(ParserState ps) {
        if (state == null) state = new LinkedList<>();
        state.push(ps);
    }

    public void pushState(ParserState ps, int at) {
        if (state == null) state = new LinkedList<>();
        state.add(state.size() - at, ps);
    }

    public void popState() {
        if (state == null) return;
        state.pop();
    }

    public int stateIndex() {
        return state == null ? 0 : state.size();
    }

    public boolean isBufferEmpty() {
        return ptr >= buffer.length;
    }

    public int consume() {
        if (ptr >= buffer.length) {
            if (buildingToken) {
                if (tokenBuffer == null ) {
                    if (tokenStart >= 0) {
                        createTokenBuffer();
                        tokenBuffer.write(buffer, tokenStart, ptr - tokenStart);
                    } else {
                        tokenStart = 0;
                    }
                } else {
                    try {
                        tokenBuffer.write(buffer);
                    } catch (IOException e) {
                        // should be unreachable
                        throw new RuntimeException(e);
                    }

                }
            }
            return 0;
        }
        return buffer[ptr++] & 0xFF;
    }

    public void createTokenBuffer() {
        tokenBuffer = new BufferBuilder(1024);
    }

    public void rewind() {
        ptr--;
    }
    public void startToken() {
        buildingToken = true;
        tokenStart = ptr - 1;
    }

    public void startTokenNextConsumed() {
        buildingToken = true;
        // if current pointer points outside of buffer, set it to start of next buffer.
        if (ptr >= buffer.length) tokenStart = -1;
        else tokenStart = ptr;
    }

    public void endToken() {
        buildingToken = false;
        if (tokenBuffer != null) {
            if (ptr - 1 > 0) tokenBuffer.write(buffer, 0, ptr - 1);
        } else {
            if (tokenStart < 0) tokenStart = 0;  // when asked to start token at next buffer tokenStart will be -1
            tokenEnd = ptr - 1;
        }
    }

    public void clearToken() {
        buildingToken = false;
        tokenBuffer = null;
        tokenStart = -1;
        tokenEnd = -1;
    }

    public int skipWhitespace() {
        int ch = 0;
        do {
            ch = consume();
            if (isWhitespace(ch)) continue;
            return ch;
        } while (ch != 0);
         return 0;
    }

    public int skipToQuote() {
        int ch = 0;
        do {
            ch = consume();
            if (ch != INT_QUOTE) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }
    public int skipDigits() {
        int ch = 0;
        do {
            ch = consume();
            if (isDigit(ch)) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }

    public int skipAlphabetic() {
        int ch = 0;
        do {
            ch = consume();
            if (Character.isAlphabetic(ch)) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }


    public int tokenCharAt(int index) {
        if (tokenBuffer == null) {
            return buffer[tokenStart + index] & 0xFF;
        } else {
            return tokenBuffer.getBuffer()[index] & 0xFF;
        }
    }

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

    public String popToken() {
        String val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new RuntimeException("Token not started.");
            if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
            val = ParsePrimitives.readString(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readString(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    public boolean popBooleanToken() {
        boolean val;
        if (tokenBuffer == null) {
            if (tokenStart < 0) throw new RuntimeException("Token not started.");
            if (tokenEnd < 0) throw new RuntimeException("Token not ended.");
            val = ParsePrimitives.readBoolean(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readBoolean(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    public int popIntToken() {
        return (int) popLongToken();
    }
    public short popShortToken() {
        return (short) popLongToken();
    }
    public byte popByteToken() {
        return (byte) popLongToken();
    }

    public float popFloatToken() {
        return Float.parseFloat(popToken());
    }

    public double popDoubleToken() {
        return Double.parseDouble(popToken());
    }

    public long popLongToken() {
        long val;
        if (tokenBuffer == null) {
            val = ParsePrimitives.readLong(buffer, tokenStart, tokenEnd);
        } else {
            val = ParsePrimitives.readLong(tokenBuffer.getBuffer(), 0, tokenBuffer.size());
        }
        clearToken();
        return val;
    }

    public boolean parse(byte[] buffer) {
        if (buffer == null || buffer.length == 0) return false;

        this.buffer = buffer;
        this.ptr = 0;

        if (state == null || state.isEmpty()) {
            return initialState.parse(this);
        }

        while (ptr < buffer.length || (state != null && !state.isEmpty())) {
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
        return parse(bytes);
    }

    public <T> T target() {
        return (T)target.peek();
    }

    public void pushTarget(Object obj) {

        target.push(obj);
    }

    public <T> T popTarget() {
        return (T)target.pop();
    }
}
