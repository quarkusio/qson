package io.quarkus.qson.test;

import io.quarkus.qson.QsonDate;
import io.quarkus.qson.deserializer.DateUtil;
import io.quarkus.qson.generator.Generator;
import io.quarkus.qson.generator.QsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DateUtilTest {
    public static class MyDate {
        private Date date;
        private Date pattern;
        private List<Date> dates;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public List<Date> getDates() {
            return dates;
        }

        public void setDates(List<Date> dates) {
            this.dates = dates;
        }
    }

    public static class MyAnnotated {
        private Date patterned;
        private Date date;
        private List<Date> dates;

        @QsonDate(format = QsonDate.Format.SECONDS)
        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @QsonDate(format = QsonDate.Format.MILLISECONDS)
        public List<Date> getDates() {
            return dates;
        }

        public void setDates(List<Date> dates) {
            this.dates = dates;
        }

        @QsonDate(pattern = "yyyy MM dd")
        public Date getPatterned() {
            return patterned;
        }

        public void setPatterned(Date patterned) {
            this.patterned = patterned;
        }
    }

    @Test
    public void generateClass() throws Exception {
        Generator generator = new Generator();
        generator.deserializer(MyAnnotated.class).output(new TestClassOutput()).generate();
    }


    @Test
    public void testDefaultPropertyISO_OFFSET_DATE_TIME() throws Exception {
        Date now = new Date();
        String nowString = DateUtil.ISO_8601_OFFSET_DATE_TIME.format(now);
        now = DateUtil.ISO_8601_OFFSET_DATE_TIME.parse(nowString);
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
    public void testDefaultPropertyRFC_1123_DATE_TIME() throws Exception {
        Date now = new Date();
        String nowString = DateUtil.RFC_1123_DATE_TIME.format(now);
        now = DateUtil.RFC_1123_DATE_TIME.parse(nowString);
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
        Date now = new Date();
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
        Date now = new Date((System.currentTimeMillis() / 1000) * 1000);
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
    public void testDefaultPropertyISO_OFFSET_DATE_TIMEList() throws Exception {
        Date now = new Date();
        String nowString = DateUtil.ISO_8601_OFFSET_DATE_TIME.format(now);
        now = DateUtil.ISO_8601_OFFSET_DATE_TIME.parse(nowString);
        String json = "{ \"dates\": [\"" + nowString + "\", \"" + nowString + "\"]}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.ISO_8601_OFFSET_DATE_TIME);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));  }

    @Test
    public void testDefaultPropertyRFC_1123_DATE_TIMEList() throws Exception {
        Date now = new Date();
        String nowString = DateUtil.RFC_1123_DATE_TIME.format(now);
        now = DateUtil.RFC_1123_DATE_TIME.parse(nowString);
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
        Date now = new Date((System.currentTimeMillis() / 1000) * 1000);
        String json = "{" +
                "\"date\": " + now.toInstant().toEpochMilli() / 1000 + ", " +
                "\"patterned\": \"2021 12 12\", " +
                " \"dates\": [" + now.toInstant().toEpochMilli() + ", " + now.toInstant().toEpochMilli() + "]}";
        QsonMapper mapper = new QsonMapper();
        mapper.dateFormat(QsonDate.Format.ISO_8601_OFFSET_DATE_TIME);

        MyAnnotated date = mapper.read(json, MyAnnotated.class);
        Assertions.assertEquals(now, date.getDate());
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        Assertions.assertEquals("2021 12 12", new SimpleDateFormat("yyyy MM dd").format(date.getPatterned()));
        json = mapper.writeString(date);
        date = mapper.read(json, MyAnnotated.class);
        Assertions.assertEquals(now, date.getDate());
        Assertions.assertEquals(now, date.getDates().get(0));
        Assertions.assertEquals(now, date.getDates().get(1));
        Assertions.assertEquals("2021 12 12", new SimpleDateFormat("yyyy MM dd").format(date.getPatterned()));
    }

    public static Date fromMillis(long millis) {
        return new Date(millis);
    }

    public static long toMillis(Date date) {
        return date.toInstant().toEpochMilli();
    }

    @Test
    public void testCustomValueMapping() throws Exception {
        Date now = new Date();
        String json = "{ \"date\": " + now.toInstant().toEpochMilli() + "}";
        QsonMapper mapper = new QsonMapper();

        Method fromMillis = DateUtilTest.class.getMethod("fromMillis", long.class);
        Method toMillis = DateUtilTest.class.getMethod("toMillis", Date.class);

        mapper.valueMappingFor(Date.class, fromMillis, toMillis);

        MyDate date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
        json = mapper.writeString(date);
        date = mapper.read(json, MyDate.class);
        Assertions.assertEquals(now, date.getDate());
    }
}
