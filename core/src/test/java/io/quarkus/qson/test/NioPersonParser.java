package io.quarkus.qson.test;

import io.quarkus.qson.parser.BaseParser;
import io.quarkus.qson.parser.ContextValue;
import io.quarkus.qson.parser.GenericParser;
import io.quarkus.qson.parser.ListParser;
import io.quarkus.qson.parser.MapParser;
import io.quarkus.qson.parser.ObjectParser;
import io.quarkus.qson.parser.ParserContext;
import io.quarkus.qson.parser.ParserState;

import java.util.List;
import java.util.Map;

public class NioPersonParser extends ObjectParser {

    public static final NioPersonParser PARSER = new NioPersonParser();

    private static final MapParser intMap = new MapParser(ContextValue.STRING_VALUE, ContextValue.INT_VALUE,
            ObjectParser.PARSER.startIntegerValue, ObjectParser.PARSER.continueStartIntegerValue);
    private static final MapParser kids = new MapParser(ContextValue.STRING_VALUE,
            ContextValue.OBJECT_VALUE,
            PARSER.start, PARSER.continueStart);
    private static final ListParser siblings = new ListParser(ContextValue.OBJECT_VALUE,
            PARSER.start);
    private static final ListParser pets = new ListParser(ContextValue.STRING_VALUE,
            ObjectParser.PARSER.startStringValue);

    private static final ListParser nested__l = new ListParser(ContextValue.OBJECT_VALUE,
            PARSER.start);
    private static final MapParser nested = new MapParser(ContextValue.STRING_VALUE,
            ContextValue.OBJECT_VALUE,
            nested__l.start, nested__l.continueStart);
    @Override
    public void beginObject(ParserContext ctx) {
        ctx.pushTarget(new Person());
    }

    @Override
    public boolean key(ParserContext ctx) {
        int c = ctx.skipToQuote();
        if (c == 0) {
            ctx.pushState(continueKey);
            return false;
        }
        ctx.endToken();
        int index = 0;
        c = ctx.tokenCharAt(index++);
        int stateIndex = ctx.stateIndex();

        switch (c) {
            case 'a':
                if (ctx.compareToken(index, "ge")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(continueStartIntegerValue, stateIndex);
                        ctx.pushState(ageEnd, stateIndex);
                        return false;
                    }
                    if (!startIntegerValue(ctx)) {
                        ctx.pushState(ageEnd, stateIndex);
                        return false;
                    }
                    return ageEnd(ctx);
                }
                break;
            case 'g':
                if (ctx.compareToken(index, "enericMap")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(GenericParser.PARSER.continueStartObject, stateIndex);
                        ctx.pushState(genericMapEnd, stateIndex);
                        return false;
                    }
                    if (!GenericParser.PARSER.startObject(ctx)) {
                        ctx.pushState(genericMapEnd, stateIndex);
                        return false;
                    }
                    return genericMapEnd(ctx);
                }
                break;
            case 'm':
                c = ctx.tokenCharAt(index++);
                if (c == 'a') {
                    if (ctx.compareToken(index, "rried")) {
                        if (!valueSeparator(ctx)) {
                            ctx.pushState(continueStartBooleanValue, stateIndex);
                            ctx.pushState(marriedEnd, stateIndex);
                            return false;
                        }
                        if (!startBooleanValue(ctx)) {
                            ctx.pushState(marriedEnd, stateIndex);
                            return false;
                        }
                        return marriedEnd(ctx);
                    }
                } else if (c == 'o') {
                    if (ctx.compareToken(index, "ney")) {
                        if (!valueSeparator(ctx)) {
                            ctx.pushState(continueStartNumberValue, stateIndex);
                            ctx.pushState(moneyEnd, stateIndex);
                            return false;
                        }
                        if (!startNumberValue(ctx)) {
                            ctx.pushState(moneyEnd, stateIndex);
                            return false;
                        }
                        return moneyEnd(ctx);
                    }
                }
                break;
            case 'n':
                c = ctx.tokenCharAt(index++);
                if (c == 'a') {
                    if (ctx.compareToken(index, "me")) {
                        if (!valueSeparator(ctx)) {
                            ctx.pushState(continueStartStringValue, stateIndex);
                            ctx.pushState(nameEnd, stateIndex);
                            return false;
                        }
                        if (!startStringValue(ctx)) {
                            ctx.pushState(nameEnd, stateIndex);
                            return false;
                        }
                        return nameEnd(ctx);
                    }
                } else if (c == 'e') {
                    if (ctx.compareToken(index, "sted")) {
                        if (!valueSeparator(ctx)) {
                            ctx.pushState(nested.continueStart, stateIndex);
                            ctx.pushState(nestedEnd, stateIndex);
                            return false;
                        }
                        if (!nested.start(ctx)) {
                            ctx.pushState(nestedEnd, stateIndex);
                            return false;
                        }
                        return nestedEnd(ctx);
                    }

                }
                break;
            case 'i':
                if (ctx.compareToken(index, "ntMap")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(intMap.continueStart, stateIndex);
                        ctx.pushState(intMapEnd, stateIndex);
                        return false;
                    }
                    if (!intMap.start(ctx)) {
                        ctx.pushState(intMapEnd, stateIndex);
                        return false;
                    }
                    return intMapEnd(ctx);
                }
                break;
            case 'k':
                if (ctx.compareToken(index, "ids")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(kids.continueStart, stateIndex);
                        ctx.pushState(kidsEnd, stateIndex);
                        return false;
                    }
                    if (!kids.start(ctx)) {
                        ctx.pushState(kidsEnd, stateIndex);
                        return false;
                    }
                    return kidsEnd(ctx);
                }
                break;
            case 'd':
                if (ctx.compareToken(index, "ad")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(NioPersonParser.PARSER.continueStart, stateIndex);
                        ctx.pushState(dadEnd, stateIndex);
                        return false;
                    }
                    if (!NioPersonParser.PARSER.start.parse(ctx)) {
                        ctx.pushState(dadEnd, stateIndex);
                        return false;
                    }
                    return dadEnd(ctx);
                }
                break;
            case 'p':
                if (ctx.compareToken(index, "ets")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(pets.continueStart, stateIndex);
                        ctx.pushState(petsEnd, stateIndex);
                        return false;
                    }
                    if (!pets.start(ctx)) {
                        ctx.pushState(petsEnd, stateIndex);
                        return false;
                    }
                    return petsEnd(ctx);
                }
                break;
            case 's':
                if (ctx.compareToken(index, "iblings")) {
                    if (!valueSeparator(ctx)) {
                        ctx.pushState(siblings.continueStart, stateIndex);
                        ctx.pushState(siblingsEnd, stateIndex);
                        return false;
                    }
                    if (!siblings.start(ctx)) {
                        ctx.pushState(siblingsEnd, stateIndex);
                        return false;
                    }
                    return siblingsEnd(ctx);
                }
                break;
        }
        return BaseParser.PARSER.skipValue(ctx);
    }

    static final ParserState siblingsEnd = (ctx) -> {
        ctx.popState();
        return siblingsEnd(ctx);
    };

    private static final boolean siblingsEnd(ParserContext ctx) {
        List<Person> siblings = ctx.popTarget();
        Person person = ctx.target();
        person.setSiblings(siblings);
        return true;
    }

    static final ParserState petsEnd = (ctx) -> {
        ctx.popState();
        return petsEnd(ctx);
    };

    private static final boolean petsEnd(ParserContext ctx) {
        List<String> pets = ctx.popTarget();
        Person person = ctx.target();
        person.setPets(pets);
        return true;
    }

    static final ParserState dadEnd = (ctx) -> {
        ctx.popState();
        return dadEnd(ctx);
    };

    private static final boolean dadEnd(ParserContext ctx) {
        Person dad = ctx.popTarget();
        Person person = ctx.target();
        person.setDad(dad);
        return true;
    }

    static final ParserState kidsEnd = (ctx) -> {
        ctx.popState();
        return kidsEnd(ctx);
    };

    private static final boolean kidsEnd(ParserContext ctx) {
        Map<String, Person> kids = ctx.popTarget();
        Person person = ctx.target();
        person.setKids(kids);
        return true;
    }

    static final ParserState nestedEnd = (ctx) -> {
        ctx.popState();
        return nestedEnd(ctx);
    };

    private static final boolean nestedEnd(ParserContext ctx) {
        Map<String, List<Person>> nested = ctx.popTarget();
        Person person = ctx.target();
        person.setNested(nested);
        return true;
    }

    static final ParserState intMapEnd = (ctx) -> {
        ctx.popState();
        return intMapEnd(ctx);
    };

    private static final boolean intMapEnd(ParserContext ctx) {
        Map<String, Integer> intMap = ctx.popTarget();
        Person person = ctx.target();
        person.setIntMap(intMap);
        return true;
    }

    static final ParserState nameEnd = (ctx) -> {
        ctx.popState();
        return nameEnd(ctx);
    };

    private static final boolean nameEnd(ParserContext ctx) {
        Person person = ctx.target();
        person.setName(ctx.popToken());
        return true;
    }

    static final ParserState moneyEnd = (ctx) -> {
        ctx.popState();
        return moneyEnd(ctx);
    };

    private static final boolean moneyEnd(ParserContext ctx) {
        float value = ctx.popFloatToken();
        Person person = ctx.target();
        person.setMoney(value);
        return true;
    }

    static final ParserState marriedEnd = (ctx) -> {
        ctx.popState();
        return marriedEnd(ctx);
    };

    private static final boolean marriedEnd(ParserContext ctx) {
        boolean value = ctx.popBooleanToken();
        Person person = ctx.target();
        person.setMarried(value);
        return true;
    }

    static final ParserState ageEnd = (ctx) -> {
        ctx.popState();
        return ageEnd(ctx);
    };

    private static final boolean ageEnd(ParserContext ctx) {
        int value = ctx.popIntToken();
        Person person = ctx.target();
        person.setAge(value);
        return true;
    }
    static final ParserState genericMapEnd = (ctx) -> {
        ctx.popState();
        return genericMapEnd(ctx);
    };

    private static final boolean genericMapEnd(ParserContext ctx) {
        Map value = ctx.popTarget();
        Person person = ctx.target();
        person.setGenericMap(value);
        return true;
    }
}
