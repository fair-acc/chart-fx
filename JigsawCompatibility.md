# Jigsaw Compatibility

To make chartfx usable for modularized projects, there are some points which have to be considered according to  [1].

> Addressing these concerns is a matter of good hygiene. If you encounter one of these issues and you can't address those, don't add the Automatic-Module-Name entry yet. For now, your library is better off on the classpath. It will only raise false expectations if you do add the entry.

So before meging this branch, these issues should probably be adressed.

## internal JDK types

> Make sure your library doesn't use internal types from the JDK (run jdeps --jdk-internals mylibrary.jar to find offending code). JDeps (as bundled with Java 9 and later) will offer publicly supported alternatives for any use of encapsulated JDK APIs. When your library runs from the classpath on Java 9 and later, you can still get away with this. Not so if your library lives on the module path as automatic module.

```
jdeps --jdk-internals chartfx-chart/target/chartfx-chart-11.1.0-SNAPSHOT.jar 
chartfx-chart-11.1.0-SNAPSHOT.jar -> JDK removed internal API
   de.gsi.chart.utils.SimplePerformanceMeter          -> com.sun.javafx.perf.PerformanceTracker             JDK internal API (JDK removed internal API)

jdeps --jdk-internals chartfx-dataset/target/chartfx-dataset-11.1.0-SNAPSHOT.jar 
chartfx-dataset-11.1.0-SNAPSHOT.jar -> jdk.unsupported
   de.gsi.dataset.serializer.spi.FastByteBuffer       -> sun.misc.Unsafe                                    JDK internal API (jdk.unsupported)

JDK Internal API                         Suggested Replacement
----------------                         ---------------------
sun.misc.Unsafe                          See http://openjdk.java.net/jeps/260
```

Unsafe replacement links to (http://openjdk.java.net/jeps/193)[Variable Handles].

 -[ ] com.sun.javafx.perf.PerformanceTracker (chartfx-chart: SimplePerformanceMeter)
 -[ ] sun.misc.Unsafe (chartfx-datasets: FastByteBuffer)

## unnamed packages

> Your library can't have classes in the default (unnamed) package. This is a bad idea regardless, but when your library is used as automatic module, this rule is enforced by the module system.

[x] no problems here

## split packages

> Your library can't split packages (two or more JARs defining the types in the same package), nor can it redefine JDK packages (javax.annotation is a notorious example, being defined in the JDK's java.xml.ws.annotation module but also in external libraries).

Are our testcases affecting this? They are in the same package as the implementation but they are not shipped, so it might be ok?

## META-INF/services

> When your library's JAR has a META-INF/services directory to specify service providers, then the specified providers must exist in this JAR (as described in the ModuleFinder JavaDoc)

[x] check
