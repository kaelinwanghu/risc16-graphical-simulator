package engine.execution;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.isa.InstructionFormat;

/**
 * Tests for the ExecutionException class
 *
 * ExecutionException is thrown when instruction execution fails due to:
 * - Invalid memory access
 * - Attempting to execute data
 * - PC pointing to invalid address
 * - Instruction limit exceeded
 */
public class ExecutionExceptionTest {

    // ========== Assembly-Based Tests ==========

    @Test
    public void testExceptionForInvalidMemoryAccess() {
        String source = "lw r1, r0, 10";
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        InstructionFormat instr = result.getInstructions().get(0);

        // Simulate exception when loading from invalid address
        ExecutionException ex = new ExecutionException(
            "Invalid memory address: 0xFFFF",
            0,
            instr.toAssembly()
        );

        assertEquals(0, ex.getPC());
        assertEquals(instr.toAssembly(), ex.getInstruction());
        assertTrue(ex.getMessage().contains("0xFFFF"));
        assertTrue(ex.getMessage().contains("PC=0x0000"));
    }

    @Test
    public void testExceptionForStoreToInvalidAddress() {
        String source = "sw r1, r2, -5";
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        InstructionFormat instr = result.getInstructions().get(0);

        ExecutionException ex = new ExecutionException(
            "Cannot write to address -5",
            0,
            instr.toAssembly()
        );

        assertTrue(ex.getMessage().contains("Cannot write"));
        assertEquals(instr.toAssembly(), ex.getInstruction());
    }

    @Test
    public void testExceptionForBranchOutOfBounds() {
        String source =
            "       beq r0, r0, target\n" +
            "target: add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        // Simulate exception when branch target is out of memory
        ExecutionException ex = new ExecutionException(
            "Branch target 0x1000 is out of bounds",
            0
        );

        assertEquals(0, ex.getPC());
        assertNull(ex.getInstruction());
        assertTrue(ex.getMessage().contains("out of bounds"));
    }

    @Test
    public void testExceptionForJALRToInvalidAddress() {
        String source = "jalr r7, r1";
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        InstructionFormat instr = result.getInstructions().get(0);

        ExecutionException ex = new ExecutionException(
            "Jump target is not word-aligned",
            0,
            instr.toAssembly()
        );

        assertTrue(ex.getMessage().contains("word-aligned"));
    }

    // ========== Constructor Tests ==========

    @Test
    public void testBasicConstructor() {
        ExecutionException ex = new ExecutionException("Test error", 0x100);

        assertEquals(0x100, ex.getPC());
        assertNull(ex.getInstruction());
        assertTrue(ex.getMessage().contains("Test error"));
        assertTrue(ex.getMessage().contains("PC=0x0100"));
    }

    @Test
    public void testConstructorWithInstruction() {
        ExecutionException ex = new ExecutionException(
            "Division by zero",
            0x200,
            "DIV r1, r2, r3"
        );

        assertEquals(0x200, ex.getPC());
        assertEquals("DIV r1, r2, r3", ex.getInstruction());
        assertTrue(ex.getMessage().contains("Division by zero"));
        assertTrue(ex.getMessage().contains("PC=0x0200"));
        assertTrue(ex.getMessage().contains("DIV r1, r2, r3"));
    }

    @Test
    public void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("Underlying error");
        ExecutionException ex = new ExecutionException(
            "Memory fault",
            0x300,
            cause
        );

        assertEquals(0x300, ex.getPC());
        assertNull(ex.getInstruction());
        assertSame(cause, ex.getCause());
        assertTrue(ex.getMessage().contains("Memory fault"));
    }

    // ========== PC Formatting Tests ==========

    @Test
    public void testPCFormattingZero() {
        ExecutionException ex = new ExecutionException("Error", 0);
        assertTrue(ex.getMessage().contains("PC=0x0000"));
    }

    @Test
    public void testPCFormattingLarge() {
        ExecutionException ex = new ExecutionException("Error", 0xABCD);
        assertTrue(ex.getMessage().contains("PC=0xABCD"));
    }

    // ========== Exception Hierarchy Test ==========

    @Test
    public void testIsException() {
        ExecutionException ex = new ExecutionException("Test", 0);
        assertTrue(ex instanceof Exception);
    }

    // ========== Error Message Content Tests ==========

    @Test
    public void testMessageContainsAllInfo() {
        ExecutionException ex = new ExecutionException(
            "Illegal instruction",
            0x50,
            "ADD r8, r0, r0"
        );

        String msg = ex.getMessage();
        assertTrue("Should contain error description", msg.contains("Illegal instruction"));
        assertTrue("Should contain PC", msg.contains("0x0050") || msg.contains("0x50"));
        assertTrue("Should contain instruction", msg.contains("ADD r8"));
    }

    @Test
    public void testInstructionLimitExceeded() {
        ExecutionException ex = new ExecutionException(
            "Instruction limit exceeded (1000000 instructions)",
            0x400
        );

        assertTrue(ex.getMessage().contains("limit exceeded"));
        assertEquals(0x400, ex.getPC());
    }

    @Test
    public void testInvalidOpcodeAtRuntime() {
        ExecutionException ex = new ExecutionException(
            "Invalid opcode 0xFF at runtime",
            0x10,
            "??? (0xFF)"
        );

        assertTrue(ex.getMessage().contains("Invalid opcode"));
        assertEquals("??? (0xFF)", ex.getInstruction());
    }

    // ========== Intentionally Failing Test ==========

    @Test
    public void testMemoryFault_INTENTIONAL_FAILURE() {
        // Assemble an instruction that would cause a memory fault
        String source = "lw r1, r0, 50";
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        InstructionFormat instr = result.getInstructions().get(0);

        // Create exception for memory fault
        ExecutionException ex = new ExecutionException(
            "Memory fault: segmentation violation",
            0x0000,
            instr.toAssembly()
        );
    }
}
