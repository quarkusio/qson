package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonException;

import static io.quarkus.qson.util.IntChar.INT_0;
import static io.quarkus.qson.util.IntChar.INT_1;
import static io.quarkus.qson.util.IntChar.INT_2;
import static io.quarkus.qson.util.IntChar.INT_3;
import static io.quarkus.qson.util.IntChar.INT_4;
import static io.quarkus.qson.util.IntChar.INT_5;
import static io.quarkus.qson.util.IntChar.INT_6;
import static io.quarkus.qson.util.IntChar.INT_7;
import static io.quarkus.qson.util.IntChar.INT_8;
import static io.quarkus.qson.util.IntChar.INT_9;
import static io.quarkus.qson.util.IntChar.INT_COLON;
import static io.quarkus.qson.util.IntChar.INT_COMMA;
import static io.quarkus.qson.util.IntChar.INT_EOF;
import static io.quarkus.qson.util.IntChar.INT_LBRACKET;
import static io.quarkus.qson.util.IntChar.INT_LCURLY;
import static io.quarkus.qson.util.IntChar.INT_MINUS;
import static io.quarkus.qson.util.IntChar.INT_PERIOD;
import static io.quarkus.qson.util.IntChar.INT_PLUS;
import static io.quarkus.qson.util.IntChar.INT_QUOTE;
import static io.quarkus.qson.util.IntChar.INT_RBRACKET;
import static io.quarkus.qson.util.IntChar.INT_RCURLY;
import static io.quarkus.qson.util.IntChar.INT_f;
import static io.quarkus.qson.util.IntChar.INT_n;
import static io.quarkus.qson.util.IntChar.INT_t;

public class BaseParser implements QsonParser {

    public static final BaseParser PARSER = new BaseParser();

    protected BaseParser() {
        // can only be subclassed
    }

    @Override
    public <T> T getTarget(final ParserContext ctx) {
        return ctx.target();
    }

    @Override
    public ParserState startState() {
        return start;
    }

    // we do fields to avoid object creations
    // as method references create a new object every time
    // they are referenced
    public ParserState start  = this::start;
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
    public final ParserState continueNullValue = this::continueNullValue;
    public final ParserState continueNullObject = this::continueNullObject;
    public final ParserState continueStartObject = this::continueStartObject;
    public ParserState continueStart = this::continueStart;
    public final ParserState continueLoopKeys = this::continueLoopKeys;
    public final ParserState continueNextKeys = this::continueNextKeys;
    public final ParserState continueKey = this::continueKey;
    public final ParserState continueStartList = this::continueStartList;

    public boolean continueStart(final ParserContext ctx) {
        ctx.popState();
        return start(ctx);
    }

    public boolean start(final ParserContext ctx) {
        return value(ctx);
    }

    public boolean continueStartList(final ParserContext ctx) {
        ctx.popState();
        return startList(ctx);
    }

    public boolean startList(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartList);
                return false;
            case INT_LBRACKET:
                beginList(ctx);
                return loopListValues(ctx);
            case INT_n:
                beginNullObject(ctx);
                return nullObject(ctx);
            default:
                throw new QsonException("Expecting start of array");
        }
    }

    public boolean continueStartStringValue(final ParserContext ctx) {
        ctx.popState();
        return startStringValue(ctx);
    }

    public boolean startStringValue(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartStringValue);
                return false;
            case INT_QUOTE:
                startTokenNextConsumed(ctx);
                return stringValue(ctx);
            case INT_n:
                startNullToken(ctx);
                return nullValue(ctx);
            default:
                throw new QsonException("Illegal value syntax");
        }
    }

    public void startTokenNextConsumed(final ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public boolean continueStartBooleanValue(final ParserContext ctx) {
        ctx.popState();
        return startBooleanValue(ctx);
    }

    public boolean startBooleanValue(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartBooleanValue);
                return false;
            case INT_t:
            case INT_f:
                startToken(ctx);
                return booleanValue(ctx);
            case INT_n:
                startNullToken(ctx);
                return nullValue(ctx);
            default:
                throw new QsonException("Illegal value syntax");
        }
    }

    public void startNullToken(final ParserContext ctx) {
    }

    public void startToken(final ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public boolean continueValue(final ParserContext ctx) {
        ctx.popState();
        return value(ctx);
    }

    public boolean value(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueValue);
                return false;
            case INT_QUOTE:
                startTokenNextConsumed(ctx);
                return stringValue(ctx);
            case INT_0:
            case INT_1:
            case INT_2:
            case INT_3:
            case INT_4:
            case INT_5:
            case INT_6:
            case INT_7:
            case INT_8:
            case INT_9:
            case INT_MINUS:
            case INT_PLUS:
                startToken(ctx);
                return numberValue(ctx);
            case INT_t:
            case INT_f:
                startToken(ctx);
                return booleanValue(ctx);
            case INT_n:
                startNullToken(ctx);
                return nullValue(ctx);
            case INT_LCURLY:
                beginObject(ctx);
                return loopKeys(ctx);
            case INT_LBRACKET:
                beginList(ctx);
                return loopListValues(ctx);
            default:
                throw new QsonException("Illegal value syntax");
        }
    }

    public boolean continueLoopListValues(final ParserContext ctx) {
        ctx.popState();
        return loopListValues(ctx);
    }

    public boolean continueAddListValue(final ParserContext ctx) {
        ctx.popState();
        addListValue(ctx);
        return true;
    }

    public boolean loopListValues(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueLoopListValues);
                return false;
            case INT_RBRACKET:
                return true;
            default:
                ctx.rewind();
                final int stateIndex = ctx.stateIndex();
                if (!listValue(ctx)) {
                    ctx.pushState(continueAddListValue, stateIndex);
                    ctx.pushState(continueNextListValues, stateIndex);
                    return false;
                }
                addListValue(ctx);
                return nextListValues(ctx);
        }
    }

    public boolean continueNextListValues(final ParserContext ctx) {
        ctx.popState();
        return nextListValues(ctx);
    }

    public boolean nextListValues(final ParserContext ctx) {
        while (true) {
            final int c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueNextListValues);
                return false;
            }
            if (c == INT_RBRACKET) {
                return true;
            }
            if (c != INT_COMMA) throw new QsonException("Expecting comma separator");
            int stateIndex = ctx.stateIndex();
            if (!listValue(ctx)) {
                ctx.pushState(continueAddListValue, stateIndex);
                ctx.pushState(continueNextListValues, stateIndex);
                return false;
            }
            addListValue(ctx);
        }
    }

    public boolean listValue(final ParserContext ctx) {
        return value(ctx);
    }

    public void beginList(final ParserContext ctx) {

    }

    public void addListValue(final ParserContext ctx) {
    }

    public void beginObject(final ParserContext ctx) {
    }

    public boolean continueValueSeparator(final ParserContext ctx) {
        ctx.popState();
        return valueSeparator(ctx);
    }

    public boolean valueSeparator(final ParserContext ctx) {
        final int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueValueSeparator);
            return false;
        }
        if (c != INT_COLON) throw new QsonException("Expecting ':' key value separator");
        return true;
    }

    public boolean continueStringValue(final ParserContext ctx) {
        ctx.popState();
        return stringValue(ctx);
    }

    public boolean stringValue(final ParserContext ctx) {
        switch (ctx.skipToQuote()) {
            case 0:
                ctx.pushState(continueStringValue);
                return false;
            case INT_EOF:
                throw new QsonException("String does not have end quote");
            default:
                endToken(ctx);
                endStringValue(ctx);
                return true;
        }
    }

    public void endToken(final ParserContext ctx) {
        // complete, do nothing if skipping
    }

    public void endStringValue(final ParserContext ctx) {

    }

    public boolean continueStartIntegerValue(final ParserContext ctx) {
        ctx.popState();
        return startIntegerValue(ctx);
    }

    public boolean startIntegerValue(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartIntegerValue);
                return false;
            case INT_0:
            case INT_1:
            case INT_2:
            case INT_3:
            case INT_4:
            case INT_5:
            case INT_6:
            case INT_7:
            case INT_8:
            case INT_9:
            case INT_MINUS:
            case INT_PLUS:
                startToken(ctx);
                return integerValue(ctx);
            case INT_n:
                startNullToken(ctx);
                return nullValue(ctx);
            default:
                throw new QsonException("Illegal integer value");
        }
    }

    public boolean continueIntegerValue(final ParserContext ctx) {
        ctx.popState();
        return integerValue(ctx);
    }

    public boolean integerValue(final ParserContext ctx) {
        if (ctx.skipDigits() == 0) {
            ctx.pushState(continueIntegerValue);
            return false;
        }
        endToken(ctx);
        endNumberValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endNumberValue(final ParserContext ctx) {

    }

    public boolean continueStartNumberValue(final ParserContext ctx) {
        ctx.popState();
        return startNumberValue(ctx);
    }

    public boolean startNumberValue(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartNumberValue);
                return false;
            case INT_0:
            case INT_1:
            case INT_2:
            case INT_3:
            case INT_4:
            case INT_5:
            case INT_6:
            case INT_7:
            case INT_8:
            case INT_9:
            case INT_MINUS:
            case INT_PLUS:
                startToken(ctx);
                return numberValue(ctx);
            case INT_n:
                startNullToken(ctx);
                return nullValue(ctx);
            default:
                throw new QsonException("Illegal number value");
        }
    }

    public boolean continueNumberValue(final ParserContext ctx) {
        ctx.popState();
        return numberValue(ctx);
    }

    public boolean numberValue(final ParserContext ctx) {
        switch (ctx.skipDigits()) {
            case 0:
                ctx.pushState(continueNumberValue);
                return false;
            case INT_PERIOD:
                return floatValue(ctx);
            default:
                endToken(ctx);
                endNumberValue(ctx);
                ctx.rewind();
                return true;
        }
    }

    public boolean continueFloatValue(final ParserContext ctx) {
        ctx.popState();
        return floatValue(ctx);
    }

    public boolean floatValue(final ParserContext ctx) {
        if (ctx.skipDigits() == 0) {
            ctx.pushState(continueFloatValue);
            return false;
        }
        endToken(ctx);
        endFloatValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endFloatValue(final ParserContext ctx) {

    }

    public boolean continueBooleanValue(final ParserContext ctx) {
        ctx.popState();
        return booleanValue(ctx);
    }

    public boolean booleanValue(final ParserContext ctx) {
        if (ctx.skipAlphabetic() == 0) {
            ctx.pushState(continueBooleanValue);
            return false;
        }
        endToken(ctx);
        endBooleanValue(ctx);
        ctx.rewind();
        return true;
    }

    public void endBooleanValue(final ParserContext ctx) {

    }

    public boolean continueNullValue(final ParserContext ctx) {
        ctx.popState();
        return nullValue(ctx);
    }

    public boolean nullValue(final ParserContext ctx) {
        if (ctx.skipAlphabetic() == 0) {
            ctx.pushState(continueNullValue);
            return false;
        }
        endNullValue(ctx);
        ctx.rewind();
        return true;
    }

    public boolean continueNullObject(final ParserContext ctx) {
        ctx.popState();
        return nullValue(ctx);
    }

    public boolean nullObject(final ParserContext ctx) {
        if (ctx.skipAlphabetic() == 0) {
            ctx.pushState(continueNullObject);
            return false;
        }
        ctx.rewind();
        return true;
    }

    public void endNullValue(final ParserContext ctx) {

    }

    public boolean startObject(final ParserContext ctx) {
        switch (ctx.skipWhitespace()) {
            case 0:
                ctx.pushState(continueStartObject);
                return false;
            case INT_LCURLY:
                beginObject(ctx);
                return loopKeys(ctx);
            case INT_n:
                beginNullObject(ctx);
                return nullObject(ctx);
            default:
                throw new QsonException("Illegal value syntax");
        }
    }

    public void beginNullObject(final ParserContext ctx) {

    }

    public boolean continueStartObject(final ParserContext ctx) {
        ctx.popState();
        return startObject(ctx);
    }

    public boolean continueLoopKeys(final ParserContext ctx) {
        ctx.popState();
        return loopKeys(ctx);
    }

    public boolean loopKeys(final ParserContext ctx) {
        final int c = ctx.skipWhitespace();
        if (c == 0) {
            ctx.pushState(continueLoopKeys);
            return false;
        }
        if (c == INT_RCURLY) {
            return true;
        }
        if (c != INT_QUOTE) throw new QsonException("Expecting key quote");
        startTokenNextConsumed(ctx);
        final int stateIndex = ctx.stateIndex();
        if (!key(ctx)) {
            ctx.pushState(continueNextKeys, stateIndex);
            return false;
        }
        return nextKeys(ctx);
    }

    public boolean continueNextKeys(final ParserContext ctx) {
        ctx.popState();
        return nextKeys(ctx);
    }

    public boolean nextKeys(final ParserContext ctx) {
        do {
            int c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueNextKeys);
                return false;
            }
            if (c == INT_RCURLY) {
                return true;
            }

            if (c != INT_COMMA) throw new QsonException("Expecting comma separator");
            c = ctx.skipWhitespace();
            if (c == 0) {
                ctx.pushState(continueLoopKeys);
                return false;
            }
            if (c != INT_QUOTE) throw new QsonException("Expecting key quote");
            startTokenNextConsumed(ctx);
            final int stateIndex = ctx.stateIndex();
            if (!key(ctx)) {
                ctx.pushState(continueNextKeys, stateIndex);
                return false;
            }
        } while (true);
    }

    public boolean key(final ParserContext ctx) {
        if (ctx.skipToQuote() == 0) {
            ctx.pushState(continueKey);
            return false;
        }
        return skipValue(ctx);
    }

    public boolean skipValue(final ParserContext ctx) {
        ctx.clearToken();
        final int stateIndex = ctx.stateIndex();
        if (!valueSeparator(ctx)) {
            ctx.pushState(continueValue, stateIndex);
            return false;
        }
        return value(ctx);
    }

    public boolean continueKey(final ParserContext ctx) {
        ctx.popState();
        return key(ctx);
    }

}
