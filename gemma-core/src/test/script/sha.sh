#!/bin/sh
# Generate a SHA encoded string representing the argument.
CLASSPATH="$CATALINA_HOME"/server/lib/catalina.jar
CLASSPATH="$CLASSPATH"\;"$CATALINA_HOME"/bin/jmx.jar
CLASSPATH="$CLASSPATH"\;"$CATALINA_HOME"/bin/commons-logging-api.jar
echo ${CLASSPATH}
java -classpath ${CLASSPATH} org.apache.catalina.realm.RealmBase -a SHA $1


