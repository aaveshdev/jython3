java -Djava.security.manager -Djava.security.policy=nowrite.policy -classpath ../../dist/jython.jar org.python.util.jython -S -Dpython.home=../../dist -Dpython.cachedir.skip=true $@
