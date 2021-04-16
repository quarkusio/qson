package io.quarkus.qson.parser;

import java.util.HashMap;
import java.util.Map;

public class MapParser extends ObjectParser {
    ContextValue keyFunction;
    ContextValue valueFunction;
    ParserState valueState;
    ParserState continueValueState;

    public MapParser(ContextValue keyFunction, ContextValue valueFunction, ParserState valueState, ParserState continueValueState) {
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction;
        this.valueState = valueState;
        this.continueValueState = continueValueState;
    }

    @Override
    public void beginObject(ParserContext ctx) {
        ctx.pushTarget(new HashMap<>());
    }

    @Override
    public boolean key(ParserContext ctx) {
        int c = ctx.skipToQuote();
        if (c == 0) {
            ctx.pushState(continueKey);
            return false;
        }
        ctx.endToken();
        Object key = keyFunction.value(ctx);
        int stateIndex = ctx.stateIndex();
        if (!valueSeparator(ctx)) {
            ctx.pushState(continueValueState, stateIndex);
            ctx.pushState((ctx1) -> {
                ctx1.popState();
                return fillKey(ctx, key);
            }, stateIndex);
            return false;
        }
        if (!valueState.parse(ctx)) {
            ctx.pushState((ctx1) -> {
                ctx1.popState();
                return fillKey(ctx1, key);
            }, stateIndex);
            return false;
        }
        return fillKey(ctx, key);
    }

    public boolean fillKey(ParserContext ctx, Object key) {
        Object value = valueFunction.value(ctx);
        Map map = ctx.target();
        map.put(key, value);

        return true;
    }
}
