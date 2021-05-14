# Jython 3 : Python for the Java Platform

This is the development repository of Jython 3,
the implementation of Python 3 in Java.

Jython 3.0 provides substantial support of the Python ecosystem.
This includes built-in support of *pip/setuptools*
(you can use `bin/pip` if the targets do not include `C` extensions)
and a native launcher for Windows (`bin/jython.exe`)
that works essentially as the `python` command.


## How to build Jython 3

The project uses Git for version-control,
and the master repository is at https://github.com/aaveshdev/jython3,
You should clone this repository to create a buildable copy of the latest state
of the Jython 3 source.

### Build using `ant` for development

Jython is normally built using `ant`.
It is necessary to have Ant and at least a Java 8 SDK on the path.
To build Jython 3 in development, we generally use the command:
```
ant
```
This leaves an executable in `dist/bin`
that you may run from the check-out root with:
```
dist/bin/jython
```
Other `ant` targets exist, notably `clean`, and `jar`.

You can test your build of Jython 3(by running the regression tests),
with the command:
```
dist/bin/jython -m test.regrtest -e -m regrtest_memo.txt
```

### Build an installer using `ant`

If you want to install a snapshot build of Jython, use the command:
```
ant installer
```
This will leave you with a snapshot installer JAR in `dist`,
that you can run with:
```
java -jar jython-installer.jar
```
for the graphical installer, or:
```
java -jar jython-installer.jar --console
```
For the console version. (A `--help` option gives you the full story.)

### Build a JAR using Gradle

Experimentally, we have a Gradle build that results in a family of JARs,
and a POM.
This is intended to provide the Jython 3 core in a form that Gradle and Maven
users can consume as a dependency.
Invoke this with:
```
PS> .\gradlew publish
```
and a JAR and POM are delivered to ` .build2\repo` 

Whereas the JARs delivered by the installer are somewhat "fat",
embedding certain dependencies in shaded (renamed) form,
the JAR from the Gradle build is "spare"
and cites its dependencies externally through a POM.
The project would like to know if this is being done suitably
for downstream use.
