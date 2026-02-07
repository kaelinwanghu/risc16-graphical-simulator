package engine;

import engine.assembly.AssemblyResult;
import engine.execution.*;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

/**
 * The RiSC-16 processor
 * 
 * This class is the main entry point for program execution. It:
 * - Manages processor state (registers, PC, etc.)
 * - Loads programs into memory
 * - Executes instructions via ExecutionEngine
 * - Provides query interface for current state
 * 
 * This is a redesign that has:
 * - No reflection
 * - No Object[] returns
 * - Clear separation of concerns
 * - Immutable state snapshots
 * - Type-safe throughout
 */
public class Processor {
    private final Memory memory;
    private final ExecutionEngine executionEngine;
    private ProgramMetadata metadata;
    private ProcessorState currentState;
    
    /**
     * Creates a processor with specified memory size
     * 
     * @param memorySize the size of memory in bytes (must be power of 2)
     */
    public Processor(int memorySize) {
        this.memory = new Memory(memorySize);
        this.executionEngine = new ExecutionEngine(memory, null);
        this.currentState = ProcessorState.builder().build();
        this.metadata = null;
    }
    
    /**
     * Creates a processor with default memory size (1024 bytes)
     */
    public Processor() {
        this(1024);
    }
    
    /**
     * Loads a program into memory
     * 
     * This clears memory, loads the program, and resets processor state.
     * 
     * @param assemblyResult the assembled program to load
     */
    public void loadProgram(AssemblyResult assemblyResult) {
        if (assemblyResult == null) {
            throw new IllegalArgumentException("Assembly result cannot be null");
        }
        
        if (!assemblyResult.isSuccess()) {
            throw new IllegalArgumentException("Cannot load program with errors");
        }
        
        // Load program into memory
        ProgramLoader loader = new ProgramLoader(memory);
        this.metadata = loader.load(assemblyResult);
		executionEngine.setMetadata(metadata);
        
        // Reset processor state to entry point
        this.currentState = ProcessorState.builder()
            .setPC(metadata.getEntryPoint())
            .build();
        
    }
    
    /**
     * Executes a single instruction
     * 
     * @return true if execution should continue, false if halted
     * @throws ExecutionException if execution fails
     */
    public boolean step() throws ExecutionException {
        if (currentState.isHalted()) {
            return false;
        }
        
        InstructionExecutor.ExecutionContext context = executionEngine.step(currentState);
        currentState = context.getNewState();
        
        return !currentState.isHalted();
    }

    /**
     * Executes a single instruction and returns the full context
     * 
     * @return the execution context containing new state and result
     * @throws ExecutionException if execution fails
     */
    public InstructionExecutor.ExecutionContext stepWithContext() throws ExecutionException {
        if (currentState.isHalted()) {
            return null;
        }
        
        InstructionExecutor.ExecutionContext context = executionEngine.step(currentState);
        currentState = context.getNewState();
        
        return context;
    }
    
    /**
     * Executes instructions until halt or error
     * 
     * @throws ExecutionException if execution fails
     */
    public void run() throws ExecutionException {
        while (!currentState.isHalted()) {
            step();
        }
    }
    
    /**
     * Resets the processor to initial state
     */
    public void reset() {
        int entryPoint = (metadata != null) ? metadata.getEntryPoint() : 0;
        this.currentState = ProcessorState.builder().setPC(entryPoint).build();
    }
    
    /**
     * Clears memory and resets processor
     */
    public void clear() {
        memory.clear();
        this.metadata = null;
        this.currentState = ProcessorState.builder().build();
    }
    
    // Query methods for current state
    
    public ProcessorState getState() {
        return currentState;
    }
    
    public short getRegister(int regNum) {
        return currentState.getRegister(regNum);
    }
    
    public short[] getRegisters() {
        return currentState.getRegisters();
    }
    
    public int getPC() {
        return currentState.getPC();
    }
    
    public boolean isHalted() {
        return currentState.isHalted();
    }
        
    public long getInstructionCount() {
        return currentState.getInstructionCount();
    }
    
    public Memory getMemory() {
        return memory;
    }
    
    public ProgramMetadata getMetadata() {
        return metadata; // May be null if no program is loaded
    }
    
    public void setInstructionLimit(int limit) {
        executionEngine.setInstructionLimit(limit);
    }
    
    public int getInstructionLimit() {
        return executionEngine.getInstructionLimit();
    }
    
    public int getMemorySize() {
        return memory.getSize();
    }

    public void restoreState(ProcessorState state) {
        this.currentState = state;
    }
}