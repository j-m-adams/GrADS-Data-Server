#! /bin/sh
# Script to start server
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

# test if file1 is newer than file2
# return 0 if file1 is newer
# return 1 if file2 is newer
newer () {
    # file 2 is newer if file1 doesn't exist
    if [ ! -f "$1" ] ; then
	return 1
    fi
    # file 1 is newer if file1 exists but not file2
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
    echo $1
    echo `date`": $1" >> $ANAGRAM_CONSOLE
}

# Delete files
rmf () {
    rm -f "$@" >/dev/null 2>&1
}

# set up standard environment variables
GDS_ENV=`dirname $0`/gds-env.sh ; . $GDS_ENV

START_STAMP=$ANAGRAM_TEMP/startstamp.$$
GDS_START=$ANAGRAM_TEMP/gds/started
GDS_FAIL=$ANAGRAM_TEMP/gds/failed
RESPAWN=$ANAGRAM_TEMP/respawn.$$

echolog "GrADS Data Server startup invoked..." 

cat <<EOF

Note: A process called gds-respawn.sh is being created, to
      automatically respawn GDS if it shuts down abnormally.

Set JAVA, JAVA_HOME, JAVA_OPTS to control JVM options.
See $ANAGRAM_CONSOLE for detailed startup/shutdown messages.

EOF

if [ ! "$GADDIR" ] ; then
    echo "Warning: GADDIR not set. GrADS netCDF/HDF interface may not function properly."
    echo ""
else
    if [ ! -f "$GADDIR/udunits.dat" ] ; then
	echo "Warning: udunits.dat not found in GADDIR ($GADDIR)."
	echo "GrADS netCDF/HDF interface may not function properly."
	echo ""
    fi
fi

# Cleanup temp file on exit
trap 'rmf $START_STAMP' 0
# Trigger cleanup if shell is aborted
trap 'exit 2' 1 2 3 15 

touch $RESPAWN $START_STAMP
$ANAGRAM_BIN/gds-respawn.sh $RESPAWN &
while newer $START_STAMP $GDS_START ; do 
    if newer $GDS_FAIL $START_STAMP ; then
	echolog "GDS servlet couldn't load. See $STDOUT_LOG for details. "
	echolog "Shutting down Tomcat..."
	$ANAGRAM_BIN/gds-stop.sh
	exit 1
    fi
    if [ ! -f $RESPAWN ] ; then
	exit 1
    else 
	sleep 1
    fi
done
echo "GrADS Data Server started."
exit 0
