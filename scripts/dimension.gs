* Copyright (C) 2000-2021 by George Mason University.
* Authored by Joe Wielgosz and maintained by Jennifer Adams.
* See file COPYRIGHT for more information.

* Generates a list of points to be read into a dods array

function read (args)

* Read parameters
outfile = subwrd(args, 1)
dataset = subwrd(args, 2)
varname = subwrd(args, 3)
start   = subwrd(args, 4)
stop    = subwrd(args, 5)

* Open data file
gopen(dataset,1)

* Make sure all dimensions are fixed
'set x 1'
'set y 1'
'set z 1'
'set t 1'
'set e 1'

* Determine dimensions of data set
'query file 1'
size = sublin(result, 5)
*say size
if (varname = 'lon')
  varsize = subwrd(size, 3)
  shortname = 'x';
endif

if (varname = 'lat')
  varsize = subwrd(size, 6)
  shortname = 'y';
endif

if (varname = 'lev')
  varsize = subwrd(size, 9)
  shortname = 'z';
endif

if (varname = 'time')
  varsize = subwrd(size, 12)
  shortname = 't';
endif

if (varname = 'ens')
  varsize = subwrd(size, 15)
  shortname = 'e';
endif

if (shortname = 'shortname')
  say 'error: invalid variable ' varname
  'quit'
endif
say shortname' start='start' stop='stop

i = start
while (i <= stop)
  'set ' shortname ' ' i
  if (shortname = 'e')
    value = subwrd(result, 4)
  else 
    if (shortname = 't')
      'q time'
      value = subwrd(result, 3)
    else 
      'set prnopts %16.11f 1'
      'set gxout print'
      'd 'varname
      line1 = sublin(result,1)
      line2 = sublin(result,2)
      line3 = sublin(result,3)
      word = subwrd(line1,1)
      if (word="Notice:")
        value = subwrd(line3, 1)
      else 
        if (word="Printing")
          value = subwrd(line2, 1)
        else
          say 'error: invalid print output for 'shortname' = 'i
          'quit'
        endif
      endif
    endif
  endif
  if (i=start)
*   clobber the file 
    rc = write(outfile, value)
 else
*   append to the file 
    rc = write(outfile, value, append)
  endif
  i = i + 1
endwhile
rc = close(outfile)
'quit'
* end of script


function gopen(dataset,fnum)
'sdfopen 'dataset
'q file 'fnum; l1=sublin(result,1); w1=subwrd(l1,1)
if (w1="File"); say 'File 'fnum' opened with sdfopen'; return; endif

'xdfopen 'dataset
'q file 'fnum; l1=sublin(result,1); w1=subwrd(l1,1)
if (w1="File"); say 'File 'fnum' opened with xdfopen'; return; endif

'open 'dataset
'q file 'fnum; l1=sublin(result,1); w1=subwrd(l1,1)
if (w1="File"); say 'File 'fnum' opened with open'; return; endif
