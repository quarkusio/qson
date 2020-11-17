package io.quarkus.qson.deserializer;

import java.util.HashSet;

public class GenericSetParser extends BaseParser implements QsonParser {

    public static final GenericSetParser PARSER = new GenericSetParser();

    @Override
    public void beginList(ParserContext ctx) {
        ctx.pushTarget(new HashSet());
    }

}
