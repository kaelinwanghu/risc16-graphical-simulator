package engine.isa;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for the RiSC-16 InstructionFormat class
 *
 * InstructionFormat is an immutable representation of a decoded instruction.
 * It contains:
 * - Opcode (ADD, ADDI, NAND, LUI, SW, LW, BEQ, JALR)
 * - Register fields (regA, regB, regC) - values 0-7
 * - Immediate field (7-bit signed for RRI, 10-bit unsigned for RI)
 * - Address of the instruction in memory
 */
public class InstructionFormatTest {

    // ========== RRR-type Instructions ==========

    @Test
    public void testCreateRRR_ADD() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        assertEquals(Opcode.ADD, instr.getOpcode());
        assertEquals(FormatType.RRR, instr.getFormat());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(3, instr.getRegC());
        assertEquals(0, instr.getAddress());
    }

    @Test
    public void testCreateRRR_NAND() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 10);

        assertEquals(Opcode.NAND, instr.getOpcode());
        assertEquals(FormatType.RRR, instr.getFormat());
        assertEquals(4, instr.getRegA());
        assertEquals(5, instr.getRegB());
        assertEquals(6, instr.getRegC());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRRR_WrongFormat() {
        // ADDI is RRI-type, not RRR
        InstructionFormat.createRRR(Opcode.ADDI, 1, 2, 3, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRRR_InvalidRegister() {
        // Register 8 is invalid (only 0-7)
        InstructionFormat.createRRR(Opcode.ADD, 8, 2, 3, 0);
    }

    // ========== RRI-type Instructions ==========

    @Test
    public void testCreateRRI_ADDI() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, 10, 0);

        assertEquals(Opcode.ADDI, instr.getOpcode());
        assertEquals(FormatType.RRI, instr.getFormat());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(10, instr.getImmediate());
    }

    @Test
    public void testCreateRRI_NegativeImmediate() {
        // 7-bit signed: -64 to 63
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, -64, 0);
        assertEquals(-64, instr.getImmediate());

        instr = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, 63, 0);
        assertEquals(63, instr.getImmediate());
    }

    @Test
    public void testCreateRRI_LW() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.LW, 1, 0, 5, 0);

        assertEquals(Opcode.LW, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(0, instr.getRegB());
        assertEquals(5, instr.getImmediate());
    }

    @Test
    public void testCreateRRI_SW() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.SW, 3, 4, -10, 0);

        assertEquals(Opcode.SW, instr.getOpcode());
        assertEquals(-10, instr.getImmediate());
    }

    @Test
    public void testCreateRRI_BEQ() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.BEQ, 0, 1, 5, 0);

        assertEquals(Opcode.BEQ, instr.getOpcode());
        assertEquals(5, instr.getImmediate());
    }

    @Test
    public void testCreateRRI_JALR() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.JALR, 7, 1, 0, 0);

        assertEquals(Opcode.JALR, instr.getOpcode());
        assertEquals(7, instr.getRegA());
        assertEquals(1, instr.getRegB());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRRI_WrongFormat() {
        // ADD is RRR-type, not RRI
        InstructionFormat.createRRI(Opcode.ADD, 1, 2, 10, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRRI_ImmediateTooLarge() {
        // 7-bit signed max is 63
        InstructionFormat.createRRI(Opcode.ADDI, 1, 2, 64, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRRI_ImmediateTooSmall() {
        // 7-bit signed min is -64
        InstructionFormat.createRRI(Opcode.ADDI, 1, 2, -65, 0);
    }

    // ========== RI-type Instructions ==========

    @Test
    public void testCreateRI_LUI() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 1, 0x3FF, 0);

        assertEquals(Opcode.LUI, instr.getOpcode());
        assertEquals(FormatType.RI, instr.getFormat());
        assertEquals(1, instr.getRegA());
        assertEquals(0x3FF, instr.getImmediate());  // Max 10-bit unsigned
    }

    @Test
    public void testCreateRI_LUI_Zero() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 0, 0, 0);

        assertEquals(Opcode.LUI, instr.getOpcode());
        assertEquals(0, instr.getImmediate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRI_WrongFormat() {
        // ADD is RRR-type, not RI
        InstructionFormat.createRI(Opcode.ADD, 1, 100, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRI_ImmediateTooLarge() {
        // 10-bit unsigned max is 1023
        InstructionFormat.createRI(Opcode.LUI, 1, 1024, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRI_NegativeImmediate() {
        // RI-type uses unsigned immediate
        InstructionFormat.createRI(Opcode.LUI, 1, -1, 0);
    }

    // ========== Field Access Restrictions ==========

    @Test(expected = IllegalStateException.class)
    public void testRRR_NoImmediate() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        instr.getImmediate();  // Should throw - RRR has no immediate
    }

    @Test(expected = IllegalStateException.class)
    public void testRI_NoRegB() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 1, 100, 0);
        instr.getRegB();  // Should throw - RI has no regB
    }

    @Test(expected = IllegalStateException.class)
    public void testRI_NoRegC() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 1, 100, 0);
        instr.getRegC();  // Should throw - only RRR has regC
    }

    @Test(expected = IllegalStateException.class)
    public void testRRI_NoRegC() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, 10, 0);
        instr.getRegC();  // Should throw - only RRR has regC
    }

    // ========== Assembly String ==========

    @Test
    public void testToAssembly_ADD() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        assertEquals("ADD r1, r2, r3", instr.toAssembly());
    }

    @Test
    public void testToAssembly_ADDI() {
        InstructionFormat instr = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, -5, 0);
        assertEquals("ADDI r1, r2, -5", instr.toAssembly());
    }

    @Test
    public void testToAssembly_LUI() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 3, 100, 0);
        assertEquals("LUI r3, 100", instr.toAssembly());
    }

    // ========== Equality ==========

    @Test
    public void testEquals() {
        InstructionFormat a = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat b = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEquals_DifferentOpcode() {
        InstructionFormat a = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat b = InstructionFormat.createRRR(Opcode.NAND, 1, 2, 3, 0);

        assertNotEquals(a, b);
    }

    @Test
    public void testNotEquals_DifferentRegisters() {
        InstructionFormat a = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat b = InstructionFormat.createRRR(Opcode.ADD, 4, 2, 3, 0);

        assertNotEquals(a, b);
    }
}
