# Driver script for the Gemma CLI.
# $Id$

# You must define $GEMMA_LIB in your env or here.
# GEMMA_LIB=~/gemma-lib

JARS=$(echo ${GEMMA_LIB}/* | tr ' ' ':') 

APPARGS=$@

JAVACMD="${JAVA_HOME}/bin/java $JAVA_OPTS"

CMD="$JAVACMD -classpath ${GEMMA_LIB}:${JARS} $APPARGS"
CMD_DEFAULT="$JAVACMD -classpath ${GEMMA_LIB}:${JARS} ubic.gemma.apps.GemmaCLI"

if [ -z "$1" ]
    then
        $CMD_DEFAULT
        exit
fi

$CMD
