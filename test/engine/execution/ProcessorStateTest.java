package engine.execution;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for the ProcessorState class
 *
 * ProcessorState is an immutable snapshot of the RiSC-16 processor state:
 * - 8 general-purpose registers (R0-R7)
 * - Program counter (PC)
 * - Halted flag
 * - Instruction count
 *
 * Key invariants:
 * - R0 is always 0 (hardwired)
 * - Immutable - modifications return new instances
 * - Builder pattern for construction
 */
public class ProcessorStateTest {

    // ========== Construction Tests ==========

    @Test
    public void testCreateViaBuilder() {
        ProcessorState state = ProcessorState.builder()
            .setPC(0x100)
            .setRegister(1, (short) 42)
            .build();

        assertEquals(0x100, state.getPC());
        assertEquals(42, state.getRegister(1));
    }

    @Test
    public void testBuilderDefaults() {
        ProcessorState state = ProcessorState.builder().build();

        // All registers should be 0 by default
        for (int i = 0; i < 8; i++) {
            assertEquals("Register " + i + " should be 0", 0, state.getRegister(i));
        }
        assertEquals(0, state.getPC());
        assertFalse(state.isHalted());
        assertEquals(0, state.getInstructionCount());
    }

    @Test
    public void testConstructorWithArrays() {
        short[] regs = {0, 1, 2, 3, 4, 5, 6, 7};
        ProcessorState state = new ProcessorState(regs, 0x50, false, 100);

        assertEquals(0x50, state.getPC());
        assertEquals(100, state.getInstructionCount());
        for (int i = 0; i < 8; i++) {
            assertEquals((short) i, state.getRegister(i));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullRegisters() {
        new ProcessorState(null, 0, false, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWrongRegisterCount() {
        short[] regs = {0, 1, 2, 3};  // Only 4 registers
        new ProcessorState(regs, 0, false, 0);
    }

    // ========== Register Tests ==========

    @Test
    public void testRegister0AlwaysZero() {
        ProcessorState state = ProcessorState.builder()
            .setRegister(0, (short) 999)  // Try to set R0
            .build();

        assertEquals("R0 must always be 0", 0, state.getRegister(0));
    }

    @Test
    public void testSetAllRegisters() {
        ProcessorState.Builder builder = ProcessorState.builder();
        for (int i = 1; i < 8; i++) {
            builder.setRegister(i, (short) (i * 10));
        }
        ProcessorState state = builder.build();

        assertEquals(0, state.getRegister(0));  // R0 always 0
        for (int i = 1; i < 8; i++) {
            assertEquals((short) (i * 10), state.getRegister(i));
        }
    }

    @Test
    public void testGetRegistersReturnsCopy() {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .build();

        short[] regs1 = state.getRegisters();
        short[] regs2 = state.getRegisters();

        assertNotSame("Should return different array instances", regs1, regs2);
        assertArrayEquals(regs1, regs2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRegisterInvalidHigh() {
        ProcessorState state = ProcessorState.builder().build();
        state.getRegister(8);  // Invalid
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRegisterInvalidNegative() {
        ProcessorState state = ProcessorState.builder().build();
        state.getRegister(-1);  // Invalid
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetRegisterInvalidHigh() {
        ProcessorState.builder().setRegister(8, (short) 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetRegisterInvalidNegative() {
        ProcessorState.builder().setRegister(-1, (short) 0);
    }

    // ========== PC Tests ==========

    @Test
    public void testSetPC() {
        ProcessorState state = ProcessorState.builder()
            .setPC(0xABCD)
            .build();

        assertEquals(0xABCD, state.getPC());
    }

    @Test
    public void testIncrementPC() {
        ProcessorState state = ProcessorState.builder()
            .setPC(0x100)
            .incrementPC(2)
            .build();

        assertEquals(0x102, state.getPC());
    }

    @Test
    public void testIncrementPCMultipleTimes() {
        ProcessorState state = ProcessorState.builder()
            .setPC(0)
            .incrementPC(2)
            .incrementPC(2)
            .incrementPC(2)
            .build();

        assertEquals(6, state.getPC());
    }

    // ========== Halted Tests ==========

    @Test
    public void testSetHalted() {
        ProcessorState running = ProcessorState.builder().build();
        ProcessorState halted = ProcessorState.builder()
            .setHalted(true)
            .build();

        assertFalse(running.isHalted());
        assertTrue(halted.isHalted());
    }

    // ========== Instruction Count Tests ==========

    @Test
    public void testSetInstructionCount() {
        ProcessorState state = ProcessorState.builder()
            .setInstructionCount(1000)
            .build();

        assertEquals(1000, state.getInstructionCount());
    }

    @Test
    public void testIncrementInstructions() {
        ProcessorState state = ProcessorState.builder()
            .incrementInstructions()
            .incrementInstructions()
            .incrementInstructions()
            .build();

        assertEquals(3, state.getInstructionCount());
    }

    // ========== Immutability Tests ==========

    @Test
    public void testImmutability() {
        ProcessorState original = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setPC(0x50)
            .build();

        // Create modified copy via toBuilder
        ProcessorState modified = original.toBuilder()
            .setRegister(1, (short) 200)
            .setPC(0x60)
            .build();

        // Original should be unchanged
        assertEquals(100, original.getRegister(1));
        assertEquals(0x50, original.getPC());

        // Modified should have new values
        assertEquals(200, modified.getRegister(1));
        assertEquals(0x60, modified.getPC());
    }

    @Test
    public void testToBuilderPreservesState() {
        ProcessorState original = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .setPC(0x100)
            .setHalted(true)
            .setInstructionCount(500)
            .build();

        // Create copy without modifications
        ProcessorState copy = original.toBuilder().build();

        assertEquals(original.getRegister(1), copy.getRegister(1));
        assertEquals(original.getRegister(2), copy.getRegister(2));
        assertEquals(original.getPC(), copy.getPC());
        assertEquals(original.isHalted(), copy.isHalted());
        assertEquals(original.getInstructionCount(), copy.getInstructionCount());
    }

    @Test
    public void testRegisterArrayImmutable() {
        short[] regs = {0, 100, 0, 0, 0, 0, 0, 0};
        ProcessorState state = new ProcessorState(regs, 0, false, 0);

        // Modify original array
        regs[1] = 999;

        // State should be unchanged
        assertEquals(100, state.getRegister(1));
    }

    // ========== ToString Test ==========

    @Test
    public void testToString() {
        ProcessorState state = ProcessorState.builder()
            .setPC(0x100)
            .setRegister(1, (short) 42)
            .setInstructionCount(10)
            .build();

        String str = state.toString();
        assertTrue(str.contains("ProcessorState"));
        assertTrue(str.contains("PC"));
        assertTrue(str.contains("Registers"));
    }

    // ========== Edge Case Tests ==========

    @Test
    public void testNegativeRegisterValues() {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) -1)  // 0xFFFF
            .setRegister(2, (short) -32768)  // 0x8000
            .build();

        assertEquals((short) -1, state.getRegister(1));
        assertEquals((short) -32768, state.getRegister(2));
    }

    @Test
    public void testMaxValues() {
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, Short.MAX_VALUE)
            .setRegister(2, Short.MIN_VALUE)
            .setPC(0xFFFF)
            .setInstructionCount(Long.MAX_VALUE)
            .build();

        assertEquals(Short.MAX_VALUE, state.getRegister(1));
        assertEquals(Short.MIN_VALUE, state.getRegister(2));
        assertEquals(0xFFFF, state.getPC());
        assertEquals(Long.MAX_VALUE, state.getInstructionCount());
    }
}
