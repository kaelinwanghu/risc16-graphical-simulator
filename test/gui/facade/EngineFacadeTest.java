package gui.facade;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.execution.ExecutionException;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the EngineFacade class
 *
 * EngineFacade wraps the processor engine and provides a clean API for the GUI,
 * managing the observer pattern for state change notifications.
 */
public class EngineFacadeTest {

    private EngineFacade facade;
    private MockObserver observer;

    @Before
    public void setUp() {
        facade = new EngineFacade(1024);  // 1KB memory
        observer = new MockObserver();
        facade.addObserver(observer);
    }

    // ========================================================================
    // Mock Observer for testing callbacks
    // ========================================================================

    private static class MockObserver implements EngineObserver {
        List<String> events = new ArrayList<>();
        List<ProcessorState> stateChanges = new ArrayList<>();
        List<AssemblyResult> programsLoaded = new ArrayList<>();
        List<AssemblyResult> assemblyErrors = new ArrayList<>();
        List<ExecutionException> executionErrors = new ArrayList<>();
        int haltCount = 0;

        @Override
        public void onStateChanged(ProcessorState oldState, ProcessorState newState) {
            events.add("stateChanged");
            stateChanges.add(newState);
        }

        @Override
        public void onProgramLoaded(AssemblyResult result) {
            events.add("programLoaded");
            programsLoaded.add(result);
        }

        @Override
        public void onAssemblyError(AssemblyResult result) {
            events.add("assemblyError");
            assemblyErrors.add(result);
        }

        @Override
        public void onExecutionError(ExecutionException error) {
            events.add("executionError");
            executionErrors.add(error);
        }

        @Override
        public void onHalt() {
            events.add("halt");
            haltCount++;
        }

        void clear() {
            events.clear();
            stateChanges.clear();
            programsLoaded.clear();
            assemblyErrors.clear();
            executionErrors.clear();
            haltCount = 0;
        }
    }

    // ========================================================================
    // Constructor and Initialization Tests
    // ========================================================================

    @Test
    public void testConstructor() {
        EngineFacade f = new EngineFacade(2048);

        assertNotNull(f.getMemory());
        assertEquals(2048, f.getMemory().getSize());
        assertNotNull(f.getState());
        assertFalse(f.isHalted());
        assertEquals(0, f.getPC());
    }

    @Test
    public void testInitialState() {
        ProcessorState state = facade.getState();

        assertNotNull(state);
        assertEquals(0, state.getPC());
        assertFalse(state.isHalted());
        assertEquals(0, state.getInstructionCount());

        // All registers should be 0
        for (int i = 0; i < 8; i++) {
            assertEquals(0, facade.getRegister(i));
        }
    }

    // ========================================================================
    // Assembly Tests - Success
    // ========================================================================

    @Test
    public void testAssembleSuccess() {
        // Use value within 6-bit signed range (-32 to 31) for ADDI immediate
        String source = "addi r1, r0, 31\njalr r0, r0";

        AssemblyResult result = facade.assemble(source);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getInstructionCount());

        // Observer should be notified
        assertTrue(observer.events.contains("programLoaded"));
        assertEquals(1, observer.programsLoaded.size());
    }

    @Test
    public void testAssembleStoresLastAssembly() {
        String source = "addi r1, r0, 10\njalr r0, r0";

        AssemblyResult result = facade.assemble(source);

        assertSame(result, facade.getLastAssembly());
    }

    @Test
    public void testAssembleLoadsProgram() {
        String source = "addi r1, r0, 5\njalr r0, r0";

        facade.assemble(source);

        // Program should be loaded into memory
        Memory memory = facade.getMemory();
        assertNotEquals(0, memory.readWord(0));  // First instruction

        // Metadata should be set
        ProgramMetadata metadata = facade.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.isInstruction(0));
    }

    // ========================================================================
    // Assembly Tests - Failure
    // ========================================================================

    @Test
    public void testAssembleFailure() {
        String source = "invalid r1, r2, r3";

        AssemblyResult result = facade.assemble(source);

        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());

        // Observer should be notified of error
        assertTrue(observer.events.contains("assemblyError"));
        assertEquals(1, observer.assemblyErrors.size());
        assertFalse(observer.events.contains("programLoaded"));
    }

    @Test
    public void testAssembleEmptyProgram() {
        String source = "";

        AssemblyResult result = facade.assemble(source);

        assertFalse(result.isSuccess());
        assertTrue(observer.events.contains("assemblyError"));
    }

    @Test
    public void testAssembleUndefinedLabel() {
        String source = "beq r0, r0, undefined";

        AssemblyResult result = facade.assemble(source);

        assertFalse(result.isSuccess());
        assertTrue(observer.events.contains("assemblyError"));
    }

    // ========================================================================
    // Execution Tests - Step
    // ========================================================================

    @Test
    public void testStep() throws ExecutionException {
        String source =
            "addi r1, r0, 10\n" +
            "addi r2, r0, 20\n" +
            "jalr r0, r0";

        facade.assemble(source);
        observer.clear();

        // Step once
        facade.step();

        // R1 should be set
        assertEquals(10, facade.getRegister(1));
        assertEquals(2, facade.getPC());

        // Observer should be notified
        assertTrue(observer.events.contains("stateChanged"));
        assertEquals(1, observer.stateChanges.size());
    }

    @Test
    public void testMultipleSteps() throws ExecutionException {
        String source =
            "addi r1, r0, 1\n" +
            "addi r2, r0, 2\n" +
            "addi r3, r0, 3\n" +
            "jalr r0, r0";

        facade.assemble(source);
        observer.clear();

        facade.step();
        assertEquals(1, facade.getRegister(1));

        facade.step();
        assertEquals(2, facade.getRegister(2));

        facade.step();
        assertEquals(3, facade.getRegister(3));

        assertEquals(3, observer.stateChanges.size());
    }

    @Test
    public void testStepToHalt() throws ExecutionException {
        String source = "jalr r0, r0";  // HALT immediately

        facade.assemble(source);
        observer.clear();

        facade.step();

        assertTrue(facade.isHalted());
        assertTrue(observer.events.contains("halt"));
        assertEquals(1, observer.haltCount);
    }

    @Test
    public void testStepWhenHalted() throws ExecutionException {
        String source = "jalr r0, r0";

        facade.assemble(source);
        facade.step();  // This halts

        assertTrue(facade.isHalted());

        // Stepping when halted should not throw, but processor stays halted
        // The underlying Processor.step() returns false when already halted
        facade.step();  // Should not throw

        // Processor should still be halted
        assertTrue(facade.isHalted());
    }

    // ========================================================================
    // Execution Tests - Run
    // ========================================================================

    @Test
    public void testRun() throws ExecutionException {
        String source =
            "addi r1, r0, 5\n" +
            "addi r2, r0, 10\n" +
            "add r3, r1, r2\n" +
            "jalr r0, r0";

        facade.assemble(source);
        observer.clear();

        facade.run();

        // Should have executed all instructions
        assertTrue(facade.isHalted());
        assertEquals(5, facade.getRegister(1));
        assertEquals(10, facade.getRegister(2));
        assertEquals(15, facade.getRegister(3));

        // Should have received state changes for each step plus halt
        assertTrue(observer.events.contains("halt"));
        assertEquals(4, observer.stateChanges.size());  // 4 instructions
    }

    @Test
    public void testRunWithLoop() throws ExecutionException {
        // Count down from 3 to 0
        String source =
            "        addi r1, r0, 3\n" +
            "loop:   beq r1, r0, done\n" +
            "        addi r1, r1, -1\n" +
            "        beq r0, r0, loop\n" +
            "done:   jalr r0, r0";

        facade.assemble(source);
        observer.clear();

        facade.run();

        assertTrue(facade.isHalted());
        assertEquals(0, facade.getRegister(1));
    }

    // ========================================================================
    // Reset and Clear Tests
    // ========================================================================

    @Test
    public void testReset() throws ExecutionException {
        // Use values within 6-bit signed range (-32 to 31) for ADDI immediate
        String source =
            "addi r1, r0, 20\n" +
            "addi r2, r0, 30\n" +
            "jalr r0, r0";

        AssemblyResult result = facade.assemble(source);
        assertTrue("Assembly should succeed", result.isSuccess());

        facade.step();
        facade.step();

        // Verify state changed
        assertEquals(20, facade.getRegister(1));
        assertEquals(30, facade.getRegister(2));
        assertEquals(4, facade.getPC());

        observer.clear();

        // Reset
        facade.reset();

        // State should be reset
        assertEquals(0, facade.getPC());
        assertFalse(facade.isHalted());

        // Registers should be cleared
        assertEquals(0, facade.getRegister(1));
        assertEquals(0, facade.getRegister(2));

        // Observer should be notified
        assertTrue(observer.events.contains("stateChanged"));
    }

    @Test
    public void testResetPreservesProgram() throws ExecutionException {
        // Use value within 6-bit signed range (-32 to 31) for ADDI immediate
        String source = "addi r1, r0, 28\njalr r0, r0";

        facade.assemble(source);
        facade.run();
        facade.reset();

        // Program should still be in memory - can run again
        facade.run();
        assertEquals(28, facade.getRegister(1));
    }

    @Test
    public void testClear() throws ExecutionException {
        // Use value within 6-bit signed range (-32 to 31) for ADDI immediate
        String source = "addi r1, r0, 18\njalr r0, r0";

        facade.assemble(source);
        facade.step();

        observer.clear();

        // Clear
        facade.clear();

        // State should be reset
        assertEquals(0, facade.getPC());
        assertEquals(0, facade.getRegister(1));
        assertFalse(facade.isHalted());

        // Last assembly should be cleared
        assertNull(facade.getLastAssembly());

        // Observer should be notified
        assertTrue(observer.events.contains("stateChanged"));
    }

    // ========================================================================
    // State Query Tests
    // ========================================================================

    @Test
    public void testGetState() throws ExecutionException {
        // Use value within 6-bit signed range (-32 to 31) for ADDI immediate
        String source = "addi r1, r0, 25\njalr r0, r0";

        AssemblyResult result = facade.assemble(source);
        assertTrue("Assembly should succeed", result.isSuccess());

        facade.step();

        ProcessorState state = facade.getState();

        assertNotNull(state);
        assertEquals(25, state.getRegister(1));
        assertEquals(2, state.getPC());
        assertEquals(1, state.getInstructionCount());
    }

    @Test
    public void testGetRegister() throws ExecutionException {
        String source =
            "addi r1, r0, 1\n" +
            "addi r2, r0, 2\n" +
            "addi r3, r0, 3\n" +
            "addi r4, r0, 4\n" +
            "addi r5, r0, 5\n" +
            "addi r6, r0, 6\n" +
            "addi r7, r0, 7\n" +
            "jalr r0, r0";

        facade.assemble(source);
        facade.run();

        assertEquals(0, facade.getRegister(0));  // R0 always 0
        assertEquals(1, facade.getRegister(1));
        assertEquals(2, facade.getRegister(2));
        assertEquals(3, facade.getRegister(3));
        assertEquals(4, facade.getRegister(4));
        assertEquals(5, facade.getRegister(5));
        assertEquals(6, facade.getRegister(6));
        assertEquals(7, facade.getRegister(7));
    }

    @Test
    public void testGetPC() throws ExecutionException {
        String source =
            "addi r1, r0, 1\n" +
            "addi r2, r0, 2\n" +
            "jalr r0, r0";

        facade.assemble(source);

        assertEquals(0, facade.getPC());
        facade.step();
        assertEquals(2, facade.getPC());
        facade.step();
        assertEquals(4, facade.getPC());
    }

    @Test
    public void testIsHalted() throws ExecutionException {
        String source = "addi r1, r0, 1\njalr r0, r0";

        facade.assemble(source);

        assertFalse(facade.isHalted());
        facade.step();
        assertFalse(facade.isHalted());
        facade.step();
        assertTrue(facade.isHalted());
    }

    @Test
    public void testGetMemory() {
        Memory memory = facade.getMemory();

        assertNotNull(memory);
        assertEquals(1024, memory.getSize());
    }

    @Test
    public void testGetProcessor() {
        assertNotNull(facade.getProcessor());
    }

    // ========================================================================
    // Observer Management Tests
    // ========================================================================

    @Test
    public void testAddObserver() {
        MockObserver observer2 = new MockObserver();
        facade.addObserver(observer2);

        facade.assemble("addi r1, r0, 1\njalr r0, r0");

        // Both observers should be notified
        assertTrue(observer.events.contains("programLoaded"));
        assertTrue(observer2.events.contains("programLoaded"));
    }

    @Test
    public void testRemoveObserver() {
        facade.removeObserver(observer);

        facade.assemble("addi r1, r0, 1\njalr r0, r0");

        // Observer should NOT be notified
        assertTrue(observer.events.isEmpty());
    }

    @Test
    public void testMultipleObservers() throws ExecutionException {
        MockObserver observer2 = new MockObserver();
        MockObserver observer3 = new MockObserver();

        facade.addObserver(observer2);
        facade.addObserver(observer3);

        facade.assemble("addi r1, r0, 1\njalr r0, r0");
        facade.step();

        // All three should receive events
        assertEquals(observer.events.size(), observer2.events.size());
        assertEquals(observer.events.size(), observer3.events.size());
    }

    // ========================================================================
    // Instruction Limit Tests
    // ========================================================================

    @Test
    public void testSetInstructionLimit() {
        facade.setInstructionLimit(100);
        assertEquals(100, facade.getInstructionLimit());
    }

    @Test
    public void testDefaultInstructionLimit() {
        // Default should be 65535
        assertEquals(65535, facade.getInstructionLimit());
    }

    @Test
    public void testInstructionLimitEnforced() {
        // Infinite loop
        String source =
            "loop:   addi r1, r1, 1\n" +
            "        beq r0, r0, loop";

        facade.assemble(source);
        facade.setInstructionLimit(10);
        observer.clear();

        try {
            facade.run();
            fail("Should have thrown ExecutionException for instruction limit");
        } catch (ExecutionException e) {
            assertTrue(e.getMessage().contains("limit"));
            assertTrue(observer.events.contains("executionError"));
        }
    }

    // ========================================================================
    // Execution Error Tests
    // ========================================================================

    @Test
    public void testExecutionErrorNotifiesObserver() {
        // Execute without loading a program - should error
        try {
            facade.step();
            // Might not throw depending on implementation
        } catch (ExecutionException e) {
            assertTrue(observer.events.contains("executionError"));
            assertEquals(1, observer.executionErrors.size());
        }
    }

    // ========================================================================
    // Event Order Tests
    // ========================================================================

    @Test
    public void testEventOrderOnSuccessfulAssembly() {
        facade.assemble("addi r1, r0, 1\njalr r0, r0");

        // Should receive programLoaded
        assertEquals("programLoaded", observer.events.get(0));
    }

    @Test
    public void testEventOrderOnFailedAssembly() {
        facade.assemble("invalid instruction");

        // Should receive assemblyError
        assertEquals("assemblyError", observer.events.get(0));
    }

    @Test
    public void testEventOrderOnExecution() throws ExecutionException {
        facade.assemble("jalr r0, r0");  // Single halt instruction
        observer.clear();

        facade.step();

        // Should receive stateChanged then halt
        assertTrue(observer.events.indexOf("stateChanged") <
                   observer.events.indexOf("halt"));
    }

    // ========================================================================
    // Integration-style Tests
    // ========================================================================

    @Test
    public void testFullWorkflow() throws ExecutionException {
        // Simulate typical GUI workflow

        // 1. Assemble
        String source =
            "        addi r1, r0, 5\n" +
            "        addi r2, r0, 3\n" +
            "        add r3, r1, r2\n" +
            "        jalr r0, r0";

        AssemblyResult result = facade.assemble(source);
        assertTrue(result.isSuccess());
        assertTrue(observer.events.contains("programLoaded"));

        observer.clear();

        // 2. Step through
        facade.step();
        assertEquals(5, facade.getRegister(1));

        facade.step();
        assertEquals(3, facade.getRegister(2));

        facade.step();
        assertEquals(8, facade.getRegister(3));

        // 3. Run to completion
        facade.step();
        assertTrue(facade.isHalted());
        assertTrue(observer.events.contains("halt"));

        // 4. Reset and run again
        observer.clear();
        facade.reset();
        assertEquals(0, facade.getPC());
        assertFalse(facade.isHalted());

        facade.run();
        assertEquals(8, facade.getRegister(3));
        assertTrue(facade.isHalted());

        // 5. Clear
        facade.clear();
        assertNull(facade.getLastAssembly());
        assertEquals(0, facade.getRegister(3));
    }

    @Test
    public void testReassemblyWorkflow() throws ExecutionException {
        // Use values within 6-bit signed range (-32 to 31) for ADDI immediate
        // Assemble first program
        AssemblyResult result1 = facade.assemble("addi r1, r0, 15\njalr r0, r0");
        assertTrue("First assembly should succeed", result1.isSuccess());

        facade.run();
        assertEquals(15, facade.getRegister(1));
        assertTrue(facade.isHalted());

        observer.clear();

        // Assemble second program (should replace first and reset state)
        AssemblyResult result2 = facade.assemble("addi r1, r0, 25\njalr r0, r0");
        assertTrue("Second assembly should succeed", result2.isSuccess());
        assertTrue(observer.events.contains("programLoaded"));

        // After new assembly, processor should be ready to run
        assertFalse(facade.isHalted());
        assertEquals(0, facade.getPC());

        // Run new program
        facade.run();
        assertEquals(25, facade.getRegister(1));
    }
}
