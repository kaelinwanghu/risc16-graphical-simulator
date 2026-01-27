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
 * Tests for SwExecutor
 *
 * SW regA, regB, immediate
 * Operation: Memory[regB + immediate] = regA
 *
 * - RRI-type instruction
 * - Immediate is 7-bit signed (-64 to 63)
 * - Effective address must be word-aligned (even)
 * - Throws ExecutionException on invalid/unaligned address
 * - Does not modify any registers
 */
public class SwExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private SwExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new SwExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testSwBasic() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 12345)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(12345, memory.readWord(50));
    }

    @Test
    public void testSwWithOffset() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .setRegister(2, (short) 20)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(42, memory.readWord(30));
    }

    @Test
    public void testSwNegativeOffset() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, -10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 99)
            .setRegister(2, (short) 60)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(99, memory.readWord(50));
    }

    @Test
    public void testSwNegativeValue() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) -1)
            .build();

        executor.execute(instr, state, memory);

        assertEquals((short) -1, memory.readWord(50));
    }

    @Test
    public void testSwZero() throws ExecutionException {
        memory.writeWord(50, (short) 9999);  // Pre-fill

        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 0)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(0, memory.readWord(50));
    }

    @Test
    public void testSwR0Value() throws ExecutionException {
        InstructionFormat instr = assemble("sw r0, r0, 50");

        executor.execute(instr, initialState, memory);

        assertEquals(0, memory.readWord(50));  // R0 is always 0
    }

    @Test
    public void testSwMaxValue() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, Short.MAX_VALUE)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(Short.MAX_VALUE, memory.readWord(50));
    }

    @Test
    public void testSwMinValue() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, Short.MIN_VALUE)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(Short.MIN_VALUE, memory.readWord(50));
    }

    @Test(expected = ExecutionException.class)
    public void testSwUnalignedAddress() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 10)  // 10 + 1 = 11 (odd)
            .build();

        executor.execute(instr, state, memory);
    }

    @Test(expected = ExecutionException.class)
    public void testSwOutOfBounds() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 0");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 2000)  // Out of 1024-byte memory
            .build();

        executor.execute(instr, state, memory);
    }

    @Test
    public void testSwDoesNotModifyRegisters() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r2, 10");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setRegister(2, (short) 20)
            .setRegister(3, (short) 300)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(100, ctx.getNewState().getRegister(1));
        assertEquals(20, ctx.getNewState().getRegister(2));
        assertEquals(300, ctx.getNewState().getRegister(3));
    }

    @Test
    public void testSwIncrementsPC() throws ExecutionException {
        InstructionFormat instr = assemble("sw r0, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testSwIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("sw r0, r0, 50");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testSwResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 42)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertFalse(ctx.getResult().hasDestinationReg());
        assertTrue(ctx.getResult().hasMemoryAccess());
        assertEquals(50, ctx.getResult().getMemoryAddress());
    }

    @Test
    public void testSwOverwritesPreviousValue() throws ExecutionException {
        memory.writeWord(50, (short) 1111);

        InstructionFormat instr = assemble("sw r1, r0, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 2222)
            .build();

        executor.execute(instr, state, memory);

        assertEquals(2222, memory.readWord(50));
    }
}
