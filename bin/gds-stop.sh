#! /bin/sh
# Script to shut down server
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

# Write output to screen and to logfile
echolog () {
    echo $1
    echo `date`": $1" >> $ANAGRAM_CONSOLE
}

# Delete files
rmf () {
    rm -f "$@" >/dev/null 2>&1
}

# set up standard environment variables
GDS_ENV=`dirname $0`/gds-env.sh ; . $GDS_ENV

STOP_STAMP=$ANAGRAM_TEMP/stopstamp.$$
RESPAWNS="$ANAGRAM_TEMP/respawn.*"

# Cleanup temp file on exit
trap 'rmf $STOP_STAMP' 0
# Trigger cleanup if shell is aborted
trap 'exit 2' 1 2 3 15 

# Shut down Tomcat
echolog "GrADS Data Server shutdown invoked..." 

touch $STOP_STAMP
if ( $ANAGRAM_BIN/tomcat-wrapper.sh stop 2>&1 >>$ANAGRAM_CONSOLE ) ; then
    echolog "Waiting for shutdown to complete..." 
    TIMER=0; TIMEOUT=300
    while [ -f $STOP_STAMP ] ; do
	sleep 1
	TIMER=`expr $TIMER + 1`
	if [ $TIMER -gt $TIMEOUT ] ; then
	    echolog "No shutdown notification after $TIMEOUT secs. gds-respawn.sh may have been killed, or server may have hung on shutdown."
	    exit 1
	fi
    done
    echolog "GrADS Data Server has shut down."
    rmf $RESPAWNS # get rid of any leftovers
    exit 0
else 
    echolog "Shutdown failed. Tomcat is either not running, hung, or still starting up."
    echo "See $ANAGRAM_CONSOLE for more details."
    exit 1
fi
    

