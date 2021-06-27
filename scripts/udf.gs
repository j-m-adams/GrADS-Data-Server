* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.

* Script to unpack a UDF-encoded dataset

function read (args)

ctl_out = subwrd(args, 1)
bin_out = subwrd(args, 2)
udf_in = subwrd(args, 3)

* TBD open udf using new grads mods
* ??? new function call

* write CTL file
'q ctlinfo'

if (rc!=0)
  say "error: dummy CTL feature not supported by GrADS executable"
  'quit'
endif

rc = write(ctl_out, result)


* TBD write contents to binary file
* ??? something using fwrite -be


'quit'

