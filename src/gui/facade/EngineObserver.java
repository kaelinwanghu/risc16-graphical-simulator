package gui.facade;

import engine.assembly.AssemblyResult;
import engine.execution.ExecutionException;
import engine.execution.ProcessorState;
import engine.execution.ExecutionResult;

/**
 * Observer interface for processor state changes.
 * 
 * GUI components implement this to react to:
 * - State changes (register/PC updates)
 * - Program loading
 * - Execution errors
 * - Halt
 */
public interface EngineObserver {
    
    /**
     * Called when processor state changes (after each instruction)
     * 
     * @param oldState the state before execution (may be null)
     * @param newState the state after execution
     * @param result the execution result of the instruction
     */
    void onStateChanged(ProcessorState oldState, ProcessorState newState, ExecutionResult result);
    
    /**
     * Called when a new program is successfully loaded
     * 
     * @param result the assembly result
     */
    void onProgramLoaded(AssemblyResult result);
    
    /**
     * Called when assembly fails
     * 
     * @param result the assembly result containing errors
     */
    void onAssemblyError(AssemblyResult result);

    /**
     * Called when execution encounters an error
     * 
     * @param error the execution exception
     */
    void onExecutionError(ExecutionException error);

    /**
     * Called when the processor halts
     */
    void onHalt();
}