# Ganymede Kernel 1.0.0-SNAPSHOT

The [Ganymede Kernel] is a [Jupyter Notebook] Java [kernel][Jupyter Kernel].
Java code is compiled and interpreted with the Java Shell tool, [JShell].
This kernel offers the following additional features:

* Integrated Project Object Model (POM) for [Apache Maven] artifact
    dependency resolution<sup id="ref1">[1](#endnote1)</sup>

* Integrated support for [JSR 223] scripting languages including:

    * [Groovy]
    * [Javascript]<sup id="ref2">[2](#endnote2)</sup>
    * [Kotlin]

* Templates (via [Thymeleaf])

* Support for [Apache Spark] and [Scala] binary distributions


## Installation

The [Ganymede Kernel] is distributed in a single JAR.

Java 11 or later is required.  In addition to Java, the [Jupyter Notebook]
must be installed first and the `jupyter` and `python` commands must be on
the `${PATH}`.  Then the typical (and minimal) installation command line:

```bash
$ java -jar ganymede-kernel-1.0.0-SNAPSHOT.jar --install
```

The kernel will be configured to use the same `java` installation as invoked
in the install command above.  These additional command line options are
supported.

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

For example, a sophisticated configuration to test the current snapshot out
of a user's local [Maven][Apache Maven] repository:

```bash
$ export JAVA_HOME=$(/usr/libexec/java_home -v 11)
$ ${JAVA_HOME}/bin/java \
      -jar ${HOME}/.m2/repository/ganymede/ganymede-kernel/1.0.0-SNAPSHOT/ganymede-kernel-1.0.0-SNAPSHOT.jar \
      --install --sys-prefix --copy-jar=false \
      --id-suffix=spark-3.1.1 --display-name-suffix="with Spark 3.1.1" \
      --env=SPARK_HOME=/path/to/spark-3.1.1-bin-hadoop3.2
```

would result in the configured
`${jupyter.data}/kernels/ganymede-1.0.0-snapshot-java-11-spark-3.1.1/kernel.json`
kernelspec:

```json
{
  "argv" : [
    "/Library/Java/JavaVirtualMachines/graalvm-ce-java11-21.0.0/Contents/Home/bin/java",
    "-Dmaven.repo.local=/Users/jdoe/Notebooks/.venv/share/jupyter/repository",
    "-jar",
    "/Users/jdoe/.m2/repository/ganymede/ganymede-kernel/1.0.0-SNAPSHOT/ganymede-kernel-1.0.0-SNAPSHOT.jar",
    "--runtime-dir=/Users/jdoe/Library/Jupyter/runtime",
    "--connection-file={connection_file}"
  ],
  "display_name" : "Ganymede 1.0.0-SNAPSHOT (Java 11) with Spark 3.1.1",
  "env" : {
    "SPARK_HOME" : "/path/to/spark-3.1.1-bin-hadoop3.2"
  },
  "interrupt_mode" : "message",
  "language" : "java"
}
```

The [Ganymede Kernel] makes extensive use of templates and POM fragments.
While not strictly required, the authors suggest that the
[Hide Input](https://jupyter-contrib-nbextensions.readthedocs.io/en/latest/nbextensions/hide_input/readme.html)
is enabled so notebook authors can hide the input templates and POMs for any
finished product.  This may be set from the command line with:

```bash
$ jupyter nbextension enable hide_input/main --sys-prefix
```

(or `--user` as appropriate).


## Features and Usage

The following subsections outline many of the features of the kernel.


### Java


### Magics


### Other Interpreted Laguages


### Shells


### Templates


### Dependency and Classpath Management


## Documentation


## License

Ganymede Kernel is released under the
[Apache License][Apache License, Version 2.0].


## Endnotes

<b id="endnote1">[1]</b>
Implemented with [Apache Maven Artifact Resolver].
[↩](#ref1)

<b id="endnote2">[2]</b>
With the built-in Oracle Nashorn engine.
[↩](#ref2)

[Ganymede Kernel]: https://github.com/allen-ball/ganymede-kernel

[Apache License, Version 2.0]: https://www.apache.org/licenses/LICENSE-2.0

[Apache Maven]: https://maven.apache.org/

[Apache Maven Artifact Resolver]: https://maven.apache.org/resolver/index.html

[Apache Spark]: http://spark.apache.org/

[Groovy]: https://groovy-lang.org/

[Javascript]: https://www.oracle.com/technical-resources/articles/java/jf14-nashorn.html

[JShell]: https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jshell/jdk/jshell/JShell.html?is-external=true

[JSR 223]: https://jcp.org/en/jsr/detail?id=223

[Jupyter Notebook]: https://jupyter-notebook.readthedocs.io/en/stable/index.html

[Jupyter Kernel]: https://jupyter-client.readthedocs.io/en/stable/kernels.html

[Kotlin]: https://kotlinlang.org/

[Scala]: https://www.scala-lang.org/

[Thymeleaf]: https://www.thymeleaf.org/index.html
