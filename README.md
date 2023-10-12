# systolic_array
 A Chisel3 systolic array implementation

## Run RTL generation and unit-tests
```
mill rtl
```
Expected output
```
[35/48] rtl.compile 
[info] compiling 3 Scala sources to /home/jimw/miniproj/systolic_array/out/rtl/compile.dest/classes ...
[warn] 50 feature warnings; re-run with -feature for details
[warn] one warning found
[info] done compiling
[48/48] rtl.run 
====== Emit SystemVerilog file ======
====== Run Tests ======
TestSystolicArray:
Systolic array of size 2 x 3
createSequentialInputs() numRow=2 numCol=3 numElem=4
createFeedInputs() numRow=2 numCol=3 numElem=4 totalCycle=8
feed() numRow=2 numCol=3 totalCycle=8
mac() numRow=2 numCol=3 numElem=4
flush() numRow=2 numCol=3 totalCycle=3
- should pass with sequantial inputs
Systolic array of size 8 x 8
createSequentialInputs() numRow=8 numCol=8 numElem=8
createFeedInputs() numRow=8 numCol=8 numElem=8 totalCycle=23
feed() numRow=8 numCol=8 totalCycle=23
mac() numRow=8 numCol=8 numElem=8
flush() numRow=8 numCol=8 totalCycle=8
- should pass with sequantial inputs
```

Then you will find `gen/misc/SystolicArray.sv`

## Install

### Install Chisel
https://github.com/chipsalliance/chisel/blob/main/SETUP.md

### Install mill
https://mill-build.com/mill/Installation_IDE_Support.html
