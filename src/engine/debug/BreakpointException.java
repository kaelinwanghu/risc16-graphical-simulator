package engine.debug;

import engine.execution.ProcessorState;

/**
 * Exception thrown when a breakpoint is hit during execution
 * 
 * This is NOT an error - it's a control flow mechanism to pause execution
 * at a breakpoint without unwinding the state.
 */
public class BreakpointException extends Exception {
    private final ProcessorState state;
    private final int sourceLine;
    private final Breakpoint breakpoint;
    
    public BreakpointException(ProcessorState state, int sourceLine, Breakpoint breakpoint) {
        super(String.format("Breakpoint hit at line %d (PC=0x%04X)", sourceLine, state.getPC()));
        this.state = state;
        this.sourceLine = sourceLine;
        this.breakpoint = breakpoint;
    }
    
    public ProcessorState getState() {
        return state;
    }
    
    public int getSourceLine() {
        return sourceLine;
    }
    
    public Breakpoint getBreakpoint() {
        return breakpoint;
    }
    
    @Override
    public String toString() {
        if (breakpoint != null && breakpoint.isConditional()) {
            return String.format("Breakpoint hit at line %d: %s", sourceLine, breakpoint.getDescription());
        }
        return String.format("Breakpoint hit at line %d", sourceLine);
    }
}