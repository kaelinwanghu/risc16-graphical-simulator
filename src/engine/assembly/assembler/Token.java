package engine.assembly.assembler;

/**
 * Represents a tokenized line of assembly code.
 * 
 * After preprocessing (comment removal, label extraction), each non-blank line
 * becomes a Token with:
 * - Optional label (if line had "label: ...")
 * - Operation (add, movi, .fill, etc.)
 * - Operands (registers, immediates, labels)
 * - Line number and original source (for error reporting)
 * 
 * Tokens are immutable
 */
public final class Token {
    private final int lineNumber;
    private final String label;          // null if no label
    private final String operation;      // Instruction, pseudo-instruction, or directive
    private final String[] operands;
    private final String originalLine;
    
    /**
     * Creates a token
     * 
     * @param lineNumber the source line number (1-based)
     * @param label the label, or null if none
     * @param operation the operation (instruction/pseudo/directive)
     * @param operands the operand strings
     * @param originalLine the original source line (for error messages)
     */
    public Token(int lineNumber, String label, String operation, String[] operands, String originalLine) {
        this.lineNumber = lineNumber;
        this.label = label;
        this.operation = operation;
        this.operands = operands;
        this.originalLine = originalLine;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean hasLabel() {
        return label != null;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String[] getOperands() {
        return operands;
    }
    
    public String getOriginalLine() {
        return originalLine;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (hasLabel()) {
            sb.append(label).append(": ");
        }
        sb.append(operation);
        if (operands.length > 0) {
            sb.append(" ");
            sb.append(String.join(", ", operands));
        }
        return sb.toString();
    }
}