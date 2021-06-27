* Copyright (C) 2000-2021 by George Mason University.
* Authored by Joe Wielgosz and maintained by Jennifer Adams.
* See file COPYRIGHT for more information.

* Prints  a  listing  of  metadata  attributes  for  a  given  dataset

function read (args)

* Read parameters
outfile = subwrd(args, 1)
dataset = subwrd(args, 2)

* Open data file
gopen(dataset,1)

* check if Esize > 1
'q file'
sizes = sublin(result,5)
esize = subwrd(sizes,15)
if (esize > 1)
* write out ensemble attributes
  rc = write(outfile, "Ensemble Attributes:")
  'q ens_name'
  line = sublin(result,1)
  rc = write(outfile, line, append)
  'q ens_length'
  line = sublin(result,1)
  rc = write(outfile, line, append)
  'q ens_tinit'
  line = sublin(result,1)
  rc = write(outfile, line, append)
  rc = write(outfile, "", append)
endif

* write out descriptor and native attributes
'q attr'
rc = write(outfile, result, append)

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

