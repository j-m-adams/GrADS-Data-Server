*  Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.

* Script to read station data from a GrADS dataset

function read (args)

* Read parameters
output = subwrd(args, 1)
dataset = subwrd(args, 2)
writeopts = subwrd(args, 3)
useid = subwrd(args, 4)
if (useid = "stid")
  stid = subwrd(args, 5)
  boundsarg = 6
else
  boundsarg = 4
endif

* Read bounds (if present)
usebounds = subwrd(args, boundsarg)
if (usebounds = "bounds")
  lon_start  = subwrd(args, boundsarg+1)
  lon_end    = subwrd(args, boundsarg+2)
  lat_start  = subwrd(args, boundsarg+3)
  lat_end    = subwrd(args, boundsarg+4)
  lev_start  = subwrd(args, boundsarg+5)
  lev_end    = subwrd(args, boundsarg+6)
  time_start = subwrd(args, boundsarg+7)
  time_end   = subwrd(args, boundsarg+8)
  ens_start  = subwrd(args, boundsarg+9)
  ens_end    = subwrd(args, boundsarg+10)
  vararg = boundsarg + 11
else 
* use large values in the hope of getting everything
* until there is some kind of "lev=ALL" function
  lev_start = 10000
  lev_end = 0
  vararg = boundsarg
endif





* Open data file
gopen(dataset,1)

* Set dimension environment
if (usebounds = "bounds")
  'set lon ' lon_start ' ' lon_end
  'set lon ' lon_start ' ' lon_end
  'set lat ' lat_start ' ' lat_end
  'set lat ' lat_start ' ' lat_end
* lev settings are put in the actual display statement
  'set time ' time_start ' ' time_end
  'set time ' time_start ' ' time_end
else 
* Determine dimensions of data set
  'query file 1'
  line = sublin(result, 5)
  tsize = subwrd(line, 3)
  say 'tsize is ' tsize
  'set t 1 ' tsize
endif  

* ignore lat/lon bounds if stid is given
if (useid = "stid")
  'set x 1 1'
  'set y 1 1'
endif

* make sure lev is decreasing
if (lev_start < lev_end) 
  swap = lev_start
  lev_start = lev_end
  lev_end = swap
endif

* build expression for display command, including stid and lev params
varnum = subwrd(args, vararg)
say 'vararg is 'vararg'  varnum is ' varnum
varstring = ""
i = 1
while (i <= varnum)
  varstring = varstring % subwrd(args, vararg + i)
  if (useid = "stid")
    varstring = varstring "(stid=" stid ",lev=" lev_start ",lev=" lev_end ")"
  else 
    varstring = varstring "(lev=" lev_start ",lev=" lev_end ")"
  endif

  if (i != varnum)
    varstring = varstring ";"
  endif
  i = i + 1
endwhile

* Set output options
'set writegds ' writeopts ' ' output

'set gxout writegds'

* Write variable
'q dims'
say result
say 'display ' varstring
'd 'varstring

* Write end-of-sequence marker
'set writegds -f ' output
'set t 1 1'
'd 'varstring


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
