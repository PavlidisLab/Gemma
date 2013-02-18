# Example unix shell script to start workers
# $Id$

# This script uses the following environment variables, which you must define here or in your profile.
# JSLIBDIR="/space/gemmaData/lib"
GIGASPACEJAR=/usr/local/tomcat/GigaSpacesCommunity5.2/lib/JSpaces.jar

JARS=$(echo ${GEMMA_LIB}/* | tr ' ' ':')

# monitorWorker, etc.
WORKERS=$@

JAVACMD="${JAVA_HOME}/bin/java $JAVA_OPTS"


CMD="$JAVACMD $JAVA_OPTS -classpath $GIGASPACEJAR:${GEMMA_LIB}:${JARS} ubic.gemma.job.executor.worker.WorkerCLI -workers  $WORKERS"
$CMD



