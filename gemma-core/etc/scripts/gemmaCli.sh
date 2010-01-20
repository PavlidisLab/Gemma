# Driver script for the Gemma CLI.
# $Id$

# You must define $GEMMA_LIB in your env or here.
GEMMA_LIB=/cygdrive/c/Temp/gemma.tmp/lib
JARS=$(echo ${GEMMA_LIB}/* | tr ' ' ':') 

APPARGS=$@

JAVACMD="${JAVA_HOME}/bin/java $JAVA_OPTS"

CMD="$JAVACMD -classpath ${JARS} $APPARGS"
$CMD
