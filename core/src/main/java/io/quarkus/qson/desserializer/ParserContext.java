package io.quarkus.qson.desserializer;

public interface ParserContext {
    void pushState(ParserState ps);

    void pushState(ParserState ps, int at);

    void popState();

    int stateIndex();

    boolean isBufferEmpty();

    int consume();

    void createTokenBuffer();

    void rewind();

    void startToken();

    void startTokenNextConsumed();

    void endToken();

    void clearToken();

    int skipWhitespace();

    int skipToQuote();

    int skipDigits();

    int skipAlphabetic();

    int tokenCharAt(int index);

    boolean compareToken(int index, String str);

    String popToken();

    boolean popBooleanToken();

    int popIntToken();

    short popShortToken();

    byte popByteToken();

    float popFloatToken();

    double popDoubleToken();

    long popLongToken();

    <T> T target();

    void pushTarget(Object obj);

    <T> T popTarget();

    <T> T finish();
}
