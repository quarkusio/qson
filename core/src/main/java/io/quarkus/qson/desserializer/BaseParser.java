package io.quarkus.qson.desserializer;

import static io.quarkus.qson.IntChar.*;

public class BaseParser implements JsonParser {
    public static final BaseParser PARSER = new BaseParser();

    protected BaseParser() {
        // can only be subclassed
    }

    @Override
    public <T> T getTarget(ParserContext ctx) {
        return ctx.target();
    }

    @Override
    public ParserState parser() {
        return start;
    }

    // we do fields to avoid object creations
    // as method references create a new object every time
    // they are referenced
    public final ParserState start  = this::start;
    public final ParserState startStringValue  = this::startStringValue;
    public final ParserState startIntegerValue  = this::startIntegerValue;
    public final ParserState startBooleanValue  = this::startBooleanValue;
    public final ParserState startNumberValue  = this::startNumberValue;
    public final ParserState continueAddListValue = this::continueAddListValue;
    public final ParserState continueStartStringValue = this::continueStartStringValue;
    public final ParserState continueStartBooleanValue = this::continueStartBooleanValue;
    public final ParserState continueValue= this::continueValue;
    public final ParserState continueLoopListValues = this::continueLoopListValues;
    public final ParserState continueNextListValues = this::continueNextListValues;
    public final ParserState continueValueSeparator = this::continueValueSeparator;
    public final ParserState continueStringValue = this::continueStringValue;
    public final ParserState continueStartIntegerValue = this::continueStartIntegerValue;
    public final ParserState continueIntegerValue = this::continueIntegerValue;
    public final ParserState continueStartNumberValue = this::continueStartNumberValue;
    public final ParserState continueNumberValue = this::continueNumberValue;
    public final ParserState continueFloatValue = this::continueFloatValue;
    public final ParserState continueBooleanValue = this::continueBooleanValue;
    public final ParserState continueStartObject = this::continueStartObject;
    public final ParserState continueStart = this::continueStart;
    public final ParserState continueLoopKeys = this::continueLoopKeys;
    public final ParserState continueNextKeys = this::continueNextKeys;
    public final ParserState continueKey = this::continueKey;
    public final ParserState continueStartList = this::continueStartList;


    public boolean continueStart(ParserContext ctx) {
        ctx.popState();
        return start(ctx);
    }

    public boolean start(ParserContext ctx) {
        return value(ctx);
    }

    public boolean continueStartList(ParserContext ctx) {
        ctx.popState();
        return startList(ctx);
    }

    public boolean startList(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartList);
            return false;
        }
        if (c == INT_LBRACKET) {
            beginList(ctx);
            return loopListValues(ctx);
        } else {
            throw new RuntimeException("Expecting start of array");
        }
    }


    public boolean continueStartStringValue(ParserContext ctx) {
        ctx.popState();
        return startStringValue(ctx);
    }

    public boolean startStringValue(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartStringValue);
            return false;
        }
        if (c == INT_QUOTE) {
            startTokenNextConsumed(ctx);
            return stringValue(ctx);
        } else {
            throw new RuntimeException("Illegal value syntax");
        }
    }

    public void startTokenNextConsumed(ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public boolean continueStartBooleanValue(ParserContext ctx) {
        ctx.popState();
        return startBooleanValue(ctx);
    }

    public boolean startBooleanValue(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartBooleanValue);
            return false;
        }
        if (c== 't' || c == 'f') {
            startToken(ctx);
            return booleanValue(ctx);
        } else {
            throw new RuntimeException("Illegal value syntax");
        }
    }

    public void startToken(ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public boolean continueValue(ParserContext ctx) {
        ctx.popState();
        return value(ctx);
    }

    public boolean value(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueValue);
            return false;
        }
        if (c == INT_QUOTE) {
            startTokenNextConsumed(ctx);
            return stringValue(ctx);
        } else if (isDigit(c) || c == INT_MINUS || c == INT_PLUS) {
            startToken(ctx);
            return numberValue(ctx);
        } else if (c == INT_t || c == INT_f) {
            startToken(ctx);
            return booleanValue(ctx);
        } else if (c == INT_LCURLY) {
            beginObject(ctx);
            return loopKeys(ctx);
        } else if (c == INT_LBRACKET) {
            beginList(ctx);
            return loopListValues(ctx);
        } else {
            throw new RuntimeException("Illegal value syntax");
        }
    }

    public boolean continueLoopListValues(ParserContext ctx) {
        ctx.popState();
        return loopListValues(ctx);
    }

    public boolean continueAddListValue(ParserContext ctx) {
        ctx.popState();
        addListValue(ctx);
        return true;
    }

    public boolean loopListValues(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueLoopListValues);
            return false;
        }
        if (c == INT_RBRACKET) {
            return true;
        }
        ctx.rewind();
        int stateIndex = ctx.stateIndex();
        if (!listValue(ctx)) {
            ctx.pushState(continueAddListValue, stateIndex);
            ctx.pushState(continueNextListValues, stateIndex);
            return false;
        }
        addListValue(ctx);
        return nextListValues(ctx);
    }

    public boolean continueNextListValues(ParserContext ctx) {
        ctx.popState();
        return nextListValues(ctx);
    }

    public boolean nextListValues(ParserContext ctx) {
        while (true) {
            int c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueNextListValues);
                return false;
            }
            if (c == INT_RBRACKET) {
                return true;
            }
            if (c != INT_COMMA) throw new RuntimeException("Expecting comma separator");
            int stateIndex = ctx.stateIndex();
            if (!listValue(ctx)) {
                ctx.pushState(continueAddListValue, stateIndex);
                ctx.pushState(continueNextListValues, stateIndex);
                return false;
            }
            addListValue(ctx);
        }
    }

    public boolean listValue(ParserContext ctx) {
        return value(ctx);
    }

    public void beginList(ParserContext ctx) {

    }

    public void addListValue(ParserContext ctx) {
    }

    public void beginObject(ParserContext ctx) {
    }

    public boolean continueValueSeparator(ParserContext ctx) {
        ctx.popState();
        return valueSeparator(ctx);
    }


    public boolean valueSeparator(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueValueSeparator);
            return false;
        }
        if (c != INT_COLON) throw new RuntimeException("Expecting ':' key value separator");
        return true;
    }

    public boolean continueStringValue(ParserContext ctx) {
        ctx.popState();
        return stringValue(ctx);
    }

    public boolean stringValue(ParserContext ctx) {
        int c = ctx.skipToQuote();
        if (c == 0) {
            ctx.pushState(continueStringValue);
            return false;
        }
        endToken(ctx);
        endStringValue(ctx);
        return true;
    }

    public void endToken(ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public void endStringValue(ParserContext ctx) {

    }

    public boolean continueStartIntegerValue(ParserContext ctx) {
        ctx.popState();
        return startIntegerValue(ctx);
    }

    public boolean startIntegerValue(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartIntegerValue);
            return false;
        }
        if (isDigit(c) || c == INT_MINUS || c == INT_PLUS) {
            startToken(ctx);
            return integerValue(ctx);
        } else {
            throw new RuntimeException("Illegal integer value");
        }
    }

    public boolean continueIntegerValue(ParserContext ctx) {
        ctx.popState();
        return integerValue(ctx);
    }

    public boolean integerValue(ParserContext ctx) {
        int c = ctx.skipDigits();
        if (c == 0) {
            ctx.pushState(continueIntegerValue);
            return false;
        }
        endToken(ctx);
        endNumberValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endNumberValue(ParserContext ctx) {

    }

    public boolean continueStartNumberValue(ParserContext ctx) {
        ctx.popState();
        return startNumberValue(ctx);
    }
    public boolean startNumberValue(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartNumberValue);
            return false;
        }
        if (isDigit(c) || c == INT_MINUS || c == INT_PLUS) {
            startToken(ctx);
            return numberValue(ctx);
        } else {
            throw new RuntimeException("Illegal number value");
        }
    }

    public boolean continueNumberValue(ParserContext ctx) {
        ctx.popState();
        return numberValue(ctx);
    }

    public boolean numberValue(ParserContext ctx) {
        int c = ctx.skipDigits();
        if (c == 0) {
            ctx.pushState(continueNumberValue);
            return false;
        }
        if (c == INT_PERIOD) {
            return floatValue(ctx);
        } else {
            endToken(ctx);
            endNumberValue(ctx);
            ctx.rewind();
            return true;
        }
    }

    public boolean continueFloatValue(ParserContext ctx) {
        ctx.popState();
        return floatValue(ctx);
    }

    public boolean floatValue(ParserContext ctx) {
        int c = ctx.skipDigits();
        if (c == 0) {
            ctx.pushState(continueFloatValue);
            return false;
        }
        endToken(ctx);
        endFloatValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endFloatValue(ParserContext ctx) {

    }

    public boolean continueBooleanValue(ParserContext ctx) {
        ctx.popState();
        return booleanValue(ctx);
    }

    public boolean booleanValue(ParserContext ctx) {
        int c = ctx.skipAlphabetic();
        if (c == 0) {
            ctx.pushState(continueBooleanValue);
            return false;
        }
        endToken(ctx);
        endBooleanValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endBooleanValue(ParserContext ctx) {

    }

    public boolean startObject(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueStartObject);
            return false;
        }
        return handleObject(ctx, c);
    }

    public boolean continueStartObject(ParserContext ctx) {
        ctx.popState();
        return startObject(ctx);
    }

    public boolean handleObject(ParserContext ctx, int c) {
        if (c == INT_LCURLY) {
            beginObject(ctx);
            return loopKeys(ctx);
        } else {
            throw new RuntimeException("Illegal value syntax");
        }
    }
    public boolean continueLoopKeys(ParserContext ctx) {
        ctx.popState();
        return loopKeys(ctx);
    }

    public boolean loopKeys(ParserContext ctx) {
        int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueLoopKeys);
            return false;
        }
        if (c == INT_RCURLY) {
            return true;
        }
        if (c != INT_QUOTE) throw new RuntimeException("Expecting key quote");
        startTokenNextConsumed(ctx);
        int stateIndex = ctx.stateIndex();
        if (!key(ctx)) {
            ctx.pushState(continueNextKeys, stateIndex);
            return false;
        }
        return nextKeys(ctx);
    }

    public boolean continueNextKeys(ParserContext ctx) {
        ctx.popState();
        return nextKeys(ctx);
    }

    public boolean nextKeys(ParserContext ctx) {
        do {
            int c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueNextKeys);
                return false;
            }
            if (c == INT_RCURLY) {
                return true;
            }

            if (c != INT_COMMA) throw new RuntimeException("Expecting comma separator");
            c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueLoopKeys);
                return false;
            }
            if (c != INT_QUOTE) throw new RuntimeException("Expecting key quote");
            startTokenNextConsumed(ctx);
            int stateIndex = ctx.stateIndex();
            if (!key(ctx)) {
                ctx.pushState(continueNextKeys, stateIndex);
                return false;
            }
        } while (true);
    }

    public boolean key(ParserContext ctx) {
        int c = ctx.skipToQuote();
        if (c == 0) {
            ctx.pushState(continueKey);
            return false;
        }
        return skipValue(ctx);
    }

    public boolean skipValue(ParserContext ctx) {
        ctx.clearToken();
        int stateIndex = ctx.stateIndex();
        if (!valueSeparator(ctx)) {
            ctx.pushState(continueValue, stateIndex);
           return false;
        }
        return value(ctx);
    }

    public boolean continueKey(ParserContext ctx) {
        ctx.popState();
        return key(ctx);
    }

}
