#!/bin/sh
# Script to start server in Jetty container
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

printenv () {
    echo "ANAGRAM_HOME = $ANAGRAM_HOME"
    echo "  ANAGRAM_BASE = $ANAGRAM_BASE"
    echo "    ANAGRAM_CONFIG = $ANAGRAM_CONFIG"
    echo "    ANAGRAM_CONSOLE = $ANAGRAM_CONSOLE"
    echo "    ANAGRAM_TEMP = $ANAGRAM_TEMP"    
    echo "  JETTY_HOME = $JETTY_HOME"
    echo "    JETTY_CONFIG = $JETTY_CONFIG"
    echo "  JETTY_PORT = $JETTY_PORT"
    echo "  JETTY_STOP_PORT = $JETTY_STOP_PORT"
}

JETTY_ARG=$1

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

# CP="-cp $CATALINA_HOME/bin/bootstrap.jar"

PROPS="-Djava.io.tmpdir=$ANAGRAM_TEMP \
-Danagram.home=$ANAGRAM_HOME \
-Danagram.base=$ANAGRAM_BASE \
-Danagram.config=$ANAGRAM_CONFIG \
-Djetty.home=$JETTY_HOME \
-Djetty.port=$JETTY_PORT \
-DSTOP.PORT=$JETTY_STOP_PORT \
"

OPTS="$CP $PROPS"

RUNNING=$ANAGRAM_TEMP/tomcat.pid

if [ "$JETTY_ARG" = "start" ] ; then
    echo $$ >$RUNNING
    echo `date`": New PID for Tomcat: $$"
    OPTS="$JAVA_OPTS $OPTS"
fi


CMD="$JAVA $OPTS -jar ${JETTY_ARG}.jar $JETTY_CONFIG"
echo "CMD = $CMD"

cd $JETTY_HOME
exec $CMD
