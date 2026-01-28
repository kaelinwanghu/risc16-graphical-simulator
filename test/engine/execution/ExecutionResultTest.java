package engine.execution;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.isa.FunctionType;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;

/**
 * Tests for the ExecutionResult class
 *
 * ExecutionResult is an immutable result of executing a single instruction.
 * Different instruction types populate different fields:
 * - ALU operations: destinationReg
 * - Load operations: destinationReg, memoryAddress
 * - Store operations: memoryAddress
 * - Branch operations: branchTaken, branchTarget
 * - Jump operations: destinationReg (for link), branchTarget
 */
public class ExecutionResultTest {

    // ========== Assembly-Based Tests ==========

    @Test
    public void testResultForAssembledADD() {
        String source = "add r1, r2, r3";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.ADD, instr.getOpcode());

        // Simulate creating result for ADD instruction
        ExecutionResult result = ExecutionResult.add(instr.getRegA());

        assertTrue(result.hasDestinationReg());
        assertEquals(1, result.getDestinationReg());
        assertFalse(result.hasMemoryAccess());
        assertFalse(result.isBranchOrJump());
    }

    @Test
    public void testResultForAssembledNAND() {
        String source = "nand r4, r5, r6";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.NAND, instr.getOpcode());

        // Simulate creating result for NAND instruction
        ExecutionResult result = ExecutionResult.alu(instr.getRegA());

        assertTrue(result.hasDestinationReg());
        assertEquals(4, result.getDestinationReg());
        assertFalse(result.hasMemoryAccess());
    }

    @Test
    public void testResultForAssembledLW() {
        String source = "lw r1, r0, 10";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.LW, instr.getOpcode());

        // Simulate creating result for LW instruction (address = r0 + offset)
        int memAddr = 0 + instr.getImmediate();  // r0 = 0
        ExecutionResult result = ExecutionResult.load(instr.getRegA(), memAddr);

        assertTrue(result.hasDestinationReg());
        assertEquals(1, result.getDestinationReg());
        assertTrue(result.hasMemoryAccess());
        assertEquals(10, result.getMemoryAddress());
    }

    @Test
    public void testResultForAssembledSW() {
        String source = "sw r1, r2, 5";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.SW, instr.getOpcode());

        // Simulate creating result for SW instruction
        int memAddr = 100 + instr.getImmediate();  // Assume r2 = 100
        ExecutionResult result = ExecutionResult.store(memAddr);

        assertFalse(result.hasDestinationReg());
        assertTrue(result.hasMemoryAccess());
        assertEquals(105, result.getMemoryAddress());
    }

    @Test
    public void testResultForAssembledBEQ_Taken() {
        String source =
            "       beq r0, r0, skip\n" +
            "       add r1, r2, r3\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat beq = asmResult.getInstructions().get(0);
        assertEquals(Opcode.BEQ, beq.getOpcode());

        // Simulate BEQ where r0 == r0 (always true)
        int target = 4;  // Address of skip label
        ExecutionResult result = ExecutionResult.branch(true, target);

        assertFalse(result.hasDestinationReg());
        assertTrue(result.isBranchOrJump());
        assertTrue(result.isBranchTaken());
        assertEquals(4, result.getBranchTarget());
    }

    @Test
    public void testResultForAssembledBEQ_NotTaken() {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        // Simulate BEQ where r1 != r2
        int target = 2;
        ExecutionResult result = ExecutionResult.branch(false, target);

        assertTrue(result.isBranchOrJump());
        assertFalse(result.isBranchTaken());
    }

    @Test
    public void testResultForAssembledJALR() {
        String source = "jalr r7, r1";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.JALR, instr.getOpcode());

        // Simulate JALR: link register = r7, target = value of r1
        int linkReg = instr.getRegA();
        int target = 100;  // Assume r1 = 100
        ExecutionResult result = ExecutionResult.jumpAndLink(linkReg, target);

        assertTrue(result.hasDestinationReg());
        assertEquals(7, result.getDestinationReg());
        assertTrue(result.isBranchOrJump());
        assertEquals(100, result.getBranchTarget());
    }

    @Test
    public void testResultForAssembledLUI() {
        String source = "lui r3, 100";
        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        InstructionFormat instr = asmResult.getInstructions().get(0);
        assertEquals(Opcode.LUI, instr.getOpcode());

        // LUI is a load operation (loads immediate to upper bits)
        ExecutionResult result = ExecutionResult.load(instr.getRegA(), -1);

        assertTrue(result.hasDestinationReg());
        assertEquals(3, result.getDestinationReg());
    }

    // ========== Factory Method Tests ==========

    @Test
    public void testAluFactory() {
        ExecutionResult result = ExecutionResult.alu(5);

        assertTrue(result.hasDestinationReg());
        assertEquals(5, result.getDestinationReg());
        assertFalse(result.hasMemoryAccess());
        assertFalse(result.isBranchOrJump());
    }

    @Test
    public void testAddFactory() {
        ExecutionResult result = ExecutionResult.add(3);

        assertTrue(result.hasDestinationReg());
        assertEquals(3, result.getDestinationReg());
    }

    @Test
    public void testLoadFactory() {
        ExecutionResult result = ExecutionResult.load(2, 0x100);

        assertTrue(result.hasDestinationReg());
        assertEquals(2, result.getDestinationReg());
        assertTrue(result.hasMemoryAccess());
        assertEquals(0x100, result.getMemoryAddress());
    }

    @Test
    public void testStoreFactory() {
        ExecutionResult result = ExecutionResult.store(0x200);

        assertFalse(result.hasDestinationReg());
        assertTrue(result.hasMemoryAccess());
        assertEquals(0x200, result.getMemoryAddress());
    }

    @Test
    public void testBranchFactory() {
        ExecutionResult taken = ExecutionResult.branch(true, 0x50);
        ExecutionResult notTaken = ExecutionResult.branch(false, 0x50);

        assertTrue(taken.isBranchTaken());
        assertFalse(notTaken.isBranchTaken());
        assertEquals(0x50, taken.getBranchTarget());
    }

    @Test
    public void testJumpAndLinkFactory() {
        ExecutionResult result = ExecutionResult.jumpAndLink(7, 0x300);

        assertTrue(result.hasDestinationReg());
        assertEquals(7, result.getDestinationReg());
        assertTrue(result.isBranchOrJump());
        assertEquals(0x300, result.getBranchTarget());
    }

    // ========== Builder Tests ==========

    @Test
    public void testBuilderWithAllFields() {
        ExecutionResult result = ExecutionResult.builder(FunctionType.JUMP_AND_LINK)
            .destinationReg(7)
            .memoryAddress(0x100)
            .branchTaken(true)
            .branchTarget(0x200)
            .build();

        assertEquals(7, result.getDestinationReg());
        assertEquals(0x100, result.getMemoryAddress());
        assertTrue(result.isBranchTaken());
        assertEquals(0x200, result.getBranchTarget());
    }

    @Test
    public void testBuilderDefaults() {
        ExecutionResult result = ExecutionResult.builder(FunctionType.ALU).build();

        assertEquals(-1, result.getDestinationReg());
        assertFalse(result.hasDestinationReg());
        assertEquals(-1, result.getMemoryAddress());
        assertFalse(result.hasMemoryAccess());
        assertFalse(result.isBranchTaken());
        assertEquals(-1, result.getBranchTarget());
        assertFalse(result.isBranchOrJump());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullFunctionType() {
        ExecutionResult.builder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidRegister() {
        ExecutionResult.builder(FunctionType.ALU).destinationReg(8);
    }

    @Test
    public void testBuilderRegisterMinusOne() {
        // -1 is valid (means no destination register)
        ExecutionResult result = ExecutionResult.builder(FunctionType.STORE)
            .destinationReg(-1)
            .build();

        assertFalse(result.hasDestinationReg());
    }

    // ========== ToString Test ==========

    @Test
    public void testToString() {
        ExecutionResult result = ExecutionResult.load(1, 0x100);
        String str = result.toString();

        assertTrue(str.contains("R1") || str.contains("dest"));
        assertTrue(str.contains("100") || str.contains("mem"));
    }
}
