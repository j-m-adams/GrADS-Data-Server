* Copyright (C) 2000-2021 by George Mason University.
* Authored by Joe Wielgosz and maintained by Jennifer Adams.
* See file COPYRIGHT for more information.

* Script to read gridded data from a GrADS dataset

function read (args)
'reinit'
testing=1

* Read parameters
output  = subwrd(args, 1)
dataset = subwrd(args, 2)
varname = subwrd(args, 3)
x_start = subwrd(args, 4)
x_end   = subwrd(args, 5)
y_start = subwrd(args, 6)
y_end   = subwrd(args, 7)
z_start = subwrd(args, 8)
z_end   = subwrd(args, 9)
t_start = subwrd(args, 10)
t_end   = subwrd(args, 11)
e_start = subwrd(args, 12)
e_end   = subwrd(args, 13)

xvar = 0; yvar = 0; zvar = 0; tvar = 0; evar = 0; vdim = 0;

if (x_end > x_start); xvar = 1; vdim = vdim + 1; endif
if (y_end > y_start); yvar = 1; vdim = vdim + 1; endif
if (z_end > z_start); zvar = 1; vdim = vdim + 1; endif
if (t_end > t_start); tvar = 1; vdim = vdim + 1; endif
if (e_end > e_start); evar = 1; vdim = vdim + 1; endif


say 'output  = 'output
say 'dataset = 'dataset
say 'varname = 'varname
xvmsg = ''; if (xvar>0); xvmsg = "Varying"; endif
yvmsg = ''; if (yvar>0); yvmsg = "Varying"; endif
zvmsg = ''; if (zvar>0); zvmsg = "Varying"; endif
tvmsg = ''; if (tvar>0); tvmsg = "Varying"; endif
evmsg = ''; if (evar>0); evmsg = "Varying"; endif
say 'X: 'x_start ' -> ' x_end' 'xvmsg 
say 'Y: 'y_start ' -> ' y_end' 'yvmsg
say 'Z: 'z_start ' -> ' z_end' 'zvmsg
say 'T: 't_start ' -> ' t_end' 'tvmsg
say 'E: 'e_start ' -> ' e_end' 'evmsg
say vdim ' Varying dimensions'

* Check that parameters were passed
if ('x'%output = 'x' | 'x'%dataset = 'x' | 'x'%varname = 'x') 
  say "error: invalid arguments to subset.gs"
  'quit'
endif

* Open data file
gopen(dataset,1)

* Determine dimensions of data set
'query file 1'
size = sublin(result, 5)
say size
lon_size = subwrd(size, 3)
lat_size = subwrd(size, 6)
lev_size = subwrd(size, 9)
time_size = subwrd(size, 12)
e_size = subwrd(size, 15)


* Write variable to disk

* If more than 2 dims vary, we must loop thru 
* Z, T, and E to get the desired result.  
* When X or Y are fixed, we want GrADS to produce 
* the X/Z or Y/Z slices and loop thru T and E.  
* In all other cases, we just loop thru Z, T, and E. 

'disable fwrite'
'set gxout fwrite'
'set fwrite -be 'output
'set undef file 1'
'set fwex'

say 'Output from q fwrite:'
'q fwrite'
say result

* If X or Y are fixed dimensions, add grid coordinate to varname
if (xvar=0 | yvar=0) 
  if (xvar=0 & yvar=0)
    varname=varname%'(x='x_start',y='y_start')'
  else
    if (xvar=0)
      varname=varname%'(x='x_start')'
    else 
      varname=varname%'(y='y_start')'
    endif
  endif
endif
say 'varname='varname

* More than 2 dims are varying
if (vdim > 2) 
  if (xvar=0 | yvar=0) 
    'set x 'x_start' 'x_end
    'set y 'y_start' 'y_end
    'set z 'z_start' 'z_end
    e = e_start
    while (e <= e_end)
      'set e 'e
      t = t_start
      while (t <= t_end)
        'set t 't
        'd ' varname
        if (testing); say result; endif
        'set fwex'
        t = t + 1
      endwhile
      e = e + 1
    endwhile
  else 
    'set x 'x_start' 'x_end
    'set y 'y_start' 'y_end
    e = e_start
    while (e <= e_end)
      'set e 'e
      t = t_start
      while (t <= t_end)
        'set t 't
        z = z_start
        while (z <= z_end)
          'set z 'z
          'd ' varname
          if (testing); say result; endif
          'set fwex'
          z = z + 1
        endwhile
        t = t + 1
      endwhile
      e = e + 1
    endwhile
  endif
else
* no dims are varying
  if (vdim = 0)
    'reset'
    'disable fwrite'
    'set gxout fwrite'
    'set fwrite -be 'output
    'set x 'x_start
    'set y 'y_start
    'set z 'z_start
    'set t 't_start
    'set e 'e_start
    'd 'varname
    if (testing); say result; endif
  else 
*   one dim is varying
    'set x 'x_start' 'x_end
    'set y 'y_start' 'y_end
    'set z 'z_start' 'z_end
    'set t 't_start' 't_end
    'set e 'e_start' 'e_end
    'set fwex'
    'd 'varname
    if (testing); say result; endif
  endif
endif

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
