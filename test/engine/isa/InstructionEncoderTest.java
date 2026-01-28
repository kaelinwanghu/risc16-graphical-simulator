package engine.isa;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;

/**
 * Tests for the RiSC-16 InstructionEncoder class
 *
 * Reference: "The RiSC-16 Instruction-Set Architecture"
 *            ENEE 446: Digital Computer Design, Fall 2000
 *            Prof. Bruce Jacob
 *
 * BINARY ENCODING (16-bit, Big Endian):
 * -------------------------------------
 * RRR-type: [15:13]=opcode [12:10]=regA [9:7]=regB [6:3]=0000 [2:0]=regC
 * RRI-type: [15:13]=opcode [12:10]=regA [9:7]=regB [6:0]=immediate (7-bit signed)
 * RI-type:  [15:13]=opcode [12:10]=regA [9:0]=immediate (10-bit unsigned)
 */
public class InstructionEncoderTest {

    // ========== Assembly-Based Encoding Tests ==========

    @Test
    public void testAssembleAndEncode_ADD() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // ADD r1, r2, r3: opcode=000, rA=001, rB=010, unused=0000, rC=011
        // Binary: 000 001 010 0000 011 = 0x0503
        assertEquals(0x0503, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_NAND() {
        String source = "nand r4, r5, r6";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // NAND r4, r5, r6: opcode=010, rA=100, rB=101, unused=0000, rC=110
        // Binary: 010 100 101 0000 110 = 0x5286
        assertEquals(0x5286, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_ADDI_Positive() {
        String source = "addi r1, r2, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // ADDI r1, r2, 5: opcode=001, rA=001, rB=010, imm=0000101
        // Binary: 001 001 010 0000101 = 0x2505
        assertEquals(0x2505, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_ADDI_Negative() {
        String source = "addi r1, r2, -1";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // ADDI r1, r2, -1: opcode=001, rA=001, rB=010, imm=1111111 (7-bit -1)
        // Binary: 001 001 010 1111111 = 0x257F
        assertEquals(0x257F, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_LUI() {
        String source = "lui r1, 100";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // LUI r1, 100: opcode=011, rA=001, imm=0001100100
        // Binary: 011 001 0001100100 = 0x6464
        assertEquals(0x6464, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_LW() {
        String source = "lw r3, r0, 10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // LW r3, r0, 10: opcode=101, rA=011, rB=000, imm=0001010
        // Binary: 101 011 000 0001010 = 0xAC0A
        assertEquals(0xAC0A, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_SW() {
        String source = "sw r1, r2, -5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // SW r1, r2, -5: opcode=100, rA=001, rB=010, imm=1111011 (7-bit -5)
        // Binary: 100 001 010 1111011 = 0x857B
        assertEquals(0x857B, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_BEQ() {
        String source =
            "       beq r0, r1, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // BEQ r0, r1, 0: opcode=110, rA=000, rB=001, imm=0000000
        // (skip is at PC+1, so offset = 0)
        // Binary: 110 000 001 0000000 = 0xC080
        assertEquals(0xC080, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_JALR() {
        String source = "jalr r7, r1";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // JALR r7, r1: opcode=111, rA=111, rB=001, imm=0000000
        // Binary: 111 111 001 0000000 = 0xFC80
        assertEquals(0xFC80, encoded & 0xFFFF);
    }

    @Test
    public void testAssembleAndEncode_NOP() {
        // NOP is add r0, r0, r0
        String source = "add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        InstructionFormat instr = result.getInstructions().get(0);
        short encoded = InstructionEncoder.encode(instr);

        // ADD r0, r0, r0: all zeros
        assertEquals(0x0000, encoded & 0xFFFF);
    }

    // ========== Assembly-Based Round-Trip Tests ==========

    @Test
    public void testAssembleEncodeDecodeRoundTrip_AllInstructions() {
        String source =
            "       add r1, r2, r3\n" +
            "       addi r4, r5, 63\n" +
            "       nand r6, r7, r0\n" +
            "       lui r1, 1023\n" +
            "       sw r2, r3, -64\n" +
            "       lw r4, r5, 10\n" +
            "       beq r0, r1, end\n" +
            "end:   jalr r7, r6";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        assertEquals(8, result.getInstructionCount());

        // Encode and decode each instruction, verify round-trip
        for (InstructionFormat original : result.getInstructions()) {
            short encoded = InstructionEncoder.encode(original);
            InstructionFormat decoded = InstructionEncoder.decode(encoded, original.getAddress());

            assertEquals("Round-trip failed for " + original.getOpcode(),
                original.getOpcode(), decoded.getOpcode());
            assertEquals(original.getRegA(), decoded.getRegA());

            if (original.getFormat() != FormatType.RI) {
                assertEquals(original.getRegB(), decoded.getRegB());
            }
            if (original.getFormat() == FormatType.RRR) {
                assertEquals(original.getRegC(), decoded.getRegC());
            }
            if (original.getFormat() != FormatType.RRR) {
                assertEquals(original.getImmediate(), decoded.getImmediate());
            }
        }
    }

    // ========== Direct Encode/Decode Tests ==========

    @Test
    public void testEncode_ADD() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        short encoded = InstructionEncoder.encode(instr);
        assertEquals(0x0503, encoded & 0xFFFF);
    }

    @Test
    public void testEncode_LUI_MaxImmediate() {
        InstructionFormat instr = InstructionFormat.createRI(Opcode.LUI, 7, 0x3FF, 0);
        short encoded = InstructionEncoder.encode(instr);
        // LUI r7, 0x3FF: opcode=011, rA=111, imm=1111111111
        // Binary: 011 111 1111111111 = 0x7FFF
        assertEquals(0x7FFF, encoded & 0xFFFF);
    }

    @Test
    public void testDecode_ADD() {
        InstructionFormat decoded = InstructionEncoder.decode((short) 0x0503, 0);

        assertEquals(Opcode.ADD, decoded.getOpcode());
        assertEquals(1, decoded.getRegA());
        assertEquals(2, decoded.getRegB());
        assertEquals(3, decoded.getRegC());
    }

    @Test
    public void testDecode_ADDI_Negative() {
        // 0x257F has immediate 0x7F = 127 unsigned, but as 7-bit signed = -1
        InstructionFormat decoded = InstructionEncoder.decode((short) 0x257F, 0);

        assertEquals(Opcode.ADDI, decoded.getOpcode());
        assertEquals(-1, decoded.getImmediate());
    }

    @Test
    public void testDecode_LUI() {
        InstructionFormat decoded = InstructionEncoder.decode((short) 0x6464, 0);

        assertEquals(Opcode.LUI, decoded.getOpcode());
        assertEquals(1, decoded.getRegA());
        assertEquals(100, decoded.getImmediate());
    }

    // ========== Byte Array Decoding ==========

    @Test
    public void testDecodeFromBytes() {
        // ADD r1, r2, r3 = 0x0503 -> bytes [0x05, 0x03] (big-endian)
        InstructionFormat decoded = InstructionEncoder.decode((byte) 0x05, (byte) 0x03, 0);

        assertEquals(Opcode.ADD, decoded.getOpcode());
        assertEquals(1, decoded.getRegA());
        assertEquals(2, decoded.getRegB());
        assertEquals(3, decoded.getRegC());
    }

    @Test
    public void testDecodeFromByteArray() {
        byte[] bytes = {0x05, 0x03, 0x25, 0x05};  // ADD r1,r2,r3 followed by ADDI r1,r2,5

        InstructionFormat first = InstructionEncoder.decode(bytes, 0, 0);
        assertEquals(Opcode.ADD, first.getOpcode());

        InstructionFormat second = InstructionEncoder.decode(bytes, 2, 2);
        assertEquals(Opcode.ADDI, second.getOpcode());
    }

    // ========== String Representations ==========

    @Test
    public void testEncodeToBinaryString() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        String binary = InstructionEncoder.encodeToBinaryString(instr);

        assertEquals(16, binary.length());
        assertEquals("0000010100000011", binary);
    }

    @Test
    public void testEncodeToHexString() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        String hex = InstructionEncoder.encodeToHexString(instr);

        assertEquals("0x0503", hex);
    }

    // ========== Validation ==========

    @Test
    public void testIsValidInstruction() {
        assertTrue(InstructionEncoder.isValidInstruction((short) 0x0503));  // Valid ADD
        assertTrue(InstructionEncoder.isValidInstruction((short) 0x6464));  // Valid LUI
    }

    @Test
    public void testIsValidInstruction_InvalidPadding() {
        // ADD with non-zero padding bits should be invalid
        // 000 001 010 0001 011 = padding bits [6:3] = 0001 instead of 0000
        assertFalse(InstructionEncoder.isValidInstruction((short) 0x050B));
    }
}
