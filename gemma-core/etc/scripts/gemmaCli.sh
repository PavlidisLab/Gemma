# Driver script for the Gemma CLI.
# $Id$

# This script uses the following environment variables, which you must define here or in your profile.
# GEMMA_LIB="/gemmaData/lib/Gemma"


APPARGS=$@


JAVACMD="${JAVA_HOME}/bin/java"

 
CMD="$JAVACMD $JAVA_OPTS -classpath ${GEMMA_LIB}/gemma-core-1.5-SNAPSHOT.jar $APPARGS"
echo $CMD
$CMD