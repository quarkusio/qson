# QSON json mapper

QSON is an object to json mapper.  It does bytecode generation of deserializer and serilizer classes using Gizmo.
QSON was born when it's author (Bill Burke) noticed that Jackson took up a decent portion of boot time for a simple Resteasy JAX-RS Quarkus application.
While Jackson is a more mature json mapper and a de facto standard, QSON aims for better integration with
Quarkus and Graal.  The primary goals of QSON are speed, both boot and runtime, limited heap allocations,
a small set of classes (metaspace size), low memory footprint, and zero reflection.

## Limitations

* must use public getter and setter methods
* only public classes
* Does not support polymorphism
* Only UTF-8 encoding supported
* Lacks other features something like Jackson may have.  Will be adding those features that do not degrade performance and memory footprint

## Basics

Classes that you want to map to JSON must have a public getter method for each property you want to be able to serialize to JSON
and a public setter method for those properties you want to be able to deserialize from JSON.  Any setter or getter method will
be assumed to be something you want to map to JSON unless you use the `@io.quarkus.qson.QsonIgnore` annnotation on the setter or
getter method, or the field of the property.  The json property name will be the same name as the Java property one.  You can use
the `@io.quarkus.qson.QsonProperty` annotation to change the json property name mapping.

Here's a simple example:

```json
{
  "name": "Cam Newton",
  "home-address": "Foxboro"
}
```

```java
public class Person {
    private String name;
    private String homeAddress;

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    @QsonProperty("home-address")
    public String getHomeAddress() {
       return homeAddress;
    }

    public void setHomeAddress(String a) {
        this.homeAddress = a;
    }
}
```

When parsing, Qson automatically ignores any extra json that does not map to your class.

Qson also supports arbitrary JSON with the `io.quarkus.qson.QsonAny` annotation.  For deserialization, this annotation
must be placed on a method that takes two parameters, a String key, and an Object value.  For serialization,
it must be placed on a method that takes no parameters and returns a `Map<String, Object>`.

```java
public class Pojo {
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
```

## Integration Outside of Quarkus

You must first pull in the QSON generator dependency
```xml
<dependency>
   <groupId>io.quarkus.qson</groupId>
   <artifactId>qson-generator</artifactId>
</dependency>
```

You and read and write json with the `io.quarkus.qson.QsonMapper` class.  Unforunately, this class does not work with
Graal as it generates bytecode at runtime to serialize and deserialize your object instances.

```java
// reading
QsonMapper mapper = new QsonMapper();

InputStream is = ...;
Person p = mapper.read(is, Person.class);

byte[] bytes = ...;
Person p = mapper.read(bytes, Person.class);

String json = "{...}";
Person p = mapper.read(json, Person.class);

// writing
OutputStream os = ...;
mapper.writeStream(p);

byte[] bytes = mapper.writeBytes(p);
String json = mapper.writeString(p);
```

Currently, QSON only supports UTF-8 input and will serialize using UTF-8 as well.

QSON also supports non-blocking parses, but it is a bit more verbose

```java
QsonMapper mapper = new QsonMapper();

QsonParser parser = mapper.parserFor(Person.class);
ByteArrayParserContext ctx = new ByteArrayParserContext(parser);

byte[] buffer;
while (notEOF()) { // pseudo-code
   buffer = readBuffer();
   parser.parse(buffer);
}
Person p = parser.finish();
```


## Integration within Quarkus

QSON is tightly integration with Quarkus and will work with Graal in this scenario.
You do not include the `qson-generator` dependency, but instead should include

```xml
<dependency>
   <groupId>io.quarkus.qson</groupId>
   <artifactId>quarkus-qson</artifactId>
</dependency>
```

The QSON Quarkus extension will automatically scan your project for classes annotated
with any QSON annotation and generate bytecode for serialization and deserialization at build time.
Instances of these generated classes are registered and available for lookup via the `io.quarkus.qson.runtime.QuarkusQsonMapper`
CDI bean which can be injected with `@Inject`.

```java

@Inject QuarkusQsonMapper mapper;

void foobar() {
   Person p = mapper.read(is, Person.class.getName());
}
```

The `QuarkusQsonMapper` is keyed by a String key which is the fully qualified classname of your class.
The reason for this is to avoid reflection in a Graal environment and thus reducing memory footprint.

## Integration with JAX-RS and Resteasy

QSON works great with Resteasy and JAX-RS too (as well as our Spring MVC integration).  Simply include
this dependency:

```xml
<dependency>
   <groupId>io.quarkus.qson</groupId>
   <artifactId>quarkus-resteasy-qson</artifactId>
</dependency>
```

The Quarkus extension will scan all your resource classes that have JSON input or output and make sure
that the appropriate QSON bytecode is generated.  It will also register a MessageBodyReader and Writer
so that QSON will do all your JSON marshalling.

