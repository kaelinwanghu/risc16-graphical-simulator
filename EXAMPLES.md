# Assembly Examples

### Program 1

* Function : Arithmetic right shifts a number once	
* Code :
```
lw r1, r0, toshift # Load the number to be shifted
addi r2, r0, 0 # Initialize r2, the result register, to 0
addi r5, r0, 1 # And the constant 1

movi r3, 0x8000

nand r4, r1, r3 
nand r4, r4, r4 # Check whether r1 is negative or not

beq r4, r0, 28  # If not negative, branch to the start of the positive loop

nand r6, r1, r5 # If negative, check whether even or odd (odd negatives truncate up, so -17 -> -9 while 17 -> 8)
nand r6, r6, r6

nand r1, r1, r1 # Convert the number to be positive so the algorithm works
addi r1, r1, 1

beq r1, r0, 8 # Do the usual loop of simulated "division"
beq r1, r5, 6
addi r1, r1, -2
addi r2, r2, 1
beq r0, r0, -10

beq r6, r0, 2 # Then, at the end of the "division", if r6 is equal to 0 (even), skip the next line
addi r2, r2, 1 # Adds an extra 1 to r2
nand r2, r2, r2
addi r2, r2, 1 # Before converting it back to a negative
beq r0, r0, 10 # Jump to end of program (for negatives)

beq r1, r0, 8 # Positive loop, simulated "division"
beq r1, r5, 6
addi r1, r1, -2
addi r2, r2, 1
beq r0, r0, -10

toshift: .fill 40
```

### Program 2

* Function : This program is equivalent to the following code snippet
```
for(int i = 0; i < 16; i++)  {
	int address = 32 + i * 2;
	Mem[address] = address;
}
```
* Code :
```
addi r1, r0, 32
addi r2, r0, 0
addi r3, r0, 16

store: sw r1, r1, 0
addi r1, r1, 2
addi r2, r2, 1
beq r2, r3, exit # If we've done 16 iterations, skip the back-jump and exit
beq r0, r0, store # Otherwise, jump back to the store
exit: halt
```