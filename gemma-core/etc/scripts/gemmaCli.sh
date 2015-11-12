# Driver script for the Gemma CLI.
# $Id$

# You must define $GEMMA_LIB in your env or here.
# GEMMA_LIB=~/gemma-lib

JARS=$(echo ${GEMMA_LIB}/* | tr ' ' ':') 

APPARGS=$@

JAVACMD="${JAVA_HOME}/bin/java $JAVA_OPTS"

CMD="$JAVACMD -classpath ${GEMMA_LIB}:${JARS} ubic.gemma.apps.GemmaCLI $APPARGS"

$CMD
