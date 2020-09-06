# Yet-another-Serialiser (YaS)
### or: why we are not reusing ... and opted to write yet another custom data serialiser 

Data serialisation is a basic core functionality when information has to be transmitted, stored and later retrieved by (often quite) 
different sub-systems. With a multitude of different serialiser libraries, a non-negligible subset of these claim to be the fastest, 
most efficient, easiest-to-use or *&lt;add your favourite superlative here&gt;*.  
While this may be certainly true for the libraries' original design use-case, this often breaks down for other applications
that are often quite diverse and may focus on different aspects depending on the application. Hence, a fair comparison of 
their performance is usually rather complex and highly non-trivial because the underlying assumptions of 'what counts as important' 
being quite different between specific domains, boundary constraints, application goals and resulting (de-)serialisation strategies.  
Rather than claiming any superlative, or needlessly bashing solutions that are well suited for their design use-cases, we wanted 
to document the considerations, constraints and application goals of our specific use-case and that guided our 
[multi-protocol serialiser developments](https://github.com/GSI-CS-CO/chart-fx/microservice). 
This also in the hope that it might find interest, perhaps adoption, inspires new ideas, or any other form of improvements.
Thus, if you find something missing, unclear, or things that could be improved, please feel encouraged to post a PR. 

DISCLAIMER: This specific implementation while not necessarily a direct one-to-one source-code copy is at least conceptually 
based upon a combination of other open-sourced implementations, long-term experience with internal-proprietary wire-formats, 
and new serialiser design ideas expressed in the references [below](#references) which were adopted, adapted and optimised 
for our specific use-case.

### [Our](https://fair-center.eu/) [Use-Case](https://fair-wiki.gsi.de/FC2WG)
We use [this](../../microservice) and [Chart-Fx](https://github.com/GSI-CS-CO/chart-fx) in order to aid the development 
of functional microservices that monitor and control a large variety of device- and beam-based parameters that are necessary 
for the operation of our [FAIR particle accelerators](https://www.youtube.com/watch?v=zy4b0ZQnsck). 
These microservices cover in particular those that require the aggregation of measurement data from different sub-systems, 
or that require domain-specific logic or real-time signal-processing algorithms that cannot be efficiently implemented 
in any other single device or sub-system.

### Quick Overview
This serialiser implementation defines three levels of interface abstractions:
  * [IoBuffer](../../microservice/src/main/java/de/gsi/serializer/IoBuffer.java) which defines the low-level byte-array format 
    of how data primitives (ie. `boolean`, `byte`, ...,`float`, `double`), `String`, and their array counter-part (ie. 
    `boolean[]`, `byte[]`, ...,`float[]', 'double[]`, `String[]`) are stored. There are two default implementations:  
      - [ByteBuffer](../../microservice/src/main/java/de/gsi/serializer/spi/ByteBuffer.java) which basically wraps around and 
    extends `java.nio.ByteBuffer` to also support `String` and primitive arrays, and
      - [FastByteBuffer](../../microservice/src/main/java/de/gsi/serializer/spi/FastByteBuffer.java) which is the recommended 
    (~ 25% faster) reimplementation using direct byte-array and cached field accesses.
  * [IoSerialiser](../../microservice/src/main/java/de/gsi/serializer/IoSerialiser.java) which defines the compound wire-format 
    for more complex objects (e.g. `List<T>`, `Map<K,V>`, multi-dimensional arrays etc), including field headers, and annotations.  
    There are three default implementations:  
    *(N.B. `IoSerialiser` allows further extensions to any other structuraly similar protocol.)*
      - [BinarySerialiser](../../microservice/src/main/java/de/gsi/serializer/spi/BinarySerialiser.java) which is the primary
      binary-based transport protocol used by this library,
      - [CmwLightSerialiser](../../microservice/src/main/java/de/gsi/serializer/spi/CmwLightSerialiser.java) which is the backward
      compatible re-implementation of an existing proprietary protocol internally used in our facility, and 
      - [JsonSerialiser](../../microservice/src/main/java/de/gsi/serializer/spi/JsonSerialiser.java) which implements the
      [JSON](https://www.json.org/) protocol commonly used in RESTful HTTP-based services. 
  * [IoClassSerialiser](../../microservice/src/main/java/de/gsi/serializer/IoClassSerialiser.java) which deals with the automatic 
    mapping and (de-)serialisation between the class field structure and specific wire-format. This class defines default strategies
    for generic and nested classes and can be further extended by custom serialiser prototypes for more complex classes,
    other custom nested protocols  or interfaces using the 
    [FieldSerialiser](../../microservice/src/main/java/de/gsi/serializer/FieldSerialiser.java) interface.
    
A short working example of how these can be used is shown in [IoClassSerialiserSimpleTest](../../microservice/src/test/java/de/gsi/serializer/IoClassSerialiserSimpleTest.java):
```Java
@Test
void simpleTest() {
    final IoBuffer byteBuffer = new FastByteBuffer(10_000); // alt: new ByteBuffer(10_000);
    final IoClassSerialiser ioClassSerialiser = new IoClassSerialiser(byteBuffer, BinarySerialiser.class);
    TestDataClass data = new TestDataClass(); // object to be serialised

    byteBuffer.reset();
    ioClassSerialiser.serialiseObject(data); // pojo -> serialised data
    // [..] stream/write serialised byteBuffer content [..]

    // [..] stream/read serialised byteBuffer content
    byteBuffer.flip(); // mark byte-buffer for reading
    TestDataClass received = ioClassSerialiser.deserialiseObject(TestDataClass.class);

    // check data equality, etc...
    assertEquals(data, received);
}
```
The specific wire-format that the [IoClassSerialiser](../../microservice/src/main/java/de/gsi/serializer/IoClassSerialiser.java) uses can be set either programmatically or dynamically (auto-detection based on serialised data content header) via:
```Java
    ioClassSerialiser.setMatchedIoSerialiser(BinarySerialiser.class);
    ioClassSerialiser.setMatchedIoSerialiser(CmwLightSerialiser.class);
    ioClassSerialiser.setMatchedIoSerialiser(JsonSerialiser.class);
    // to auto-detect the suitable serialiser based on serialised data header:
    ioClassSerialiser.setAutoMatchSerialiser(true);
```
The extension for arbitrary custom classes or interfaces can be achieved through (here for the `DoubleArrayList` class) via: 
```Java
    serialiser.addClassDefinition(new FieldSerialiser<>(
        (io, obj, field) -> field.getField().set(obj, DoubleArrayList.wrap(io.getDoubleArray())), // IoBuffer &rightarrow; class field reader function
        (io, obj, field) -> DoubleArrayList.wrap(io.getDoubleArray()), // return function - generates new object based on IoBuffer content
        (io, obj, field) -> { // class field &rightarrow; IoBuffer writer function
            final DoubleArrayList retVal = (DoubleArrayList) field.getField().get(obj);
            io.put(field, retVal.elements(), retVal.size());
        }, 
        DoubleArrayList.class));
```
The [DataSetSerialiser](../../microservice/src/main/java/de/gsi/serializer/spi/iobuffer/DataSetSerialiser.java) serialiser 
implementation is a representative example and serialises the [DataSet](../../chartfx-dataset/src/main/java/de/gsi/dataset/DataSet.java) 
interface into an abstract implementation-independet wire-format using the [FieldDataSetHelper](../../microservice/src/main/java/de/gsi/serializer/spi/iobuffer/FieldDataSetHelper.java) 
function. This is also the most prominent common domain object definition that is used within our MVC-pattern driven microservice-, 
data-processing-, and UI-applications and one of the original primary motivations why we designed and built the `IoClassSerialiser` implementation.

### Primary Serialiser Functionality Goals and Constraints
Some of the aspects that were incorporated into the design, loosely ordered according to their importance:
  1. performance: providing an optimised en-/decoding that minimises the effective total latency between the data object 
     content being ready to be serialised and sent by the server until the object is received, fully de-serialised and ready 
     for further processing on the client-side.      
     *N.B. some serialisers trade-off size for en-/decoding speed, which may be suitable for primarily network-io limited 
     systems. Since io-bandwidth is not a primary concern for our local network, we chose a rather simple encoder with no 
     explicit compression stage to save CPU clock cycles.*
  2. facilitate multi-protocol implementations, protocol evolution and loose coupling between data object definitions on
     the server- and corresponding client-side, ie. services and clients may communicate with different protocol versions 
     and need to agree only on a mandatory small sub-set of information they both need to share.    
     *N.B. most micro-services develop naturally and grow their functionality with time. This decoupling is necessary to 
     provide a smooth and soft upgrade path for early adopters that require these new functionalities (ie. thus also being updated 
     during regular facility operation), and clients that may require a controlled maintenance period, e.g. safety related systems,
     that need a formal qualification process prior to being deployed into regular operation with a modified data-model.*
  3. same client- and server-side API, decoupling the serialisers' wire-formats (ie. different binary formats, JSON, XML, YML, ...) 
     from the specific microservice APIs and low-level transport protocols that transmit the serialised data  
     *N.B. encapsulates domain-specific control as well as the generic microservice logic into reusable code blocks that 
     can be re-implemented if deemed necessary, and that are decoupled from the specific required io-formats, which are usually 
     either driven by technical necessity (e.g. device supporting only one data wire-format) and/or client-side preferences 
     (e.g. web-based clients typically favouring RESTful JSON-based protocols while high-throughput clients with real-time requirements 
     often favour more optimised binary data protocols over TCP/UDP-based sockets).*
  4. derive schemas for generic data directly from C++ or Java class structures and basic types rather than a 3rd-party IDL 
     definition (ie. using [Pocos](https://en.wikipedia.org/wiki/Plain_Old_C%2B%2B_Object) & [Pojos](https://en.wikipedia.org/wiki/Plain_old_Java_object) as IDL)
      - aims at a high compatibility between C++, Java and other languages derived thereof and leverages existing experience 
        of developers with those languages 
        *N.B. this improves the productivity of new/occasional/less-experienced users who need to be ony vaguely familiar
        with C++/Java and do not need to learn yet another new dedicated DSL. This also inverts the problem: rather than 
        'here are the data structures you allowed to use to be serialised' to 'what can be done to serialise the structures 
        one already is using'.* 
      - enforces stronger type-safety  
        *N.B. some other serialisers encode only sub-sets of the possible data structures, and or reduce the specific type
        to encompassing super types. For example, integer-types such as `byte`, `short`, `int` all being mapped to `long`, 
        or all floating-point-type numbers to `double` which due to the ambiguity causes unnecessary numerical decoding errors 
        on the deserialisation side.*
      - support for simple data primitives, nested class objects or common data container, such as `Collection<T>`, `List<T>`, 
        `Set<T>`, ..., `Map<K,V>`, etc.      
        *N.B. We found, that due to the evolution of our microservices and data protocol definitions, we frequently had to 
        remap and rewrite adapters between our internal map-based data-formats and class objects which proved to be a frequent 
        and unnecessary source of coding errors.*
      - efficient (first-class) support of large collections of numeric (floating-point) data  
        *N.B. many of the serialiser supporting binary wire-format seem to be optimised for simple data structure that are typically
        much smaller than 1k Bytes rather than large numeric arrays that were eiter slow to encode and/or required custom serialiser
        extensions.* 
      - usage of compile-time reflection ie. offline/one-time optimisation prior to running %rightarrow; run deterministic/optimally while
        online w/o relying on dynamic parsing optimisations  
        *N.B. this particularly simplifies the evolution, modification of data structures, and removes one of the common source 
        of coding errors, since the synchronisation between class-structure, serialised-data-structure and formal-IDL-structure 
        is omitted.* 
      - optional: support run-time reflection as a fall-back solution for new data/users that haven't used the compile-time reflection
      - optional support of UTF-8-based and fall-back to ISO8859-1-based String encoding if a faster or more efficient en-/decoding is needed.
  5. allow schema extensions through optional custom (de-)serialiser routines for known classes or interface that are either more optimised, 
     or that implement a specific data-exchange format for a given generic class interface.  
  6. self-documented data-structures with optional data field annotations to document and communicate the data-exchange-API-intend to the client
      - some examples: 'unit' and 'description' of specific data fields, read/write field access definitions, definition of field sub-sets, etc.    
      - allows on-the-fly data structure (full schema) documentation for users based on the transmitted wire-format structure 
        w/o the explicite need to have access to the exact service class domain object definition  
        *N.B. we keep the code public, this also facilitate automatic documentation updates whenever the code is being modified
        and opens the possibility of [OpenAPI specification](https://swagger.io/specification/) -style extensions common for RESTful service.*
      - optional: full schema information is transmitted only for the first and (optionally) suppressed in subsequent transmissions for improved performance.  
        *N.B. trade-off between optimise latency/throughput in high-volume paths vs. feature-rich/documented data storage protocol for 
        less critical low-volume 'get/set' operations.*
  7. minimise code-base and code-bloat -- for two reasons:
      - smaller code usually leads to smaller compiled binary sizes that are more likely to fit into CPU cache, thus are less 
        likely to be evicted on context changes, and result into overall faster code.
        *N.B. while readability is an important issue, we found that certain needless use of 'interface + impl pattern' 
        (ie. only one implementation for given per interface) are harder to read and harder to optimise for the (JIT) compiler too. 
        As an example, in-lining and keeping the code in one (albeit larger) source file proved to yield much faster results 
        for the `CmwLightSerialiser` reimplementation of an existing internally used wire-format.* 
      - maintenance: code should be able to be re-engineered or optimised within typically 2 weeks by one skilled developer.
        *N.B. more code requires more time to read and to understand. While there are many skilled developer, having a simple
        code base also implies that the code can be more easily be modified, tested, fixed or maintained by any internally 
        available developer. Also, be believe that this makes it possibly more likely to be adopted by external users that 
        want to understand, upgrade, or bug-fix of 'what is under the hood' and is of specific interest to them. Having too 
        many paradigms, patterns or library dependencies -- even with modern IDEs -- makes it unnecessarily hard for new or 
        occasional users for getting started.*  
  8. unit-test driven development  
     *N.B. this to minimise errors, loop-holes, and to detect potential regression early-on as part of a general CI/CD strategy, 
     but also to continuously re-evaluate design choices and quantitative evolution of the performance (for both: potential 
     regressions and/or improvements, if possible).* 
  9. free- and open-source code basis w/o strings-attached: 
      - it is important to us that this code can be re-used, built- and improved-upon by anybody and not limited by 
      unnecessary hurdles to due proprietary or IP-protected interfaces or licenses. 
     *N.B. we chose the [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.txt) license in order that this remains free for future use,
     and to foster evolution of ideas and further developments that build upon this. See also [this](https://github.com/GSI-CS-CO/chart-fx/issues/221).*
  
### Some Serialiser Performance Comparison Results
The following examples are qualitative and primarily used to verify that our implementation is not significantly slower than 
another reference implementation and to document possible performance regression when refactoring the code base.  
Example output of [SerialiserQuickBenchmark.java](../src/test/java/de/gsi/serializer/benchmark/SerialiserQuickBenchmark.java) which compares the 
map-only, custom and full-pojo-to-pojo (de-)serialisation performance for the given low-level wire-format:
<a name="SerialiserQuickBenchmarkRef"></a>
```text
Example output - numbers should be compared relatively (nIterations = 100000):
(openjdk 11.0.7 2020-04-14, ASCII-only, nSizePrimitiveArrays = 10, nSizeString = 100, nestedClassRecursion = 1)
[..] more string-heavy TestDataClass
- run 1
- JSON Serializer (Map only)  throughput = 371.4 MB/s for 5.2 kB per test run (took 1413.0 ms)
- CMW Serializer (Map only) throughput = 220.2 MB/s for 6.3 kB per test run (took 2871.0 ms)
- CmwLight Serializer (Map only)  throughput = 683.1 MB/s for 6.4 kB per test run (took 935.0 ms)
- IO Serializer (Map only)  throughput = 810.0 MB/s for 7.4 kB per test run (took 908.0 ms)

- FlatBuffers (custom FlexBuffers) throughput = 173.7 MB/s for 6.1 kB per test run (took 3536.0 ms)
- CmwLight Serializer (custom) throughput = 460.5 MB/s for 6.4 kB per test run (took 1387.0 ms)
- IO Serializer (custom) throughput = 545.0 MB/s for 7.3 kB per test run (took 1344.0 ms)

- JSON Serializer (POJO) throughput = 53.8 MB/s for 5.2 kB per test run (took 9747.0 ms)
- CMW Serializer (POJO) throughput = 182.8 MB/s for 6.3 kB per test run (took 3458.0 ms)
- CmwLight Serializer (POJO) throughput = 329.2 MB/s for 6.3 kB per test run (took 1906.0 ms)
- IO Serializer (POJO) throughput = 374.9 MB/s for 7.2 kB per test run (took 1925.0 ms)

[..] more primitive-array-heavy TestDataClass
(openjdk 11.0.7 2020-04-14, UTF8, nSizePrimitiveArrays = 1000, nSizeString = 0, nestedClassRecursion = 0)
- run 1
- JSON Serializer (Map only)  throughput = 350.7 MB/s for 34.3 kB per test run (took 9793.0 ms)
- CMW Serializer (Map only) throughput = 1.7 GB/s for 29.2 kB per test run (took 1755.0 ms)
- CmwLight Serializer (Map only)  throughput = 6.7 GB/s for 29.2 kB per test run (took 437.0 ms)
- IO Serializer (Map only)  throughput = 6.1 GB/s for 29.7 kB per test run (took 485.0 ms)

- FlatBuffers (custom FlexBuffers) throughput = 123.1 MB/s for 30.1 kB per test run (took 24467.0 ms)
- CmwLight Serializer (custom) throughput = 3.9 GB/s for 29.2 kB per test run (took 751.0 ms)
- IO Serializer (custom) throughput = 3.8 GB/s for 29.7 kB per test run (took 782.0 ms)

- JSON Serializer (POJO) throughput = 31.7 MB/s for 34.3 kB per test run (took 108415.0 ms)
- CMW Serializer (POJO) throughput = 1.5 GB/s for 29.2 kB per test run (took 1924.0 ms)
- CmwLight Serializer (POJO) throughput = 3.5 GB/s for 29.1 kB per test run (took 824.0 ms)
- IO Serializer (POJO) throughput = 3.4 GB/s for 29.7 kB per test run (took 870.0 ms)
```

A more thorough test using the Java micro-benchmark framework [JMH](https://openjdk.java.net/projects/code-tools/jmh/) output 
of [SerialiserBenchmark.java](../src/test/java/de/gsi/serializer/benchmark/SerialiserBenchmark.java), with 
'testClassId 1' being a string-heavy test data class and 'testClassId 2' being a numeric-data-heavy test data class:
<a name="SerialiserBenchmarkRef"></a>
```text
Benchmark                                     (testClassId)   Mode  Cnt      Score      Error  Units
SerialiserBenchmark.customCmwLight                        1  thrpt   10  22738.741 ±  100.954  ops/s
SerialiserBenchmark.customCmwLight                        2  thrpt   10  22382.762 ± 1583.852  ops/s
SerialiserBenchmark.customFlatBuffer                      1  thrpt   10    227.740 ±    5.658  ops/s
SerialiserBenchmark.customFlatBuffer                      2  thrpt   10    230.471 ±    1.453  ops/s
SerialiserBenchmark.customIoSerialiser                    1  thrpt   10  24177.429 ±  159.683  ops/s
SerialiserBenchmark.customIoSerialiser                    2  thrpt   10  24253.067 ±  153.410  ops/s
SerialiserBenchmark.customIoSerialiserOptim               1  thrpt   10  24402.375 ±  101.936  ops/s
SerialiserBenchmark.customIoSerialiserOptim               2  thrpt   10  24280.526 ±  153.846  ops/s
SerialiserBenchmark.mapCmwLight                           1  thrpt   10  66713.301 ± 1154.371  ops/s
SerialiserBenchmark.mapCmwLight                           2  thrpt   10  66585.727 ± 1541.359  ops/s
SerialiserBenchmark.mapIoSerialiser                       1  thrpt   10  69326.547 ± 1638.850  ops/s
SerialiserBenchmark.mapIoSerialiser                       2  thrpt   10  67812.717 ± 1938.834  ops/s
SerialiserBenchmark.mapIoSerialiserOptimized              1  thrpt   10  69835.103 ±  545.613  ops/s
SerialiserBenchmark.mapIoSerialiserOptimized              2  thrpt   10  69129.255 ± 2679.170  ops/s
SerialiserBenchmark.pojoCmwLight                          1  thrpt   10  34084.692 ±  277.714  ops/s
SerialiserBenchmark.pojoCmwLight                          2  thrpt   10  33909.100 ±  445.808  ops/s
SerialiserBenchmark.pojoIoSerialiser                      1  thrpt   10  33582.440 ±  517.115  ops/s
SerialiserBenchmark.pojoIoSerialiser                      2  thrpt   10  33521.426 ±  659.651  ops/s
SerialiserBenchmark.pojoIoSerialiserOptim                 1  thrpt   10  32668.111 ±  539.256  ops/s
SerialiserBenchmark.pojoIoSerialiserOptim                 2  thrpt   10  32724.097 ±  234.088  ops/s
```
*N.B. The 'FlatBuffer' implementation is bit of an outlier and uses internally FlatBuffer's `FlexBuffer` builder which does not 
support or is optimised for large primitive arrays. `FlexBuffer` was chosen primarily for comparison since it supported flexible 
compile/run-time map-type structures similar to the other implementations, whereas the faster Protobuf and Flatbuffer builder 
require IDL-based desciptions that are used during compile-time to generate the necessary data-serialiser stubs.*

JSON-compatible strings are easy to construct and write. Nevertheless, we chose the [Json-Itererator](https://github.com/json-iterator/java) 
library as backend for implementing the [JsonSerialiser](../../microservice/src/main/java/de/gsi/serializer/spi/JsonSerialiser.java) 
for purely pragmatic reasons and to initially avoid common pitfalls in implementing a robust JSON deserialiser.
The [JsonSelectionBenchmark.java](../src/test/java/de/gsi/serializer/benchmark/JsonSelectionBenchmark.java) compares the 
choice with several other commonly used JSON serialisation libraries for a string-heavy and a numeric-data-heavy test data class:
```text
Benchmark                                   (testClassId)   Mode  Cnt      Score     Error  Units
 JsonSelectionBenchmark.pojoFastJson          string-heavy  thrpt   10  12857.850 ± 109.050  ops/s
 JsonSelectionBenchmark.pojoFastJson         numeric-heavy  thrpt   10     91.458 ±   0.437  ops/s
 JsonSelectionBenchmark.pojoGson              string-heavy  thrpt   10   6253.698 ±  50.267  ops/s
 JsonSelectionBenchmark.pojoGson             numeric-heavy  thrpt   10     48.215 ±   0.265  ops/s
 JsonSelectionBenchmark.pojoJackson           string-heavy  thrpt   10  16563.604 ± 244.329  ops/s
 JsonSelectionBenchmark.pojoJackson          numeric-heavy  thrpt   10    135.780 ±   1.074  ops/s
 JsonSelectionBenchmark.pojoJsonIter          string-heavy  thrpt   10  10733.539 ±  35.605  ops/s
 JsonSelectionBenchmark.pojoJsonIter         numeric-heavy  thrpt   10     86.629 ±   1.122  ops/s
 JsonSelectionBenchmark.pojoJsonIterCodeGen   string-heavy  thrpt   10  41048.034 ± 396.628  ops/s
 JsonSelectionBenchmark.pojoJsonIterCodeGen  numeric-heavy  thrpt   10    377.412 ±   9.755  ops/s
```
Performance was not of primary concern for us, since JSON-based protocols are anyway slow. The heavy penalty for 
numeric-heavy data is largely related to the inefficient string representation of double values.

### References
<a name="references"></a>
  * Brian Goetz, "[Towards Better Serialization](https://cr.openjdk.java.net/~briangoetz/amber/serialization.html)", 2019 
  * Pieter Hintjens et al., "[ZeroMQ's ZGuide on Serialisation](http://zguide.zeromq.org/page:chapter7#Serializing-Your-Data)" in 'ØMQ - The Guide', 2020
  * N. Trofimov et al., "[Remote Device Access in the new CERN accelerator controls middleware](https://accelconf.web.cern.ch/ica01/papers/THAP003.pdf)", ICALEPCS 2001, San Jose, USA, 2001, (proprietary, closed-source)
  * J. Lauener, W. Sliwinski, "[How to Design & Implement a Modern Communication Middleware based on ZeroMQ](https://cds.cern.ch/record/2305650/files/mobpl05.pdf)", ICALEPS2017, Barcelona, Spain, 2017, (proprietary, closed-source)
  * [Google's own Protobuf Serialiser](https://github.com/protocolbuffers/protobuf)
  * [Google's own FlatBuffer Serialiser](https://github.com/google/flatbuffers)
  * [Implementing High Performance Parsers in Java](https://www.infoq.com/articles/HIgh-Performance-Parsers-in-Java-V2/)
  * [Is Protobuf 5x Faster Than JSON?](https://dzone.com/articles/is-protobuf-5x-faster-than-json-part-ii) ([Part 1](https://dzone.com/articles/is-protobuf-5x-faster-than-json), [Part 2](https://dzone.com/articles/is-protobuf-5x-faster-than-json-part-ii)) and reference therein
  
  
