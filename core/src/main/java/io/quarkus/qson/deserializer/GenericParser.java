package io.quarkus.qson.deserializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GenericParser extends BaseParser implements QsonParser {

    public static final GenericParser PARSER = new GenericParser();
    private ParserState value = this::value;

    public GenericParser() {
    }

    @Override
    public void startToken(ParserContext ctx) {
        ctx.startToken();
    }

    @Override
    public void startTokenNextConsumed(ParserContext ctx) {
        ctx.startTokenNextConsumed();
    }

    @Override
    public void endToken(ParserContext ctx) {
        ctx.endToken();
    }

    @Override
    public void startNullToken(ParserContext ctx) {
        ctx.startNullToken();
    }

    @Override
    public void endNullValue(ParserContext ctx) {
        ctx.endNullToken();
        ctx.pushTarget(ParserContext.NULL);
    }

    @Override
    public ParserState startState() {
        return value;
    }

    @Override
    public void beginList(ParserContext ctx) {
        ctx.pushTarget(new LinkedList());
    }

    @Override
    public void addListValue(ParserContext ctx) {
        Object val = ctx.popTarget();
        Collection list = ctx.target();
        list.add(val);
    }

    @Override
    public boolean key(ParserContext ctx) {
        int c = ctx.skipToQuote();
        if (c == 0) {
            ctx.pushState(continueKey);
            return false;
        }
        endToken(ctx);
        String key = ctx.popToken();
        int stateIndex = ctx.stateIndex();
        if (!valueSeparator(ctx)) {
            ctx.pushState(continueValue, stateIndex);
            ctx.pushState((ctx1) -> {
                ctx1.popState();
                return fillKey(ctx1, key);
            }, stateIndex);
            return false;
        }
        if (!value(ctx)) {
            ctx.pushState((ctx1) -> {
                ctx1.popState();
                return fillKey(ctx1, key);
            }, stateIndex);
            return false;
        }
        return fillKey(ctx, key);
    }

    public boolean fillKey(ParserContext ctx, String key) {
        Object val = ctx.popTarget();
        Map map = ctx.target();
        map.put(key, val);

        return true;
    }

    @Override
    public void beginObject(ParserContext ctx) {
        ctx.pushTarget(new HashMap());
    }

    @Override
    public void beginNullObject(ParserContext ctx) {
        ctx.pushTarget(ParserContext.NULL);
    }
    @Override
    public void endStringValue(ParserContext ctx) {
        String obj = ctx.popToken();
        if (obj == null) ctx.pushTarget(ParserContext.NULL);
        else ctx.pushTarget(obj);
    }

    @Override
    public void endNumberValue(ParserContext ctx) {
        Long obj = ctx.popLongObjectToken();
        if (obj == null) ctx.pushTarget(ParserContext.NULL);
        else ctx.pushTarget(obj);
    }

    @Override
    public void endFloatValue(ParserContext ctx) {
        Float obj = ctx.popFloatObjectToken();
        if (obj == null) ctx.pushTarget(ParserContext.NULL);
        else ctx.pushTarget(obj);
    }

    @Override
    public void endBooleanValue(ParserContext ctx) {
        Boolean obj = ctx.popBooleanObjectToken();
        if (obj == null) ctx.pushTarget(ParserContext.NULL);
        else ctx.pushTarget(obj);
    }
}
