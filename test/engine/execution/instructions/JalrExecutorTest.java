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
 * Tests for JalrExecutor
 *
 * JALR regA, regB
 * Operation: regA = PC + 2; PC = regB
 *
 * - RRI-type instruction (immediate typically 0)
 * - Stores return address (PC + 2) in regA
 * - Jumps to address in regB
 * - JALR r0, r0 is the HALT instruction
 * - JALR r0, rX is indirect jump (discard return)
 * - JALR r7, rX is typical function call
 */
public class JalrExecutorTest {

    private Memory memory;
    private ProcessorState initialState;
    private JalrExecutor executor;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        initialState = ProcessorState.builder().build();
        executor = new JalrExecutor();
    }

    private InstructionFormat assemble(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());
        return result.getInstructions().get(0);
    }

    @Test
    public void testJalrBasic() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setPC(50)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(52, ctx.getNewState().getRegister(7));  // Return address
        assertEquals(100, ctx.getNewState().getPC());        // Jump target
        assertFalse(ctx.getNewState().isHalted());
    }

    @Test
    public void testJalrHalt() throws ExecutionException {
        // JALR r0, r0 is HALT
        InstructionFormat instr = assemble("jalr r0, r0");

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getNewState().isHalted());
        assertEquals(0, ctx.getNewState().getPC());
        assertEquals(0, ctx.getNewState().getRegister(0));  // R0 always 0
    }

    @Test
    public void testJalrIndirectJump() throws ExecutionException {
        // JALR r0, r1 - jump to r1, discard return address
        InstructionFormat instr = assemble("jalr r0, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 200)
            .setPC(100)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(0, ctx.getNewState().getRegister(0));  // R0 always 0
        assertEquals(200, ctx.getNewState().getPC());
        assertFalse(ctx.getNewState().isHalted());  // Not halt
    }

    @Test
    public void testJalrFunctionCall() throws ExecutionException {
        // Typical function call: JALR r7, r1
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 500)
            .setPC(100)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(102, ctx.getNewState().getRegister(7));  // Return to 102
        assertEquals(500, ctx.getNewState().getPC());         // Jump to function
    }

    @Test
    public void testJalrReturn() throws ExecutionException {
        // Return from function: JALR r0, r7
        InstructionFormat instr = assemble("jalr r0, r7");
        ProcessorState state = ProcessorState.builder()
            .setRegister(7, (short) 102)  // Return address
            .setPC(500)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(102, ctx.getNewState().getPC());  // Return to caller
    }

    @Test
    public void testJalrJumpToZero() throws ExecutionException {
        // Jump to address 0, but not halt (regA != 0)
        InstructionFormat instr = assemble("jalr r7, r0");

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertEquals(2, ctx.getNewState().getRegister(7));  // Return address
        assertEquals(0, ctx.getNewState().getPC());
        assertFalse(ctx.getNewState().isHalted());  // Not halt (regA != 0)
    }

    @Test
    public void testJalrAllRegisters() throws ExecutionException {
        for (int destReg = 1; destReg < 8; destReg++) {
            for (int srcReg = 1; srcReg < 8; srcReg++) {
                InstructionFormat instr = assemble("jalr r" + destReg + ", r" + srcReg);
                ProcessorState state = ProcessorState.builder()
                    .setRegister(srcReg, (short) 100)
                    .setPC(0)
                    .build();

                InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

                if (destReg != srcReg) {
                    assertEquals(2, ctx.getNewState().getRegister(destReg));
                }
                assertEquals(100, ctx.getNewState().getPC());
            }
        }
    }

    @Test
    public void testJalrSameRegister() throws ExecutionException {
        // JALR r1, r1 - store return address in same reg as jump target
        InstructionFormat instr = assemble("jalr r1, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 200)
            .setPC(50)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        // Return address overwrites jump target
        assertEquals(52, ctx.getNewState().getRegister(1));
        assertEquals(200, ctx.getNewState().getPC());
    }

    @Test
    public void testJalrIncrementsInstructionCount() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(1, ctx.getNewState().getInstructionCount());
    }

    @Test
    public void testJalrResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 50)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertTrue(ctx.getResult().isBranchOrJump());
        assertTrue(ctx.getResult().hasDestinationReg());
        assertEquals(7, ctx.getResult().getDestinationReg());
        assertEquals(50, ctx.getResult().getBranchTarget());
    }

    @Test
    public void testJalrHaltResultMetadata() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r0, r0");

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, initialState, memory);

        assertTrue(ctx.getResult().isBranchOrJump());
        assertEquals(0, ctx.getResult().getBranchTarget());
    }

    @Test
    public void testJalrPreservesOtherRegisters() throws ExecutionException {
        InstructionFormat instr = assemble("jalr r7, r1");
        ProcessorState state = ProcessorState.builder()
            .setRegister(1, (short) 100)
            .setRegister(2, (short) 200)
            .setRegister(3, (short) 300)
            .setPC(50)
            .build();

        InstructionExecutor.ExecutionContext ctx = executor.execute(instr, state, memory);

        assertEquals(100, ctx.getNewState().getRegister(1));
        assertEquals(200, ctx.getNewState().getRegister(2));
        assertEquals(300, ctx.getNewState().getRegister(3));
    }
}
