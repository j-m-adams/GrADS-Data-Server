#! /bin/sh
#
# Script to restart server and remove temporary files
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

# Write output to screen and to logfile
echolog () {
    echo $1
    echo `date`": $1" >> $ANAGRAM_CONSOLE
}

# set up standard environment variables
GDS_ENV=`dirname $0`/gds-env.sh ; . $GDS_ENV

$ANAGRAM_BIN/gds-stop.sh

echolog "Deleting contents of $ANAGRAM_TEMP ..."
rm -rf $ANAGRAM_TEMP/* >/dev/null 2>&1

exec $ANAGRAM_BIN/gds-start.sh
