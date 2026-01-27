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
 * Tests for LuiExecutor
 *
 * LUI regA, immediate
 * Operation: regA = immediate << 6
 *
 * - RI-type instruction
 * - Immediate is 10-bit unsigned (0 to 1023)
 * - Loads immediate into upper 10 bits, lower 6 bits are 0
 * - Used with ADDI to load full 16-bit constants
 */
public class LuiExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private LuiExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new LuiExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testLuiBasic() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 100");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // 100 << 6 = 6400
        assertEquals(6400, ctx.getNewState().getRegister(1) & 0xFFFF);
    }

    @Test
    public void testLuiZero() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(0, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLuiOne() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 1");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // 1 << 6 = 64
        assertEquals(64, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLuiMaxImmediate() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 1023");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // 1023 << 6 = 65472 = 0xFFC0
        assertEquals(65472, ctx.getNewState().getRegister(1) & 0xFFFF);
    }

    @Test
    public void testLuiLowerBitsZero() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 1");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        short result = ctx.getNewState().getRegister(1);
        assertEquals(0, result & 0x3F);  // Lower 6 bits should be 0
    }

    @Test
    public void testLuiUpperBitsSet() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 512");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        // 512 << 6 = 32768 = 0x8000 (sign bit set)
        assertEquals((short) 0x8000, ctx.getNewState().getRegister(1));
    }

    @Test
    public void testLuiToR0Ignored() throws ExecutionException {
        InstructionFormat instr = assemble("lui r0, 100");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));
    }

    @Test
    public void testLuiOverwritesPrevious() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 50");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 9999)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // Should overwrite previous value
        assertEquals(50 << 6, ctx.getNewState().getRegister(1) & 0xFFFF);
    }

    @Test
    public void testLuiAllRegisters() throws ExecutionException {
        for (int reg = 1; reg < 8; reg++) {
            InstructionFormat instr = assemble("lui r" + reg + ", 10");
            InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

            assertEquals(10 << 6, ctx.getNewState().getRegister(reg) & 0xFFFF);
        }
    }

    @Test
    public void testLuiIncrementsPC() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getPC());
    }

    @Test
    public void testLuiIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("lui r1, 0");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testLuiResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("lui r3, 100");
        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(3, ctx.getResult().getDestinationReg());
    }

    @Test
    public void testLuiWithAddiPattern() throws ExecutionException {
        // Common pattern: LUI + ADDI to load 16-bit constant
        // To load 1000 (0x03E8): LUI r1, 15; ADDI r1, r1, 40
        // 15 << 6 = 960, 960 + 40 = 1000
        InstructionFormat lui = assemble("lui r1, 15");
        InstructionExecutor.ExecutionContext ctx1 = executor.execute(lui, initialState, memory);

        assertEquals(960, ctx1.getNewState().getRegister(1));

        // Then ADDI would add 40 to get 1000
    }
}
