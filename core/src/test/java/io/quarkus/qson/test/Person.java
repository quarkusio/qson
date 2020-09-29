package io.quarkus.qson.test;

import java.util.List;
import java.util.Map;

public class Person {
    private String name;
    private int age;
    private float money;
    private boolean married;
    private Map<String, Integer> intMap;
    private Person dad;
    private Map genericMap;
    private Object genericBag;
    private List genericList;
    private Map<String, Person> kids;
    private List<Person> siblings;
    private List<String> pets;
    private Map<String, List<Person>> nested;

    public Map<String, Person> getKids() {
        return kids;
    }

    public void setKids(Map<String, Person> kids) {
        this.kids = kids;
    }

    public List<Person> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<Person> siblings) {
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

    public Person getDad() {
        return dad;
    }

    public void setDad(Person dad) {
        this.dad = dad;
    }

    public float getMoney() {
        return money;
    }

    public void setMoney(float money) {
        this.money = money;
    }

    public boolean isMarried() {
        return married;
    }

    public void setMarried(boolean married) {
        this.married = married;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Integer> getIntMap() {
        return intMap;
    }

    public void setIntMap(Map<String, Integer> intMap) {
        this.intMap = intMap;
    }

    public Map<String, List<Person>> getNested() {
        return nested;
    }

    public void setNested(Map<String, List<Person>> nested) {
        this.nested = nested;
    }
}
