package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonException;

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
    protected boolean nullToken;
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
        Object obj = target.peek();
        if (obj == ParserContext.NULL) return null;
        return (T)obj;
    }

    @Override
    public void pushTarget(Object obj) {
        target.push(obj);
    }

    @Override
    public <T> T popTarget() {
        Object obj = target.pop();
        if (obj == ParserContext.NULL) return null;
        return (T)obj;
    }

    @Override
    public void rewind() {
        if (!eof) ptr--;
    }

    @Override
    public void startToken() {
        nullToken = false;
        escaped = false;
        buildingToken = true;
        tokenStart = ptr - 1;
    }

    @Override
    public void startNullToken() {
        nullToken = true;
    }

    @Override
    public void endNullToken() {
        nullToken = false;
    }

    @Override
    public int skipWhitespace() {
        int ch = 0;
        do {
            ch = consume();
            if (isWhitespace(ch)) continue;
            return ch;
        } while (true);
    }

    protected boolean escaped = false;

    @Override
    public int skipToQuote() {
        int ch = 0;
        do {
            ch = consume();
            // make sure that last character wasn't escape character
            if (ch > 0 && (ch != INT_QUOTE || (escaped && ch == INT_QUOTE))) {
                escaped = !escaped && ch == INT_BACKSLASH;
                continue;
            }
            return ch;
        } while (true);
    }

    @Override
    public int skipDigits() {
        int ch ;
        do {
            ch = consume();
            if (isDigit(ch)) continue;
            return ch;
        } while (true);
    }

    @Override
    public int skipAlphabetic() {
        int ch;
        do {
            ch = consume();
            if (Character.isAlphabetic(ch)) continue;
            return ch;
        } while (true);
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
                throw new QsonException("Parser incomplete.  EOF reached.");
            }
        }
        parserComplete = true;
        result = parser.getTarget(this);
        return (T)result;
    }

}
