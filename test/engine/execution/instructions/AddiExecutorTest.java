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
 * Tests for AddiExecutor
 *
 * ADDI regA, regB, immediate
 * Operation: regA = regB + immediate
 *
 * - RRI-type instruction
 * - Immediate is 7-bit signed (-64 to 63)
 * - Wraps on overflow (no exception)
 */
public class AddiExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private AddiExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new AddiExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testAddiPositive() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 5)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(15, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiNegative() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, -5");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 20)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(15, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiZero() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, 0");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 42)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(42, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiMaxPositive() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, 63");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(63, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiMaxNegative() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, -64");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(-64, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiFromR0() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, 42");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(42, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiToR0Ignored() throws ExecutionException {
        InstructionFormat instr = assemble("addi r0, r1, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 5)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    @Test
    public void testAddiOverflow() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r2, 1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, Short.MAX_VALUE)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(Short.MIN_VALUE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiSameSourceAndDest() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r1, 5");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 10)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(15, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testAddiIncrementsPC() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, 0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testAddiIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("addi r1, r0, 0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testAddiResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("addi r3, r1, 5");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(3, ctx.getResult().getDestinationReg());
        assertFalse(ctx.getResult().hasMemoryAccess());
    }
}
