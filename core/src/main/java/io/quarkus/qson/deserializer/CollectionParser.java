package io.quarkus.qson.deserializer;

import java.util.Collection;

public class CollectionParser extends ObjectParser {
    final ContextValue valueFunction;
    final ParserState valueState;

    public CollectionParser(ContextValue valueFunction, ParserState valueState) {
        this.valueFunction = valueFunction;
        this.valueState = valueState;
    }

    @Override
    public boolean start(ParserContext ctx) {
        return startList(ctx);
    }

    public void addListValue(ParserContext ctx) {
        Object value = valueFunction.value(ctx);
        Collection collection = ctx.target();
        collection.add(value);
    }

    @Override
    public boolean listValue(ParserContext ctx) {
        return valueState.parse(ctx);
    }

}
