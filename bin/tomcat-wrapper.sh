#!/bin/sh
# Script to start server in Tomcat container
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

printenv () {
    echo "ANAGRAM_HOME = $ANAGRAM_HOME"
    echo "  CATALINA_HOME_ = $CATALINA_HOME"
    echo "    CATALINA_BASE = $CATALINA_BASE"
    echo "  ANAGRAM_BASE = $ANAGRAM_BASE"
    echo "    ANAGRAM_CONFIG = $ANAGRAM_CONFIG"
    echo "    ANAGRAM_CONSOLE = $ANAGRAM_CONSOLE"
    echo "    ANAGRAM_TEMP = $ANAGRAM_TEMP"
}

TOMCAT_ARG=$1

# set up standard environment variables
GDS_ENV=`dirname $0`/gds-env.sh ; . $GDS_ENV

printenv

if [ ! "$JAVA" ] ; then
    if [ "$JAVA_HOME" ] ; then
	JAVA=$JAVA_HOME/bin/java
    else 
       JAVA=java
    fi
fi

if [ ! "$JAVA_OPTS" ] ; then
    JAVA_OPTS="-server"
fi

CP="-cp $CATALINA_HOME/bin/bootstrap.jar"

PROPS="-Djava.io.tmpdir=$ANAGRAM_TEMP \
-Danagram.home=$ANAGRAM_HOME \
-Danagram.base=$ANAGRAM_BASE \
-Danagram.config=$ANAGRAM_CONFIG \
-Dcatalina.home=$CATALINA_HOME \
-Dcatalina.base=$CATALINA_BASE"
# note that $ANAGRAM_LOG does not directly control the gds logfile, it is for 


OPTS="$CP $PROPS"

RUNNING=$ANAGRAM_TEMP/tomcat.pid

if [ "$TOMCAT_ARG" = "start" ] ; then
    echo $$ >$RUNNING
    echo `date`": New PID for Tomcat: $$"
    OPTS="$JAVA_OPTS $OPTS"
fi

CMD="$JAVA $OPTS org.apache.catalina.startup.Bootstrap $TOMCAT_ARG"
echo "CMD = $CMD"

exec $CMD
