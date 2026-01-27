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
 * Tests for LwExecutor
 *
 * LW regA, regB, immediate
 * Operation: regA = Memory[regB + immediate]
 *
 * - RRI-type instruction
 * - Immediate is 7-bit signed (-64 to 63)
 * - Effective address must be word-aligned (even)
 * - Throws ExecutionException on invalid/unaligned address
 */
public class LwExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private LwExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new LwExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testLwBasic() throws ExecutionException {
        memory.writeWord(50, (short) 12345);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(12345, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwWithOffset() throws ExecutionException {
        memory.writeWord(30, (short) 999);

        InstructionFormat instr = assemble("lw r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 20)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(999, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwNegativeOffset() throws ExecutionException {
        memory.writeWord(40, (short) 777);

        InstructionFormat instr = assemble("lw r1, r2, -10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 50)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(777, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwNegativeValue() throws ExecutionException {
        memory.writeWord(50, (short) -1);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwZero() throws ExecutionException {
        memory.writeWord(50, (short) 0);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(0, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwMaxValue() throws ExecutionException {
        memory.writeWord(50, Short.MAX_VALUE);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(Short.MAX_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwMinValue() throws ExecutionException {
        memory.writeWord(50, Short.MIN_VALUE);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(Short.MIN_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLwToR0Ignored() throws ExecutionException {
        memory.writeWord(50, (short) 9999);

        InstructionFormat instr = assemble("lw r0, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    @Test(expected = ExecutionException.class)
    public void testLwUnalignedAddress() throws ExecutionException {
        InstructionFormat instr = assemble("lw r1, r2, 1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 10)  // 10 + 1 = 11 (odd)
            .build();

        executor.execute(instr, state, memory);
    }

    @Test(expected = ExecutionException.class)
    public void testLwOutOfBounds() throws ExecutionException {
        InstructionFormat instr = assemble("lw r1, r2, 0");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 2000)  // Out of 1024-byte memory
            .build();

        executor.execute(instr, state, memory);
    }

    @Test
    public void testLwIncrementsPC() throws ExecutionException {
        memory.writeWord(50, (short) 0);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testLwIncrementsInstructionCount() throws ExecutionException {
        memory.writeWord(50, (short) 0);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testLwResultMetadata() throws ExecutionException {
        memory.writeWord(50, (short) 42);

        InstructionFormat instr = assemble("lw r3, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(3, ctx.getResult().getDestinationReg());
        assertTrue(ctx.getResult().hasMemoryAccess());
        assertEquals(50, ctx.getResult().getMemoryAddress());
    }

    @Test
    public void testLwDoesNotModifyMemory() throws ExecutionException {
        memory.writeWord(50, (short) 12345);

        InstructionFormat instr = assemble("lw r1, r0, 50");
        executor.execute(instr, initialState, memory);

        assertEquals(12345, memory.readWord(50));
    }
}
