package io.quarkus.qson.parser;

import io.quarkus.qson.QsonException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ContextValue {
    Object value(ParserContext ctx);

    ContextValue BYTE_VALUE = (ctx) -> ctx.popByteObjectToken();
    ContextValue BOOLEAN_VALUE = (ctx) -> ctx.popBooleanObjectToken();
    ContextValue INT_VALUE = (ctx) -> ctx.popIntObjectToken();
    ContextValue SHORT_VALUE = (ctx) -> ctx.popShortObjectToken();
    ContextValue LONG_VALUE = (ctx) -> ctx.popLongObjectToken();
    ContextValue FLOAT_VALUE = (ctx) -> ctx.popFloatObjectToken();
    ContextValue DOUBLE_VALUE = (ctx) -> ctx.popDoubleObjectToken();
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
    ContextValue STRING_VALUE = (ctx) -> ctx.popToken();
    ContextValue OBJECT_VALUE = (ctx) -> ctx.popTarget();
    ContextValue CHAR_VALUE = (ctx) -> {
        String val = ctx.popToken();
        if (val == null) return null;
        if (val.length() != 1) throw new QsonException("Expecting single character for string value");
        return val.charAt(0);
    };
}
