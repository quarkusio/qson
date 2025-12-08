package org.sample.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.sample.Person2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom Jackson deserializer for Person2 using streaming API for better performance.
 */
public class Person2Deserializer extends JsonDeserializer<Person2> {

    @Override
    public Person2 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        Person2 person = new Person2();
        
        if (p.currentToken() != JsonToken.START_OBJECT) {
            p.nextToken();
        }
        
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken(); // move to value
            
            if (fieldName == null) continue;
            
            switch (fieldName) {
                case "name":
                    person.setName(p.getValueAsString());
                    break;
                case "age":
                    person.setAge(p.getIntValue());
                    break;
                case "married":
                    person.setMarried(p.getBooleanValue());
                    break;
                case "money":
                    person.setMoney(p.getFloatValue());
                    break;
                case "intMap":
                    person.setIntMap(deserializeIntMap(p));
                    break;
                case "dad":
                    person.setDad(deserialize(p, ctxt));
                    break;
                case "kids":
                    person.setKids(deserializePersonMap(p, ctxt));
                    break;
                case "siblings":
                    person.setSiblings(deserializePersonList(p, ctxt));
                    break;
                case "pets":
                    person.setPets(deserializeStringList(p));
                    break;
                case "genericMap":
                    person.setGenericMap(deserializeGenericMap(p, ctxt));
                    break;
                case "genericBag":
                    person.setGenericBag(deserializeGenericValue(p, ctxt));
                    break;
                case "genericList":
                    person.setGenericList(deserializeGenericList(p, ctxt));
                    break;
                case "nested":
                    person.setNested(deserializeNestedMap(p, ctxt));
                    break;
                default:
                    p.skipChildren();
                    break;
            }
        }
        
        return person;
    }
    
    private Map<String, Integer> deserializeIntMap(JsonParser p) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        Map<String, Integer> map = new LinkedHashMap<>();
        
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, p.getIntValue());
        }
        
        return map;
    }
    
    private Map<String, Person2> deserializePersonMap(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        Map<String, Person2> map = new LinkedHashMap<>();
        
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, deserialize(p, ctxt));
        }
        
        return map;
    }
    
    private List<Person2> deserializePersonList(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        List<Person2> list = new ArrayList<>();
        
        while (p.nextToken() != JsonToken.END_ARRAY) {
            list.add(deserialize(p, ctxt));
        }
        
        return list;
    }
    
    private List<String> deserializeStringList(JsonParser p) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        List<String> list = new ArrayList<>();
        
        while (p.nextToken() != JsonToken.END_ARRAY) {
            list.add(p.getValueAsString());
        }
        
        return list;
    }
    
    @SuppressWarnings("rawtypes")
    private Map deserializeGenericMap(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        Map<String, Object> map = new LinkedHashMap<>();
        
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, deserializeGenericValue(p, ctxt));
        }
        
        return map;
    }
    
    @SuppressWarnings("rawtypes")
    private List deserializeGenericList(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        List<Object> list = new ArrayList<>();
        
        while (p.nextToken() != JsonToken.END_ARRAY) {
            list.add(deserializeGenericValue(p, ctxt));
        }
        
        return list;
    }
    
    private Object deserializeGenericValue(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        
        switch (token) {
            case VALUE_NULL:
                return null;
            case VALUE_STRING:
                return p.getText();
            case VALUE_NUMBER_INT:
                return p.getLongValue();
            case VALUE_NUMBER_FLOAT:
                return p.getDoubleValue();
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            case START_ARRAY:
                return deserializeGenericList(p, ctxt);
            case START_OBJECT:
                return deserializeGenericMap(p, ctxt);
            default:
                return null;
        }
    }
    
    private Map<String, List<Person2>> deserializeNestedMap(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        
        Map<String, List<Person2>> map = new LinkedHashMap<>();
        
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, deserializePersonList(p, ctxt));
        }
        
        return map;
    }
}

