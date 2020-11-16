package io.quarkus.qson.deserializer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface ContextValue {
    Object value(ParserContext ctx);

    ContextValue BYTE_VALUE = (ctx) -> Byte.valueOf(ctx.popToken());
    ContextValue BOOLEAN_VALUE = (ctx) -> ctx.popBooleanToken();
    ContextValue INT_VALUE = (ctx) -> ctx.popIntToken();
    ContextValue SHORT_VALUE = (ctx) -> Short.valueOf(ctx.popToken());
    ContextValue LONG_VALUE = (ctx) -> ctx.popLongToken();
    ContextValue FLOAT_VALUE = (ctx) -> Float.valueOf(ctx.popToken());
    ContextValue DOUBLE_VALUE = (ctx) -> Double.valueOf(ctx.popToken());
    ContextValue OFFSET_DATETIME_VALUE = (ctx) -> OffsetDateTime.parse(ctx.popToken());
    ContextValue BIGDECIMAL_VALUE = (ctx) -> new BigDecimal(ctx.popToken());
    ContextValue STRING_VALUE = (ctx) -> ctx.popToken();
    ContextValue OBJECT_VALUE = (ctx) -> ctx.popTarget();
    ContextValue CHAR_VALUE = (ctx) -> {
        String val = ctx.popToken();
        if (val.length() != 1) throw new RuntimeException("Expecting single character for string value");
        return val.charAt(0);
    };
}
