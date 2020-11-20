package io.quarkus.qson.test;

import io.quarkus.qson.generator.QsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class NullTest {

    public static class NullTester {
        private String name;
        private int age;
        private Integer value;
        private List<String> nullList;
        private boolean nullListCalled;
        private Map<String, String> nullMap;
        private boolean nullMapCalled;
        private List<String> list;
        private Map<String, String> map;
        private NullTester nullTester;
        private boolean nullTesterCalled;

        private boolean nameCalled;
        private boolean ageCalled;
        private boolean valueCalled;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            nameCalled = true;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
            ageCalled = true;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
            valueCalled = true;
        }

        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        public List<String> getNullList() {
            return nullList;
        }

        public void setNullList(List<String> nullList) {
            nullListCalled = true;
            this.nullList = nullList;
        }

        public Map<String, String> getNullMap() {
            return nullMap;
        }

        public void setNullMap(Map<String, String> nullMap) {
            nullMapCalled = true;
            this.nullMap = nullMap;
        }

        public NullTester getNullTester() {
            return nullTester;
        }

        public void setNullTester(NullTester nullTester) {
            nullTesterCalled = true;
            this.nullTester = nullTester;
        }
    }
    @Test
    public void testNull() throws Exception {
        QsonMapper mapper = new QsonMapper();
        String json = "{\n" +
                "  \"name\": null,\n" +
                "  \"age\": null,\n" +
                "  \"value\": null,\n" +
                "  \"nullTester\": null,\n" +
                "  \"nullList\": null,\n" +
                "  \"nullMap\": null,\n" +
                "  \"list\": [null],\n" +
                "  \"map\" : {\n" +
                "    \"null\": null\n" +
                "  }\n" +
                "}";
        NullTester t = mapper.read(json, NullTester.class);
        Assertions.assertNull(t.getName());
        Assertions.assertTrue(t.nameCalled);
        Assertions.assertNull(t.getNullTester());
        Assertions.assertTrue(t.nullTesterCalled);
        Assertions.assertEquals(0, t.getAge());
        Assertions.assertTrue(t.ageCalled);
        Assertions.assertNull(t.getValue());
        Assertions.assertTrue(t.valueCalled);
        Assertions.assertNull(t.getNullList());
        Assertions.assertTrue(t.nullListCalled);
        Assertions.assertNull(t.getNullMap());
        Assertions.assertTrue(t.nullMapCalled);
        Assertions.assertEquals(1, t.getList().size());
        Assertions.assertEquals(1, t.getMap().size());
        Assertions.assertNull(t.getList().get(0));
        Assertions.assertNull(t.getMap().get("null"));


    }

    @Test
    public void testNullGeneric() {
        String json = "{\n" +
                "  \"name\": null,\n" +
                "  \"age\": null,\n" +
                "  \"value\": null,\n" +
                "  \"nullTester\": null,\n" +
                "  \"nullList\": null,\n" +
                "  \"nullMap\": null,\n" +
                "  \"list\": [null],\n" +
                "  \"map\" : {\n" +
                "    \"null\": null\n" +
                "  }\n" +
                "}";
        QsonMapper mapper = new QsonMapper();
        Map map = mapper.read(json, Map.class);
        Assertions.assertEquals(8, map.size());
        Assertions.assertEquals(1, ((List)map.get("list")).size());
        Assertions.assertEquals(1, ((Map)map.get("map")).size());
        Assertions.assertNull(((List)map.get("list")).get(0));
        Assertions.assertNull(((Map)map.get("map")).get("null"));
    }
}
