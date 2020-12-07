package io.quarkus.qson.test;

import io.quarkus.qson.QsonAny;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonAny extends Person2 {
    private Map<String, Object> any;

    @QsonAny
    public Map<String, Object> getAny() {
        return any;
    }

    @QsonAny
    public void setAny(String key, Object val) {
        if (this.any == null) this.any = new HashMap<>();
        this.any.put(key, val);
    }
}
