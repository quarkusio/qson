package io.quarkus.qson.parser;

public interface ParserContext {
    Object NULL = new Object();

    void pushState(ParserState ps);

    void pushState(ParserState ps, int at);

    void popState();

    int stateIndex();

    boolean isBufferEmpty();

    int consume();

    void rewind();

    void startToken();

    void startTokenNextConsumed();

    void endToken();

    void clearToken();

    void startNullToken();

    void endNullToken();

    int skipWhitespace();

    int skipToQuote();

    int skipDigits();

    int skipAlphabetic();

    int tokenCharAt(int index);

    boolean compareToken(int index, String str);

    String popToken();

    boolean popBooleanToken();

    Boolean popBooleanObjectToken();

    int popIntToken();

    Integer popIntObjectToken();

    short popShortToken();

    Short popShortObjectToken();

    byte popByteToken();

    Byte popByteObjectToken();

    float popFloatToken();

    Float popFloatObjectToken();

    double popDoubleToken();

    Double popDoubleObjectToken();

    long popLongToken();

    <T> T target();

    void pushTarget(Object obj);

    <T> T popTarget();

    <T> T finish();

    Long popLongObjectToken();

    boolean handleAny(AnySetter setter);
}
