/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sample;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import io.quarkus.qson.desserializer.ByteArrayParserContext;
import io.quarkus.qson.desserializer.JsonParser;
import io.quarkus.qson.generator.QsonMapper;
import io.quarkus.qson.serializer.ByteArrayJsonWriter;
import io.quarkus.qson.serializer.ObjectWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
public class MyBenchmark {

    static String json = "{\n" +
            "  \"intMap\": {\n" +
            "    \"one\": 1,\n" +
            "    \"two\": 2\n" +
            "  },\n" +
            "  \"genericMap\": {\n" +
            "    \"three\": 3,\n" +
            "    \"four\": 4\n" +
            "  },\n" +
            "  \"genericList\": [\n" +
            "    \"a\",\n" +
            "    \"b\"\n" +
            "  ],\n" +
            "  \"name\": \"Bill\",\n" +
            " \"nested\": {\n" +
            "  \"one\": [\n" +
            "    {\n" +
            "      \"name\": \"Ritchie\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Joani\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"two\": [\n" +
            "    {\n" +
            "      \"name\": \"Fonzi\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Potsi\"\n" +
            "    }\n" +
            "  ]\n" +
            "},\n" +
            "  \"age\": 50,\n" +
            "  \"money\": 123.23,\n" +
            "  \"married\": true,\n" +
            "  \"junkInt\": 666,\n" +
            "  \"pets\": [ \"itchy\", \"scratchy\"],\n" +
            "  \"junkFloat\": 6.66,\n" +
            "  \"kids\": {\n" +
            "    \"Sammy\": {\n" +
            "      \"name\": \"Sammy\",\n" +
            "      \"age\": 6\n" +
            "    },\n" +
            "    \"Suzi\": {\n" +
            "      \"name\": \"Suzi\",\n" +
            "      \"age\": 7\n" +
            "    }\n" +
            "  },\n" +
            "  \"siblings\": [\n" +
            "    {\n" +
            "      \"name\": \"Ritchie\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Joani\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"junkList\": [\"1\", \"2\"]," +
            "  \"junkBool\": true,\n" +
            "  \"junkMap\": {\n" +
            "    \"foo\": \"bar\",\n" +
            "    \"one\": 1,\n" +
            "    \"list\": [1, 2, 3, 4]\n" +
            "  },\n" +
            "  \"dad\": {\n" +
            "    \"name\": \"John\",\n" +
            "    \"married\": true\n" +
            "  }\n" +
            "}";

    @State(Scope.Benchmark)
    public static class QsonWriter {
        public ObjectWriter objectWriter;
        public Person2 person;

        @Setup(Level.Trial)
        public void setup() {
            QsonMapper mapper = new QsonMapper();
            person = mapper.read(json, Person2.class);
            objectWriter = mapper.writerFor(Person2.class);
        }
    }

    @State(Scope.Benchmark)
    public static class JacksonWriter {
        public com.fasterxml.jackson.databind.ObjectWriter objectWriter;
        public Person2 person;

        @Setup(Level.Trial)
        public void setup() {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            objectWriter = mapper.writerFor(Person2.class);
            try {
                person = mapper.readerFor(Person2.class).readValue(new StringReader(json));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class AfterburnerWriter {
        public com.fasterxml.jackson.databind.ObjectWriter objectWriter;
        public Person2 person;

        @Setup(Level.Trial)
        public void setup() {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            mapper.registerModule(new AfterburnerModule());
            objectWriter = mapper.writerFor(Person2.class);
            try {
                person = mapper.readerFor(Person2.class).readValue(new StringReader(json));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @State(Scope.Benchmark)
    public static class SimpleQsonWriter {
        public ObjectWriter objectWriter;
        public Simple simple;

        @Setup(Level.Trial)
        public void setup() {
            simple = createSimple();
            QsonMapper mapper = new QsonMapper();
            objectWriter = mapper.writerFor(Simple.class);
        }
    }

    @State(Scope.Benchmark)
    public static class SimpleJacksonWriter {
        public com.fasterxml.jackson.databind.ObjectWriter objectWriter;
        public Simple simple;

        @Setup(Level.Trial)
        public void setup() {
            simple = createSimple();
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            objectWriter = mapper.writerFor(Simple.class);
        }
    }

    @State(Scope.Benchmark)
    public static class SimpleAfterburnerWriter {
        public com.fasterxml.jackson.databind.ObjectWriter objectWriter;
        public Simple simple;

        @Setup(Level.Trial)
        public void setup() {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            mapper.registerModule(new AfterburnerModule());
            objectWriter = mapper.writerFor(Simple.class);
            simple = createSimple();
        }
    }

    static Simple createSimple() {
        Simple simple = new Simple();
        simple.setName("Bill Belichick");
        simple.setAge(67);
        simple.setMarried(false);
        simple.setMoney(1234567.21f);
        return simple;
    }

    @State(Scope.Benchmark)
    public static class QsonParser {
        public JsonParser parser;
        public byte[] jsonBytes;


        @Setup(Level.Trial)
        public void setup() {
            QsonMapper mapper = new QsonMapper();
            parser = mapper.parserFor(Person2.class);

            try {
                jsonBytes = json.getBytes("UTF-8");
            } catch (Exception e) {
                throw new RuntimeException();
            }

        }

    }

    @State(Scope.Benchmark)
    public static class JacksonParser {
        public ObjectReader reader;
        public byte[] jsonBytes;

        @Setup(Level.Trial)
        public void setup() {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            reader = mapper.readerFor(Person2.class);
            try {
                jsonBytes = json.getBytes("UTF-8");
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

    @State(Scope.Benchmark)
    public static class AfterburnerParser {
        public ObjectReader reader;
        public byte[] jsonBytes;

        @Setup(Level.Trial)
        public void setup() {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, false);
            mapper.registerModule(new AfterburnerModule());
            reader = mapper.readerFor(Person2.class);
            try {
                jsonBytes = json.getBytes("UTF-8");
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

    @Benchmark
    public Object testParserQson(QsonParser q) {
        ByteArrayParserContext ctx = new ByteArrayParserContext(q.parser);
        return ctx.finish(q.jsonBytes);
    }

    @Benchmark
    public Object testParserJackson(JacksonParser j) {
        try {
            return j.reader.readValue(j.jsonBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public Object testParserAfterburner(AfterburnerParser a) {
        try {
            return a.reader.readValue(a.jsonBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public Object testWriterQson(QsonWriter q) {
        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter(1024);
        q.objectWriter.write(jsonWriter, q.person);
        return jsonWriter.getBytes();
    }

    @Benchmark
    public Object testWriterJackson(JacksonWriter q) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            q.objectWriter.writeValue(out, q.person);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Benchmark
    public Object testWriterAfterburner(AfterburnerWriter q) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            q.objectWriter.writeValue(out, q.person);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //@Benchmark
    public Object testSimpleQsonWriter(SimpleQsonWriter q) {
        ByteArrayJsonWriter jsonWriter = new ByteArrayJsonWriter();
        q.objectWriter.write(jsonWriter, q.simple);
        return jsonWriter.getBytes();
    }

    //@Benchmark
    public Object testSimpleJacksonWriter(SimpleJacksonWriter q) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            q.objectWriter.writeValue(out, q.simple);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //@Benchmark
    public Object testSimpleAfterburnerWriter(SimpleAfterburnerWriter q) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            q.objectWriter.writeValue(out, q.simple);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
