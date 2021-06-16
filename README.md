[![Ganymede](README/ganymede.png)](https://images.nasa.gov/details-GSFC_20171208_Archive_e002091)


# Ganymede: Jupyter Notebook Java Kernel

The [Ganymede Kernel] is a [Jupyter Notebook] Java [kernel][Jupyter Kernel].
Java code is compiled and interpreted with the Java Shell tool, [JShell].
This kernel offers the following additional features:

* Integrated Project Object Model (POM) for [Apache Maven] artifact
    dependency resolution<sup id="ref1">[1](#endnote1)</sup>

* Integrated support for [JSR 223] scripting languages including:

    * [Groovy]
    * [Javascript]<sup id="ref2">[2](#endnote2)</sup>
    * [Kotlin]

* Templates (via any of [Thymeleaf], [Markdown] ([CommonMark]) with
  [Handlebars][Handlebars.java], [FreeMarker][Apache FreeMarker], and
  [Velocity][Apache Velocity])

* Support for [Apache Spark] and [Scala] binary distributions


## Installation

The [Ganymede Kernel] is distributed as a single JAR
([download here][Ganymede Kernel download]).

Java 11 or later is required.  In addition to Java, the [Jupyter Notebook]
must be [installed][Jupyter Notebook Installation] first and the `jupyter`
and `python` commands must be on the `${PATH}`.  Then the typical (and
minimal) installation command line:

```bash
$ java -jar ganymede-kernel-1.1.0.20210614.jar --install
```

The [kernel][Ganymede Kernel] will be configured to use the same `java`
installation as invoked in the install command above.  These additional
command line options are supported.

| Option                               | Action                                                                                    | Default                                                  |
| ---                                  | ---                                                                                       | ---                                                      |
| --id-prefix=&lt;prefix&gt;           | Adds prefix to kernel ID                                                                  | &lt;none&gt;                                             |
| --id=&lt;id&gt;                      | Specifies kernel ID                                                                       | ganymede-${version}-java-${java.specification.version}   |
| --id-suffix=&lt;suffix&gt;           | Adds suffix to kernel ID                                                                  | &lt;none&gt;                                             |
| --display-name-prefix=&lt;prefix&gt; | Adds prefix to kernel display name                                                        | &lt;none&gt;                                             |
| --display-name=&lt;name&gt;          | Specifies kernel display name                                                             | Ganymede ${version} (Java ${java.specification.version}) |
| --display-name-suffix=&lt;suffix&gt; | Adds suffix to kernel display name                                                        | &lt;none&gt;                                             |
| --env                                | Specify NAME=VALUE pair(s) to add to kernel environment                                   |                                                          |
| --copy-jar=&lt;boolean&gt;           | Copies the [Ganymede Kernel] JAR to the `kernelspec` directory                            | true                                                     |
| --sys-prefix<br/>or --user           | Install in the system prefix or user path (see the `jupyter kernelspec install` command). | --user                                                   |

The following Java system properties may be configured.

| System Properties | Action                                | Default(s)                                                                                                                          |
| ---               | ---                                   | ---                                                                                                                                 |
| maven.repo.local  | Configures the local Maven repository | <table><tr><td>--sys-prefix</td><td>${jupyter.data}/repository/</td></tr><tr><td>--user</td><td>${user.home}/.m2/</td></tr></table> |

The following OS environment variables may be configured:

| Environment Variable | Action                                                                       |
| ---                  | ---                                                                          |
| SPARK_HOME           | If configured, the kernel will add the Spark JARs to the kernel's classpath. |

For example, a sophisticated configuration to test a snapshot out of a
user's local [Maven][Apache Maven] repository:

```bash
$ export JAVA_HOME=$(/usr/libexec/java_home -v 11)
$ ${JAVA_HOME}/bin/java \
      -jar ${HOME}/.m2/repository/ganymede/ganymede-kernel/1.2.0-SNAPSHOT/ganymede-kernel-1.2.0-SNAPSHOT.jar \
      --install --sys-prefix --copy-jar=false \
      --id-suffix=spark-3.1.2 --display-name-suffix="with Spark 3.1.2" \
      --env=SPARK_HOME=/path/to/spark-3.1.2-bin-hadoop3.2
$ jupyter kernelspec list
Available kernels:
...
  ganymede-1.2.0-java-11-spark-3.1.2             /.../share/jupyter/kernels/ganymede-1.2.0-java-11-spark-3.1.2
...
```

would result in the configured
`${jupyter.data}/kernels/ganymede-1.2.0-java-11-spark-3.1.2/kernel.json`
kernelspec:

```json
{
    "argv" :
    [
        "/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/bin/java",
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--illegal-access=permit",
        "-Djava.awt.headless=true",
        "-Djdk.disableLastUsageTracking=true",
        "-Dmaven.repo.local=/Users/jdoe/Notebooks/.venv/share/jupyter/repository",
        "-jar",
        "/Users/jdoe/.m2/repository/ganymede/ganymede-kernel/1.2.0-SNAPSHOT/ganymede-kernel-1.2.0-SNAPSHOT.jar",
        "--runtime-dir=/Users/jdoe/Library/Jupyter/runtime",
        "--connection-file={connection_file}"
    ],
    "display_name" : "Ganymede 1.2.0 (Java 11) with Spark 3.1.2",
    "env" : {
        "SPARK_HOME" : "/path/to/spark-3.1.2-bin-hadoop3.2"
    },
    "interrupt_mode" : "message",
    "language" : "java"
}
```

The [kernel][Ganymede Kernel] makes extensive use of templates and POM
fragments.  While not strictly required, the authors suggest that the
[Hide Input] extension is enabled so notebook authors can hide the input
templates and POMs for any finished product.  This may be set from the
command line with:

```bash
$ jupyter nbextension enable hide_input/main --sys-prefix
```

(or `--user` as appropriate).


## Features and Usage

The following subsections outline many of the features of the
[kernel][Ganymede Kernel].


### Java

The Java REPL is [JShell] and has all the Java features of the installed
JVM.  The minimum required Java version is 11 and subsequent versions are
supported.

The [JShell] environment includes builtin functions implemented through
methods that wrap the `public` methods defined in [NotebookContext] class
annotated with [@NotebookFunction][NotebookFunction].  These functions
include:

| Method          | Description                            |
| ---             | ---                                    |
| print(Object)   | Render the Object to a Notebook format |
| display(Object) | Render the Object to a Notebook format |
| toJson(Object)  | Convert argument to JsonNode           |

The builtin functions are mostly concerned with "printing" or displaying
(rendering) Objects to multimedia formats.  For example, `print(byte[])`
will render the byte array as an image.  Integrated renderers for chart and
plot objects include:

* [JFreeChart]

* [Tablesaw] (wrapping [Plotly])

* [XChart]

The [trig.ipynb] notebook demonstrates rendering of an [XChart].


### Magics

Cell magic commands are identified by `%%` starting the first line of a code
cell.  The list of available magic commands is shown below.  The default
cell magic is `java`.

<div>
  <table border="1" class="magics">
    <thead>
      <tr><th>Name(s)</th><th>Description</th></tr>
    </thead>
    <tbody>
      <tr>
        <td>!, script</td><td>Execute script with the argument command</td>
      </tr>
      <tr>
        <td>bash</td><td>Execute script with &#39;bash&#39; command</td>
      </tr>
      <tr>
        <td>classpath</td><td>Add to or print JShell classpath</td>
      </tr>
      <tr>
        <td>env</td><td>Add/Update or print the environment</td>
      </tr>
      <tr>
        <td>freemarker</td><td>FreeMarker template evaluator</td>
      </tr>
      <tr>
        <td>groovy</td><td>Execute code in groovy REPL</td>
      </tr>
      <tr>
        <td>handlebars</td><td>Handlebars template evaluator</td>
      </tr>
      <tr>
        <td>html</td><td>HTML template evaluator</td>
      </tr>
      <tr>
        <td>java</td><td>Execute code in Java REPL</td>
      </tr>
      <tr>
        <td>javascript, js</td><td>Execute code in javascript REPL</td>
      </tr>
      <tr>
        <td>kotlin</td><td>Execute code in kotlin REPL</td>
      </tr>
      <tr>
        <td>magics</td><td>Lists available cell magics</td>
      </tr>
      <tr>
        <td>markdown</td><td>Markdown template evaluator</td>
      </tr>
      <tr>
        <td>perl</td><td>Execute script with &#39;perl&#39; command</td>
      </tr>
      <tr>
        <td>pom</td><td>Define the Notebook&#39;s Project Object Model</td>
      </tr>
      <tr>
        <td>ruby</td><td>Execute script with &#39;ruby&#39; command</td>
      </tr>
      <tr>
        <td>scala</td><td>Execute code in scala REPL</td>
      </tr>
      <tr>
        <td>sh</td><td>Execute script with &#39;sh&#39; command</td>
      </tr>
      <tr>
        <td>thymeleaf</td><td>Thymeleaf template evaluator</td>
      </tr>
      <tr>
        <td>velocity</td><td>Velocity template evaluator</td>
      </tr>
    </tbody>
  </table>
</div>

`script`, `bash`, `perl`, etc. are executed by creating a [Process]
instance.  [`groovy`][Groovy], [`javascript`][Javascript],
[`kotlin`][Kotlin], etc. are provided through their respective [JSR 223]
interfaces.<sup id="ref3">[3](#endnote3)</sup>  Dependency and classpath
management are provided with the `classpath` and `pom` magics and are
described in detail in a subsequent subsection.  `thymeleaf` and `html`
provide [Thymeleaf] template evaluation.

The [kernel][Ganymede Kernel] does not implement any "line" magics.


### Dependency and Classpath Management

The `classpath` magic adds JAR and directory paths to the [JShell]
classpath.  The `pom` magic resolves and downloads [Maven][Apache Maven]
artifacts and then adds those artifacts to the classpath.

The [trig.ipynb] notebook demonstrates the use of the `pom` magic to resolve
the `org.knowm.xchart:xchart:3.8.0` artifact and its transient dependencies.

```yaml
%%pom
dependencies:
- org.knowm.xchart:xchart:3.8.0
```

The POM is expressed in [YAML] and repositories and dependencies may be
expressed.  The Notebook's POM may be split across multiple cells since each
repository and dependency is added or merged and dependency resolution is
attempted whenever a `pom` cell is executed.  The default/initial Notebook
POM is:

```yaml
repositories:
  - id: central
    layout: default
    url: https://repo1.maven.org/maven2
    snapshots:
      enabled: false
```

Dependencies may either be expressed in "expanded" YAML or in
`groupId:artifactId[:extension]:version` format:

```yaml
dependencies:
  - groupId: groupA
    artifactId: groupAartifact1
    version: 1.0
  - groupB:groupB-artifact2:2.0
```

The specific attributes for repositories and dependencies are defined by the
[Apache Maven Artifact Resolver] classes [RemoteRepository] (with
[RepositoryPolicy]) and [Dependency].  (Note that these classes are slightly
different than their [Maven][Apache Maven] settings counterparts.)

Whenever a JAR is added to the classpath, it is analyzed to determine if its
[Maven coordinates] can be determined and, if they can be determined, the
JAR is added as an artifact to the
[resolver][Apache Maven Artifact Resolver].  The following checks are made
before adding the JAR to the [JShell] classpath:

1. It is a new, unique path

2. No previously resolved artifact with the same `groupId:artifactId` on the
   classpath

3. Special heuristics for logging configuration:

    a. Ignore `commons-logging:commons-logging:jar`

    b. Allow only one of `org.slf4j:jcl-over-slf4j:jar` or
    `org.springframework:spring-jcl:jar` to be configured

    c. Allow only one of `org.slf4j:slf4j-log4j12:jar` and
    `ch.qos.logback:logback-classic:jar` to be configured

Artifacts that fail any of the above checks will be (mostly silently)
ignored.  Because only the first version of a resolved artifact is ever
added to the classpath, the [kernel][Ganymede Kernel] must be restarted if a
different version of the same artifact is specified for the change to take
effect.

Finally, the [kernel][Ganymede Kernel] provides special processing to add
artifacts from [Apache Spark] binary distributions.  The dependencies for
[Spark SQL][Apache Spark SQL] and corresponding [Scala] compiler artifacts
for currently available Spark binary distributions as resources.  The kernel
searches the `${SPARK_HOME}` for JARs for which it has the corresponding
dependencies and then resolves the dependencies from the `${SPARK_HOME}`
hierarchy with the heuristics described above.


### Other Laguages ([JSR 223])

The [kernel][Ganymede Kernel] leverages the [java.scripting API] to provide
[`groovy`][Groovy], [`javascript`][Javascript], [`kotlin`][Kotlin], and
[`scala`][Scala].<sup id="ref4">[4](#endnote4)</sup>


### Shells

The `script` magic (with the alias `!`) maybe used to run an operating
system command with the remaining code in the cell fed to the [Process]'s
standard input.  `bash`, `perl`, `ruby`, and `sh` are provided as aliases
for `%%!bash`, `%%!perl`, etc., respectively.


### Templates

A number of templating languages are supported as magics:

* [Markdown] ([CommonMark] preprocessed with [Handlebars.java])
* [Apache FreeMarker]
* [Apache Velocity]
* [Handlebars.java]
* [Thymeleaf]

The following subsections provide examples of the `markdown` and `thymeleaf`
magics but the other template magics are similar.  Please refer to the
installation instructions for discussion of enabling the [Hide Input]
extension so only the template output is displayed in the notebook.


### Markdown and Handlebars.java

The template magic `markdown` provides [Markdown] processing with
[Handlebars][Handlebars.java] preprocessing:

```java
%%java
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

var fib =
    Stream.iterate(new int[] { 0, 1 }, t -> new int[] { t[1], t[0] + t[1] })
    .mapToInt(t -> t[0])
    .limit(10)
    .boxed()
    .collect(toList());
```


```java
%%markdown
| Index | Value |
| --- | --- |
{{#each fib}}| {{@index}} | {{this}} |
{{/each}}
```

<table>
<thead>
<tr>
<th>Index</th>
<th>Value</th>
</tr>
</thead>
<tbody>
<tr>
<td>0</td>
<td>0</td>
</tr>
<tr>
<td>1</td>
<td>1</td>
</tr>
<tr>
<td>2</td>
<td>1</td>
</tr>
<tr>
<td>3</td>
<td>2</td>
</tr>
<tr>
<td>4</td>
<td>3</td>
</tr>
<tr>
<td>5</td>
<td>5</td>
</tr>
<tr>
<td>6</td>
<td>8</td>
</tr>
<tr>
<td>7</td>
<td>13</td>
</tr>
<tr>
<td>8</td>
<td>21</td>
</tr>
<tr>
<td>9</td>
<td>34</td>
</tr>
</tbody>
</table>


#### Thymeleaf

The template magics `thymeleaf` and `html` offer templating with
[Thymeleaf].  All defined Java variables are bound into the Thymeleaf
context before evaluation.  For example (Java implementation detail
removed):

```java
%%java
...
var map = new TreeMap<Ranking,List<Card>>(Ranking.COMPARATOR.reversed());
...
var rankings = Arrays.asList(Ranking.values());
...
```

```html
%%html
<table>
  <tr th:each="ranking : ${rankings}">
    <th:block th:if="${map.containsKey(ranking)}">
      <th th:text="${ranking}"/><td th:each="card : ${map.get(ranking)}" th:text="${card}"/>
    </th:block>
  </tr>
  <tr><th>Remaining</th><td th:each="card : ${deck}" th:text="${card}"/></tr>
</table>
```

Would generate:

<table>
  <tr>
    <th>RoyalFlush</th><td>A-♧</td><td>K-♧</td><td>Q-♧</td><td>J-♧</td><td>10-♧</td>
  </tr>
  <tr>
    <th>StraightFlush</th><td>K-♢</td><td>Q-♢</td><td>J-♢</td><td>10-♢</td><td>9-♢</td>
  </tr>
  <tr>
    <th>FourOfAKind</th><td>8-♧</td><td>8-♢</td><td>8-♡</td><td>8-♤</td><td>J-♤</td>
  </tr>
  <tr>
    <th>FullHouse</th><td>A-♢</td><td>A-♡</td><td>A-♤</td><td>K-♡</td><td>K-♤</td>
  </tr>
  <tr>
    <th>Flush</th><td>Q-♡</td><td>J-♡</td><td>10-♡</td><td>9-♡</td><td>7-♡</td>
  </tr>
  <tr>
    <th>Straight</th><td>7-♧</td><td>6-♧</td><td>5-♧</td><td>4-♧</td><td>3-♧</td>
  </tr>
  <tr>
    <th>ThreeOfAKind</th><td>6-♢</td><td>6-♡</td><td>6-♤</td><td>10-♤</td><td>5-♤</td>
  </tr>
  <tr>
    <th>TwoPair</th><td>9-♧</td><td>9-♤</td><td>7-♢</td><td>7-♤</td><td>4-♢</td>
  </tr>
  <tr>
    <th>Pair</th><td>5-♢</td><td>5-♡</td><td>4-♡</td><td>4-♤</td><td>3-♢</td>
  </tr>
  <tr>
    <th>HighCard</th><td>Q-♤</td><td>3-♡</td><td>3-♤</td><td>2-♧</td><td>2-♢</td>
  </tr>
  <tr>
  </tr>
  <tr><th>Remaining</th><td>2-♡</td><td>2-♤</td></tr>
</table>


## Documentation

[Javadoc][Ganymede API Javadoc] is published at
<https://allen-ball.github.io/ganymede>.


## License

[Ganymede Kernel] is released under the
[Apache License][Apache License, Version 2.0].


## Endnotes

<b id="endnote1">[1]</b>
Implemented with [Apache Maven Artifact Resolver].
[↩](#ref1)

<b id="endnote2">[2]</b>
With the built-in Oracle Nashorn engine.
[↩](#ref2)

<b id="endnote3">[3]</b>
[`scala`][Scala] is special cased: It requires additional dependencies be
specified at runtime and is optimized to be used with [Apache Spark].
[↩](#ref3)

<b id="endnote4">[4]</b>
Ibid.
[↩](#ref4)

[Apache License, Version 2.0]: https://www.apache.org/licenses/LICENSE-2.0

[Apache FreeMarker]: https://freemarker.apache.org/

[Apache Maven]: https://maven.apache.org/
[Maven coordinates]: https://maven.apache.org/pom.html#Maven_Coordinates

[Apache Maven Artifact Resolver]: https://maven.apache.org/resolver/index.html
[RemoteRepository]: https://maven.apache.org/resolver/maven-resolver-api/apidocs/org/eclipse/aether/repository/RemoteRepository.html?is-external=true
[RepositoryPolicy]: https://maven.apache.org/resolver/maven-resolver-api/apidocs/org/eclipse/aether/repository/RepositoryPolicy.html
[Dependency]: https://maven.apache.org/resolver/maven-resolver-api/apidocs/org/eclipse/aether/graph/Dependency.html?is-external=true

[Apache Spark]: http://spark.apache.org/
[Apache Spark SQL]: https://spark.apache.org/sql/

[Apache Velocity]: https://velocity.apache.org/

[Groovy]: https://groovy-lang.org/

[Handlebars.java]: https://github.com/jknack/handlebars.java

[Javascript]: https://www.oracle.com/technical-resources/articles/java/jf14-nashorn.html

[JFreeChart]: https://github.com/jfree/jfreechart

[JShell]: https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jshell/jdk/jshell/JShell.html?is-external=true

[JSR 223]: https://jcp.org/en/jsr/detail?id=223
[java.scripting API]: https://docs.oracle.com/en/java/javase/11/docs/api/java.scripting/module-summary.html

[Jupyter Notebook]: https://jupyter-notebook.readthedocs.io/en/stable/index.html
[Jupyter Notebook Installation]: https://jupyter.readthedocs.io/en/latest/install/notebook-classic.html
[Hide Input]: https://jupyter-contrib-nbextensions.readthedocs.io/en/latest/nbextensions/hide_input/readme.html

[Jupyter Kernel]: https://jupyter-client.readthedocs.io/en/stable/kernels.html

[Kotlin]: https://kotlinlang.org/

[Markdown]: https://en.wikipedia.org/wiki/Markdown
[CommonMark]: https://commonmark.org/

[Plotly]: https://github.com/plotly

[Scala]: https://www.scala-lang.org/

[Tablesaw]: https://github.com/jfree/jfreechart

[Thymeleaf]: https://www.thymeleaf.org/index.html

[XChart]: https://github.com/knowm/XChart

[YAML]: https://yaml.org/


[Process]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Process.html


[Ganymede Kernel]: https://github.com/allen-ball/ganymede
[Ganymede Kernel download]: https://github.com/allen-ball/ganymede/releases/download/v1.1.0.20210614/ganymede-kernel-1.1.0.20210614.jar
[Ganymede API Javadoc]: https://allen-ball.github.io/ganymede/index.html?overview-summary.html
[NotebookContext]: https://allen-ball.github.io/ganymede/ganymede/notebook/NotebookContext.html
[NotebookFunction]: https://allen-ball.github.io/ganymede/ganymede/notebook/NotebookFunction.html


[trig.ipynb]: https://github.com/allen-ball/ganymede-notebooks/blob/trunk/trig.ipynb
