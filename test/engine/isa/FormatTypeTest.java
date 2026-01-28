package engine.isa;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;

/**
 * Tests for the RiSC-16 FormatType enum
 *
 * THREE INSTRUCTION FORMATS:
 * --------------------------
 * RRR-type: [opcode:3][regA:3][regB:3][unused:4][regC:3]
 *           Used by: ADD, NAND
 *
 * RRI-type: [opcode:3][regA:3][regB:3][signed imm:7]
 *           Immediate range: -64 to 63
 *           Used by: ADDI, SW, LW, BEQ, JALR
 *
 * RI-type:  [opcode:3][regA:3][unsigned imm:10]
 *           Immediate range: 0 to 1023 (0x3FF)
 *           Used by: LUI
 */
public class FormatTypeTest {

    // ========== Assembly-Based Format Tests ==========

    @Test
    public void testAssemble_RRR_ADD() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRR, result.getInstructions().get(0).getFormat());
        assertEquals("Register-Register-Register", result.getInstructions().get(0).getFormat().getDescription());
    }

    @Test
    public void testAssemble_RRR_NAND() {
        String source = "nand r4, r5, r6";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRR, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssemble_RRI_ADDI() {
        String source = "addi r1, r2, 10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
        assertEquals("Register-Register-Immediate", result.getInstructions().get(0).getFormat().getDescription());
    }

    @Test
    public void testAssemble_RRI_LW() {
        String source = "lw r1, r0, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssemble_RRI_SW() {
        String source = "sw r1, r2, -3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssemble_RRI_BEQ() {
        String source =
            "       beq r0, r1, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssemble_RRI_JALR() {
        String source = "jalr r7, r1";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RRI, result.getInstructions().get(0).getFormat());
    }

    @Test
    public void testAssemble_RI_LUI() {
        String source = "lui r1, 100";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(FormatType.RI, result.getInstructions().get(0).getFormat());
        assertEquals("Register-Immediate", result.getInstructions().get(0).getFormat().getDescription());
    }

    @Test
    public void testAssembleAllFormats() {
        String source =
            "add r1, r2, r3\n" +    // RRR
            "nand r4, r5, r6\n" +   // RRR
            "addi r1, r2, 10\n" +   // RRI
            "lw r1, r0, 5\n" +      // RRI
            "sw r2, r3, -1\n" +     // RRI
            "lui r7, 500";          // RI

        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(6, result.getInstructionCount());

        // RRR instructions
        assertEquals(FormatType.RRR, result.getInstructions().get(0).getFormat());
        assertEquals(FormatType.RRR, result.getInstructions().get(1).getFormat());

        // RRI instructions
        assertEquals(FormatType.RRI, result.getInstructions().get(2).getFormat());
        assertEquals(FormatType.RRI, result.getInstructions().get(3).getFormat());
        assertEquals(FormatType.RRI, result.getInstructions().get(4).getFormat());

        // RI instruction
        assertEquals(FormatType.RI, result.getInstructions().get(5).getFormat());
    }

    // ========== Direct FormatType Tests ==========

    @Test
    public void testThreeFormats() {
        assertEquals("RiSC-16 has exactly 3 format types", 3, FormatType.values().length);
    }

    @Test
    public void testFormatTypesExist() {
        assertNotNull(FormatType.RRR);
        assertNotNull(FormatType.RRI);
        assertNotNull(FormatType.RI);
    }

    @Test
    public void testRRRDescription() {
        assertEquals("Register-Register-Register", FormatType.RRR.getDescription());
    }

    @Test
    public void testRRIDescription() {
        assertEquals("Register-Register-Immediate", FormatType.RRI.getDescription());
    }

    @Test
    public void testRIDescription() {
        assertEquals("Register-Immediate", FormatType.RI.getDescription());
    }

    // ========== Opcode to Format Mapping ==========

    @Test
    public void testOpcodeFormatMapping_RRR() {
        assertEquals(FormatType.RRR, Opcode.ADD.getFormat());
        assertEquals(FormatType.RRR, Opcode.NAND.getFormat());
    }

    @Test
    public void testOpcodeFormatMapping_RRI() {
        assertEquals(FormatType.RRI, Opcode.ADDI.getFormat());
        assertEquals(FormatType.RRI, Opcode.SW.getFormat());
        assertEquals(FormatType.RRI, Opcode.LW.getFormat());
        assertEquals(FormatType.RRI, Opcode.BEQ.getFormat());
        assertEquals(FormatType.RRI, Opcode.JALR.getFormat());
    }

    @Test
    public void testOpcodeFormatMapping_RI() {
        assertEquals(FormatType.RI, Opcode.LUI.getFormat());
    }
}
