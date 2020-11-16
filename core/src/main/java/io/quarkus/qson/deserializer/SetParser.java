package io.quarkus.qson.deserializer;

import java.util.HashSet;

public class SetParser extends CollectionParser {
    public SetParser(ContextValue valueFunction, ParserState valueState) {
        super(valueFunction, valueState);
    }

    @Override
    public void beginList(ParserContext ctx) {
        ctx.pushTarget(new HashSet<>());
    }
}
