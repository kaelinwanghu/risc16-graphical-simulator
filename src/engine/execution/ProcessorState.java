package engine.execution;

/**
 * Immutable snapshot of processor state at a point in time.
 * 
 * This class represents the complete architectural state of the RiSC-16 processor:
 * - 8 general-purpose registers (R0-R7)
 * - Program counter (PC)
 * - Execution status (running, halted)
 * - Cycle count
 * 
 * Immutable snapshots are useful for:
 * - Debugging (examining state at breakpoints)
 * - Testing (verifying state transitions)
 * - GUI updates (displaying current state)
 * - Rollback (restoring previous state)
 */
public class ProcessorState {
    private final short[] registers;
    private final int pc;
    private final boolean halted;
    private final long cycleCount;
    private final long instructionCount;
    
    /**
     * Creates a processor state snapshot
     * 
     * @param registers array of 8 register values (will be copied)
     * @param pc the program counter
     * @param halted whether the processor is halted
     * @param cycleCount the total number of cycles executed
     * @param instructionCount the total number of instructions executed
     */
    public ProcessorState(short[] registers, int pc, boolean halted, long cycleCount, long instructionCount) {
        if (registers == null || registers.length != 8) {
            throw new IllegalArgumentException("Must provide exactly 8 registers");
        }
        
        // Deep copy to ensure immutability
        this.registers = registers.clone();
        this.pc = pc;
        this.halted = halted;
        this.cycleCount = cycleCount;
        this.instructionCount = instructionCount;
    }
    
    public short getRegister(int regNum) {
        if (regNum < 0 || regNum > 7) {
            throw new IllegalArgumentException("Register must be 0-7, got: " + regNum);
        }
        return registers[regNum];
    }
    
    /**
     * Gets a copy of all register values
     * 
     * @return array of 8 register values
     */
    public short[] getRegisters() {
        return registers.clone();
    }
    
    public int getPC() {
        return pc;
    }
    
    public boolean isHalted() {
        return halted;
    }
    
    public long getCycleCount() {
        return cycleCount;
    }
    
    public long getInstructionCount() {
        return instructionCount;
    }
    
    /**
     * Creates a builder initialized with this state
     */
    public Builder toBuilder() {
        return new Builder(this);
    }
    
    /**
     * Creates a new builder for constructing processor states
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating processor states
     */
    public static class Builder {
        private short[] registers;
        private int pc;
        private boolean halted;
        private long cycleCount;
        private long instructionCount;
        
        public Builder() {
            this.registers = new short[8];
            this.pc = 0;
            this.halted = false;
            this.cycleCount = 0;
            this.instructionCount = 0;
        }
        
        public Builder(ProcessorState state) {
            this.registers = state.registers.clone();
            this.pc = state.pc;
            this.halted = state.halted;
            this.cycleCount = state.cycleCount;
            this.instructionCount = state.instructionCount;
        }
        
        public Builder setRegister(int regNum, short value) {
            if (regNum < 0 || regNum > 7) {
                throw new IllegalArgumentException("Register must be 0-7");
            }
            // R0 is always 0 (hardware enforced)
            if (regNum != 0) {
                registers[regNum] = value;
            }
            return this;
        }
        
        public Builder setPC(int pc) {
            this.pc = pc;
            return this;
        }
        
        public Builder setHalted(boolean halted) {
            this.halted = halted;
            return this;
        }
        
        public Builder setCycleCount(long count) {
            this.cycleCount = count;
            return this;
        }
        
        public Builder setInstructionCount(long count) {
            this.instructionCount = count;
            return this;
        }
        
        public Builder incrementPC(int offset) {
            this.pc += offset;
            return this;
        }
        
        public Builder incrementCycles(long cycles) {
            this.cycleCount += cycles;
            return this;
        }
        
        public Builder incrementInstructions() {
            this.instructionCount++;
            return this;
        }
        
        public ProcessorState build() {
            return new ProcessorState(registers, pc, halted, cycleCount, instructionCount);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProcessorState[\n");
        sb.append("  PC=0x").append(Integer.toHexString(pc)).append("\n");
        sb.append("  Registers: ");
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("R%d=0x%04X ", i, registers[i] & 0xFFFF));
        }
        sb.append("\n");
        sb.append("  Cycles=").append(cycleCount).append("\n");
        sb.append("  Instructions=").append(instructionCount).append("\n");
        sb.append("  Halted=").append(halted).append("\n");
        sb.append("]");
        return sb.toString();
    }
}