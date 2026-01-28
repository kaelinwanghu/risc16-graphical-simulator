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
 * Tests for NandExecutor
 *
 * NAND regA, regB, regC
 * Operation: regA = ~(regB & regC)
 *
 * - RRR-type instruction
 * - NAND is functionally complete (can build all logic from it)
 * - NOT A = NAND A, A
 * - AND A, B = NOT (NAND A, B)
 * - OR A, B = NAND (NOT A), (NOT B)
 */
public class NandExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private NandExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new NandExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testNandBasic() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0xFF00)
            .setRegister(3, (short) 0x0FF0)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0xFF00, 0x0FF0) = ~(0x0F00) = 0xF0FF
        assertEquals((short) 0xF0FF, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandAsNot() throws ExecutionException {
        // NOT is NAND A, A
        InstructionFormat instr = assemble("nand r1, r2, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0x00FF)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NOT(0x00FF) = 0xFF00
        assertEquals((short) 0xFF00, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandZeroWithZero() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0)
            .setRegister(3, (short) 0)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0, 0) = ~(0 & 0) = ~0 = 0xFFFF = -1
        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandAllOnes() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) -1)  // 0xFFFF
            .setRegister(3, (short) -1)  // 0xFFFF
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0xFFFF, 0xFFFF) = ~(0xFFFF) = 0
        assertEquals(0, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandOneWithZero() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) -1)  // 0xFFFF
            .setRegister(3, (short) 0)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0xFFFF, 0) = ~(0) = 0xFFFF = -1
        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandBitPattern() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r2, r3");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0b0101010101010101)
            .setRegister(3, (short) 0b0011001100110011)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0x5555, 0x3333) = ~(0x1111) = 0xEEEE
        assertEquals((short) 0xEEEE, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandToR0Ignored() throws ExecutionException {
        InstructionFormat instr = assemble("nand r0, r1, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 0)
            .setRegister(2, (short) 0)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    @Test
    public void testNandWithR0() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r0, r2");
        ProcessorState state = ProcessorState.builder()
            .setRegister(2, (short) 0x1234)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // NAND(0, anything) = ~0 = 0xFFFF
        assertEquals((short) -1, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testNandIncrementsPC() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r0, r0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testNandIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("nand r1, r0, r0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testNandResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("nand r4, r1, r2");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(4, ctx.getResult().getDestinationReg());
        assertFalse(ctx.getResult().hasMemoryAccess());
        assertFalse(ctx.getResult().isBranchOrJump());
    }
}
