# java-phantom-reference-example

## How to run

```java
$ export MAVEN_OPTS="-Xmx256m -verbose:gc"
$ mvn -q -e compile exec:java -Dexec.mainClass=com.example.Example
```
