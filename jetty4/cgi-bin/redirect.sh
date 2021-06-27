#!/bin/sh
# Test redirecting from CGI
# $Id: redirect.sh,v 1.1 2004/10/04 18:48:14 joew Exp $
echo "Status: 302 Moved"
echo "Location: http://${HTTP_HOST}/"
echo
