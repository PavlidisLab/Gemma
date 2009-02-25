# Simple unix shell script to start the JavaSpaces server. 

# This script uses the following environment variables

# JSHOMEDIR must be defined in your profile, as the Gigaspaces startup script uses it.
# JSHOMEDIR="/proj/GigaSpacesCommunity5.2"

# GEMMA_LIB is only used here, so it can be defined here if you prefer not to put it in your profile.
# GEMMA_LIB="/spacef/grp/lib/Gemma"

nohup $JSHOMEDIR/bin/gsInstance.sh "/./remotingSpace?fifo&schema=default" `cat ${GEMMA_LIB}/CLASSPATH` > $JSHOMEDIR/logs/gigaspacesConsole.log &

