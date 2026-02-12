
# RiSC-16 Assembly Good Practices

This document is a beginner-friendly set of habits that make RiSC-16 assembly programs easier to write, debug, and maintain.

**Supported ISA reminders (this simulator):**

- Real instructions: `add`, `nand`, `addi`, `lw`, `sw`, `beq`, `jalr`, `lui`
- Pseudo-instructions: `movi`, `lli`, `halt`, `nop`
- Registers: `r0`...`r7` (**`r0` is always 0**)

---

## Table of Contents

- [RiSC-16 Assembly Good Practices](#risc-16-assembly-good-practices)
  - [Table of Contents](#table-of-contents)
  - [Use labels instead of magic numbers](#use-labels-instead-of-magic-numbers)
  - [Prefer address registers for `lw`/`sw`](#prefer-address-registers-for-lwsw)
  - [Keep immediates in range (important!)](#keep-immediates-in-range-important)
  - [Write loop structure explicitly](#write-loop-structure-explicitly)
  - [Make comments (please)](#make-comments-please)
  - [Plan your register usage](#plan-your-register-usage)
  - [Data section habits: `.fill` and `.space`](#data-section-habits-fill-and-space)
    - [`.fill`](#fill)
    - [`.space`](#space)
  - [Jumps and function calls with `jalr`](#jumps-and-function-calls-with-jalr)
    - [Far jump (when `beq` can’t reach)](#far-jump-when-beq-cant-reach)
    - [Simple “function call” pattern](#simple-function-call-pattern)
  - [Common mini-patterns](#common-mini-patterns)
    - [Unconditional branch](#unconditional-branch)
    - [Bitwise NOT](#bitwise-not)
    - [Load a 16-bit constant](#load-a-16-bit-constant)
    - [Load/store “global” data reliably](#loadstore-global-data-reliably)

---

## Use labels instead of magic numbers

Avoid hardcoding branch offsets and data addresses.

**Good:**
```asm
loop:
	addi r2, r2, 1
	beq  r2, r3, done
	beq  r0, r0, loop

done:
	halt
```

This is more readable and survives edits (adding/removing lines) without forcing you to recalculate offsets.

---

## Prefer address registers for `lw`/`sw`

You *can* write `lw`/`sw` using a label directly (the assembler supports symbolic immediates), but it forces the label’s address to fit in the **7-bit signed immediate** range of `lw`/`sw`, which is 31 instructions at most.

For beginners, a good habit is:

1. Load the address into a register using `movi`
2. Use `lw`/`sw` with an offset of `0`

**Good:**
```asm
movi r1, value  # r1 = address of 'value'
lw   r2, r1, 0  # r2 = Mem[value]

addi r2, r2, 1
sw   r2, r1, 0  # Mem[value] = r2

halt

value: .fill 42
```

This works even when `value` is far away in memory (up to ~16k instructions).

---

## Keep immediates in range (important!)

In this simulator, the immediate sizes are:

- `addi`, `lw`, `sw`, `beq`: **7-bit signed** immediate: **-64 .. 63**
- `lui`: **10-bit unsigned** immediate: **0 .. 1023** (loads into the upper bits)
- `movi`: **16-bit unsigned** immediate: **0 .. 65535** (`0x0000 .. 0xFFFF`)

Practical consequences:

- `beq` can only branch relatively a short distance (~30 instructions)
- `lw rX, r0, label` only works if the resolved address fits `-64..63`.
- Use `movi` for large constants and for addresses.

**Note on `movi`:** it expands to two instructions (`lui` + `addi`). A `movi` line effectively occupies **two instruction slots**.

---

## Write loop structure explicitly

There is no “compare-and-branch-less-than” instruction in the base ISA, so structure loops around equality and unconditional branches.

**Good simple counted loop (runs exactly 16 times):**
```asm
addi r2, r0, 0      # i = 0
addi r3, r0, 16     # n = 16

loop:
	addi r2, r2, 1
	beq  r2, r3, done
	beq  r0, r0, loop

done:
	halt
```

This pattern is also easy to step through and inspect in the simulator, or even to set breakpoints against (debugging on!)

---

## Make comments (please)

Comments are removed by the assembler, so use them freely. Prefer comments that explain *why* something is happening.

**Good comments:**

- Register purpose (`# r2 = loop counter`)
- Invariants (`# r0 is always 0`)
- Non-obvious bit tricks (`# NOT r4 (two’s complement sign check)`) 

Try to keep each comment short and aligned with the line it describes.

---

## Plan your register usage

Before writing code, decide what each register is “for” and stick to it.

A simple beginner convention:

- `r0`: constant 0 (never changes)
- `r1`: address pointer / base pointer
- `r2`: loop counter
- `r3`: loop limit / constant
- `r4`..`r6`: temporaries
- `r7`: return address for subroutines (when using `jalr`)

If you use `r7` as a return register, don’t overwrite it inside your subroutine unless you save/restore it (learn more about in Comp Arch 1).

---

## Data section habits: `.fill` and `.space`

### `.fill`

Use `.fill` to store constants or initial values.

```asm
one: .fill 1
mask: .fill 0x00FF
```

If you use a label as the operand of `.fill`, it stores the **address** of that label (not the value at that label).

### `.space`

Use `.space N` to reserve `N` words initialized to 0.

```asm
buffer: .space 16   # 16 words of zero
```
Because the simulator default initializes everything to 0 (not always guaranteed elsewhere), the .space directive is mainly used to push memory words far away if necessary.

---

## Jumps and function calls with `jalr`

`jalr ra, rb` does two things:

- `ra = PC` (stores a return address)
- `PC = rb` (jumps to the address in `rb`)

### Far jump (when `beq` can’t reach)

If a label is too far for a `beq` offset, you can jump unconditionally via a register. To discard return addresses, use r0 as rA

```asm
movi r4, far_target
jalr r0, r4         # jump to far_target
...
far_target:
	halt
```

### Simple “function call” pattern

```asm
	movi r4, func
	jalr r7, r4        # call: r7 gets return address

	# execution resumes here after the function returns
	halt

func:
    ...
	jalr r0, r7        # return: jump back via r7
```

---

## Common mini-patterns

### Unconditional branch

```asm
beq r0, r0, some_label
```

### Bitwise NOT

`nand x, x, x` computes `~x`:

```asm
nand r4, r4, r4
```

### Load a 16-bit constant

Use `movi` (it expands to `lui` + `addi`):

```asm
movi r3, 0x8000
```

### Load/store “global” data reliably

```asm
movi r1, counter
lw r2, r1, 0
addi r2, r2, 1
sw r2, r1, 0
halt

counter: .fill 0 # Use a dedicated memory address instead of a register
```

---

*Happy assembling!*