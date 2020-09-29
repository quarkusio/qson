package io.quarkus.qson.desserializer;

public class ObjectParser extends BaseParser {
    public static final ObjectParser PARSER = new ObjectParser();

    protected ObjectParser() {
        // can only be subclassed
    }

    public boolean start(ParserContext ctx) {
        return startObject(ctx);
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


}
