package engine.execution;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

/**
 * Tests for the ExecutionEngine class
 *
 * ExecutionEngine is the core execution engine for RiSC-16:
 * - Fetches instructions from memory
 * - Decodes using InstructionEncoder
 * - Dispatches to appropriate instruction executor
 * - Updates and manages processor state
 * - Enforces instruction limits
 */
public class ExecutionEngineTest {

    private Memory memory;
    private ProgramLoader loader;
    private ExecutionEngine engine;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        loader = new ProgramLoader(memory);
    }

    // ========== Helper Methods ==========

    private ProcessorState loadAndCreateState(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly failed: " + (result.isSuccess() ? "" : result.getErrors().get(0).getMessage()),
            result.isSuccess());
        ProgramMetadata metadata = loader.load(result);
        engine = new ExecutionEngine(memory, metadata);
        return ProcessorState.builder().build();
    }

    // ========== Single Step Execution Tests ==========

    @Test
    public void testStepADD() throws ExecutionException {
        String source =
            "addi r1, r0, 5\n" +   // r1 = 5
            "addi r2, r0, 3\n" +   // r2 = 3
            "add r3, r1, r2";      // r3 = r1 + r2 = 8

        ProcessorState state = loadAndCreateState(source);

        // Execute addi r1, r0, 5
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(5, state.getRegister(1));
        assertEquals(2, state.getPC());

        // Execute addi r2, r0, 3
        ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(3, state.getRegister(2));
        assertEquals(4, state.getPC());

        // Execute add r3, r1, r2
        ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(8, state.getRegister(3));
        assertEquals(6, state.getPC());
    }

    @Test
    public void testStepNAND() throws ExecutionException {
        String source =
            "addi r1, r0, 0\n" +       // r1 = 0
            "nand r2, r1, r1";         // r2 = ~(0 & 0) = 0xFFFF = -1

        ProcessorState state = loadAndCreateState(source);

        // Execute addi
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        // Execute nand
        ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals((short) -1, state.getRegister(2));  // NAND(0, 0) = -1
    }

    @Test
    public void testStepLUI() throws ExecutionException {
        String source = "lui r1, 100";  // r1 = 100 << 6 = 6400

        ProcessorState state = loadAndCreateState(source);

        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        // LUI loads immediate into upper 10 bits, lower 6 bits are 0
        assertEquals(100 << 6, state.getRegister(1) & 0xFFFF);
    }

    @Test
    public void testStepLWandSW() throws ExecutionException {
        String source =
            "       addi r1, r0, 42\n" +  // r1 = 42
            "       sw r1, r0, 50\n" +    // Memory[50] = r1
            "       lw r2, r0, 50";       // r2 = Memory[50]

        ProcessorState state = loadAndCreateState(source);

        // Execute addi
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(42, state.getRegister(1));

        // Execute sw
        ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(42, memory.readWord(50));

        // Execute lw
        ctx = engine.step(state);
        state = ctx.getNewState();
        assertEquals(42, state.getRegister(2));
    }

    @Test
    public void testStepBEQ_Taken() throws ExecutionException {
        String source =
            "       beq r0, r0, skip\n" +  // Branch always taken (r0 == r0)
            "       addi r1, r0, 1\n" +    // Skipped
            "skip:  addi r1, r0, 2";       // r1 = 2

        ProcessorState state = loadAndCreateState(source);

        // Execute beq (should branch)
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        assertTrue(ctx.getResult().isBranchTaken());
        assertEquals(4, state.getPC());  // Jumped to skip (address 4)
    }

    @Test
    public void testStepBEQ_NotTaken() throws ExecutionException {
        String source =
            "       addi r1, r0, 1\n" +    // r1 = 1
            "       beq r1, r0, skip\n" +  // Branch not taken (r1 != r0)
            "       addi r2, r0, 10\n" +   // r2 = 10
            "skip:  add r0, r0, r0";       // NOP

        ProcessorState state = loadAndCreateState(source);

        // Execute addi r1
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        // Execute beq (not taken)
        ctx = engine.step(state);
        state = ctx.getNewState();

        assertFalse(ctx.getResult().isBranchTaken());
        assertEquals(4, state.getPC());  // Sequential execution
    }

    @Test
    public void testStepJALR() throws ExecutionException {
        String source =
            "       addi r1, r0, 10\n" +   // r1 = 10
            "       jalr r7, r1";          // r7 = PC+2, jump to r1

        ProcessorState state = loadAndCreateState(source);

        // Execute addi
        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        // Execute jalr
        ctx = engine.step(state);
        state = ctx.getNewState();

        assertEquals(4, state.getRegister(7));  // Return address = PC+2 = 4
        assertEquals(10, state.getPC());        // Jumped to address 10
    }

    @Test
    public void testStepJALR_Halt() throws ExecutionException {
        String source = "jalr r0, r0";  // HALT: jump to r0 (0), discard return

        ProcessorState state = loadAndCreateState(source);

        InstructionExecutor.ExecutionContext ctx = engine.step(state);
        state = ctx.getNewState();

        assertTrue(state.isHalted());
    }

    // ========== Run Until Halt Tests ==========

    @Test
    public void testRunSimpleProgram() throws ExecutionException {
        String source =
            "       addi r1, r0, 10\n" +   // r1 = 10
            "       addi r2, r0, 20\n" +   // r2 = 20
            "       add r3, r1, r2\n" +    // r3 = 30
            "       jalr r0, r0";          // HALT

        ProcessorState state = loadAndCreateState(source);

        ProcessorState finalState = engine.run(state);

        assertTrue(finalState.isHalted());
        assertEquals(10, finalState.getRegister(1));
        assertEquals(20, finalState.getRegister(2));
        assertEquals(30, finalState.getRegister(3));
        assertEquals(4, finalState.getInstructionCount());
    }

    @Test
    public void testRunLoop() throws ExecutionException {
        // Count from 0 to 5
        String source =
            "       addi r1, r0, 0\n" +     // r1 = 0 (counter)
            "       addi r2, r0, 5\n" +     // r2 = 5 (limit)
            "loop:  addi r1, r1, 1\n" +     // r1++
            "       beq r1, r2, done\n" +   // if r1 == r2, exit
            "       beq r0, r0, loop\n" +   // unconditional jump to loop
            "done:  jalr r0, r0";           // HALT

        ProcessorState state = loadAndCreateState(source);

        ProcessorState finalState = engine.run(state);

        assertTrue(finalState.isHalted());
        assertEquals(5, finalState.getRegister(1));  // Counter reached 5
    }

    // ========== Error Handling Tests ==========

    @Test(expected = ExecutionException.class)
    public void testStepWhenHalted() throws ExecutionException {
        String source = "jalr r0, r0";  // HALT

        ProcessorState state = loadAndCreateState(source);
        state = ProcessorState.builder().setHalted(true).build();

        engine.step(state);  // Should throw
    }

    @Test(expected = ExecutionException.class)
    public void testStepPCOutOfBounds() throws ExecutionException {
        String source = "add r0, r0, r0";

        ProcessorState state = loadAndCreateState(source);
        state = ProcessorState.builder().setPC(2000).build();  // Out of bounds

        engine.step(state);  // Should throw
    }

    @Test(expected = ExecutionException.class)
    public void testStepPCUnaligned() throws ExecutionException {
        String source = "add r0, r0, r0";

        ProcessorState state = loadAndCreateState(source);
        state = ProcessorState.builder().setPC(3).build();  // Unaligned

        engine.step(state);  // Should throw
    }

    @Test
    public void testInstructionLimit() throws ExecutionException {
        // Infinite loop
        String source =
            "loop: beq r0, r0, loop";  // Unconditional branch to self

        ProcessorState state = loadAndCreateState(source);
        engine.setInstructionLimit(10);

        try {
            engine.run(state);
            fail("Expected ExecutionException for instruction limit");
        } catch (ExecutionException e) {
            assertTrue(e.getMessage().contains("limit"));
        }
    }

    @Test
    public void testInstructionLimitGetSet() {
        String source = "add r0, r0, r0";
        loadAndCreateState(source);

        assertEquals(65535, engine.getInstructionLimit());  // Default

        engine.setInstructionLimit(1000);
        assertEquals(1000, engine.getInstructionLimit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstructionLimitZero() {
        String source = "add r0, r0, r0";
        loadAndCreateState(source);
        engine.setInstructionLimit(0);  // Should throw
    }

    // ========== Constructor Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullMemory() {
        new ExecutionEngine(null, null);
    }

    @Test
    public void testConstructorNullMetadata() {
        // Null metadata is allowed
        ExecutionEngine engine = new ExecutionEngine(memory, null);
        assertNotNull(engine);
        assertNull(engine.getMetadata());
    }

    // ========== Accessor Tests ==========

    @Test
    public void testGetMemory() {
        String source = "add r0, r0, r0";
        loadAndCreateState(source);
        assertSame(memory, engine.getMemory());
    }

    @Test
    public void testGetMetadata() {
        String source = "add r0, r0, r0";
        AssemblyResult result = Assembler.assemble(source);
        ProgramMetadata metadata = loader.load(result);
        engine = new ExecutionEngine(memory, metadata);

        assertSame(metadata, engine.getMetadata());
    }

    @Test
    public void testSetMetadata() {
        String source = "add r0, r0, r0";
        loadAndCreateState(source);

        ProgramMetadata newMetadata = new ProgramMetadata(0);
        engine.setMetadata(newMetadata);

        assertSame(newMetadata, engine.getMetadata());
    }

    // ========== Complex Program Tests ==========

    @Test
    public void testFibonacci() throws ExecutionException {
        // Calculate first few Fibonacci numbers
        // F(0)=0, F(1)=1, F(2)=1, F(3)=2, F(4)=3, F(5)=5
        String source =
            "       addi r1, r0, 0\n" +     // r1 = F(n-2) = 0
            "       addi r2, r0, 1\n" +     // r2 = F(n-1) = 1
            "       addi r3, r0, 5\n" +     // r3 = iterations
            "       addi r4, r0, 0\n" +     // r4 = counter
            "loop:  add r5, r1, r2\n" +     // r5 = F(n) = F(n-1) + F(n-2)
            "       add r1, r0, r2\n" +     // r1 = r2
            "       add r2, r0, r5\n" +     // r2 = r5
            "       addi r4, r4, 1\n" +     // counter++
            "       beq r4, r3, done\n" +   // if counter == 5, done
            "       beq r0, r0, loop\n" +   // else continue
            "done:  jalr r0, r0";           // HALT

        ProcessorState state = loadAndCreateState(source);

        ProcessorState finalState = engine.run(state);

        assertTrue(finalState.isHalted());
        assertEquals(8, finalState.getRegister(2));  // F(6) = 8
    }

    @Test
    public void testMemoryOperations() throws ExecutionException {
        // Store and load values
        String source =
            "       addi r1, r0, 42\n" +    // r1 = 42
            "       sw r1, r0, 50\n" +      // Memory[50] = 42
            "       addi r1, r0, 0\n" +     // r1 = 0 (clear)
            "       lw r2, r0, 50\n" +      // r2 = Memory[50] = 42
            "       jalr r0, r0";           // HALT

        ProcessorState state = loadAndCreateState(source);

        ProcessorState finalState = engine.run(state);

        assertTrue(finalState.isHalted());
        assertEquals(0, finalState.getRegister(1));   // Cleared
        assertEquals(42, finalState.getRegister(2));  // Loaded from memory
    }
}
