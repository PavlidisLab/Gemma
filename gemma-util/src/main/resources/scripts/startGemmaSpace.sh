
GS_HOME="/proj/GigaSpacesCommunity5.2"
GEMMA_LIB="/spacef/grp/lib/Gemma"

$GS_HOME/bin/gsInstance.sh "/./remotingSpace?schema=default" "${GEMMA_LIB}/gemma-core-1.0-SNAPSHOT.jar:${GEMMA_LIB}/gemma-mda-1.0-SNAPSHOT.jar:${GEMMA_LIB}/gemma-util-1.0-SNAPSHOT.jar"

