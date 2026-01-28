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
 * Tests for AddExecutor
 *
 * ADD regA, regB, regC
 * Operation: regA = regB + regC
 *
 * - RRR-type instruction
 * - Wraps on overflow (no exception)
 * - R0 as destination is ignored (R0 always 0)
 */
public class AddExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private AddExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new AddExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testAddBasic() throws ExecutionException {
        InstructionFormat instr = assemble("add r3, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(30, ctx.getNewState().getRegister(3));
    }

    @Test
    public void testAddZeros() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r0, r0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(0, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddNegativeNumbers() throws ExecutionException {
        InstructionFormat instr = assemble("add r3, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) -10)
            .setRegister(2, (short) -5)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(-15, ctx.getNewState().getRegister(3));
    }

    @Test
    public void testAddMixedSigns() throws ExecutionException {
        InstructionFormat instr = assemble("add r3, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setRegister(2, (short) -30)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(70, ctx.getNewState().getRegister(3));
    }

    @Test
    public void testAddOverflowPositive() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, Short.MAX_VALUE)
            .setRegister(3, (short) 1)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(Short.MIN_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddOverflowNegative() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, Short.MIN_VALUE)
            .setRegister(3, (short) -1)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(Short.MAX_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddToR0Ignored() throws ExecutionException {
        InstructionFormat instr = assemble("add r0, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .setRegister(2, (short) 20)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    @Test
    public void testAddSameRegister() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r2, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 25)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(50, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddIncrementsPC() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r0, r0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testAddIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("add r1, r0, r0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testAddResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("add r5, r1, r2");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(5, ctx.getResult().getDestinationReg());
        assertFalse(ctx.getResult().hasMemoryAccess());
        assertFalse(ctx.getResult().isBranchOrJump());
    }
}
