# <a href="https://github.com/JetBrains/xodus/wiki"><img src="https://raw.githubusercontent.com/wiki/jetbrains/xodus/xodus.png" width=160></a>

[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jetbrains.xodus/xodus-openAPI/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Corg.jetbrains.xodus%20-dnq%20-time)
[![Last Release](https://img.shields.io/github/release-date/jetbrains/xodus.svg?logo=github)](https://github.com/jetbrains/xodus/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
![Pure Java + Kotlin](https://img.shields.io/badge/100%25-java%2bkotlin-orange.svg)
[![Stack Overflow](https://img.shields.io/:stack%20overflow-xodus-brightgreen.svg)](https://stackoverflow.com/questions/tagged/xodus)

JetBrains Xodus is a transactional schema-less embedded database that is written in Java and [Kotlin](https://kotlinlang.org).
It was initially developed for [JetBrains YouTrack](https://jetbrains.com/youtrack), an issue tracking and project
management tool. Xodus is also used in [JetBrains Hub](https://jetbrains.com/hub), the user management platform
for JetBrains' team tools, and in some internal JetBrains projects.

- Xodus is transactional and fully ACID-compliant.
- Xodus is highly concurrent. Reads are completely non-blocking due to [MVCC](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) and
true [snapshot isolation](https://en.wikipedia.org/wiki/Snapshot_isolation).
- Xodus is schema-less and agile. It does not require schema migrations or refactorings.
- Xodus is embedded. It does not require installation or administration.
- Xodus is written in pure Java and [Kotlin](https://kotlinlang.org).
- Xodus is free and licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Hello Worlds!

To start using Xodus, define dependencies:
```xml
<!-- in Maven project -->
<dependency>
    <groupId>org.jetbrains.xodus</groupId>
    <artifactId>xodus-openAPI</artifactId>
    <version>2.0.1</version>
</dependency>
```
```groovy
// in Gradle project
dependencies {
    compile 'org.jetbrains.xodus:xodus-openAPI:2.0.1'
}
```
Read more about [managing dependencies](https://github.com/JetBrains/xodus/wiki/Managing-Dependencies).

There are two different ways to deal with data, which results in two different API layers: [Environments](https://github.com/JetBrains/xodus/wiki/Environments) and [Entity Stores](https://github.com/JetBrains/xodus/wiki/Entity-Stores).
 
### Environments

Add dependency on `org.jetbrains.xodus:xodus-environment:2.0.1`.

```java
try (Environment env = Environments.newInstance("/home/me/.myAppData")) {
    env.executeInTransaction(txn -> {
        final Store store = env.openStore("Messages", StoreConfig.WITHOUT_DUPLICATES, txn);
        store.put(txn, StringBinding.stringToEntry("Hello"), StringBinding.stringToEntry("World!"));
    });
}
```
### Entity Stores

Add dependency on `org.jetbrains.xodus:xodus-entity-store:2.0.1`, `org.jetbrains.xodus:xodus-environment:2.0.1` and `org.jetbrains.xodus:xodus-vfs:2.0.1`.

```java
try (PersistentEntityStore entityStore = PersistentEntityStores.newInstance("/home/me/.myAppData")) {
    entityStore.executeInTransaction(txn -> {
        final Entity message = txn.newEntity("Message");
        message.setProperty("hello", "World!");
    });
}
```

## Building from Source
[Gradle](https://www.gradle.org) is used to build, test, and publish. JDK 1.8 or higher is required. To build the project, run:

    ./gradlew build

To assemble JARs and skip running tests, run:

    ./gradlew assemble


## Find out More
- [Xodus wiki](https://github.com/JetBrains/xodus/wiki)
- [Report an issue](https://youtrack.jetbrains.com/issues/XD)
