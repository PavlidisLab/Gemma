# Driver script for the Gemma CLI.
# $Id$

# This script uses the following environment variables, which you must define here or in your profile.
# GEMMA_LIB="/gemmaData/lib/Gemma"

MAIN_CLASS=$3 
APPARGS=$4


JAVACMD="${JAVA_HOME}/bin/java"

$JAVACMD $JAVA_OPTS \
-classpath "${GEMMA_LIB}/*" $MAIN_CLASS \
$APPARGS
