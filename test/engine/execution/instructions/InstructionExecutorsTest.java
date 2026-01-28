package engine.execution.instructions;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Tests for individual instruction executors
 *
 * Each RiSC-16 instruction has its own executor implementing InstructionExecutor:
 * - AddExecutor:  ADD regA, regB, regC  -> regA = regB + regC
 * - AddiExecutor: ADDI regA, regB, imm  -> regA = regB + imm
 * - NandExecutor: NAND regA, regB, regC -> regA = ~(regB & regC)
 * - LuiExecutor:  LUI regA, imm         -> regA = imm << 6
 * - SwExecutor:   SW regA, regB, imm    -> Mem[regB + imm] = regA
 * - LwExecutor:   LW regA, regB, imm    -> regA = Mem[regB + imm]
 * - BeqExecutor:  BEQ regA, regB, imm   -> if (regA == regB) PC = PC + 2 + imm
 * - JalrExecutor: JALR regA, regB       -> regA = PC + 2; PC = regB
 */
public class InstructionExecutorsTest {

    private Memory memory;
    private ProcessorState initialState;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
    }

    // ========== Helper Methods ==========

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly failed: " + (result.isSuccess() ? "" : result.getErrors().get(0).getMessage()),
            result.isSuccess());
        return result.getInstructions().get(0);
    }

    private ProcessorState stateWith(int reg, short value) {
        return initialState.toBuilder().setRegister(reg, value).build();
    }

    // ========== ADD Tests ==========

    @Test
    public void testAddExecutor_Basic() throws ExecutionException {
        InstructionFormat instr = assemble("add r3, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();

        AddExecutor executor = new AddExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(30, ctx.getNewState().getRegister(3));
        assertEquals(2, ctx.getNewState().getPC());
        assertEquals(1, ctx.getNewState().getInstructionCount());
        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(3, ctx.getResult().getDestinationReg());
    }

    @Test
    public void testAddExecutor_Overflow() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, Short.MAX_VALUE)
            .setRegister(3, (short) 1)
            .build();

        AddExecutor executor = new AddExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // Should wrap to negative (overflow)
        assertEquals(Short.MIN_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddExecutor_R0AlwaysZero() throws ExecutionException {
        InstructionFormat instr = assemble("add r0, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();

        AddExecutor executor = new AddExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // R0 should remain 0
        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    // ========== ADDI Tests ==========

    @Test
    public void testAddiExecutor_Positive() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 5)
            .build();

        AddiExecutor executor = new AddiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(15, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiExecutor_Negative() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, -5");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 20)
            .build();

        AddiExecutor executor = new AddiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(15, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiExecutor_MaxPositive() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, 63");  // Max positive 7-bit
        AddiExecutor executor = new AddiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(63, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiExecutor_MaxNegative() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, -64");  // Min negative 7-bit
        AddiExecutor executor = new AddiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(-64, ctx.getNewState().getRegister(1));
    }

    // ========== NAND Tests ==========

    @Test
    public void testNandExecutor_Basic() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0xFF00)
            .setRegister(3, (short) 0x0FF0)
            .build();

        NandExecutor executor = new NandExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0xFF00, 0x0FF0) = ~(0x0F00) = 0xF0FF
        assertEquals((short) 0xF0FF, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandExecutor_NOT() throws ExecutionException {
        // NOT is implemented as NAND A, A
        InstructionFormat instr = assemble("nand r1, r2, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0)
            .build();

        NandExecutor executor = new NandExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0, 0) = ~(0 & 0) = ~0 = -1 (0xFFFF)
        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandExecutor_AllOnes() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) -1)  // 0xFFFF
            .setRegister(3, (short) -1)  // 0xFFFF
            .build();

        NandExecutor executor = new NandExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0xFFFF, 0xFFFF) = ~(0xFFFF) = 0
        assertEquals(0, ctx.getNewState().getRegister(1));
    }

    // ========== LUI Tests ==========

    @Test
    public void testLuiExecutor_Basic() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 100");
        LuiExecutor executor = new LuiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // LUI 100 = 100 << 6 = 6400
        assertEquals(6400, ctx.getNewState().getRegister(1) & 0xFFFF);
    }

    @Test
    public void testLuiExecutor_MaxImmediate() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 1023");  // Max 10-bit
        LuiExecutor executor = new LuiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // LUI 1023 = 1023 << 6 = 65472 (0xFFC0)
        assertEquals(65472, ctx.getNewState().getRegister(1) & 0xFFFF);
    }

    @Test
    public void testLuiExecutor_LowerBitsZero() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 1");
        LuiExecutor executor = new LuiExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // LUI 1 = 1 << 6 = 64 = 0x40 (lower 6 bits are 0)
        short result = ctx.getNewState().getRegister(1);
        assertEquals(64, result);
        assertEquals(0, result & 0x3F);  // Lower 6 bits should be 0
    }

    // ========== SW Tests ==========

    @Test
    public void testSwExecutor_Basic() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 12345)
            .build();

        SwExecutor executor = new SwExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(12345, memory.readWord(50));
        assertTrue(ctx.getResult().hasMemoryAccess());
        assertEquals(50, ctx.getResult().getMemoryAddress());
    }

    @Test
    public void testSwExecutor_WithOffset() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .setRegister(2, (short) 20)
            .build();

        SwExecutor executor = new SwExecutor();
        executor.execute(instr, state, memory);

        // Address = r2 + offset = 20 + 10 = 30
        assertEquals(42, memory.readWord(30));
    }

    @Test
    public void testSwExecutor_NegativeOffset() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, -10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 99)
            .setRegister(2, (short) 60)
            .build();

        SwExecutor executor = new SwExecutor();
        executor.execute(instr, state, memory);

        // Address = r2 + offset = 60 + (-10) = 50
        assertEquals(99, memory.readWord(50));
    }

    @Test(expected = ExecutionException.class)
    public void testSwExecutor_UnalignedAddress() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 10)  // 10 + 1 = 11 (odd, unaligned)
            .build();

        SwExecutor executor = new SwExecutor();
        executor.execute(instr, state, memory);
    }

    @Test(expected = ExecutionException.class)
    public void testSwExecutor_OutOfBounds() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 0");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 2000)  // Out of bounds for 1KB memory
            .build();

        SwExecutor executor = new SwExecutor();
        executor.execute(instr, state, memory);
    }

    // ========== LW Tests ==========

    @Test
    public void testLwExecutor_Basic() throws ExecutionException {
        memory.writeWord(50, (short) 12345);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        LwExecutor executor = new LwExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(12345, ctx.getNewState().getRegister(1));
        assertTrue(ctx.getResult().hasMemoryAccess());
        assertEquals(50, ctx.getResult().getMemoryAddress());
    }

    @Test
    public void testLwExecutor_WithOffset() throws ExecutionException {
        memory.writeWord(30, (short) 999);

        InstructionFormat instr = assemble("lw r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 20)
            .build();

        LwExecutor executor = new LwExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(999, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwExecutor_NegativeValue() throws ExecutionException {
        memory.writeWord(50, (short) -1);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        LwExecutor executor = new LwExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test(expected = ExecutionException.class)
    public void testLwExecutor_UnalignedAddress() throws ExecutionException {
        InstructionFormat instr = assemble("lw r1, r2, 1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 10)
            .build();

        LwExecutor executor = new LwExecutor();
        executor.execute(instr, state, memory);
    }

    // ========== BEQ Tests ==========

    @Test
    public void testBeqExecutor_Taken() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);
        InstructionFormat instr = result.getInstructions().get(0);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .setRegister(2, (short) 42)  // Equal
            .build();

        BeqExecutor executor = new BeqExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchTaken());
        assertTrue(ctx.getResult().isBranchOrJump());
    }

    @Test
    public void testBeqExecutor_NotTaken() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);
        InstructionFormat instr = result.getInstructions().get(0);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)  // Not equal
            .build();

        BeqExecutor executor = new BeqExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertFalse(ctx.getResult().isBranchTaken());
        assertEquals(2, ctx.getNewState().getPC());  // Sequential
    }

    @Test
    public void testBeqExecutor_R0EqualToR0() throws ExecutionException {
        // beq r0, r0, X is an unconditional branch
        String source =
            "       beq r0, r0, skip\n" +
            "       add r1, r0, r0\n" +
            "skip:  add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);
        InstructionFormat instr = result.getInstructions().get(0);

        BeqExecutor executor = new BeqExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchTaken());
        assertEquals(4, ctx.getNewState().getPC());  // Jump to skip
    }

    // ========== JALR Tests ==========

    @Test
    public void testJalrExecutor_Basic() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setPC(50)
            .build();

        JalrExecutor executor = new JalrExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(52, ctx.getNewState().getRegister(7));  // Return address = PC + 2
        assertEquals(100, ctx.getNewState().getPC());        // Jump to r1
        assertFalse(ctx.getNewState().isHalted());
    }

    @Test
    public void testJalrExecutor_Halt() throws ExecutionException {
        // JALR r0, r0 is the HALT instruction
        InstructionFormat instr = assemble("jalr r0, r0");

        JalrExecutor executor = new JalrExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getNewState().isHalted());
        assertEquals(0, ctx.getNewState().getPC());  // Jump to 0 (r0)
        assertEquals(0, ctx.getNewState().getRegister(0));  // R0 still 0
    }

    @Test
    public void testJalrExecutor_DiscardReturnAddress() throws ExecutionException {
        // JALR r0, r1 - jump to r1, discard return address (into r0)
        InstructionFormat instr = assemble("jalr r0, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 200)
            .setPC(100)
            .build();

        JalrExecutor executor = new JalrExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));  // R0 always 0
        assertEquals(200, ctx.getNewState().getPC());
        assertFalse(ctx.getNewState().isHalted());  // Not halt (r1 != r0)
    }

    @Test
    public void testJalrExecutor_ResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 50)
            .build();

        JalrExecutor executor = new JalrExecutor();
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchOrJump());
        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(7, ctx.getResult().getDestinationReg());
        assertEquals(50, ctx.getResult().getBranchTarget());
    }

    // ========== Common Behavior Tests ==========

    @Test
    public void testAllExecutorsIncrementPC() throws ExecutionException {
        // All instructions increment PC by 2 (except branches that are taken)
        AddExecutor add = new AddExecutor();
        AddiExecutor addi = new AddiExecutor();
        NandExecutor nand = new NandExecutor();
        LuiExecutor lui = new LuiExecutor();
        SwExecutor sw = new SwExecutor();
        LwExecutor lw = new LwExecutor();

        memory.writeWord(50, (short) 0);  // For LW

        ProcessorState state = initialState;

        assertEquals(2, add.execute(assemble("add r1, r0, r0"), state, memory).getNewState().getPC());
        assertEquals(2, addi.execute(assemble("addi r1, r0, 0"), state, memory).getNewState().getPC());
        assertEquals(2, nand.execute(assemble("nand r1, r0, r0"), state, memory).getNewState().getPC());
        assertEquals(2, lui.execute(assemble("lui r1, 0"), state, memory).getNewState().getPC());
        assertEquals(2, sw.execute(assemble("sw r0, r0, 50"), state, memory).getNewState().getPC());
        assertEquals(2, lw.execute(assemble("lw r1, r0, 50"), state, memory).getNewState().getPC());
    }

    @Test
    public void testAllExecutorsIncrementInstructionCount() throws ExecutionException {
        AddExecutor add = new AddExecutor();
        AddiExecutor addi = new AddiExecutor();
        NandExecutor nand = new NandExecutor();
        LuiExecutor lui = new LuiExecutor();

        ProcessorState state = initialState;

        assertEquals(1, add.execute(assemble("add r1, r0, r0"), state, memory).getNewState().getInstructionCount());
        assertEquals(1, addi.execute(assemble("addi r1, r0, 0"), state, memory).getNewState().getInstructionCount());
        assertEquals(1, nand.execute(assemble("nand r1, r0, r0"), state, memory).getNewState().getInstructionCount());
        assertEquals(1, lui.execute(assemble("lui r1, 0"), state, memory).getNewState().getInstructionCount());
    }
}
