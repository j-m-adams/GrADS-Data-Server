* Copyright (C) 2000-2021 by George Mason University.
* Authored by Joe Wielgosz and maintained by Jennifer Adams.
* See file COPYRIGHT for more information.

* Script to take an "encapsulated" analysis task and
* output the result along with the descriptor file.
*
*  This script requires a long arg list.
*
*  arg1:  output file (with no extension)
*  arg2:  lon1
*  arg3:  lon2
*  arg4:  lat1
*  arg5:  lat2
*  arg6:  lev1
*  arg7:  lev2
*  arg8:  time1
*  arg9:  time2
*  arg10: ens1
*  arg11: ens2
*  arg12: expression
*  arg13: size limit of results
*  arg14:  number of files to open
*  arg15 and beyond:  file names  
*
*  Error handling is mediocre at this point.
*
*  existing files with the names of the output files will be 
*  overwritten
*

function task(args)

* Parse all the arguments
oname = subwrd(args,1)
_octl = oname'.ctl'
_odat = oname'.dat'
_dim1.0 = subwrd(args,2)
_dim2.0 = subwrd(args,3)
_dim1.1 = subwrd(args,4)
_dim2.1 = subwrd(args,5)
_dim1.2 = subwrd(args,6)
_dim2.2 = subwrd(args,7)
_dim1.3 = subwrd(args,8)
_dim2.3 = subwrd(args,9)
_dim1.4 = subwrd(args,10)
_dim2.4 = subwrd(args,11)
_expr   = subwrd(args,12)
sizelimit = subwrd(args,13)
dnum = subwrd(args,14)

* Load the data file name(s) into script variable _dset
i = 1
while (i<=dnum)
  _dset.i = subwrd(args,14+i)
  i = i + 1
endwhile

* Open the file(s) and calculate the size of the result
i = 1
while (i<=dnum) 
  gopen(_dset.i, i)
  if (rc!=0) 
    say "error: can't open dataset "_dset.i
    'quit'
  endif
  i = i + 1
endwhile

* Get varying dimensions and size of result based on default file 1
'set dfile 1'
setdims(_dim1.0, _dim2.0, _dim1.1, _dim2.1, _dim1.2, _dim2.2, _dim1.3, _dim2.3, _dim1.4, _dim2.4);
result_size = size()
* If result is too big, return error message
if (sizelimit > 0) 
  if (result_size > sizelimit) 
    say 'error: result size exceeded ' sizelimit ' byte limit'
    'quit'
  endif 
endif

* Set gxout to be fwrite, set stat on, and set up looping environment 
'set fwrite -be '_odat
'set gxout fwrite'
'set stat on'
'set warn off'
ee = _e1
while (ee <= _e2)
  'set e 'ee
  tt = _t1
  while (tt <= _t2)
    'set t 'tt
    zz = _z1
    while (zz <= _z2)
      'set z 'zz
*      now display the expression
      'd '_expr
      if (rc!=0) 
        say "error: Invalid expression -- display failed"
        'quit'
      endif
      dres = result
      zz = zz + 1
    endwhile
    tt = tt + 1
  endwhile
  ee = ee + 1
endwhile
'disable fwrite'

say 'Statistical output from display of expression:'
say dres


* Get the varying dimensions of the 2D display 
* If three dimensions are varying, the loopdim is assumed to be the default: time
i = 1
while (i<50) 
  rec = sublin(dres,i)
  if (subwrd(rec,1)='Dimensions')  ;* grabs the crucial line from the stat output
    break
  endif
  i = i + 1
endwhile
if (i=50) 
  say "error: More than 50 lines of statistical output from display of expression"
  'quit'
endif

_vdim1 = subwrd(rec,3)  ;* gets the 1st varying dimension of the 2D display
_vdim2 = subwrd(rec,4)  ;* gets the 2nd varying dimension of the 2D display
_rec1 = sublin(dres,i+1) ;* gets the I Dimension output from stats
_rec2 = sublin(dres,i+2) ;* gets the J DImension output from stats
urec  = sublin(dres,i+4) ;* gets the Undef value
undef = subwrd(urec,4)  

if (_vdim1=-1 & _vdim2=-1)
* make sure X and Y are fixed
  if ((_xvaries) | (_yvaries))
    say "error: logic error 1 in expression.gs"
    'quit'
  endif
else
  if (_vdim2 = -1) 
    if (_vdim1 = 0)
*     make sure X is varying and Y is fixed
      if (_xvaries=1 & _yvaries=0)
        _xrec = _rec1
      else
        say "error: logic error 2 in expression.gs"
        'quit'
      endif
    else
*     make sure X is fixed and Y is varying
      if (_xvaries=0 & _yvaries=1)
        _yrec = _rec1
      else
        say "error: logic error 3 in expression.gs"
        'quit'
      endif
    endif
  else
*   make sure both X and Y are varying
    if (_xvaries=1 & _yvaries=1)
      _xrec = _rec1
      _yrec = _rec2
    else
      say "error: logic error 4 in expression.gs"
      'quit'
    endif
  endif
endif


* Write out the descriptor file 
_znum = 0
rc = write (_octl,'dset '_odat)
rc = write (_octl,'title Result of Expression: '_expr)
rc = write (_octl,'options big_endian')
rc = write (_octl,'undef 'undef)
if (_xvaries=1); xdef(); else; def = 'xdef 1 linear '_dim1.0' 1  '; rc = write(_octl,def); endif
if (_yvaries=1); ydef(); else; def = 'ydef 1 linear '_dim1.1' 1  '; rc = write(_octl,def); endif
if (_zvaries=1); zdef(); else; def = 'zdef 1 linear '_dim1.2' 1  '; rc = write(_octl,def); endif
if (_tvaries=1); tdef(); else; def = 'tdef 1 linear '_dim1.3' 1hr'; rc = write(_octl,def); endif
if (_evaries=1); edef(); endif
rc = write (_octl,'vars 1')
rc = write (_octl,'result '_zsize' 99 Result of Expression')
rc = write (_octl,'endvars')

say "GrADS server-side analysis expression evaluation was a success!"
'quit'


* * * END OF MAIN SCRIPT * * * 


* Write an X-varying grid definition record
function xdef()
typ = subwrd(_xrec,7)
if (typ = '') 
  say "error: Can't write XDEF descriptor file entry"
  'quit'
endif
if (typ = "Levels") 
  xdef = 'xdef '_xsize' levels '              ;* set up the levels axis definition
  rc = write (_octl,xdef)                     ;* write it out
  i = 0
  j = 0
  orec = ''
  while (i<_xsize)                            ;* write out each level
    orec = orec%' '%subwrd(_xrec,i+8)
    i = i + 1
    j = j + 1
    if (j>9)  
      rc = write(_octl,orec)
      j = 0
      orec=''
    endif
  endwhile
  if (j>0)
    rc = write(_octl,orec)
  endif
else                                           ;* write out the linear axis defnintion
  xdef = 'xdef '_xsize' linear 'subwrd(_xrec,8)' 'subwrd(_xrec,9)
  rc = write(_octl,xdef)
endif

* Write a Y-varying grid definition record
function ydef()
typ = subwrd(_yrec,7)
if (typ = '') 
  say "error: Can't write YDEF descriptor file entry"
  'quit'
endif
if (typ = "Levels") 
  ydef = 'ydef '_ysize' levels '              ;* set up the levels axis definition
  rc = write (_octl,ydef)                     ;* write it out
  i = 0
  j = 0
  orec = ''
  while (i<_ysize)                            ;* write out each level
    orec = orec%' '%subwrd(_yrec,i+8)
    i = i + 1
    j = j + 1
    if (j>9)  
      rc = write(_octl,orec)
      j = 0
      orec=''
    endif
  endwhile
  if (j>0)
    rc = write(_octl,orec)
  endif
else                                           ;* write out the linear axis defnintion
  ydef = 'ydef '_ysize' linear 'subwrd(_yrec,8)' 'subwrd(_yrec,9)
  rc = write(_octl,ydef)
endif

* Write a Z-varying grid definition record
function zdef()
zdef = 'zdef '_zsize' levels '                 ;* set up the levels axis definition
rc = write (_octl,zdef)                        ;* write it out
levels=''
zz = _z1
while (zz <= _z2)                              ;* write out each level
  'set z 'zz
  lev = subwrd(result,4)
  levels = levels%lev%' '
  zz = zz + 1
endwhile
rc = write(_octl,levels)

* Write a T-varying grid definition record
function tdef()
'q ctlinfo'
_ctl = result
oldtdef = getctl(tdef)
tincr = subwrd(oldtdef,5)
newtdef = 'tdef '_tsize' linear '_dim1.3' 'tincr
rc = write(_octl,newtdef)  

* Write an E-varying grid definition record
function edef()
edef = 'edef '_esize' names '
rc = write(_octl,edef)  
names='' 
ee = _e1
while (ee <= _e2)
  names = names%ee%' '
  ee = ee + 1
endwhile
rc = write(_octl,names)


* Set the dimension environment
function setdims(minlon, maxlon, minlat, maxlat, minlev, maxlev, mintime, maxtime, minens, maxens)
'set lon 'minlon' 'maxlon
if (rc!=0) 
  say "error: invalid dimension environment -- longitude"
  'quit'
endif
'set lat 'minlat' 'maxlat
if (rc!=0) 
  say "error: invalid dimension environment -- latitude"
  'quit'
endif
'set lev 'minlev' 'maxlev
if (rc!=0) 
  say "error: invalid dimension environment -- level"
  'quit'
endif
'set time 'mintime' 'maxtime
if (rc!=0) 
  say "error: invalid dimension environment -- time"
  'quit'
endif
'set ens 'minens' 'maxens
if (rc!=0) 
  say "error: invalid dimension environment -- ensemble"
  'quit'
endif


* Calculate the size for the current dataset
function size()
'q dims'
bounds = result
say bounds
line = sublin(bounds,2)
if (subwrd(line,3)='varying')
  _xvaries = 1
  _x1 = subwrd(line,11)
  _x2 = subwrd(line,13)
else
  _xvaries = 0
  _x1 = subwrd(line,9)
  _x2 = subwrd(line,9)
endif
line = sublin(bounds,3)
if (subwrd(line,3)='varying')
  _yvaries = 1
  _y1 = subwrd(line,11)
  _y2 = subwrd(line,13)
else 
  _yvaries = 0
  _y1 = subwrd(line,9)
  _y2 = subwrd(line,9)
endif
line = sublin(bounds,4)
if (subwrd(line,3)='varying')
  _zvaries = 1
  _z1 = subwrd(line,11)
  _z2 = subwrd(line,13)
else 
  _zvaries = 0
  _z1 = subwrd(line,9)
  _z2 = subwrd(line,9)
endif
line = sublin(bounds,5)
if (subwrd(line,3)='varying')
  _tvaries = 1
  _t1 = subwrd(line,11)
  _t2 = subwrd(line,13)
else 
  _tvaries = 0
  _t1 = subwrd(line,9)
  _t2 = subwrd(line,9)
endif
line = sublin(bounds,6)
if (subwrd(line,3)='varying')
  _evaries = 1
  _e1 = subwrd(line,11)
  _e2 = subwrd(line,13)
else 
  _evaries = 0
  _e1 = subwrd(line,9)
  _e2 = subwrd(line,9)
  say _e1' '_e2
endif

_xsize = _x2 - _x1 + 1
_ysize = _y2 - _y1 + 1
_zsize = _z2 - _z1 + 1
_tsize = _t2 - _t1 + 1
_esize = _e2 - _e1 + 1
say  'result size = '_xsize' * '_ysize' * '_zsize' * '_tsize' * '_esize
resultsize = 4 * _xsize * _ysize * _zsize * _tsize * _esize

return resultsize

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

function getctl(handle)
line = 1
found = 0
while (!found)
  info = sublin(_ctl,line)
  if (subwrd(info,1)=handle)
    _handle = info
    found = 1
  endif
  line = line + 1
endwhile
return _handle
