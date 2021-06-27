#! /bin/sh
# Script that automatically restarts server if process dies
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

# test if file1 is newer than file2
newer () {
    # if file1 doesn't exist, it is not newer
    if [ ! -f "$1" ] ; then
	return 1
    fi
    # if file1 exists but not file2, consider file1 newer
    if [ ! -f "$2" ] ; then
	return 0
    fi
    FILE1=`ls -d $1`
    FILE2=`ls -d $2`
    NEWER=`ls -dt $FILE1 $FILE2 | sed -e 1q`
    if [ "$FILE1" = "$NEWER" ] ; then
	return 0
    else
	return 1
    fi
}

# Write output to screen and to logfile
echolog () {
    echo `basename $0`": $1"
    echo `date`" $0: $1" >> $ANAGRAM_CONSOLE
}

# Delete files
rmf () {
    rm -f "$@" >/dev/null 2>&1
}

if [ ! "$1" ] ; then
    echo "No argument detected. (gds-respawn.sh should not be invoked directly)"
    exit 1
fi

# set up standard environment variables
GDS_ENV=`dirname $0`/gds-env.sh ; . $GDS_ENV

SPAWN_STAMP=$ANAGRAM_TEMP/spawnstamp.$$
STOP_STAMPS="$ANAGRAM_TEMP/stopstamp.*"
GDS_START=$ANAGRAM_TEMP/gds/started
GDS_STOP=$ANAGRAM_TEMP/gds/stopped
RUNNING=$ANAGRAM_TEMP/tomcat.pid

RESPAWN=$1

# Ignore HUP signals, so server stays alive after logout
trap '' 1
# Cleanup temp files on exit ($RESPAWN was created by gds-start.sh)
trap 'rmf $SPAWN_STAMP $RESPAWN' 0
# Trigger cleanup if shell is aborted
trap 'exit 2' 2 3 15 

# Respawn Tomcat process until gds-stop.sh is run
PAUSE=5 # number of seconds to wait between respawns
while true ; do

    echolog "Starting GrADS Data Server (Tomcat 4.1)..."
    touch $SPAWN_STAMP
    $ANAGRAM_BIN/tomcat-wrapper.sh start 2>&1 >>$ANAGRAM_CONSOLE
    rmf $RUNNING  # created by tomcat-wrapper.sh
    echolog "Tomcat process exited"

    rmf $STOP_STAMPS

    if newer $GDS_START $SPAWN_STAMP ; then
	if newer $GDS_STOP $SPAWN_STAMP ; then
	    echolog "GrADS Data Server has shut down."
	    exit 0
	else
	    echolog "GrADS Data Server died unexpectedly. Respawning in $PAUSE secs..."
            # In case Tomcat is broken, keep respawn to a sane rate
	    sleep $PAUSE
	fi
    else
	echolog "GrADS Data Server failed to start. Server may be misconfigured, or already running."
	exit 1
    fi
done
