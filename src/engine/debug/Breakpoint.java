package engine.debug;

/**
 * Represents a breakpoint with optional conditions
 */
public class Breakpoint {
    
    /**
     * Comparison operators for conditional breakpoints
     */
    public enum Operator {
        EQUALS("=="),
        NOT_EQUALS("!="),
        LESS_THAN("<"),
        LESS_EQUAL("<="),
        GREATER_THAN(">"),
        GREATER_EQUAL(">=");
        
        private final String symbol;
        
        Operator(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        @Override
        public String toString() {
            return symbol;
        }
    }
    
    /**
     * Type of value to watch in condition
     */
    public enum WatchType {
        REGISTER,       // Watch a register value
        MEMORY,         // Watch a memory location
        PC,             // Watch program counter
        INSTRUCTION_COUNT  // Watch instruction count
    }
    
    private final int lineNumber;
    private boolean enabled;
    
    // Conditional breakpoint fields
    private boolean isConditional;
    private WatchType watchType;
    private int watchTarget;     // Register number (0-7) or memory address
    private Operator operator;
    private int compareValue;
    
    /**
     * Creates an unconditional breakpoint
     */
    public Breakpoint(int lineNumber) {
        this.lineNumber = lineNumber;
        this.enabled = true;
        this.isConditional = false;
    }
    
    /**
     * Creates a conditional breakpoint
     */
    public Breakpoint(int lineNumber, WatchType watchType, int watchTarget, Operator operator, int compareValue) {
        this.lineNumber = lineNumber;
        this.enabled = true;
        this.isConditional = true;
        this.watchType = watchType;
        this.watchTarget = watchTarget;
        this.operator = operator;
        this.compareValue = compareValue;
    }
    
    // Getters
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isConditional() {
        return isConditional;
    }
    
    public WatchType getWatchType() {
        return watchType;
    }
    
    public int getWatchTarget() {
        return watchTarget;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public int getCompareValue() {
        return compareValue;
    }
    
    /**
     * Gets a human-readable description of the breakpoint
     */
    public String getDescription() {
        if (!isConditional) {
            return "Break at line " + lineNumber;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Break when ");
        
        switch (watchType) {
            case REGISTER:
                sb.append("R").append(watchTarget);
                break;
            case MEMORY:
                sb.append(String.format("Mem[0x%04X]", watchTarget));
                break;
            case PC:
                sb.append("PC");
                break;
            case INSTRUCTION_COUNT:
                sb.append("Instructions");
                break;
        }
        
        sb.append(" ").append(operator.getSymbol()).append(" ");
        sb.append(compareValue);
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("Breakpoint[line=%d, enabled=%s, %s]", 
            lineNumber, enabled, getDescription());
    }
}