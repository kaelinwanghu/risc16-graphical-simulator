# Architectural Simulator

A Java simulator capable of assessing the performance of a single-issue out-of-order 16-bit RISC processor that uses Tomasulo’s algorithm with speculation, taking into account the effect of the cache and memory organization.

### Features

* Instruction Set Architecture : A simplified RISC ISA is assumed, inspired by the ISA of the [Ridiculously Simple Computer (RiSC-16)](http://www.ece.umd.edu/~blj/RiSC/). The word size of this computer is 16-bit. The processor has 8 general purpose registers R0 to R7 (16-bit each), with register R0 always containing the value 0.
* Memory Hierarchy : The system being simulated is assumed to have separate L1 instruction and data caches. Moreover, the user can specify the number of data cache levels, full cache geometry, write policies, and the number of cycles required to access data. While simulating the execution, the contents of all storage units are shown, along with the number of accesses, hits and hit ratio. 
* Scheduling: The simulator follows the speculative version of Tomasulo’s algorithm. The user can specify the number of ROB entries available, functional units and cycles needed by each unit. The prediction for conditional branches depends on the sign of the offset (taken if positive and not taken if negative). Moreover, the total execution time (expressed as the number of cycles spanned) and IPC ratio are shown.

### Guide

See [GUIDE.md](/GUIDE.md).

### Assembly Examples

See [EXAMPLES.md](/EXAMPLES.md).

### Download

[Version 1.0](https://github.com/bishoybassem/architectural-simulator/releases/download/v1.0/Architectural.Simulator.jar)

### Screenshots

![screen1](/screenshots/screen3.jpg)

![screen2](/screenshots/screen2.jpg)

![screen3](/screenshots/screen5.jpg)

![screen3](/screenshots/screen7.jpg)