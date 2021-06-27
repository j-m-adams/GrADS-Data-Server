#!/bin/sh
#
# Script to set up GDS environment
#
# Copyright (C) 2000-2021 by George Mason University.
# Authored by Joe Wielgosz and maintained by Jennifer Adams.
# See file COPYRIGHT for more information.

ANAGRAM_BIN=`dirname $0`

if [ ! "$ANAGRAM_HOME" ] ; then # path to GDS code (e.g. scripts)
    ANAGRAM_HOME=`cd $ANAGRAM_BIN/..; pwd`; export ANAGRAM_HOME; 
fi
if [ ! "$CATALINA_HOME" ] ; then # path to Tomcat code
    CATALINA_HOME="$ANAGRAM_HOME/tomcat4"; export CATALINA_HOME; 
fi
if [ ! "$CATALINA_BASE" ] ; then # path to Tomcat webapps
    CATALINA_BASE="$CATALINA_HOME"; export CATALINA_BASE; 
fi
if [ ! "$JETTY_HOME" ] ; then # path to Jetty code
    JETTY_HOME="$ANAGRAM_HOME/jetty4"; export JETTY_HOME; 
fi
if [ ! "$JETTY_CONFIG" ] ; then # path to Jetty XML file
    JETTY_CONFIG="$JETTY_HOME/jetty-gds.xml"; export JETTY_CONFIG; 
fi
if [ ! "$JETTY_PORT" ] ; then # main port for Jetty
    JETTY_PORT="9090"; export JETTY_PORT; 
fi
if [ ! "$JETTY_STOP_PORT" ] ; then # port to shut down Jetty
    JETTY_STOP_PORT="9089"; export JETTY_STOP_PORT; 
fi
if [ ! "$ANAGRAM_BASE" ] ; then # path to GDS instance data (config, temp, log)
    ANAGRAM_BASE="$ANAGRAM_HOME"; export ANAGRAM_BASE; 
fi
if [ ! "$ANAGRAM_CONFIG" ] ; then # path to GDS config file
    ANAGRAM_CONFIG="$ANAGRAM_BASE/gds.xml"; export ANAGRAM_CONFIG ; 
fi
if [ ! "$ANAGRAM_CONSOLE" ] ; then # path to console output
    ANAGRAM_CONSOLE="$ANAGRAM_BASE/log/console.out"; export ANAGRAM_CONSOLE; 
fi
if [ ! "$ANAGRAM_TEMP" ] ; then # path to temp data
    ANAGRAM_TEMP="$ANAGRAM_BASE/temp"; export ANAGRAM_TEMP; 
fi

mkdir -p $ANAGRAM_TEMP `dirname $ANAGRAM_CONSOLE`
