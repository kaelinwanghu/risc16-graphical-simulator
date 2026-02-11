# RiSC-16 Simulator Debugging Guide

This document describes the debugging features available in the RiSC-16 Simulator.

## Table of Contents

1. [Enabling Debugging](#enabling-debugging)
2. [Execution Snapshots](#execution-snapshots)
3. [Breakpoints](#breakpoints)
4. [Live Editing](#live-editing)
5. [Visual Indicators](#visual-indicators)
6. [Troubleshooting](#troubleshooting)

---

## Enabling Debugging

### How to Enable

1. Click **Settings** in the Storage panel
2. Check **"Enable Debugging Features (Experimental)"**
3. (Optional) Set **Snapshot Limit** (default: 100)
4. Click **Apply**

### Features Enabled

- Execution snapshots (capture/restore state)
- Breakpoints (unconditional and conditional)
- Live register/memory editing
- Debug menu with management tools

**[Screenshot: Debug button with popup menu]**
![screen10](/screenshots/screen10.png)

---

## Execution Snapshots

### What Are Snapshots?

Snapshots capture the complete processor state (registers, PC, memory) before each instruction executes.

### Viewing Snapshots

1. Click **Debug** -> **View Snapshots...**
2. Snapshots show: Number, Time, PC, Instruction Count

![screen11](/screenshots/screen11.png)

### Restoring State

1. Select a snapshot
2. Click **Restore Selected** (or double-click)
3. Processor returns to that exact state

**Requirements:** Program must be loaded and in execution mode (not halted).

### Management

- **Delete Selected** - Remove one snapshot
- **Clear All** - Remove all snapshots
- Snapshots auto-clear when: program halts, execution error, return to edit mode

---

## Breakpoints

### Setting Breakpoints

**Method 1:** Click a line number in the Program panel
- A bullet (●) will appear to indicate an active breakpoint

**[Screenshot: Line numbers with breakpoint indicators]**

**Method 2:** Use **Debug** -> **Manage Breakpoints...**

![screen12](/screenshots/screen12.png)

### Unconditional Breakpoints

Pauses execution **every time** the line is reached.

### Conditional Breakpoints

Pauses execution only when a **condition is met**.

**To create:**
1. Set a breakpoint (click line number)
2. Open **Debug** -> **Manage Breakpoints...**
3. Select breakpoint and click **Edit Condition...**

![screen13](/screenshots/screen13.png)

### Condition Types

| Watch Type | Description | Example |
|------------|-------------|---------|
| **Register** | Monitor register value | Break when R5 == 42 |
| **Memory** | Monitor memory location | Break when Mem[0x20] > 100 |
| **PC** | Monitor program counter | Break when PC == 0x10 |
| **Instruction Count** | Monitor step count | Break when count >= 1000 |

**Operators:** `==`, `!=`, `<`, `<=`, `>`, `>=`

### When Breakpoint Hits

1. Execution stops immediately
2. Dialog shows: PC, instruction count, condition (if applicable)
3. You can:
   - Inspect state
   - Click **Execute Step** to step one instruction
   - Click **Execute** to continue running

![screen14](/screenshots/screen14.png)

### Important: MOVI Behavior

The `movi` pseudo-instruction expands to **two** instructions (LUI + ADDI). A breakpoint on a `movi` line will trigger **twice** - once for each instruction.

---

## Live Editing

### Requirements

Live editing works when:
- Debugging enabled
- Program assembled and loaded
- In execution mode (not halted)

### Editing Registers

1. View **Registers** in Storage panel
2. **Double-click** a register value
3. Enter new value, press **Enter**

![screen15](/screenshots/screen15.png)

**Note:** R0 cannot be edited (always 0).

![screen16](/screenshots/screen16.png)

### Editing Memory (Similar to the above)

1. View **Memory** in Storage panel
2. **Double-click** a memory value
3. Enter new value, press **Enter**

![screen17](/screenshots/screen17.png)

### Input Formats

| Format | Example | Description |
|--------|---------|-------------|
| Hexadecimal | `0x2A` | Prefix with `0x` |
| Decimal | `42` or `-17` | Plain numbers |
| Octal | `052` | Prefix with `0` |

**Range:** -32768 to 32767 (signed) or 0 to 65535 (unsigned)

### Visual Feedback

Edited values turn **red** and remain red until:
- Program halts
- Execution error
- Return to edit mode

![screen17](/screenshots/screen18.png)

---

## Visual Indicators

### Register View Colors

| Color | Meaning |
|-------|---------|
| **Red** | Manually edited |
| **Yellow** | R0 write attempt |
| **Green** | Changed by last instruction |
| **White** | No change |

**Priority:** Red > Yellow > Green > White

### Memory View Colors

| Color | Meaning |
|-------|---------|
| **Red** | Manually edited |
| **Green** | Written by last instruction |
| **Light Blue** | Initial program data |
| **White** | No change |

**Priority:** Red > Green > Blue > White

---

## Troubleshooting

### Can't set breakpoints
**Solution:** Enable debugging in Settings.

### Can't edit values
**Solution:** Ensure program is loaded and in execution mode (not halted).

### Breakpoint doesn't trigger
**Check:**
- Is breakpoint enabled? (checkbox in manager)
- Is condition ever met? (for conditional breakpoints)
- Is line ever executed? (check if branch taken)

### App crashes or freezes
**Solution:** Contact wanghuk@rose-hulman.edu

---

## Quick Reference

### Keyboard Shortcuts

| Action | Key | Context |
|--------|-----|---------|
| Confirm edit | **Enter** | Value editor |
| Cancel edit | **Escape** | Value editor |

### Feature Availability

| Feature | Without Debug | With Debug |
|---------|--------------|------------|
| Execute programs | Y | Y |
| View state | Y | Y |
| Snapshots | N | Y |
| Breakpoints | N | Y |
| Live editing | N | Y |

---

## Tips

- Use snapshots for "time travel" debugging
- Use conditional breakpoints for "break when interesting"
- Edit values to test edge cases without restarting
- **Red** = you changed it, **Green** = instruction changed it
- Disable debugging for performance testing

---

*Happy debugging!*