rem ---------------------------------------------------------------------------
rem Append to CLASSPATH
rem
rem $Id: cpappend.bat,v 1.1 2003/03/30 00:32:13 joew Exp $
rem ---------------------------------------------------------------------------

rem Process the first argument
if ""%1"" == """" goto end
set CLASSPATH=%CLASSPATH%;%1
shift

rem Process the remaining arguments
:setArgs
if ""%1"" == """" goto doneSetArgs
set CLASSPATH=%CLASSPATH% %1
shift
goto setArgs
:doneSetArgs
:end
