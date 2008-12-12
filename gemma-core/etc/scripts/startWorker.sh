# Example unix shell script to start workers
# $Id$

# This script uses the following environment variables, which you must define here or in your profile.
# GEMMA_LIB="/gemmaData/lib/Gemma"

GEMMA_USER=administrator
GEMMA_PWD=$1
WORKER_CLASS=$2
VMARGS=$3
APPARGS=$4

CP=`cat ${GEMMA_LIB}/CLASSPATH`

JAVACMD="${JAVA_HOME}/bin/java"

#Not necessary.  Also means every worker needs an installation of gigaspaces avaliable for it. 
#. ${JSHOMEDIR}/bin/setenv.sh
#JARS="${JSHOMEDIR}${CPS}${JSHOMEDIR}/lib/JSpaces.jar${CPS}${COMMON_JARS}${CPS}$CP"; export JARS

$JAVACMD $VMARGS -Dcom.gs.home=$GEMMA_LIB -Dehcache.disk.store.dir=$HOME/scratch \
-classpath "${CP}" ubic.gemma.grid.javaspaces.worker.$WORKER_CLASS \
-u $GEMMA_USER -p $GEMMA_PWD -gigaspacesOn $APPARGS


