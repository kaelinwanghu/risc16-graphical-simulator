package engine.assembly;

/**
 * Represents an error encountered during assembly.
 * 
 * This class provides rich error context including line number, column,
 * the problematic source text, and a descriptive error message. This allows
 * the GUI or CLI to present helpful error messages to the user.
 */
public class AssemblyError {
    
    /**
     * Categories of assembly errors
     */
    public enum ErrorType {
        SYNTAX_ERROR,           // Malformed instruction or directive
        INVALID_OPCODE,         // Unknown instruction mnemonic
        INVALID_OPERAND,        // Wrong operand type or count
        INVALID_REGISTER,       // Register number out of range or invalid name
        INVALID_IMMEDIATE,      // Immediate value out of range
        UNDEFINED_LABEL,        // Reference to undefined label
        DUPLICATE_LABEL,        // Label defined multiple times
        LABEL_SYNTAX_ERROR,     // Invalid label format
        OUT_OF_RANGE,           // Branch offset or immediate too large
        INVALID_DIRECTIVE,      // Unknown or malformed directive
        MEMORY_OVERFLOW,        // Program too large for memory
        EMPTY_PROGRAM           // No instructions provided
    }
    
    private final int lineNumber;        // line number of error
    private final int columnNumber;      // column number of error, or -1 if unknown
    private final String message;
    private final String sourceText;     // The line of source code that caused the error
    private final ErrorType errorType;
    
    /**
     * Creates a new assembly error
     * 
     * @param lineNumber the line number where the error occurred (1-based)
     * @param columnNumber the column number where error occurred, or -1 if unknown
     * @param message the error message
     * @param sourceText the source code line that caused the error
     * 
     * @param errorType the category of error
     */
    public AssemblyError(int lineNumber, int columnNumber, String message, String sourceText, ErrorType errorType) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.message = message;
        this.sourceText = sourceText;
        this.errorType = errorType;
    }
    
    /**
     * Creates an assembly error without column information
     */
    public AssemblyError(int lineNumber, String message, String sourceText, ErrorType errorType) {
        this(lineNumber, -1, message, sourceText, errorType);
    }
    
    // Getters
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public int getColumnNumber() {
        return columnNumber;
    }
    
    public boolean hasColumnNumber() {
        return columnNumber != -1;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getSourceText() {
        return sourceText;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Returns a formatted error message suitable for display
     * 
     * Example output:
     * Error on line 5: Invalid register name
     *   add r9, r2, r3
     *       ^^
     *   Register names must be r0-r7
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Error on line ").append(lineNumber);
        
        if (hasColumnNumber()) {
            sb.append(", column ").append(columnNumber);
        }
        
        sb.append(": ").append(message).append("\n");
        
        if (sourceText != null && !sourceText.trim().isEmpty()) {
            sb.append("  ").append(sourceText).append("\n");
            
            // Add caret indicator if we have column information
            if (hasColumnNumber()) {
                sb.append("  ");
                for (int i = 1; i < columnNumber; i++) {
                    sb.append(" ");
                }
                sb.append("^\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a compact single-line error message
     * 
     * Example: "Line 5: Invalid register name"
     */
    public String getCompactMessage() {
        return "Line " + lineNumber + ": " + message;
    }
    
    @Override
    public String toString() {
        return getFormattedMessage();
    }
    
    // Factory methods for common error types
    
    public static AssemblyError syntaxError(int line, String message, String source) {
        return new AssemblyError(line, message, source, ErrorType.SYNTAX_ERROR);
    }
    
    public static AssemblyError invalidOpcode(int line, String opcode, String source) {
        return new AssemblyError(line, "Invalid opcode: '" + opcode + "'", source, ErrorType.INVALID_OPCODE);
    }
    
    public static AssemblyError invalidRegister(int line, String register, String source) {
        return new AssemblyError(line, "Invalid register: '" + register + "'. Expected r0-r7", source, ErrorType.INVALID_REGISTER);
    }
    
    public static AssemblyError invalidImmediate(int line, String value, int min, int max, String source) {
        return new AssemblyError(line, "Immediate value " + value + " out of range [" + min + ", " + max + "]", source, ErrorType.INVALID_IMMEDIATE);
    }
    
    public static AssemblyError undefinedLabel(int line, String label, String source) {
        return new AssemblyError(line, "Undefined label: '" + label + "'", source, ErrorType.UNDEFINED_LABEL);
    }
    
    public static AssemblyError duplicateLabel(int line, String label, int firstDefinedLine, String source) {
        return new AssemblyError(line, "Label '" + label + "' already defined on line " + firstDefinedLine, source, ErrorType.DUPLICATE_LABEL);
    }
    
    public static AssemblyError wrongOperandCount(int line, String opcode, int expected, int actual, String source) {
        return new AssemblyError(line, "Instruction '" + opcode + "' expects " + expected + " operands, got " + actual, source, ErrorType.INVALID_OPERAND);
    }
    
    public static AssemblyError branchOutOfRange(int line, String label, int offset, String source) {
        return new AssemblyError(line, "Branch to label '" + label + "' is out of range (offset: " + offset + 
            ", max: ±63)", source, ErrorType.OUT_OF_RANGE);
    }
    
    public static AssemblyError emptyProgram() {
        return new AssemblyError(1, "Program contains no instructions", "", ErrorType.EMPTY_PROGRAM);
    }
}