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
 * Tests for BeqExecutor
 *
 * BEQ regA, regB, immediate
 * Operation: if (regA == regB) then PC = PC + 2 + immediate
 *
 * - RRI-type instruction
 * - Immediate is 7-bit signed (-64 to 63)
 * - If not taken, PC increments by 2 (next instruction)
 * - If taken, PC = PC + 2 + immediate
 * - BEQ r0, r0, X is unconditional branch
 */
public class BeqExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private BeqExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new BeqExecutor();
    }

    private InstructionFormat assembleBeq(String fullSource) {
        AssemblyResult result = Assembler.assemble(fullSource);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testBeqTaken() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .setRegister(2, (short) 42)  // Equal
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchTaken());
        assertTrue(ctx.getResult().isBranchOrJump());
    }

    @Test
    public void testBeqNotTaken() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)  // Not equal
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertFalse(ctx.getResult().isBranchTaken());
        assertEquals(2, ctx.getNewState().getPC());  // Sequential
    }

    @Test
    public void testBeqUnconditional() throws ExecutionException {
        // BEQ r0, r0, X is always taken
        String source =
            "       beq r0, r0, skip\n" +
            "       add r1, r0, r0\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchTaken());
        assertEquals(4, ctx.getNewState().getPC());  // Jumped to skip
    }

    @Test
    public void testBeqEqualZeros() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 0)
            .setRegister(2, (short) 0)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchTaken());
    }

    @Test
    public void testBeqEqualNegatives() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) -100)
            .setRegister(2, (short) -100)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchTaken());
    }

    @Test
    public void testBeqNotEqualOppositeSign() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setRegister(2, (short) -100)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertFalse(ctx.getResult().isBranchTaken());
    }

    @Test
    public void testBeqWithR0() throws ExecutionException {
        // Check if r1 equals 0
        String source =
            "       beq r1, r0, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        // r1 = 0, so should be taken
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchTaken());
    }

    @Test
    public void testBeqForwardBranch() throws ExecutionException {
        String source =
            "       beq r0, r0, far\n" +
            "       add r0, r0, r0\n" +
            "       add r0, r0, r0\n" +
            "       add r0, r0, r0\n" +
            "far:   add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchTaken());
        assertEquals(8, ctx.getNewState().getPC());  // 4 instructions * 2 bytes
    }

    @Test
    public void testBeqSameRegister() throws ExecutionException {
        // BEQ r3, r3 is always taken (register equals itself)
        String source =
            "       beq r3, r3, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        ProcessorState state = ProcessorState.builder()
            .setRegister(3, (short) 12345)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchTaken());
    }

    @Test
    public void testBeqIncrementsInstructionCount() throws ExecutionException {
        String source =
            "       beq r0, r0, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testBeqResultMetadata() throws ExecutionException {
        String source =
            "       beq r1, r2, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchOrJump());
        assertFalse(ctx.getResult().hasDestinationReg());
        assertFalse(ctx.getResult().hasMemoryAccess());
    }

    @Test
    public void testBeqBranchTargetInResult() throws ExecutionException {
        String source =
            "       beq r0, r0, skip\n" +
            "skip:  add r0, r0, r0";
        InstructionFormat instr = assembleBeq(source);

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getResult().getBranchTarget());
    }
}
