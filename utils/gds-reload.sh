#! /bin/sh
# gds-reload.sh: sends an administrative command to the server. 


#########################

# Customize these parameters if needed. 
#
# Note: if using a utility other than curl, the script must be edited to
# use that utility's syntax for sending POST data. See example below
# for 'lynx'.
# 
# Data can also be sent using GET method if necessary; however this will
# cause the auth string to appear in the GDS log files.

GETURL="curl -s"
MAXTRIES=3
WAIT=30
CMD=reload
SERVER=localhost:9090

#########################

if [ ! "$1" ] ; then
    echo "Using default server name: $SERVER"
else
    SERVER=$1
fi


# Find home directory
if [ ! "$ANAGRAM_HOME" ] ; then
   ANAGRAM_UTIL=`dirname $0`; export ANAGRAM_UTIL
   ANAGRAM_HOME=`cd $ANAGRAM_UTIL/..; pwd`; export ANAGRAM_HOME
fi 

if [ ! "$ANAGRAM_CONFIG" ] ; then
    ANAGRAM_CONFIG=gds.xml
fi

URL="http://$SERVER/dods/admin"

AUTHSTR=`grep "auth=" $ANAGRAM_HOME/$ANAGRAM_CONFIG`
AUTH=`expr "$AUTHSTR" : '.*auth="\([^"]*\)"'`
POSTDATA="cmd=$CMD&auth=$AUTH"

# The following line must be edited for different URL utilities:

# for curl:
URLCMD="$GETURL -d $POSTDATA $URL"

# for lynx:
#URLCMD="eval echo '$POSTDATA' | $GETURL -post_data $URL"

TRIES=0
while [ $TRIES -lt $MAXTRIES ] ; do 
    RESULT=`$URLCMD | grep -c successfully`
    if [ "$RESULT" -eq 1 ] ; then
	echo "GDS $CMD successful."
	exit 0;
    fi
    echo "GDS $CMD failed. Retrying in $WAIT secs..."
    TRIES=`expr $TRIES + 1`
    sleep $WAIT
done

echo "Failed after $MAXTRIES tries. Rebooting server..."
$ANAGRAM_HOME/bin/gds-stop.sh
$ANAGRAM_HOME/bin/gds-start.sh &

