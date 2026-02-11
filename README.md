# Architectural Simulator

An accessible Java simulator for the 16-bit RISC processor with pseudoinstructions, rich debugging features, labels, and more!

### Features

* Instruction Set Architecture : A simplified ISA based off of the [Ridiculously Simple Computer (RiSC-16)](http://www.ece.umd.edu/~blj/RiSC/). The word size of this computer is 16-bit. The processor has 8 general purpose registers R0 to R7 (16-bit each), with register R0 always containing the value 0.
* Pseudo-Instruction Support: Quality of Life instructions such as "movi" and "halt" and memory loading instructions like ".fill" and ".space". Specified in the RiSC manual, but not implemented until now.
* Label Support: Ability to use labels as symbolic immediates to avoid otherwise hardcoded calculation of jumps and memory instructions.
* Debugging Suite: Unconditional and conditional breakpoints are present, along with live memory editing of both registers and memory and snapshots to revert back to. This feature is opt-in since it does have overhead.
* Program Flexibility: Execution limits to prevent infinite loops, memory viewing at all the valid addresses for any range, program size specification to simulate larger or smaller programs are all supported.
* File Saving: Ability to save and load files with the .as16 extension. Also supports autosaving so that otherwise lost work can easily be recovered.
* Overall Accessibility: GUI is simple and ucluttered while still being quite flexible. Simple but effective debugging is available and targeted for novices and the advanced alike. Ctrl + Z exists to recover work and push away mistakes.

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