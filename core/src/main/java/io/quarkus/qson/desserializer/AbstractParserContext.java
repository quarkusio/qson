package io.quarkus.qson.desserializer;

import java.util.ArrayDeque;
import java.util.LinkedList;

import static io.quarkus.qson.IntChar.*;

public abstract class AbstractParserContext implements ParserContext {
    protected LinkedList<ParserState> state;
    protected ArrayDeque<Object> target = new ArrayDeque<>();
    protected int ptr;
    protected ParserState initialState;
    protected boolean buildingToken;
    protected int tokenStart = -1;
    protected int tokenEnd = -1;

    public AbstractParserContext(ParserState initialState) {
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
        ptr--;
    }

    @Override
    public void startToken() {
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
        } while (ch != 0);
         return 0;
    }

    @Override
    public int skipToQuote() {
        int ch = 0;
        do {
            ch = consume();
            if (ch != INT_QUOTE) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }

    @Override
    public int skipDigits() {
        int ch = 0;
        do {
            ch = consume();
            if (isDigit(ch)) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }

    @Override
    public int skipAlphabetic() {
        int ch = 0;
        do {
            ch = consume();
            if (Character.isAlphabetic(ch)) continue;
            return ch;
        } while (ch != 0);
        return 0;
    }
}
