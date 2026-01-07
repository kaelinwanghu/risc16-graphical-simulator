package engine.execution;

/**
 * Exception thrown when instruction execution fails
 * 
 * This can occur due to:
 * - Invalid memory access
 * - Arithmetic errors (though RiSC-16 has none)
 * - Attempting to execute data
 * - PC pointing to invalid address
 * - Instruction limit exceeded
 */
public class ExecutionException extends Exception {
    private final int pc;
    private final String instruction;
    
    /**
     * Creates an execution exception
     * 
     * @param message the error message
     * @param pc the program counter where the error occurred
     */
    public ExecutionException(String message, int pc) {
        super(String.format("Execution error at PC=0x%04X: %s", pc, message));
        this.pc = pc;
        this.instruction = null;
    }
    
    /**
     * Creates an execution exception with instruction context
     * 
     * @param message the error message
     * @param pc the program counter where the error occurred
     * @param instruction the instruction that caused the error
     */
    public ExecutionException(String message, int pc, String instruction) {
        super(String.format("Execution error at PC=0x%04X (%s): %s", pc, instruction, message));
        this.pc = pc;
        this.instruction = instruction;
    }
    
    /**
     * Creates an execution exception with a cause
     * 
     * @param message the error message
     * @param pc the program counter where the error occurred
     * @param cause the underlying cause
     */
    public ExecutionException(String message, int pc, Throwable cause) {
        super(String.format("Execution error at PC=0x%04X: %s", pc, message), cause);
        this.pc = pc;
        this.instruction = null;
    }
    
    public int getPC() {
        return pc;
    }
    
    public String getInstruction() {
        return instruction;
    }
}