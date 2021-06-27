* Copyright (C) 2000-2021 by George Mason University.
* Authored by Joe Wielgosz and maintained by Jennifer Adams.
* See file COPYRIGHT for more information.

* Script to generate a dummy CTL for a NetCDF or HDF dataset

function read (args)

output = subwrd(args, 1)
dataset = subwrd(args, 2)

gopen(dataset,1)

'q ctlinfo'

if (rc!=0)
  say "error: dummy CTL feature not supported by GrADS executable"
  'quit'
endif

rc = write(output, result)

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

