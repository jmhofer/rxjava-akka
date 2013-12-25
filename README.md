# rxjava-akka

Attempt at a bridge from RxJava to Akka and vice versa.

## Dependency

This module is published on [Bintray](https://bintray.com/).

### sbt

This is how you declare the Bintray resolver and add the dependency on `rxjava-akka` for [sbt](http://scala-sbt.org):

    resolvers += "bintray-jmhofer" at "http://dl.bintray.com/jmhofer/maven"

    libraryDependencies += "de.johoop" %% "rxjava-akka" % "1.0.0"

### Maven

Add the repository to Maven:

    <repository>
      <id>bintray-jmhofer</id>
      <url>http://dl.bintray.com/jmhofer/maven</url>
    </repository>

Resolve the library:

    <dependency>
      <groupId>de.johoop</groupId>
      <artifactId>rxjava-akka_2.10</artifactId>
      <version>1.0.0</version>
    </dependency>

## Usage

to do

## Building

This build uses [sbt](http://scala-sbt.org).

## License

See [LICENSE](https://github.com/jmhofer/rxjava-akka/blob/master/LICENSE) (Apache License 2.0).
