package engine.assembly.assembler;

import static org.junit.Assert.*;

import org.junit.Test;

import engine.assembly.AssemblyError;
import engine.assembly.AssemblyResult;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;

public class AssemblerTest {

    // ========== Basic Instruction Tests ==========

    @Test
    public void testAssembleAdd() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals("Should have 1 instruction", 1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.ADD, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(3, instr.getRegC());
    }

    @Test
    public void testAssembleAddi() {
        String source = "addi r1, r2, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.ADDI, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(5, instr.getImmediate());
    }

    @Test
    public void testAssembleNand() {
        String source = "nand r4, r5, r6";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.NAND, instr.getOpcode());
        assertEquals(4, instr.getRegA());
        assertEquals(5, instr.getRegB());
        assertEquals(6, instr.getRegC());
    }

    @Test
    public void testAssembleLui() {
        String source = "lui r1, 100";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.LUI, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(100, instr.getImmediate());
    }

    @Test
    public void testAssembleLw() {
        String source = "lw r1, r2, 10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.LW, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(10, instr.getImmediate());
    }

    @Test
    public void testAssembleSw() {
        String source = "sw r1, r2, -5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.SW, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
        assertEquals(-5, instr.getImmediate());
    }

    @Test
    public void testAssembleJalr() {
        String source = "jalr r1, r2";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(1, result.getInstructionCount());

        InstructionFormat instr = result.getInstructions().get(0);
        assertEquals(Opcode.JALR, instr.getOpcode());
        assertEquals(1, instr.getRegA());
        assertEquals(2, instr.getRegB());
    }

    // ========== Multiple Instructions ==========

    @Test
    public void testMultipleInstructions() {
        String source =
            "add r1, r2, r3\n" +
            "addi r4, r5, 10\n" +
            "nand r6, r7, r0";

        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals("Should have 3 instructions", 3, result.getInstructionCount());

        assertEquals(Opcode.ADD, result.getInstructions().get(0).getOpcode());
        assertEquals(Opcode.ADDI, result.getInstructions().get(1).getOpcode());
        assertEquals(Opcode.NAND, result.getInstructions().get(2).getOpcode());
    }

    // ========== Label Tests ==========

    @Test
    public void testLabelDefinition() {
        String source =
            "start: add r1, r2, r3\n" +
            "       addi r4, r5, 1";

        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(2, result.getInstructionCount());
        assertTrue("Symbol table should contain 'start'",
            result.getSymbolTable().contains("start"));
    }

    @Test
    public void testBeqWithLabel() {
        String source =
            "       beq r0, r0, skip\n" +
            "       add r1, r2, r3\n" +
            "skip:  addi r4, r5, 0";

        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(3, result.getInstructionCount());

        // First instruction is BEQ with forward reference
        InstructionFormat beq = result.getInstructions().get(0);
        assertEquals(Opcode.BEQ, beq.getOpcode());
    }

    // ========== Comment Tests ==========

    @Test
    public void testCommentsIgnored() {
        // Note: This assembler uses # for comments, not ;
        String source =
            "# This is a comment\n" +
            "add r1, r2, r3  # inline comment\n" +
            "# Another comment\n" +
            "addi r4, r5, 1";

        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals("Should have 2 instructions (comments ignored)", 2, result.getInstructionCount());
    }

    // ========== Error Tests ==========

    @Test
    public void testEmptyProgramError() {
        String source = "";
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
        assertEquals(AssemblyError.ErrorType.EMPTY_PROGRAM,
            result.getErrors().get(0).getErrorType());
    }

    @Test
    public void testCommentsOnlyError() {
        String source = "; Just a comment\n; Another comment";
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail for comments-only program", result.isSuccess());
    }

    @Test
    public void testInvalidOpcodeError() {
        String source = "invalid r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
    }

    @Test
    public void testInvalidRegisterError() {
        String source = "add r8, r2, r3";  // r8 is invalid (only r0-r7)
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
    }

    @Test
    public void testUndefinedLabelError() {
        String source = "beq r0, r1, undefined_label";
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
        assertEquals(AssemblyError.ErrorType.UNDEFINED_LABEL,
            result.getErrors().get(0).getErrorType());
    }

    @Test
    public void testDuplicateLabelError() {
        String source =
            "label: add r1, r2, r3\n" +
            "label: addi r4, r5, 1";  // Duplicate label

        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
        assertEquals(AssemblyError.ErrorType.DUPLICATE_LABEL,
            result.getErrors().get(0).getErrorType());
    }

    @Test
    public void testWrongOperandCountError() {
        String source = "add r1, r2";  // Missing third operand
        AssemblyResult result = Assembler.assemble(source);

        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have errors", result.getErrors().isEmpty());
    }

    // ========== Case Insensitivity Tests ==========

    @Test
    public void testCaseInsensitiveOpcode() {
        String source1 = "ADD r1, r2, r3";
        String source2 = "add r1, r2, r3";
        String source3 = "Add r1, r2, r3";

        AssemblyResult result1 = Assembler.assemble(source1);
        AssemblyResult result2 = Assembler.assemble(source2);
        AssemblyResult result3 = Assembler.assemble(source3);

        assertTrue("Uppercase should work", result1.isSuccess());
        assertTrue("Lowercase should work", result2.isSuccess());
        assertTrue("Mixed case should work", result3.isSuccess());
    }

    @Test
    public void testCaseInsensitiveRegisters() {
        String source1 = "add R1, R2, R3";
        String source2 = "add r1, r2, r3";

        AssemblyResult result1 = Assembler.assemble(source1);
        AssemblyResult result2 = Assembler.assemble(source2);

        assertTrue("Uppercase registers should work", result1.isSuccess());
        assertTrue("Lowercase registers should work", result2.isSuccess());
    }

    // ========== Immediate Value Tests ==========

    @Test
    public void testNegativeImmediate() {
        String source = "addi r1, r2, -10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(-10, result.getInstructions().get(0).getImmediate());
    }

    @Test
    public void testHexImmediate() {
        String source = "lui r1, 0x10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(16, result.getInstructions().get(0).getImmediate());
    }

    @Test
    public void testOctalImmediate() {
        // Note: This assembler supports octal (0 prefix), not binary (0b prefix)
        String source = "lui r1, 012";  // Octal 12 = decimal 10
        AssemblyResult result = Assembler.assemble(source);

        assertTrue("Assembly should succeed", result.isSuccess());
        assertEquals(10, result.getInstructions().get(0).getImmediate());
    }

    // ========== Address Tests ==========

    @Test
    public void testInstructionAddresses() {
        // Note: RiSC-16 uses word addressing (each instruction is 2 bytes)
        String source =
            "add r1, r2, r3\n" +
            "addi r4, r5, 1\n" +
            "nand r6, r7, r0";

        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getInstructions().get(0).getAddress());
        assertEquals(2, result.getInstructions().get(1).getAddress());
        assertEquals(4, result.getInstructions().get(2).getAddress());
    }
}
