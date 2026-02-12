package engine.assembly.assembler;

import engine.assembly.AssemblyError;

/**
 * Handles memory directives (.fill, .space)
 * 
 * Directives:
 * - .fill value - Initialize one word with a number or label
 * - .space count - Reserve N words of zeros
 * 
 * This is very extensible - new directives can be added easily
 */
public final class DirectiveHandler {
    
    // Prevent instantiation
    private DirectiveHandler() {}
    
    /**
     * Checks if an operation is a directive
     */
    public static boolean isDirective(String operation) {
        return operation.equals(".fill") || operation.equals(".space");
    }
    
    /**
     * Handles a directive token (number or label)
     * 
     * @param token the directive token
     * @param context the assembly context
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    public static void handleDirective(Token token, AssemblyContext context) {
        String operation = token.getOperation();
        
        switch (operation) {
            case ".fill":
                handleFill(token, context);
                break;
            case ".space":
                handleSpace(token, context);
                break;
            default:
                throw new AssemblyException(token.getLineNumber(), "Unknown directive: " + operation, token.getOriginalLine(), AssemblyError.ErrorType.INVALID_DIRECTIVE);
        }
    }
    
    /**
     * Handles .fill directive - initializes one word
     * 
     * .fill value (can be number or label)
     * 
     * @param token the directive token
     * @param context the assembly context
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    private static void handleFill(Token token, AssemblyContext context) {
        String[] operands = token.getOperands();
        
        if (operands.length != 1) {
            throw new AssemblyException(token.getLineNumber(), ".fill requires exactly 1 operand, got " + operands.length, token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }
        
        String value = operands[0];
        Integer number = NumberParser.parse(value);
        
        if (number != null) {
            // Number - store directly
            context.addDataSegment(context.getCurrentAddress(), number.shortValue());
        } else {
            // Label - create placeholder and unresolved reference
            int dataIndex = context.getDataSegments().size();
            context.addDataSegment(context.getCurrentAddress(), (short) 0);  // Placeholder
            
            context.addUnresolvedReference(UnresolvedReference.data(value, dataIndex, token.getLineNumber(), token.getOriginalLine()));
        }
    }
    
    /**
     * Handles .space directive - reserves N words of zeros
     * 
     * .space count (must be a positive number)
     */
    private static void handleSpace(Token token, AssemblyContext context) {
        String[] operands = token.getOperands();
        
        if (operands.length != 1) {
            throw new AssemblyException(token.getLineNumber(), ".space requires exactly 1 operand, got " + operands.length, token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }
        
        Integer count = NumberParser.parse(operands[0]);
        if (count == null) {
            throw new AssemblyException(token.getLineNumber(), ".space count must be a number, got: " + operands[0], token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }
        
        if (count < 1) {
            throw new AssemblyException(token.getLineNumber(), ".space count must be positive, got: " + count, token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }
        
        // Reserve N words of zeros
        for (int i = 0; i < count; i++) {
            context.addDataSegment(context.getCurrentAddress(), (short) 0);
        }
    }
}