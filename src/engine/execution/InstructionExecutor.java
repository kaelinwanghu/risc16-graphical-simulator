package engine.execution;

import engine.isa.InstructionFormat;
import engine.memory.Memory;

/**
 * Interface for executing a single RiSC-16 instruction.
 * 
 * Each instruction type (ADD, ADDI, LW, SW, etc.) has its own implementation
 * of this interface, replacing the old reflection-based dispatch with
 * a clean, type-safe approach
 * 
 * The execution model is functional:
 * - Takes current state and instruction as input
 * - Returns new state and execution result
 * - Does not mutate the input state
 * - Memory is mutated (since it represents physical RAM)
 */
public interface InstructionExecutor {
    
    /**
     * Executes an instruction
     * 
     * @param instruction the decoded instruction to execute
     * @param state the current processor state
     * @param memory the memory to read from / write to
     * 
     * @return the execution result containing updated state and metadata
     * 
     * @throws ExecutionException if execution fails
     */
    ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException;

    /**
     * Container for execution results
     */
    class ExecutionContext {
        private final ProcessorState newState;
        private final ExecutionResult result;
        
        public ExecutionContext(ProcessorState newState, ExecutionResult result) {
            this.newState = newState;
            this.result = result;
        }
        
        public ProcessorState getNewState() {
            return newState;
        }
        
        public ExecutionResult getResult() {
            return result;
        }
    }
}