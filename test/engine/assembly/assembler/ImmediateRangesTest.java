package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for the ImmediateRanges constants and validation methods
 *
 * Tests the immediate value range constraints for RiSC-16 instructions.
 */
public class ImmediateRangesTest {

    // ========== RRI Constants Tests ==========

    @Test
    public void testRRIConstants() {
        assertEquals(-64, ImmediateRanges.RRI_MIN);
        assertEquals(63, ImmediateRanges.RRI_MAX);
        assertEquals(7, ImmediateRanges.RRI_BITS);
    }

    // ========== RI Constants Tests ==========

    @Test
    public void testRIConstants() {
        assertEquals(0, ImmediateRanges.RI_MIN);
        assertEquals(1023, ImmediateRanges.RI_MAX);
        assertEquals(10, ImmediateRanges.RI_BITS);
    }

    // ========== MOVI Constants Tests ==========

    @Test
    public void testMOVIConstants() {
        assertEquals(0, ImmediateRanges.MOVI_MIN);
        assertEquals(65535, ImmediateRanges.MOVI_MAX);
    }

    // ========== Bit Manipulation Constants ==========

    @Test
    public void testBitManipulationConstants() {
        assertEquals(0x3F, ImmediateRanges.LLI_MASK);  // Lower 6 bits
        assertEquals(6, ImmediateRanges.LUI_SHIFT);
    }

    // ========== isValidRRI Tests ==========

    @Test
    public void testIsValidRRIInRange() {
        assertTrue(ImmediateRanges.isValidRRI(0));
        assertTrue(ImmediateRanges.isValidRRI(63));
        assertTrue(ImmediateRanges.isValidRRI(-64));
        assertTrue(ImmediateRanges.isValidRRI(10));
        assertTrue(ImmediateRanges.isValidRRI(-10));
    }

    @Test
    public void testIsValidRRIOutOfRange() {
        assertFalse(ImmediateRanges.isValidRRI(64));
        assertFalse(ImmediateRanges.isValidRRI(-65));
        assertFalse(ImmediateRanges.isValidRRI(100));
        assertFalse(ImmediateRanges.isValidRRI(-100));
    }

    @Test
    public void testIsValidRRIBoundary() {
        assertTrue(ImmediateRanges.isValidRRI(ImmediateRanges.RRI_MIN));
        assertTrue(ImmediateRanges.isValidRRI(ImmediateRanges.RRI_MAX));
        assertFalse(ImmediateRanges.isValidRRI(ImmediateRanges.RRI_MIN - 1));
        assertFalse(ImmediateRanges.isValidRRI(ImmediateRanges.RRI_MAX + 1));
    }

    // ========== isValidRI Tests ==========

    @Test
    public void testIsValidRIInRange() {
        assertTrue(ImmediateRanges.isValidRI(0));
        assertTrue(ImmediateRanges.isValidRI(100));
        assertTrue(ImmediateRanges.isValidRI(1023));
        assertTrue(ImmediateRanges.isValidRI(512));
    }

    @Test
    public void testIsValidRIOutOfRange() {
        assertFalse(ImmediateRanges.isValidRI(-1));
        assertFalse(ImmediateRanges.isValidRI(1024));
        assertFalse(ImmediateRanges.isValidRI(-100));
        assertFalse(ImmediateRanges.isValidRI(2000));
    }

    @Test
    public void testIsValidRIBoundary() {
        assertTrue(ImmediateRanges.isValidRI(ImmediateRanges.RI_MIN));
        assertTrue(ImmediateRanges.isValidRI(ImmediateRanges.RI_MAX));
        assertFalse(ImmediateRanges.isValidRI(ImmediateRanges.RI_MIN - 1));
        assertFalse(ImmediateRanges.isValidRI(ImmediateRanges.RI_MAX + 1));
    }

    // ========== isValidMOVI Tests ==========

    @Test
    public void testIsValidMOVIInRange() {
        assertTrue(ImmediateRanges.isValidMOVI(0));
        assertTrue(ImmediateRanges.isValidMOVI(100));
        assertTrue(ImmediateRanges.isValidMOVI(65535));
        assertTrue(ImmediateRanges.isValidMOVI(32768));
    }

    @Test
    public void testIsValidMOVIOutOfRange() {
        assertFalse(ImmediateRanges.isValidMOVI(-1));
        assertFalse(ImmediateRanges.isValidMOVI(65536));
        assertFalse(ImmediateRanges.isValidMOVI(-100));
    }

    @Test
    public void testIsValidMOVIBoundary() {
        assertTrue(ImmediateRanges.isValidMOVI(ImmediateRanges.MOVI_MIN));
        assertTrue(ImmediateRanges.isValidMOVI(ImmediateRanges.MOVI_MAX));
        assertFalse(ImmediateRanges.isValidMOVI(ImmediateRanges.MOVI_MIN - 1));
        assertFalse(ImmediateRanges.isValidMOVI(ImmediateRanges.MOVI_MAX + 1));
    }

    // ========== Bit Manipulation Tests ==========

    @Test
    public void testLLIMaskExtractsLower6Bits() {
        assertEquals(0, 0x00 & ImmediateRanges.LLI_MASK);
        assertEquals(0x3F, 0xFF & ImmediateRanges.LLI_MASK);
        assertEquals(0x15, 0x55 & ImmediateRanges.LLI_MASK);
    }

    @Test
    public void testLUIShiftExtractsUpper10Bits() {
        int value = 0xFFFF;
        int upper = value >> ImmediateRanges.LUI_SHIFT;
        assertEquals(0x3FF, upper);  // Upper 10 bits of 16-bit value
    }
}
