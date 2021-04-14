package io.quarkus.qson.test;

import io.quarkus.qson.GenericType;
import io.quarkus.qson.QsonDate;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.generator.QsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OffsetDateTimeTest {
    public static class MyDate {
        private OffsetDateTime date;
        private OffsetDateTime pattern;
        private List<OffsetDateTime> dates;

        public OffsetDateTime getDate() {
            return date;
        }

        public void setDate(OffsetDateTime date) {
            this.date = date;
        }

        public List<OffsetDateTime> getDates() {
            return dates;
        }

        public void setDates(List<OffsetDateTime> dates) {
            this.dates = dates;
        }
    }

    public static class MyAnnotated {
        private OffsetDateTime patterned;
        private OffsetDateTime date;
        private List<OffsetDateTime> dates;

        @QsonDate(format = QsonDate.Format.SECONDS)
        public OffsetDateTime getDate() {
            return date;
        }

        public void setDate(OffsetDateTime date) {
            this.date = date;
        }

        @QsonDate(format = QsonDate.Format.MILLISECONDS)
        public List<OffsetDateTime> getDates() {
            return dates;
        }

        public void setDates(List<OffsetDateTime> dates) {
            this.dates = dates;
        }

        @QsonDate(pattern = "yyyy MM dd HH:mm:ss X")
        public OffsetDateTime getPatterned() {
            return patterned;
        }

        public void setPatterned(OffsetDateTime patterned) {
            this.patterned = patterned;
        }
    }

    @Test
    public void generateClass() throws Exception {
        Generator generator = new Generator();
        generator.dateFormat(QsonDate.Format.MILLISECONDS);
        generator.serializer(MyDate.class).output(new TestClassOutput()).generate();
    }


    @Test
    public void testDefaultPropertyISO_OFFSET_DATE_TIME() {
        OffsetDateTime now = OffsetDateTime.now();
        String nowString = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        now = OffsetDateTime.parse(nowString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String json = "{ \"date\": \"" + nowString + "\"}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.ISO_8601_OFFSET_DATE_TIME);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }
    @Test
    public void testDefaultPropertyRFC_1123_DATE_TIME() {
        OffsetDateTime now = OffsetDateTime.now();
        String nowString = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        now = OffsetDateTime.parse(nowString, DateTimeFormatter.RFC_1123_DATE_TIME);
        String json = "{ \"date\": \"" + nowString + "\"}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.RFC_1123_DATE_TIME);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }
    @Test
    public void testDefaultPropertyMillis() {
        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC);
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() + "}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.MILLISECONDS);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }
    @Test
    public void testDefaultPropertySeconds() {
        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.ofEpochSecond(System.currentTimeMillis() / 1000, 0), ZoneOffset.UTC);
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() / 1000 + "}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.SECONDS);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }

    @Test
    public void testDefaultPropertyISO_OFFSET_DATE_TIMEList() {
        OffsetDateTime now = OffsetDateTime.now();
        String nowString = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        now = OffsetDateTime.parse(nowString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String json = "{ \"dates\": [\"" + nowString + "\", \"" + nowString + "\"]}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.ISO_8601_OFFSET_DATE_TIME);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
    }

    @Test
    public void testDefaultPropertyRFC_1123_DATE_TIMEList() {
        OffsetDateTime now = OffsetDateTime.now();
        String nowString = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        now = OffsetDateTime.parse(nowString, DateTimeFormatter.RFC_1123_DATE_TIME);
        String json = "{ \"dates\": [\"" + nowString + "\", \"" + nowString + "\"]}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.RFC_1123_DATE_TIME);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
    }

    @Test
    public void testAnnotated() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss X");
        OffsetDateTime parsed = OffsetDateTime.parse("2021 12 12 03:23:23 Z", formatter);



        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.ofEpochSecond(System.currentTimeMillis() / 1000, 0), ZoneOffset.UTC);
        String json = "{" +
                "\"date\": " + now.toInstant().toEpochMilli() / 1000 + ", " +
                "\"patterned\": \"2021 12 12 03:23:23 Z\", " +
                " \"dates\": [" + now.toInstant().toEpochMilli() + ", " + now.toInstant().toEpochMilli() + "]}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.ISO_8601_OFFSET_DATE_TIME);

        MyAnnotated date = mapper.read(json, MyAnnotated.class);
        Assertions.assertEquals(now, date.getDate());
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        Assertions.assertEquals("2021 12 12 03:23:23 Z", date.getPatterned().format(formatter));
        json = mapper.writeString(date);
        date = mapper.read(json, MyAnnotated.class);
        Assertions.assertEquals(now, date.getDate());
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        Assertions.assertEquals("2021 12 12 03:23:23 Z", date.getPatterned().format(formatter));
    }

    public static OffsetDateTime fromMillis(long millis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }

    public static long toMillis(OffsetDateTime date) {
        return date.toInstant().toEpochMilli();
    }

    @Test
    public void testCustomValueMapping() throws Exception {
        OffsetDateTime now = OffsetDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC);
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() + "}";
        QsonMapper mapper = new QsonMapper();

        Method fromMillis = OffsetDateTimeTest.class.getMethod("fromMillis", long.class);
        Method toMillis = OffsetDateTimeTest.class.getMethod("toMillis", OffsetDateTime.class);

        mapper.valueMappingFor(OffsetDateTime.class, fromMillis, toMillis);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }



}
