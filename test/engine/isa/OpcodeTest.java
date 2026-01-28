package engine.isa;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;

/**
 * Tests for the RiSC-16 Opcode enum
 *
 * Reference: "The RiSC-16 Instruction-Set Architecture"
 *            ENEE 446: Digital Computer Design, Fall 2000
 *            Prof. Bruce Jacob
 *
 * OPCODES:
 * ---------
 * 000 - ADD   (RRR)  Add contents of regB with regC, store in regA
 * 001 - ADDI  (RRI)  Add contents of regB with imm, store in regA
 * 010 - NAND  (RRR)  NAND contents of regB with regC, store in regA
 * 011 - LUI   (RI)   Load upper immediate (top 10 bits)
 * 100 - SW    (RRI)  Store word: Mem[regB + imm] <- regA
 * 101 - LW    (RRI)  Load word: regA <- Mem[regB + imm]
 * 110 - BEQ   (RRI)  Branch if equal: if regA == regB, PC <- PC+1+imm
 * 111 - JALR  (RRI)  Jump and link: PC <- regB, regA <- PC+1
 */
public class OpcodeTest {

    // ========== Assembly-Based Opcode Tests ==========

    @Test
    public void testAssembleADD() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.ADD, result.getInstructions().get(0).getOpcode());
        assertEquals(0b000, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRR, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleADDI() {
        String source = "addi r1, r2, 10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.ADDI, result.getInstructions().get(0).getOpcode());
        assertEquals(0b001, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleNAND() {
        String source = "nand r4, r5, r6";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.NAND, result.getInstructions().get(0).getOpcode());
        assertEquals(0b010, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRR, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleLUI() {
        String source = "lui r1, 100";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.LUI, result.getInstructions().get(0).getOpcode());
        assertEquals(0b011, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleSW() {
        String source = "sw r1, r2, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.SW, result.getInstructions().get(0).getOpcode());
        assertEquals(0b100, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleLW() {
        String source = "lw r1, r2, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.LW, result.getInstructions().get(0).getOpcode());
        assertEquals(0b101, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleBEQ() {
        String source =
            "       beq r0, r1, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.BEQ, result.getInstructions().get(0).getOpcode());
        assertEquals(0b110, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleJALR() {
        String source = "jalr r7, r1";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(Opcode.JALR, result.getInstructions().get(0).getOpcode());
        assertEquals(0b111, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssembleAllOpcodes() {
        // Program with all 8 RiSC-16 instructions
        String source =
            "       add r1, r2, r3\n" +   // 000 - ADD
            "       addi r1, r2, 1\n" +   // 001 - ADDI
            "       nand r1, r2, r3\n" +  // 010 - NAND
            "       lui r1, 100\n" +      // 011 - LUI
            "       sw r1, r2, 0\n" +     // 100 - SW
            "       lw r1, r2, 0\n" +     // 101 - LW
            "       beq r0, r0, end\n" +  // 110 - BEQ
            "end:   jalr r0, r1";         // 111 - JALR

        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(8, result.getInstructionCount());

        // Verify each opcode in order
        assertEquals(Opcode.ADD, result.getInstructions().get(0).getOpcode());
        assertEquals(Opcode.ADDI, result.getInstructions().get(1).getOpcode());
        assertEquals(Opcode.NAND, result.getInstructions().get(2).getOpcode());
        assertEquals(Opcode.LUI, result.getInstructions().get(3).getOpcode());
        assertEquals(Opcode.SW, result.getInstructions().get(4).getOpcode());
        assertEquals(Opcode.LW, result.getInstructions().get(5).getOpcode());
        assertEquals(Opcode.BEQ, result.getInstructions().get(6).getOpcode());
        assertEquals(Opcode.JALR, result.getInstructions().get(7).getOpcode());

        // Verify opcode binary values
        assertEquals(0b000, result.getInstructions().get(0).getOpcode().getCode());
        assertEquals(0b001, result.getInstructions().get(1).getOpcode().getCode());
        assertEquals(0b010, result.getInstructions().get(2).getOpcode().getCode());
        assertEquals(0b011, result.getInstructions().get(3).getOpcode().getCode());
        assertEquals(0b100, result.getInstructions().get(4).getOpcode().getCode());
        assertEquals(0b101, result.getInstructions().get(5).getOpcode().getCode());
        assertEquals(0b110, result.getInstructions().get(6).getOpcode().getCode());
        assertEquals(0b111, result.getInstructions().get(7).getOpcode().getCode());
    }

    // ========== Opcode Enum Direct Tests ==========

    @Test
    public void testOpcodeValues() {
        assertEquals(0b000, Opcode.ADD.getCode());
        assertEquals(0b001, Opcode.ADDI.getCode());
        assertEquals(0b010, Opcode.NAND.getCode());
        assertEquals(0b011, Opcode.LUI.getCode());
        assertEquals(0b100, Opcode.SW.getCode());
        assertEquals(0b101, Opcode.LW.getCode());
        assertEquals(0b110, Opcode.BEQ.getCode());
        assertEquals(0b111, Opcode.JALR.getCode());
    }

    @Test
    public void testFromCode() {
        assertEquals(Opcode.ADD, Opcode.fromCode(0));
        assertEquals(Opcode.ADDI, Opcode.fromCode(1));
        assertEquals(Opcode.NAND, Opcode.fromCode(2));
        assertEquals(Opcode.LUI, Opcode.fromCode(3));
        assertEquals(Opcode.SW, Opcode.fromCode(4));
        assertEquals(Opcode.LW, Opcode.fromCode(5));
        assertEquals(Opcode.BEQ, Opcode.fromCode(6));
        assertEquals(Opcode.JALR, Opcode.fromCode(7));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromCodeInvalid() {
        Opcode.fromCode(8);
    }

    @Test
    public void testFromMnemonic() {
        assertEquals(Opcode.ADD, Opcode.fromMnemonic("add"));
        assertEquals(Opcode.ADDI, Opcode.fromMnemonic("addi"));
        assertEquals(Opcode.NAND, Opcode.fromMnemonic("nand"));
        assertEquals(Opcode.LUI, Opcode.fromMnemonic("lui"));
        assertEquals(Opcode.SW, Opcode.fromMnemonic("sw"));
        assertEquals(Opcode.LW, Opcode.fromMnemonic("lw"));
        assertEquals(Opcode.BEQ, Opcode.fromMnemonic("beq"));
        assertEquals(Opcode.JALR, Opcode.fromMnemonic("jalr"));
    }

    @Test
    public void testFromMnemonicCaseInsensitive() {
        assertEquals(Opcode.ADD, Opcode.fromMnemonic("ADD"));
        assertEquals(Opcode.ADD, Opcode.fromMnemonic("Add"));
        assertEquals(Opcode.LW, Opcode.fromMnemonic("LW"));
    }

    @Test
    public void testFromMnemonicInvalid() {
        assertNull(Opcode.fromMnemonic("invalid"));
        assertNull(Opcode.fromMnemonic("sub"));
    }

    @Test
    public void testAllEightOpcodes() {
        assertEquals("RiSC-16 has exactly 8 opcodes", 8, Opcode.values().length);
    }
}
