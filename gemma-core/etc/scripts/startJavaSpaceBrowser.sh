# Simple unix shell script to start the JavaSpaces browser (to monitor the space). 

# This script uses the following environment variables

# JSHOMEDIR must be defined in your profile, as the Gigaspaces startup script uses it.
# JSHOMEDIR="/proj/GigaSpacesCommunity5.2"

# GEMMA_LIB is only used here, so it can be defined here if you prefer not to put it in your profile.
# GEMMA_LIB="/spacef/grp/lib/Gemma"

nohup $JSHOMEDIR/bin/SpaceBrowser.sh "/./remotingSpace?schema=default" `cat ${GEMMA_LIB}/CLASSPATH` > $JSHOMEDIR/logs/gigaspacesBrowserConsole.log &

