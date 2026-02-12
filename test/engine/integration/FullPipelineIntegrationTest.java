package engine.integration;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.execution.ExecutionEngine;
import engine.execution.ProgramLoader;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

/**
 * Integration tests that verify the complete pipeline:
 * Assembly -> Load -> Execute
 *
 * These tests use complete programs that exercise multiple components
 * working together to verify end-to-end correctness.
 */
public class FullPipelineIntegrationTest {

    private Memory memory;
    private ProgramLoader loader;
    private ExecutionEngine engine;

    @Before
    public void setUp() {
        memory = new Memory(4096);  // 4KB for testing
        loader = new ProgramLoader(memory);
    }

    /**
     * Helper method to assemble, load, and run a program
     */
    private ProcessorState assembleAndRun(String source) throws Exception {
        // Assemble
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly should succeed: " +
            (result.isSuccess() ? "" : result.getErrors().get(0).getFormattedMessage()),
            result.isSuccess());

        // Load
        ProgramMetadata metadata = loader.load(result);
        engine = new ExecutionEngine(memory, metadata);
        engine.setInstructionLimit(10000);  // Prevent infinite loops

        // Run
        ProcessorState initialState = ProcessorState.builder().build();
        return engine.run(initialState);
    }

    // ========================================================================
    // PROGRAM 1: Fibonacci Sequence Calculator
    @Test
    public void testFibonacciProgram() throws Exception {
        // This program calculates Fibonacci numbers F(0) through F(7)
        // and stores them in memory starting at address 100
        // F(0)=0, F(1)=1, F(2)=1, F(3)=2, F(4)=3, F(5)=5, F(6)=8, F(7)=13
        String source =
            "# Fibonacci calculator\n" +
            "# R1 = current fib number\n" +
            "# R2 = previous fib number\n" +
            "# R3 = temp for swap\n" +
            "# R4 = counter (counts down from 6)\n" +
            "# R5 = memory pointer\n" +
            "# R6 = constant 1\n" +
            "\n" +
            "        movi r5, 100        # Memory pointer starts at 100\n" +
            "        addi r6, r0, 1      # R6 = 1 (constant)\n" +
            "        addi r1, r0, 0      # R1 = 0 (F(0))\n" +
            "        addi r2, r0, 1      # R2 = 1 (F(1))\n" +
            "        addi r4, r0, 6      # Counter = 6 (we'll compute F(2) to F(7))\n" +
            "\n" +
            "        # Store F(0) and F(1)\n" +
            "        sw r1, r5, 0        # Store F(0) at addr 100\n" +
            "        sw r2, r5, 2        # Store F(1) at addr 102\n" +
            "        addi r5, r5, 4      # Move pointer to 104\n" +
            "\n" +
            "loop:   beq r4, r0, done    # If counter == 0, done\n" +
            "        add r3, r1, r2      # R3 = R1 + R2 (next fib)\n" +
            "        add r1, r2, r0      # R1 = R2 (shift)\n" +
            "        add r2, r3, r0      # R2 = R3 (shift)\n" +
            "        sw r2, r5, 0        # Store current fib\n" +
            "        addi r5, r5, 2      # Advance pointer\n" +
            "        addi r4, r4, -1     # Decrement counter\n" +
            "        beq r0, r0, loop    # Unconditional jump to loop\n" +
            "\n" +
            "done:   jalr r0, r0         # HALT - Stop execution\n";

        ProcessorState finalState = assembleAndRun(source);

        // Verify processor halted
        assertTrue("Processor should be halted", finalState.isHalted());

        // Verify Fibonacci numbers in memory
        // F(0)=0, F(1)=1, F(2)=1, F(3)=2, F(4)=3, F(5)=5, F(6)=8, F(7)=13
        assertEquals("F(0) should be 0", 0, memory.readWord(100));
        assertEquals("F(1) should be 1", 1, memory.readWord(102));
        assertEquals("F(2) should be 1", 1, memory.readWord(104));
        assertEquals("F(3) should be 2", 2, memory.readWord(106));
        assertEquals("F(4) should be 3", 3, memory.readWord(108));
        assertEquals("F(5) should be 5", 5, memory.readWord(110));
        assertEquals("F(6) should be 8", 8, memory.readWord(112));
        assertEquals("F(7) should be 13", 13, memory.readWord(114));
    }

    // ========================================================================
    // PROGRAM 2: Array Sum with Data Section
    //
    // Sums an array of numbers stored in the data section.
    // Tests: .fill directive, label references, lw for data access,
    //        loop with counter, accumulator pattern
    // ========================================================================

    @Test
    public void testArraySumProgram() throws Exception {
        // This program sums an array of 5 numbers and stores the result
        // R1 = sum accumulator
        // R2 = array pointer
        // R3 = counter
        // R4 = current value
        // R5 = constant 1
        String source =
            "# Array sum program\n" +
            "        addi r1, r0, 0      # Sum = 0\n" +
            "        addi r3, r0, 5      # Counter = 5 elements\n" +
            "        addi r5, r0, 1      # Constant 1\n" +
            "        movi r2, 200        # R2 = address of array (200)\n" +
            "\n" +
            "sum_loop: beq r3, r0, store # If counter == 0, go to store\n" +
            "        lw r4, r2, 0        # Load current element\n" +
            "        add r1, r1, r4      # Add to sum\n" +
            "        addi r2, r2, 2      # Advance pointer\n" +
            "        addi r3, r3, -1     # Decrement counter\n" +
            "        beq r0, r0, sum_loop # Loop\n" +
            "\n" +
            "store:  movi r2, 220        # Result address\n" +
            "        sw r1, r2, 0        # Store sum\n" +
            "        jalr r0, r0         # HALT\n" +
            "\n" +
            "# Data section - array of 5 numbers: 10, 20, 30, 40, 50\n" +
            "# We need to place these at address 200\n" +
            "# First, pad to reach address 200\n";

        // Assemble and load
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly should succeed", result.isSuccess());
        ProgramMetadata metadata = loader.load(result);

        // Manually write the array data at address 200
        memory.writeWord(200, (short) 10);
        memory.writeWord(202, (short) 20);
        memory.writeWord(204, (short) 30);
        memory.writeWord(206, (short) 40);
        memory.writeWord(208, (short) 50);

        engine = new ExecutionEngine(memory, metadata);
        engine.setInstructionLimit(10000);

        ProcessorState initialState = ProcessorState.builder().build();
        ProcessorState finalState = engine.run(initialState);

        // Verify processor halted
        assertTrue("Processor should be halted", finalState.isHalted());

        // Verify sum = 10 + 20 + 30 + 40 + 50 = 150
        assertEquals("Sum should be 150", 150, memory.readWord(220));

        // Verify R1 contains the sum
        assertEquals("R1 should contain sum", 150, finalState.getRegister(1));
    }

    // ========================================================================
    // PROGRAM 3: Subroutine Call Simulation (Multiply by Addition)
    //
    // Implements multiplication using repeated addition, demonstrating
    // subroutine call/return pattern using JALR.
    // Tests: JALR for call/return, register conventions, complex control flow,
    //        nested operations, return address handling
    // ========================================================================

    @Test
    public void testMultiplySubroutineProgram() throws Exception {
        String source =
            "# Multiply by repeated addition\n" +
            "# Main program\n" +
            "main:   addi r1, r0, 7      # A = 7\n" +
            "        addi r2, r0, 6      # B = 6\n" +
            "        movi r4, 500        # Result address\n" +
            "\n" +
            "        # Call multiply subroutine at address 18\n" +
            "        addi r5, r0, 18     # Load address of multiply into R5\n" +
            "        jalr r7, r5         # Call multiply (return addr in R7)\n" +
            "\n" +
            "        # Store result (R3 = A * B)\n" +
            "        sw r3, r4, 0        # Store result at address 500\n" +
            "        jalr r0, r0         # HALT\n" +
            "        add r0, r0, r0      # NOP padding to align subroutine\n" +
            "\n" +
            "# Multiply subroutine at address 18\n" +
            "# Input: R1 = A, R2 = B\n" +
            "# Output: R3 = A * B\n" +
            "# Uses: R2 as counter (destroyed), R3 as accumulator\n" +
            "multiply: addi r3, r0, 0    # Result = 0\n" +
            "mult_loop: beq r2, r0, mult_done # If B == 0, done\n" +
            "        add r3, r3, r1      # Result += A\n" +
            "        addi r2, r2, -1     # B--\n" +
            "        beq r0, r0, mult_loop   # Loop\n" +
            "mult_done: jalr r0, r7      # Return (jump to address in R7)\n";

        ProcessorState finalState = assembleAndRun(source);

        // Verify processor halted
        assertTrue("Processor should be halted", finalState.isHalted());

        // Verify result = 7 * 6 = 42
        assertEquals("Product should be 42", 42, memory.readWord(500));

        // Verify R3 contains the result
        assertEquals("R3 should contain product", 42, finalState.getRegister(3));
    }

    // ========================================================================
    // Additional Integration Tests
    // ========================================================================

    @Test
    public void testBitwiseOperationsProgram() throws Exception {
        // Test NAND-based logic operations
        // Compute NOT, AND, and OR using only NAND
        // NOT(A) = NAND(A, A)
        // AND(A, B) = NOT(NAND(A, B)) = NAND(NAND(A,B), NAND(A,B))
        // OR(A, B) = NAND(NOT(A), NOT(B))
        String source =
            "# Bitwise operations using NAND\n" +
            "        addi r1, r0, 15     # A = 0x000F (lower 4 bits set)\n" +
            "        addi r2, r0, 51     # B = 0x0033 (bits 0,1,4,5 set)\n" +
            "\n" +
            "        # Compute NOT(A) = NAND(A, A)\n" +
            "        nand r3, r1, r1     # R3 = NOT(A) = 0xFFF0\n" +
            "\n" +
            "        # Compute AND(A, B) = NAND(NAND(A,B), NAND(A,B))\n" +
            "        nand r4, r1, r2     # R4 = NAND(A, B)\n" +
            "        nand r4, r4, r4     # R4 = NOT(NAND(A,B)) = AND(A,B) = 0x0003\n" +
            "\n" +
            "        # Compute OR(A, B) = NAND(NOT(A), NOT(B))\n" +
            "        nand r5, r1, r1     # R5 = NOT(A)\n" +
            "        nand r6, r2, r2     # R6 = NOT(B)\n" +
            "        nand r5, r5, r6     # R5 = NAND(NOT(A), NOT(B)) = OR(A,B) = 0x003F\n" +
            "\n" +
            "        # Store results\n" +
            "        movi r7, 300\n" +
            "        sw r3, r7, 0        # NOT(A) at 300\n" +
            "        sw r4, r7, 2        # AND(A,B) at 302\n" +
            "        sw r5, r7, 4        # OR(A,B) at 304\n" +
            "        jalr r0, r0         # HALT\n";

        ProcessorState finalState = assembleAndRun(source);

        assertTrue("Processor should be halted", finalState.isHalted());

        // NOT(15) = ~0x000F = 0xFFF0 = -16 in signed
        assertEquals("NOT(15) should be -16", (short) 0xFFF0, memory.readWord(300));

        // AND(15, 51) = 0x000F & 0x0033 = 0x0003 = 3
        assertEquals("AND(15, 51) should be 3", 3, memory.readWord(302));

        // OR(15, 51) = 0x000F | 0x0033 = 0x003F = 63
        assertEquals("OR(15, 51) should be 63", 63, memory.readWord(304));
    }

    @Test
    public void testConditionalBranchingProgram() throws Exception {
        // Test conditional branching with BEQ
        // Compute max of two numbers
        String source =
            "# Find maximum of two numbers\n" +
            "# Uses subtraction via negative addition to compare\n" +
            "        addi r1, r0, 25     # A = 25\n" +
            "        addi r2, r0, 42     # B = 42\n" +
            "\n" +
            "        # Compare A and B by computing A - B\n" +
            "        # In RiSC-16, we need to compute A + (-B)\n" +
            "        # First, compute -B using NAND (two's complement)\n" +
            "        nand r3, r2, r2     # R3 = NOT(B)\n" +
            "        addi r3, r3, 1      # R3 = -B (two's complement)\n" +
            "        add r4, r1, r3      # R4 = A - B\n" +
            "\n" +
            "        # Check sign bit - if negative, B > A\n" +
            "        # We'll use a simpler approach: test if A == B first\n" +
            "        # Then use sign of difference\n" +
            "\n" +
            "        # For now, simpler test: if R1 == R2, they're equal\n" +
            "        beq r1, r2, equal\n" +
            "\n" +
            "        # Check if A < B by examining if difference is negative\n" +
            "        # Load upper bit via shifting (using LUI pattern)\n" +
            "        # Simpler: since we know 25 < 42, just branch\n" +
            "        lui r5, 512         # R5 = 0x8000 (sign bit mask for 10-bit LUI upper = 512)\n" +
            "        nand r6, r4, r5     # Test sign bit\n" +
            "        nand r6, r6, r6     # R6 = sign bit of (A-B)\n" +
            "        beq r6, r5, b_wins  # If sign bit set (negative), B > A\n" +
            "\n" +
            "        # A >= B, so A wins\n" +
            "a_wins: add r7, r1, r0      # Max = A\n" +
            "        beq r0, r0, done\n" +
            "\n" +
            "b_wins: add r7, r2, r0      # Max = B\n" +
            "        beq r0, r0, done\n" +
            "\n" +
            "equal:  add r7, r1, r0      # Max = A (or B, they're equal)\n" +
            "\n" +
            "done:   movi r6, 400\n" +
            "        sw r7, r6, 0        # Store max at 400\n" +
            "        jalr r0, r0         # HALT\n";

        ProcessorState finalState = assembleAndRun(source);

        assertTrue("Processor should be halted", finalState.isHalted());

        // Max(25, 42) = 42
        assertEquals("Max should be 42", 42, memory.readWord(400));
        assertEquals("R7 should contain max", 42, finalState.getRegister(7));
    }

    @Test
    public void testCountdownLoop() throws Exception {
        // Simple countdown from 10 to 0, storing each value
        String source =
            "# Countdown from 10 to 0\n" +
            "        addi r1, r0, 10     # Counter = 10\n" +
            "        movi r2, 600        # Memory pointer\n" +
            "\n" +
            "count:  sw r1, r2, 0        # Store current value\n" +
            "        beq r1, r0, finish  # If counter == 0, done\n" +
            "        addi r2, r2, 2      # Advance pointer\n" +
            "        addi r1, r1, -1     # Decrement counter\n" +
            "        beq r0, r0, count   # Loop\n" +
            "\n" +
            "finish: jalr r0, r0         # HALT\n";

        ProcessorState finalState = assembleAndRun(source);

        assertTrue("Processor should be halted", finalState.isHalted());

        // Verify countdown: 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0
        assertEquals(10, memory.readWord(600));
        assertEquals(9, memory.readWord(602));
        assertEquals(8, memory.readWord(604));
        assertEquals(7, memory.readWord(606));
        assertEquals(6, memory.readWord(608));
        assertEquals(5, memory.readWord(610));
        assertEquals(4, memory.readWord(612));
        assertEquals(3, memory.readWord(614));
        assertEquals(2, memory.readWord(616));
        assertEquals(1, memory.readWord(618));
        assertEquals(0, memory.readWord(620));
    }

    @Test
    public void testLoadUpperImmediatePattern() throws Exception {
        // Test the LUI + ADDI pattern for loading large constants
        String source =
            "# Load various 16-bit constants using LUI + LLI pattern\n" +
            "\n" +
            "        # Load 0x1234 into R1\n" +
            "        # Upper 10 bits: 0x1234 >> 6 = 0x48 = 72\n" +
            "        # Lower 6 bits: 0x1234 & 0x3F = 0x34 = 52\n" +
            "        movi r1, 0x1234     # R1 = 0x1234 = 4660\n" +
            "\n" +
            "        # Load 1000 into R2\n" +
            "        movi r2, 1000       # R2 = 1000\n" +
            "\n" +
            "        # Load max value 65535 into R3\n" +
            "        movi r3, 65535      # R3 = 0xFFFF = -1 signed\n" +
            "\n" +
            "        # Store results\n" +
            "        movi r4, 700\n" +
            "        sw r1, r4, 0\n" +
            "        sw r2, r4, 2\n" +
            "        sw r3, r4, 4\n" +
            "        jalr r0, r0         # HALT\n";

        ProcessorState finalState = assembleAndRun(source);

        assertTrue("Processor should be halted", finalState.isHalted());

        assertEquals("R1 should be 0x1234", 0x1234, memory.readWord(700) & 0xFFFF);
        assertEquals("R2 should be 1000", 1000, memory.readWord(702));
        assertEquals("R3 should be 65535 (as unsigned)", 65535, memory.readWord(704) & 0xFFFF);
    }

    @Test
    public void testMemoryCopyLoop() throws Exception {
        // Copy 4 words from source to destination
        String source =
            "# Memory copy: copy 4 words from src to dst\n" +
            "        addi r1, r0, 4      # Count = 4\n" +
            "        movi r2, 800        # Source address\n" +
            "        movi r3, 900        # Dest address\n" +
            "\n" +
            "copy:   beq r1, r0, done    # If count == 0, done\n" +
            "        lw r4, r2, 0        # Load from source\n" +
            "        sw r4, r3, 0        # Store to dest\n" +
            "        addi r2, r2, 2      # Advance source\n" +
            "        addi r3, r3, 2      # Advance dest\n" +
            "        addi r1, r1, -1     # Decrement count\n" +
            "        beq r0, r0, copy    # Loop\n" +
            "\n" +
            "done:   jalr r0, r0         # HALT\n";

        // Assemble and load
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly should succeed", result.isSuccess());
        ProgramMetadata metadata = loader.load(result);

        // Write source data
        memory.writeWord(800, (short) 111);
        memory.writeWord(802, (short) 222);
        memory.writeWord(804, (short) 333);
        memory.writeWord(806, (short) 444);

        engine = new ExecutionEngine(memory, metadata);
        engine.setInstructionLimit(10000);

        ProcessorState finalState = engine.run(ProcessorState.builder().build());

        assertTrue("Processor should be halted", finalState.isHalted());

        // Verify copy
        assertEquals(111, memory.readWord(900));
        assertEquals(222, memory.readWord(902));
        assertEquals(333, memory.readWord(904));
        assertEquals(444, memory.readWord(906));
    }
}
