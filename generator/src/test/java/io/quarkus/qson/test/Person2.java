package io.quarkus.qson.test;

import java.util.List;
import java.util.Map;

public class Person2 {
    private String name;
    private int age;
    private boolean married;
    private float money;
    private Map<String, Integer> intMap;
    private Person2 dad;
    private Map<String, Person2> kids;
    private List<Person2> siblings;
    private List<String> pets;
    private Map genericMap;
    private Object genericBag;
    private List genericList;
    private Map<String, List<Person2>> nested;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isMarried() {
        return married;
    }

    public void setMarried(boolean married) {
        this.married = married;
    }

    public float getMoney() {
        return money;
    }

    public void setMoney(float money) {
        this.money = money;
    }

    public Map<String, Integer> getIntMap() {
        return intMap;
    }

    public void setIntMap(Map<String, Integer> intMap) {
        this.intMap = intMap;
    }

    public Person2 getDad() {
        return dad;
    }

    public void setDad(Person2 dad) {
        this.dad = dad;
    }

    public Map<String, Person2> getKids() {
        return kids;
    }

    public void setKids(Map<String, Person2> kids) {
        this.kids = kids;
    }

    public List<Person2> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<Person2> siblings) {
        this.siblings = siblings;
    }

    public List<String> getPets() {
        return pets;
    }

    public void setPets(List<String> pets) {
        this.pets = pets;
    }

    public Map getGenericMap() {
        return genericMap;
    }

    public void setGenericMap(Map genericMap) {
        this.genericMap = genericMap;
    }

    public Object getGenericBag() {
        return genericBag;
    }

    public void setGenericBag(Object genericBag) {
        this.genericBag = genericBag;
    }

    public List getGenericList() {
        return genericList;
    }

    public void setGenericList(List genericList) {
        this.genericList = genericList;
    }

    public Map<String, List<Person2>> getNested() {
        return nested;
    }

    public void setNested(Map<String, List<Person2>> nested) {
        this.nested = nested;
    }
}
