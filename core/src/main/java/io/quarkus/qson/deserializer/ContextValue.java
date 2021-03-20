package io.quarkus.qson.deserializer;

import io.quarkus.qson.QsonException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ContextValue {
    Object value(ParserContext ctx);

    ContextValue BYTE_VALUE = ParserContext::popByteObjectToken;
    ContextValue BOOLEAN_VALUE = ParserContext::popBooleanObjectToken;
    ContextValue INT_VALUE = ParserContext::popIntObjectToken;
    ContextValue SHORT_VALUE = ParserContext::popShortObjectToken;
    ContextValue LONG_VALUE = ParserContext::popLongObjectToken;
    ContextValue FLOAT_VALUE = ParserContext::popFloatObjectToken;
    ContextValue DOUBLE_VALUE = ParserContext::popDoubleObjectToken;
    ContextValue OFFSET_DATETIME_VALUE = (ctx) -> {
        String token = ctx.popToken();
        if (token == null) return null;
        return OffsetDateTime.parse(token);
    };
    ContextValue BIGDECIMAL_VALUE = (ctx) -> {
        String val = ctx.popToken();
        if (val == null) return null;
        return new BigDecimal(val);
    };
    ContextValue STRING_VALUE = ParserContext::popToken;
    ContextValue OBJECT_VALUE = ParserContext::popTarget;
    ContextValue CHAR_VALUE = (ctx) -> {
        String val = ctx.popToken();
        if (val == null) return null;
        if (val.length() != 1) throw new QsonException("Expecting single character for string value");
        return val.charAt(0);
    };
}
