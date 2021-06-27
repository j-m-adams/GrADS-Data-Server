#!/bin/sh

###############################################################
# gds_check
#
# A script that checks whether a GDS is responding to requests,
# and if not, automatically reboots it and notifies the 
# administrator(s).
#
# If set as a cron job, this script can be temporarily disabled by 
# creating a file with the name "block_gds_check" in the home 
# directory of the GDS.

# Written by Joe Wielgosz and Don Hooper
#
###############################################################

# Uses the following system commands:
GET_URL="lynx -source"
MAIL="mail"
RM="rm -f"
ECHO="echo"
CMP="cmp"

# Default settings for filenames
TMP_FILE=gds_check_page
SKIP_FILE=block_gds_check

#  not currently used
#  GDS_CORRECT_OUTPUT=

GDS_DIR=$1
GDS_URL=$2
ADMIN_EMAIL=$3

if [ ! "$3" ] ; then
	$ECHO usage: check_gds dir url \"email1 email2 ... \"
	$ECHO "dir = home dir of the GDS"
	$ECHO "url = url for the GDS"
	$ECHO "email1 email2 ... = people to notify when the GDS is down"
	exit
fi

gds_down() {

        $RM gds_check_page
        $ECHO `date`: GDS is down! restarting it..

        $MAIL -s "GDS restarted automatically" \
            $ADMIN_EMAIL \
            <<-END_OF_MSG
        Checked $GDS_URL at `date` and got no response.
        Restarting server in $GDS_DIR.
END_OF_MSG

        ./rebootserver
	exit;
}

gds_up() {
        $RM gds_check_page
        $ECHO `date`: GDS is running
	exit;
}

gds_skip() {
        $ECHO GDS check skipped
        exit
}

cd $GDS_DIR

if [ -f $SKIP_FILE ] ; then
    gds_skip
fi

$GET_URL $GDS_URL > $TMP_FILE
STATUS=$?  # save exit status for evaluation

if [ -s $TMP_FILE ] ; then  # we have a non-zero length output to check
    if [ $STATUS -eq 0 ] ; then  # lynx terminated OK; guess all is well
	gds_up
    else  # lynx terminated abnormally; GDS is in trouble, Buckaroo! ;)
	gds_down
    fi

#
# alternative strategy - if lynx doesn't exit non-zero when page fails to load
# compare output with output from when GDS is known to be operational
#
# $CMP $TMP_FILE $GDS_CORRECT_OUTPUT
# STATUS=$?
# if [ $STATUS -eq 0 ] ; then
#     gds_up
# else
#     gds_down
# fi

else # if there was a non-zero length output from lynx above; otherwise below
	gds_down
fi

