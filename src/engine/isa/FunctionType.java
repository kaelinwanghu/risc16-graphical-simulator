package engine.isa;

/**
 * Categories of instruction functionality for scheduling and pipeline analysis
 * 
 * These categories are used by the out-of-order execution scheduler to determine
 * which functional units can execute which instructions, and to track dependencies
 * between instructions.
 */
public enum FunctionType {
    /**
     * Generic ALU operations (NAND)
     */
    ALU,
    
    /**
     * Addition operations (ADD, ADDI)
     */
    ADD,
    
    /**
     * Load operations (LW, LUI)
     * Accesses memory or moves immediate values
     */
    LOAD,
    
    /**
     * Store operations (SW)
     * Writes to memory
     */
    STORE,
    
    /**
     * Conditional branch operations (BEQ)
     * May alter control flow
     */
    BRANCH,
        
    /**
     * Jump and link operations (JALR)
     * Unconditional control transfer with return address saving
     */
    JUMP_AND_LINK;
    
    /**
     * Returns true if this is a memory access operation
     */
    public boolean isMemoryOperation() {
        return this == LOAD || this == STORE;
    }
    
    /**
     * Returns true if this is a control flow operation
     */
    public boolean isControlFlow() {
        return this == BRANCH || this == JUMP_AND_LINK;
    }
    
    /**
     * Returns true if this operation writes to a register
     */
    public boolean writesRegister() {
        return this != STORE && this != BRANCH;
    }
}