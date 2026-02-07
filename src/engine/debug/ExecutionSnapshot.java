package engine.debug;

import engine.execution.ProcessorState;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable snapshot of processor state and memory at a point in execution
 * 
 * Used for debugging - allows stepping backward, examining state, etc.
 */
public class ExecutionSnapshot {
    private final ProcessorState state;
    private final byte[] memorySnapshot;  // Full memory copy TODO turn this into more memory efficient map
    private final LocalDateTime timestamp;
    private final String description;
    
    /**
     * Creates a snapshot
     * 
     * @param state the processor state
     * @param memory the complete memory contents (will be copied)
     * @param description human-readable description
     */
    public ExecutionSnapshot(ProcessorState state, byte[] memory, String description) {
        this.state = state;
        this.memorySnapshot = memory.clone();  // Deep copy
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }
    
    public ProcessorState getState() {
        return state;
    }
    
    public byte[] getMemorySnapshot() {
        return memorySnapshot.clone();  // Return copy to maintain immutability
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets a formatted timestamp for display
     */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
    
    /**
     * Gets a compact display string
     */
    public String getDisplayString() {
        return String.format("[%s] PC=0x%04X, Inst=%d - %s",
            getFormattedTimestamp(),
            state.getPC(),
            state.getInstructionCount(),
            description);
    }
    
    @Override
    public String toString() {
        return getDisplayString();
    }
}