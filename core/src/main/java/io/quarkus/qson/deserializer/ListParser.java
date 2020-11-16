package io.quarkus.qson.deserializer;

import java.util.LinkedList;

public class ListParser extends CollectionParser {
    public ListParser(ContextValue valueFunction, ParserState valueState) {
        super(valueFunction, valueState);
    }

    @Override
    public void beginList(ParserContext ctx) {
        ctx.pushTarget(new LinkedList());
    }
}
