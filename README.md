# Quarkus JSON Compiler

Generates deserializer and serializer classes for JSON for specific class input.  The goals over Jackson and other
JSON marshallers is speed, limited heap allocations, a small set of classes, and zero reflection.