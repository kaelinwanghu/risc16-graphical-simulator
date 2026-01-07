package engine.execution;

import engine.isa.FunctionType;

/**
 * Immutable result of executing a single instruction.
 * 
 * This class replaces the old Object[] return type from instruction execution,
 * providing type safety and clear semantics for what each instruction produces.
 * 
 * Different instruction types populate different fields:
 * - ALU operations: functionType, destinationReg
 * - Load operations: functionType, destinationReg, memoryAddress, cycleCount
 * - Store operations: functionType, memoryAddress, cycleCount
 * - Branch operations: functionType, branchTaken, branchTarget
 * - Jump operations: functionType, destinationReg (for link), branchTarget
 */
public final class ExecutionResult {
    private final FunctionType functionType;
    private final int destinationReg;    // -1 if no destination register
    private final int memoryAddress;     // -1 if no memory access
    private final int cycleCount;        // Additional cycles beyond base (for cache misses)
    private final boolean branchTaken;
    private final int branchTarget;      // -1 if not a branch/jump
    
    private ExecutionResult(Builder builder) {
        this.functionType = builder.functionType;
        this.destinationReg = builder.destinationReg;
        this.memoryAddress = builder.memoryAddress;
        this.cycleCount = builder.cycleCount;
        this.branchTaken = builder.branchTaken;
        this.branchTarget = builder.branchTarget;
    }
    
    // Getters
    
    public FunctionType getFunctionType() {
        return functionType;
    }
    
    public int getDestinationReg() {
        return destinationReg;
    }
    
    public boolean hasDestinationReg() {
        return destinationReg != -1;
    }
    
    public int getMemoryAddress() {
        return memoryAddress;
    }
    
    public boolean hasMemoryAccess() {
        return memoryAddress != -1;
    }
    
    public int getCycleCount() {
        return cycleCount;
    }
    
    public boolean isBranchTaken() {
        return branchTaken;
    }
    
    public int getBranchTarget() {
        return branchTarget;
    }
    
    public boolean isBranchOrJump() {
        return branchTarget != -1;
    }
    
    // Builder pattern for flexible construction
    
    public static Builder builder(FunctionType functionType) {
        return new Builder(functionType);
    }
    
    public static class Builder {
        private final FunctionType functionType;
        private int destinationReg = -1;
        private int memoryAddress = -1;
        private int cycleCount = 0;
        private boolean branchTaken = false;
        private int branchTarget = -1;
        
        private Builder(FunctionType functionType) {
            if (functionType == null) {
                throw new IllegalArgumentException("Function type cannot be null");
            }
            this.functionType = functionType;
        }
        
        public Builder destinationReg(int reg) {
            if (reg < -1 || reg > 7) {
                throw new IllegalArgumentException("Invalid register: " + reg);
            }
            this.destinationReg = reg;
            return this;
        }
        
        public Builder memoryAddress(int address) {
            this.memoryAddress = address;
            return this;
        }
        
        public Builder cycleCount(int cycles) {
            if (cycles < 0) {
                throw new IllegalArgumentException("Cycle count cannot be negative");
            }
            this.cycleCount = cycles;
            return this;
        }
        
        public Builder branchTaken(boolean taken) {
            this.branchTaken = taken;
            return this;
        }
        
        public Builder branchTarget(int target) {
            this.branchTarget = target;
            return this;
        }
        
        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
    
    // Factory methods for common cases
    
    /**
     * Creates a result for ALU operations (ADD, NAND, etc.)
     */
    public static ExecutionResult alu(int destinationReg) {
        return builder(FunctionType.ALU).destinationReg(destinationReg).build();
    }
    
    /**
     * Creates a result for ADD operations
     */
    public static ExecutionResult add(int destinationReg) {
        return builder(FunctionType.ADD).destinationReg(destinationReg).build();
    }
    
    /**
     * Creates a result for LOAD operations
     */
    public static ExecutionResult load(int destinationReg, int memoryAddress, int cycleCount) {
        return builder(FunctionType.LOAD).destinationReg(destinationReg).memoryAddress(memoryAddress).cycleCount(cycleCount).build();
    }
    
    /**
     * Creates a result for STORE operations
     */
    public static ExecutionResult store(int memoryAddress, int cycleCount) {
        return builder(FunctionType.STORE).memoryAddress(memoryAddress).cycleCount(cycleCount).build();
    }
    
    /**
     * Creates a result for BRANCH operations
     */
    public static ExecutionResult branch(boolean taken, int target) {
        return builder(FunctionType.BRANCH).branchTaken(taken).branchTarget(target).build();
    }
    
    /**
     * Creates a result for JUMP_AND_LINK operations
     */
    public static ExecutionResult jumpAndLink(int linkReg, int target) {
        return builder(FunctionType.JUMP_AND_LINK).destinationReg(linkReg).branchTarget(target).build();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExecutionResult[").append(functionType);
        
        if (hasDestinationReg()) {
            sb.append(", dest=R").append(destinationReg);
        }
        if (hasMemoryAccess()) {
            sb.append(", mem=0x").append(Integer.toHexString(memoryAddress));
        }
        if (cycleCount > 0) {
            sb.append(", cycles=").append(cycleCount);
        }
        if (isBranchOrJump()) {
            sb.append(", target=0x").append(Integer.toHexString(branchTarget));
            if (functionType == FunctionType.BRANCH) {
                sb.append(", taken=").append(branchTaken);
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
}