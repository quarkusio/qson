# QSON json mapper

QSON is an object to json mapper.  It does bytecode generation of deserializer (parser) and serializer (writer) classes using Gizmo.
QSON was born when it's author (Bill Burke) noticed that Jackson took up a decent portion of boot time for a simple Resteasy JAX-RS Quarkus application.
While Jackson is a more mature json mapper and a de facto standard, QSON aims for better integration with
Quarkus and Graal.  The primary goals of QSON are speed, both boot and runtime, limited heap allocations,
a small set of classes (metaspace size), low memory footprint, and zero reflection at runtime.

## Limitations

* must use public getter and setter methods that are prefixed with `get`, `is`, or `set`, or that are annotated with @QsonProperty
* only public classes
* Does not support polymorphism
* Only UTF-8 encoding supported
* No array support yet  
* Lacks other features something like Jackson may have.  Will be adding those features that do not degrade performance and memory footprint
* Qson works best right now with Quarkus.  We don't have maven/gradle plugin integration yet to compile bytecode at buildtime in these non-Quarkus environments


## Basics

Classes that you want to map to JSON must have a public getter method for each property you want to be able to serialize to JSON
and a public setter method for those properties you want to be able to deserialize from JSON.  
If your getter and setter methods are not prefixed with `get`, `is`, or `set`, then you must
mark it as a property using the `@QsonProperty` annotation.

Every setter or getter method will
be assumed to be something you want to map to JSON unless you use the `@io.quarkus.qson.QsonIgnore` annotation on the setter or
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
    private String phone;
    
    @QsonProperty("zip-code")
    private String zip;

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    @QsonProperty("phone-number")
    public String getPhone() {
       return phone; 
    }
    
    public void setPhone(String p) {
        this.phone = p;
    }
    
    public String getZip() {
        return zip;
    }
    
    public void setZip(String z) {
        this.zip = z;
    }

    @QsonProperty
    public String homeAddress() {
       return homeAddress;
    }

    @QsonProperty
    public Person homeAddress(String a) {
        this.homeAddress = a;
    }
}
```
## Unmapped json

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

## Date Time support

Qson has out-of-the-box support for `java.util.Date` and `java.time.OffsetDateTime`.  The default
global setting is ISO 8601 Offset Date Time.  You can change this by invoking the `QsonMapper.dateFormat()` method

```java
{
    QsonMapper mapper = new QsonMapper();
    mapper.dateFormat(QsonDate.Format.MILLISECONDS);
}
```

The default formats supported are milliseconds, seconds, ISO 8601 OFfset Date Time, and RFC 1123 Date Time. You can
also configure date formatting on a per-property bases using the `@io.quarkus.qson.QsonDate` annotation.  For example

```java
public class MyDates {
    private Date patterned;
    private OffsetDateTime date;
    private List<Date> dates;

    @QsonDate(format = QsonDate.Format.SECONDS)
    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
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
```

## Qson Value mappings

Qson supports mapping json literal values directly to a class.  For example, if you want to map a number
value to a specific class.   You use the `@io.quarkus.qson.QsonValue` annotation as follows:

```java
   public class MyConstructorStringValue {
        private String string;

        @QsonValue
        public MyConstructorStringValue(String str) {
            this.string = str;
        }

        @QsonValue
        public String value() {
            return string;
        }
    }
```

In the above example if you have this json: `"hello"`, the string value will be passed
to the constructor `MyConstructorStringValue`.  For writes, instances of `MyConstructorStringValue`
will be written as a json string using the value returned from the `MyConstructorStringValue.value()` method.

You can also place `@QsonValue` on a setter method instead of on a constructor.  For example:

```java
public class MyMethodIntValue {
    private int val;

    @QsonValue
    public void value(int val) {
        this.val = val;
    }

    @QsonValue
    public int value() {
        return val;
    }
}
```

## Programmatic Custom Mappings

Qson has an API, `io.quarkus.qson.generator.QsonGenerator`, that you can
programmatically use to specify, modify, or augment qson class mappings. `QsonMapper`
implements this interface.  You can override or create a completely new mapping.  This
is especially useful for thirdparty libraries where you are not able to annotate the class
you want to map to json.

The `QsonGenerator.mappingFor(Class)` method will scan the class you pass as a parameter
for qson annotations and return you a `io.quarkus.qson.generator.ClassMapping` instance
from which you can modify this mapping.  The `QsonGenerator.overrideMappingFor(Class)` does not
scan for annotations and just gives you a `ClassMapping` instance from which you can specify
the whole mapping for your class.

## Custom Parsers with @QsonTransformer

Sometimes you have a thirdparty library that has one or more classess you want to map to 
json.  The `@io.quarkus.qson.QsonTransformer` annotation provides you an easier way to define
a mapping for these un-annotatable classes.  You can define a *transformer* class that has
all the annotations you want and mark a method on that transformer class that allocates an
instance of that thirdparty class.

```java
    public class Thirdparty {
        int val;

        public Thirdparty(int val) {
            this.val = val;
        }

        public int val() {
            return val;
        }
    }
    public class Transformer {
        int x;

        @QsonTransformer
        public Thirdparty createThirdparty() {
            return new Thirdparty(x);
        }

        @QsonProperty("x")
        public void setX(int x) {
            this.x = x;
        }
    }

    @Test
    public void testTransformer() throws Exception {
        String json = "{ \"x\": 42 }";
        QsonMapper mapper = new QsonMapper();
        mapper.overrideMappingFor(Thirdparty.class).transformer(Transformer.class);
        Thirdparty t = mapper.read(json, Thirdparty.class);
        Assertions.assertEquals(42, t.getX());
    }
```

This is especially useful for cases where your third party class can only be allocated
with a constructor with one or more parameters.  The example above shows this.

## Custom Writers

You can register custom writers that will produce the json you want for a 
specific class by implementing the `io.quarkus.qson.writer.QsonObjectWriter` interface.

```java
    public class Thirdparty {
        int val;

        public Thirdparty(int val) {
            this.val = val;
        }

        public int val() {
            return val;
        }
    }
    public class Custom implements QsonObjectWriter {
    @Override
        public void write(JsonWriter writer, Object target) {
            Thirdparty t = (Thirdparty)target;
            String json = "{ \"foobar\": " + target.val() + " }";
            writer.writeBytes(json.getBytes());
        }
    }
    @Test
    public void testCustomWriter() throws Exception {
        QsonMapper mapper = new QsonMapper();
        mapper.mappingFor(Thirdparty.class).customWriter(Custom.class);
        String json = mapper.writeString(new Thirdparty(12));
        Assertions.assertEquals("{ \"foobar\": 12 }", json);
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

You can read and write json with the `io.quarkus.qson.QsonMapper` class.  Unfortunately, this class does not work with
Graal as it generates bytecode and loads generated parsers and writers at runtime using a custom classloader.  Quarkus
has better integration if you have custom settings for Qson (more on that later).

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

## Non-blocking parses

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
This includes custom parsers defined by the `@QsonTransformer` annotations.  It will also
automatically register custom writer classes annotated with the `@io.quarkus.qson.QsonCustomWriter` annotation.

Instances of these generated classes are registered and available for lookup via the `io.quarkus.qson.runtime.QuarkusQsonMapper`
CDI bean which can be injected with `@Inject`.

```java

@Inject QuarkusQsonMapper mapper;

void foobar() {
   Person p = mapper.parserFor(Person.class).read(json);
}
```

## Programmatic mappings in Quarkus

You can still programmatically provide qson configuration and mappings even though Quarkus
generates Qson mappings at build time.  You can mark one or more static methods
in your deployment with the `@io.quarkus.qson.runtime.QuarkusQsonInitializer` annotation.
These methods must return `void` and take `io.quarkus.qson.runtime.QuarkusQsonGenerator`
as its only parameter.

```java
    public class Mydate {
        Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        @QuarkusQsonInitializer
        public static void initDate(QuarkusQsonGenerator gen) {
            gen.dateFormat(QsonDate.Format.MILLISECONDS);
            gen.register(Mydate.class, true, true);
        }
    }
```

In this example, the `Mydate.initDate()` method is invoked at build time by Quarkus.
This method sets the default date format for Qson and registers the `Mydate` class
to have a parser and writer class generated for it at build time.

Its extremely important to note that `@QuarkusQsonInitializer` methods run at *BUILD TIME*!
Nothing you do inside these methods will be around at runtime.

## Config application.properties

With Quarkus, Qson only has one config option at the moment: `quarkus.qson.date-format`.
The value can be any enum constant defined within `io.quarkus.qson.QsonDate.Format`.

```
quarkus.qson.date-format=MILLISECONDS
```


## Integration with Quarkus JAX-RS support

QSON works great with Resteasy Reactive, Resteasy class, and JAX-RS too (as well as our Spring MVC integration).  Simply include
this dependency:

Resteasy Reactive:
```xml
<dependency>
    <groupId>io.quarkus.qson</groupId>
    <artifactId>quarkus-resteasy-reactive-qson</artifactId>
</dependency>
```

Resteasy classic:
```xml
<dependency>
   <groupId>io.quarkus.qson</groupId>
   <artifactId>quarkus-resteasy-qson</artifactId>
</dependency>
```

The Quarkus extension will scan all your resource classes that have JSON input or output and make sure
that the appropriate QSON bytecode is generated.  It will also register a MessageBodyReader and Writer
so that QSON will do all your JSON marshalling.

