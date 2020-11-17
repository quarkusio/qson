package io.quarkus.qson.deserializer;

import java.util.ArrayDeque;
import java.util.LinkedList;

import static io.quarkus.qson.util.IntChar.*;

public abstract class AbstractParserContext implements ParserContext {
    protected LinkedList<ParserState> state;
    protected ArrayDeque<Object> target = new ArrayDeque<>();
    protected int ptr;
    protected ParserState initialState;
    protected QsonParser parser;
    protected boolean buildingToken;
    protected int tokenStart = -1;
    protected int tokenEnd = -1;
    protected boolean eof;
    protected boolean parserComplete;
    protected Object result;

    public AbstractParserContext(QsonParser parser, ParserState initialState) {
        this.parser = parser;
        this.initialState = initialState;
    }

    @Override
    public void pushState(ParserState ps) {
        if (state == null) state = new LinkedList<>();
        state.push(ps);
    }

    @Override
    public void pushState(ParserState ps, int at) {
        if (state == null) state = new LinkedList<>();
        state.add(state.size() - at, ps);
    }

    @Override
    public void popState() {
        if (state == null) return;
        state.pop();
    }

    @Override
    public int stateIndex() {
        return state == null ? 0 : state.size();
    }

    @Override
    public <T> T target() {
        return (T)target.peek();
    }

    @Override
    public void pushTarget(Object obj) {
        target.push(obj);
    }

    @Override
    public <T> T popTarget() {
        return (T)target.pop();
    }

    @Override
    public void rewind() {
        if (!eof) ptr--;
    }

    @Override
    public void startToken() {
        escaped = false;
        buildingToken = true;
        tokenStart = ptr - 1;
    }

    @Override
    public int skipWhitespace() {
        int ch = 0;
        do {
            ch = consume();
            if (isWhitespace(ch)) continue;
            return ch;
        } while (ch > 0);
        return 0;
    }

    protected boolean escaped = false;

    @Override
    public int skipToQuote() {
        int ch = 0;
        do {
            ch = consume();
            if (ch <= 0) return ch;
            // make sure that last character wasn't escape character
            if (ch != INT_QUOTE || (escaped && ch == INT_QUOTE)) {
                escaped = escaped ? false : ch == INT_BACKSLASH;
                continue;
            }
            return ch;
        } while (ch > 0);
        return 0;
    }

    @Override
    public int skipDigits() {
        int ch ;
        do {
            ch = consume();
            if (isDigit(ch)) continue;
            return ch;
        } while (ch > 0);
        return 0;
    }

    @Override
    public int skipAlphabetic() {
        int ch = 0;
        do {
            ch = consume();
            if (Character.isAlphabetic(ch)) continue;
            return ch;
        } while (ch > 0);
        return 0;
    }

    @Override
    public <T> T finish() {
        if (result != null) return (T)result;
        parserComplete = state == null || state.isEmpty();
        if (parserComplete) {
            result = parser.getTarget(this);
            return (T)result;
        }
        eof = true;
        while (state != null && !state.isEmpty()) {
            if (!state.peek().parse(this)) {
                throw new RuntimeException("Parser incomplete.  EOF reached.");
            }
        }
        parserComplete = state == null || state.isEmpty();
        if (!parserComplete) throw new RuntimeException("Parser incomplete.  EOF reached.");
        result = parser.getTarget(this);
        return (T)result;
    }

}
