# Assembly Examples

### Program 1

* Function : sums all values specified in the data part and stores them in R5.
* Code :
```
addi r1, r0, 32
addi r2, r0, 36
addi r3, r0, 52

lw r4, r1, 0
add r5, r5, r4
addi r1, r1, 2
ble r1, r2, -8

addi r1, r1, 4
addi r2, r2, 10
ble r1, r3, -14
```
* Data :
```
32 1
34 2
36 3
42 1
44 2
46 3
52 1
54 2
56 3
```

### Program 2

* Function : loads and doubles all values specified in the data part, then stores them back. The aim of this program is to test the write hit policies (since all stores are preceded by loads of the same address).
* Code :
```
addi r1, r0, 32
addi r2, r0, 36
addi r3, r0, 0
addi r4, r0, 5

lw r5, r1, 0
muli r5, r5, 2
sw r5, r1, 0
addi r1, r1, 2
ble r1, r2, -10

addi r1, r1, 10
addi r2, r2, 16
addi r3, r3, 1
blt r3, r4, -18
```
* Data :
```
32 1
34 2
36 3
48 4
50 5
52 6
64 7
66 8
68 9
80 10
82 11
84 12
96 13
98 14
100 15
```

### Program 3

* Function : This program is equivalent to the following code snippet. It's aim is to test the write miss policies.
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

sw r1, r1, 0
addi r1, r1, 2
addi r2, r2, 1
ble r2, r3, -8
```