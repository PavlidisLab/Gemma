# Example unix shell script to start workers
# $Id$

# TODO: there's a way to specify this with wildcards. (i.e. without tr stuff)
JARS=$(echo ${GEMMA_LIB}/* | tr ' ' ':')

# Path to local configuration files. log4j.properties for example.
CONFIGS=~/configs

JAVACMD="${JAVA_HOME}/bin/java $JAVA_OPTS"

CMD="$JAVACMD -classpath ${CONFIGS}:${JARS} ubic.gemma.job.executor.worker.WorkerCLI"
$CMD



