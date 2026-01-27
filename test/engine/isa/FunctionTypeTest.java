package engine.isa;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;

/**
 * Tests for the RiSC-16 FunctionType enum
 *
 * FunctionType categorizes instructions by their functional behavior
 * for pipeline scheduling and dependency analysis.
 *
 * FUNCTION TYPES:
 * ---------------
 * ALU           - Generic ALU operations (NAND)
 * ADD           - Addition operations (ADD, ADDI)
 * LOAD          - Load operations (LW, LUI)
 * STORE         - Store operations (SW)
 * BRANCH        - Conditional branches (BEQ)
 * JUMP_AND_LINK - Unconditional jumps (JALR)
 */
public class FunctionTypeTest {

    // ========== Assembly-Based Function Type Tests ==========

    @Test
    public void testAssemble_ADD_FunctionType() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.ADD, opcode);
    }

    @Test
    public void testAssemble_ADDI_FunctionType() {
        String source = "addi r1, r2, 10";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.ADDI, opcode);
    }

    @Test
    public void testAssemble_NAND_FunctionType() {
        String source = "nand r4, r5, r6";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.NAND, opcode);
    }

    @Test
    public void testAssemble_LW_FunctionType() {
        String source = "lw r1, r0, 5";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.LW, opcode);
    }

    @Test
    public void testAssemble_LUI_FunctionType() {
        String source = "lui r1, 100";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.LUI, opcode);
    }

    @Test
    public void testAssemble_SW_FunctionType() {
        String source = "sw r1, r2, 0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.SW, opcode);
    }

    @Test
    public void testAssemble_BEQ_FunctionType() {
        String source =
            "       beq r0, r1, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.BEQ, opcode);
    }

    @Test
    public void testAssemble_JALR_FunctionType() {
        String source = "jalr r7, r1";
        AssemblyResult result = Assembler.assemble(source);

        assertTrue(result.isSuccess());
        Opcode opcode = result.getInstructions().get(0).getOpcode();
        assertEquals(Opcode.JALR, opcode);
    }

    // ========== Direct FunctionType Tests ==========

    @Test
    public void testSixFunctionTypes() {
        assertEquals(6, FunctionType.values().length);
    }

    // ========== Memory Operations ==========

    @Test
    public void testLoadIsMemoryOperation() {
        assertTrue(FunctionType.LOAD.isMemoryOperation());
    }

    @Test
    public void testStoreIsMemoryOperation() {
        assertTrue(FunctionType.STORE.isMemoryOperation());
    }

    @Test
    public void testNonMemoryOperations() {
        assertFalse(FunctionType.ALU.isMemoryOperation());
        assertFalse(FunctionType.ADD.isMemoryOperation());
        assertFalse(FunctionType.BRANCH.isMemoryOperation());
        assertFalse(FunctionType.JUMP_AND_LINK.isMemoryOperation());
    }

    // ========== Control Flow Operations ==========

    @Test
    public void testBranchIsControlFlow() {
        assertTrue(FunctionType.BRANCH.isControlFlow());
    }

    @Test
    public void testJumpAndLinkIsControlFlow() {
        assertTrue(FunctionType.JUMP_AND_LINK.isControlFlow());
    }

    @Test
    public void testNonControlFlowOperations() {
        assertFalse(FunctionType.ALU.isControlFlow());
        assertFalse(FunctionType.ADD.isControlFlow());
        assertFalse(FunctionType.LOAD.isControlFlow());
        assertFalse(FunctionType.STORE.isControlFlow());
    }

    // ========== Register Writing ==========

    @Test
    public void testStoreDoesNotWriteRegister() {
        assertFalse(FunctionType.STORE.writesRegister());
    }

    @Test
    public void testBranchDoesNotWriteRegister() {
        assertFalse(FunctionType.BRANCH.writesRegister());
    }

    @Test
    public void testOperationsThatWriteRegisters() {
        assertTrue(FunctionType.ALU.writesRegister());
        assertTrue(FunctionType.ADD.writesRegister());
        assertTrue(FunctionType.LOAD.writesRegister());
        assertTrue(FunctionType.JUMP_AND_LINK.writesRegister());
    }
}
